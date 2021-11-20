package io.xauth.web.controller.admin.users

import io.xauth.model.ContactType.Email
import io.xauth.service.auth.AuthUserService
import io.xauth.service.auth.model.AuthRole.Admin
import io.xauth.service.auth.model.AuthStatus.{Blocked, Disabled, Enabled}
import io.xauth.web.action.auth.JwtAuthenticationAction
import io.xauth.web.action.auth.JwtAuthenticationAction.{roleAction, userAction}
import io.xauth.web.controller.admin.users.model.UserRes._
import io.xauth.web.controller.admin.users.model.{AccountTrustReq, UserReq, UserRoles, UserStatus}
import io.xauth.{JsonSchemaLoader, Uuid}
import play.api.libs.json.Json.{obj, toJson}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

/**
  * Handles all administrative actions for users.
  */
@Singleton
class AdminUserController @Inject()
(
  jwtAuthAction: JwtAuthenticationAction,
  jsonSchema: JsonSchemaLoader,
  authUserService: AuthUserService,
  cc: ControllerComponents
)
(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // admin authenticated composed action
  private val adminAction =
    jwtAuthAction andThen userAction andThen roleAction(Admin)

  def create: Action[JsValue] = adminAction.async(parse.json) { request =>
    // json schema validation
    jsonSchema.AdminUsersPost.validateObj[UserReq](request.body) match {
      case s: JsSuccess[UserReq] =>
        val usr = s.value

        // logic validation
        def valid(f: => Future[Result]): Future[Result] = {
          // password fields must be equal
          if (usr.password != usr.passwordCheck)
            successful(BadRequest(obj("message" -> "password fields are different")))

          // at least one email contact
          else if (!usr.userInfo.contacts.exists(_.`type` == Email))
            successful(BadRequest(obj("message" -> "email contact not found")))

          else f
        }

        valid {
          // using first email contact as username
          val username = usr.userInfo.contacts.find(_.`type` == Email).get.value

          // check username existence
          authUserService.findByUsername(username) flatMap {
            case Some(u) => successful(BadRequest(obj("message" -> "username already registered")))
            case _ => authUserService.save(username, usr.password, usr.description, usr.userInfo) flatMap {
              u => successful(Created(toJson(u.toResource)))
            }
          }
        }

      // json schema validation has been failed
      case e: JsError => successful(BadRequest(JsError.toJson(e)))
    }
  }

  def find(id: Uuid): Action[AnyContent] = adminAction.async { r =>
    authUserService.findById(id).map {
      case Some(u) => Ok(Json.toJson(u.toResource))
      case _ => NotFound
    }
  }

  def unblock(id: Uuid): Action[AnyContent] = adminAction.async {
    authUserService.findById(id) map {
      case Some(u) =>
        if (u.status == Blocked) {
          authUserService.updateStatusById(id, Enabled)
          Ok
        }
        else BadRequest
      case _ => NotFound
    }
  }

  def patchRoles(id: Uuid): Action[JsValue] = adminAction.async(parse.json) {
    request =>

      // validating by json schema
      jsonSchema.AdminUserRolesPatch.validateObj[UserRoles](request.body) match {
        // json schema validation has been succeeded
        case s: JsSuccess[UserRoles] =>

          val body = s.value

          // saving new roles collection
          authUserService.updateRoles(id, s.value.roles: _*) map {
            case Some(u) => Ok(toJson(UserRoles(u.roles)))
            case _ => NotFound
          }

        // json schema validation has been failed
        case JsError(e) => successful(BadRequest(JsError.toJson(e)))
      }
  }

  def patchStatus(id: Uuid): Action[JsValue] = adminAction.async(parse.json) {
    request =>

      // validating by json schema
      jsonSchema.AdminUserStatusPatch.validateObj[UserStatus](request.body) match {
        // json schema validation has been succeeded
        case s: JsSuccess[UserStatus] =>

          val body = s.value

          // saving new user status
          authUserService.updateStatusById(id, body.status) map {
            case Some(u) => Ok(toJson(UserStatus(u.status)))
            case _ => NotFound
          }

        // json schema validation has been failed
        case JsError(e) => successful(BadRequest(JsError.toJson(e)))
      }
  }

  def accountTrust: Action[JsValue] = adminAction.async(parse.json) {
    request =>
      // validating by json schema
      jsonSchema.AdminAccountTrustPostReq.validateObj[AccountTrustReq](request.body) match {
        // json schema validation has been succeeded
        case s: JsSuccess[AccountTrustReq] =>

          val body = s.value

          authUserService.findByUsername(body.username) flatMap {
            case Some(u) if u.status == Disabled =>
              authUserService.trustAccount(u)
              successful(Accepted)
            case Some(u) if u.status != Disabled =>
              successful(BadRequest(obj("message" -> s"user status is '${u.status}'")))
            case _ =>
              successful(NotFound)
          }

        // json schema validation has been failed
        case JsError(e) => successful(BadRequest(JsError.toJson(e)))
      }
  }

}
