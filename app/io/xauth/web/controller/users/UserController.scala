package io.xauth.web.controller.users

import io.xauth.model.ContactType.Email
import io.xauth.service.auth.model.AuthCodeType
import io.xauth.service.auth.model.AuthRole.{Admin, HelpDeskOperator, Responsible, User}
import io.xauth.service.auth.model.AuthStatus.Disabled
import io.xauth.service.auth.{AuthCodeService, AuthUserService}
import io.xauth.service.invitation.InvitationService
import io.xauth.service.workspace.model.Workspace
import io.xauth.web.action.auth.AuthenticationManager
import io.xauth.web.action.auth.model.{UserRequest, WorkspaceRequest}
import io.xauth.web.controller.users.model.UserReq
import io.xauth.web.controller.users.model.UserRes._
import io.xauth.{JsonSchemaLoader, Uuid}
import play.api.libs.json.Json.obj
import play.api.libs.json._
import play.api.mvc._

import javax.inject._
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

/**
  * Controller that exposes user routes.
  */
@Singleton
class UserController @Inject
(
  cc: ControllerComponents,
  auth: AuthenticationManager,
  authService: AuthUserService,
  authCodeService: AuthCodeService,
  jsonSchema: JsonSchemaLoader,
  invitationService: InvitationService
)
(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // admin authenticated composed action
  private val adminAction = auth.RoleAction(Admin)

  private val helpDeskAction = auth.RoleAction(Admin, HelpDeskOperator, Responsible)

  /**
    * Creates new user
    */
  def create: Action[JsValue] = auth.WorkspaceAction.async(parse.json) { request =>
    // json schema validation
    jsonSchema.UsersPost.validateObj[UserReq](request.body) match {
      case s: JsSuccess[UserReq] =>
        implicit val workspace: Workspace =
          request.asInstanceOf[WorkspaceRequest[_]].workspace

        val usr = s.value

        // logic validation
        def valid(f: => Future[Result]): Future[Result] = {
          // password fields must be equal
          if (usr.password != usr.passwordCheck)
            successful(BadRequest(obj("message" -> "password fields are different")))

          // at least one email contact
          else if (!usr.userInfo.contacts.exists(_.`type` == Email))
            successful(BadRequest(obj("message" -> "email contact not found")))

          // privacy flag must be true
          else if (!usr.privacy)
            successful(BadRequest(obj("message" -> "privacy not confirmed")))

          else f
        }

        valid {
          // using first email contact as username
          val username = usr.userInfo.contacts.find(_.`type` == Email).get.value

          // check username existence
          authService.findByUsername(username) flatMap {
            case Some(_) => successful(BadRequest(obj("message" -> "username already registered")))
            case _ => usr.invitationCode match {
              // registration by invitation id
              case Some(invitationCode) =>

                // todo: validation based on pre-registered user info

                authCodeService.find(invitationCode, AuthCodeType.Invitation) flatMap {
                  // invitation from reference id
                  case Some(authCode) => invitationService.find(authCode.referenceId) flatMap {
                    // creating new user from invitation
                    case Some(i) =>
                      val userInfo = i.userInfo.copy(
                        contacts = usr.userInfo.contacts
                      )
                      authService.save(username, usr.password, usr.description, Some(i.registeredBy), userInfo, Disabled, i.applications, User) flatMap {
                        u =>
                          // deleting invitation code
                          authCodeService.delete(authCode.code)
                          // deleting invitation
                          i.id.foreach(invitationService.delete)
                          // returning created user
                          successful(Created(Json.toJson(u.toResource)))
                      }
                    // invitation found
                    case None => successful(BadRequest(obj("message" -> "invitation not found")))
                  }
                  // invitation code not found
                  case None => successful(BadRequest(obj("message" -> "invitation code not found")))
                }

              // autonomous registration
              // todo: specific validation
              case None => ???
            }
          }
        }

      // json schema validation has been failed
      case e: JsError => successful(BadRequest(JsError.toJson(e)))
    }
  }

  /**
    * Retrieves the user.
    */
  def find(id: Uuid): Action[AnyContent] = helpDeskAction.async { request =>
    implicit val workspace: Workspace =
      request.asInstanceOf[UserRequest[_]].workspace

    authService.findById(id).map {
      case Some(u) => Ok(Json.toJson(u.toResource))
      case None => NotFound
    }
  }

  /**
    * Deletes the user.
    */
  def delete(id: Uuid): Action[AnyContent] = adminAction.async { r =>
    val request = r.asInstanceOf[UserRequest[_]]
    implicit val workspace: Workspace = request.workspace
    authService.delete(id).map { b =>
      if (b) NoContent else NotFound
    }
  }

}
