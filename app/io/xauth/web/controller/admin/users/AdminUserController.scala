package io.xauth.web.controller.admin.users

import io.xauth.model.ContactType.Email
import io.xauth.model.pagination.{OffsetSpecs, PagedData, Pagination}
import io.xauth.service.auth.AuthUserService
import io.xauth.service.auth.model.AuthRole.{Admin, System}
import io.xauth.service.auth.model.AuthStatus.{Blocked, Disabled, Enabled}
import io.xauth.service.auth.model.AuthUser
import io.xauth.service.workspace.model.Workspace
import io.xauth.web.action.auth.AuthenticationManager
import io.xauth.web.controller.admin.users.model.PagedUserRes._
import io.xauth.web.controller.admin.users.model.UserRes._
import io.xauth.web.controller.admin.users.model._
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
  auth: AuthenticationManager,
  jsonSchema: JsonSchemaLoader,
  authUserService: AuthUserService,
  cc: ControllerComponents
)
(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // admin authenticated composed action
  private val adminAction = auth.RoleAction(Admin)

  /**
    * Creates new user and sends it the activation code.
    */
  def create: Action[JsValue] = adminAction.async(parse.json) { request =>
    // json schema validation
    jsonSchema.AdminUsersPost.validateObj[UserReq](request.body) match {
      case s: JsSuccess[UserReq] =>
        implicit val workspace: Workspace = request.workspace

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
            case Some(_) => successful(BadRequest(obj("message" -> "username already registered")))
            case _ => authUserService.save(username, usr.password, usr.description, None, usr.userInfo) flatMap {
              u => successful(Created(toJson(u.toResource)))
            }
          }
        }

      // json schema validation has been failed
      case e: JsError => successful(BadRequest(JsError.toJson(e)))
    }
  }

  def search: Action[JsValue] = adminAction.async(parse.json) { r =>
    // json schema validation
    jsonSchema.AdminUserSearch.validateObj[UserSearchReq](r.body) match {
      case s: JsSuccess[UserSearchReq] =>
        implicit val workspace: Workspace = r.workspace
        authUserService.findByUsername(s.value.username).map {
          case Some(u) => Ok(Json.toJson(u.toResource))
          case _ => NotFound
        }
      // json schema validation has been failed
      case e: JsError => successful(BadRequest(JsError.toJson(e)))
    }
  }

  /**
    * Finds and retrieves paged results of all users in workspace.
    */
  def findAll: Action[AnyContent] = adminAction.async { request =>
    implicit val workspace: Workspace = request.workspace
    implicit val pagination: Pagination = Pagination.fromRequest(request)(OffsetSpecs.MongoOffsetSpec)

    authUserService.findAll map {
      case p: PagedData[AuthUser] => Ok(Json.toJson(p.toResource))
      case _ => InternalServerError
    }
  }

  /**
    * Finds the user by id.
    */
  def find(id: Uuid): Action[AnyContent] = adminAction.async { request =>
    implicit val workspace: Workspace = request.workspace
    authUserService.findById(id).map {
      case Some(u) => Ok(Json.toJson(u.toResource))
      case _ => NotFound
    }
  }

  /**
    * Deletes the user by id.
    */
  def delete(id: Uuid): Action[AnyContent] = adminAction.async { request =>
    implicit val workspace: Workspace = request.workspace
    authUserService.findById(id).map {
      case Some(_) =>
        authUserService.delete(id)
        NoContent
      case _ => NotFound
    }
  }

  def unblock(id: Uuid): Action[AnyContent] = adminAction.async { request =>
    implicit val workspace: Workspace = request.workspace
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

  def patchRoles(id: Uuid): Action[JsValue] = adminAction.async(parse.json) { request =>
    // validating by json schema
    jsonSchema.AdminUserRolesPatch.validateObj[UserRoles](request.body) match {
      // json schema validation has been succeeded
      case s: JsSuccess[UserRoles] =>
        implicit val workspace: Workspace = request.workspace

        if (s.value.roles.contains(System) && !request.authUser.roles.contains(System)) {
          successful(BadRequest(Json.toJson("message" -> "permissions too low to assign a system role")))
        }
        // saving new roles collection
        else authUserService.updateRoles(id, s.value.roles: _*) map {
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
          implicit val workspace: Workspace = request.workspace

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

  def patchApplications(id: Uuid): Action[JsValue] = adminAction.async(parse.json) { r =>
    // validating by json schema
    jsonSchema.AdminUserApplicationsPatch.validateObj[UserApplications](r.body) match {
      // json schema validation has been succeeded
      case s: JsSuccess[UserApplications] =>
        implicit val workspace: Workspace = r.workspace

        val workspaceApps = workspace.configuration.applications

        // verifying with workspace applications
        if (s.value.applications.forall(a => workspaceApps.contains(a.name))) {
          // saving new user status
          authUserService.updateApplications(id, s.value.applications: _*) map {
            case Some(uu) => Ok(toJson(UserApplications(uu.applications)))
            case _ => NotFound
          }
        }
        else successful(BadRequest(Json.obj("message" -> s"allowed applications are: ${workspaceApps.mkString("[", ", ", "]")}")))

      // json schema validation has been failed
      case JsError(e) => successful(BadRequest(JsError.toJson(e)))
    }
  }

  def accountTrust: Action[JsValue] = adminAction.async(parse.json) { request =>
    // validating by json schema
    jsonSchema.AdminAccountTrustPostReq.validateObj[AccountTrustReq](request.body) match {
      // json schema validation has been succeeded
      case s: JsSuccess[AccountTrustReq] =>
        implicit val workspace: Workspace = request.workspace
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
