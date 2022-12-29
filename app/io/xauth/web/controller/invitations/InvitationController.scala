package io.xauth.web.controller.invitations

import io.xauth.model.ContactType.Email
import io.xauth.model.Permission
import io.xauth.service.auth.model.AuthRole.{HelpDeskOperator, HumanResource, Responsible}
import io.xauth.service.auth.model.{AuthCode, AuthCodeType}
import io.xauth.service.auth.{AuthCodeService, AuthUserService}
import io.xauth.service.invitation.InvitationService
import io.xauth.service.invitation.model.Invitation
import io.xauth.service.invitation.model.Invitation.generateCode
import io.xauth.service.workspace.model.Workspace
import io.xauth.web.action.auth.AuthenticationManager
import io.xauth.web.controller.invitations.model.InvitationResource
import io.xauth.web.controller.invitations.model.InvitationResource._
import io.xauth.{JsonSchemaLoader, Uuid}
import play.api.Logger
import play.api.libs.json.Json.{obj, toJson}
import play.api.libs.json._
import play.api.mvc._

import java.time.Duration
import java.util.Date
import javax.inject._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful
import scala.util.{Failure, Success, Try}

/**
  * Controller that exposes registration invitations routes.
  */
@Singleton
class InvitationController @Inject()
(
  cc: ControllerComponents,
  auth: AuthenticationManager,
  authService: AuthUserService,
  authCodeService: AuthCodeService,
  invitationService: InvitationService,
  jsonSchema: JsonSchemaLoader
)
(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val logger: Logger = Logger(this.getClass)

  // human resource authenticated composed action
  private val hrAction = auth.RoleAction(HumanResource, HelpDeskOperator, Responsible)

  /**
    * Creates new registration invitation
    */
  def create: Action[JsValue] = hrAction.async(parse.json) { request =>
    // validating by json schema
    jsonSchema.InvitationsPost.validateObj[InvitationResource](request.body) match {
      // json schema validation has been succeeded
      case s: JsSuccess[InvitationResource] =>
        val inv = s.value

        val appNames = inv.applications.map(_.name)
        val workspaceApps = request.workspace.configuration.applications

        // all supplied applications are recognized
        if ((appNames diff workspaceApps) isEmpty) {
          val ownApps = request.authUser.applications.filter(_.permissions.contains(Permission.Owner)).map(_.name)
          val apps = inv.applications.map(_.name) diff ownApps

          if (ownApps.forall(workspaceApps.contains)) {
            // verifying that all application are allowed
            if (!request.authUser.roles.contains(HelpDeskOperator) && apps.nonEmpty) {
              if (ownApps.nonEmpty) successful(BadRequest(obj("message" -> s"allowed applications are: ${ownApps.mkString(",")}")))
              else successful(BadRequest(obj("message" -> "unable to assign applications")))
            }

            // logic validation
            else inv.userInfo.contacts.find(_.`type` == Email).map(_.value) match {
              case Some(email) =>

                implicit val workspace: Workspace = request.workspace

                // checking for existing user by email
                authService.findByUsername(email).map {
                  case Some(_) => successful(
                    BadRequest(obj("message" -> s"an existing user already corresponds to email '$email'"))
                  )
                  case _ =>
                    invitationService.findByEmail(email).map {
                      case Some(_) => successful(BadRequest(obj("message" -> s"an existing invitation already corresponds to email '$email'")))
                      case _ =>

                        val newInv = Invitation(
                          description = inv.description,
                          applications = inv.applications,
                          userInfo = inv.userInfo,
                          validFrom = inv.validFrom,
                          validTo = inv.validTo,
                        )

                        // creating new invitation
                        invitationService.create(newInv) transformWith {
                          case Success(i) => successful(Created(Json.toJson(i.toResource)))
                          case Failure(_) => successful(BadRequest(obj("message" -> s"No user found for email '$email'")))
                        }
                    }.flatten
                }.flatten

              case _ => successful(
                BadRequest(obj("message" -> "at least one contact of type 'EMAIL' is required"))
              )
            }

          }
          else successful(BadRequest(obj("message" -> s"allowed applications are: ${workspaceApps.mkString(",")}")))
        }
        else successful(BadRequest(obj("message" -> s"allowed applications are: ${workspaceApps.mkString(",")}")))

      // json schema validation has been failed
      case JsError(e) => successful(BadRequest(JsError.toJson(e)))
    }
  }

  /**
    * Retrieves the registration invitation by `q` parameter in query string.
    */
  def findAll: Action[AnyContent] = hrAction.async { request =>
    implicit val workspace: Workspace = request.workspace

    Try(request.queryString("q")) match {
      case Success(v) if v.size == 1 && v.head.nonEmpty =>
        invitationService.findByEmail(v.head) flatMap {
          case Some(i) => successful(Ok(toJson(i)))
          case _ => authCodeService.find(v.head) flatMap {
            case Some(c) => invitationService.find(c.referenceId) map {
              case Some(i) => Ok(toJson(i.toResource))
              case _ => NotFound
            }
            case _ => successful(NotFound(obj("message" -> "invitation code not found")))
          }
        }
      case Failure(_) => successful(BadRequest(obj("message" -> "no valid invitation code in 'q' parameter")))
    }
  }

  /**
    * Retrieves the registration invitation.
    */
  def find(id: Uuid): Action[AnyContent] = hrAction.async { request =>
    implicit val workspace: Workspace = request.workspace
    invitationService.find(id).map {
      case Some(i) => Ok(toJson(i.toResource))
      case _ => NotFound
    }
  }

  /**
    * Deletes the registration invitation.
    */
  def delete(id: Uuid): Action[AnyContent] = hrAction.async { request =>
    implicit val workspace: Workspace = request.workspace
    invitationService.delete(id).map {
      if (_) NoContent else NotFound
    }
  }

  /**
    * Generates invitation code.
    */
  def createCode(id: Uuid): Action[JsValue] = hrAction.async(parse.json) { request =>

    import play.api.libs.json.Writes._

    def code = (c: String, d: Date) => obj(
      "invitationCode" -> c, "expiresAt" -> dateWrites(iso8601DateFormat).writes(d)
    )

    // validating by json schema
    jsonSchema.InvitationsCodePost.validate(request.body) match {
      // json schema validation has been succeeded
      case _: JsSuccess[JsValue] =>
        implicit val workspace: Workspace = request.workspace

        // checking for code existence
        authCodeService.find(id, AuthCodeType.Invitation).map {

          // invitation code has been already generated
          case Some(_) => successful {
            BadRequest(obj("message" -> "An invitation code already exists for this item"))
          }

          // invitation code not found
          case _ =>
            invitationService.find(id).map {
              case Some(i) =>
                val contact = i.userInfo.contacts.headOption

                // invitation code validity
                val validity = i.validTo match {
                  case Some(d) => Right(d)
                  case _ => Left(Duration.ofDays(7))
                }

                // generating new invitation code
                authCodeService.save(AuthCodeType.Invitation, generateCode, id, contact, validity).map {
                  case c: AuthCode =>
                    logger.info(s"registration code has been created")
                    successful(Created(code(c.code, c.expiresAt)))
                  case _ => successful(InternalServerError)
                }.flatten

              case _ => successful(NotFound)
            }.flatten
        }.flatten

      // json schema validation has been failed
      case JsError(e) => successful {
        BadRequest(JsError.toJson(e))
      }
    }
  }

}
