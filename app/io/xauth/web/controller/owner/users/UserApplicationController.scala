package io.xauth.web.controller.owner.users

import io.xauth.model.Permission.Owner
import io.xauth.service.applications.ApplicationService
import io.xauth.service.auth.AuthUserService
import io.xauth.service.auth.model.AuthRole.{Admin, HelpDeskOperator, Responsible}
import io.xauth.service.workspace.model.Workspace
import io.xauth.web.action.auth.AuthenticationManager
import io.xauth.web.controller.owner.users.model.UserApplications
import io.xauth.{JsonSchemaLoader, Uuid}
import play.api.libs.json.Json.{obj, toJson}
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful

/**
  * Handles all administrative actions for users by owner.
  */
@Singleton
class UserApplicationController @Inject()
(
  auth: AuthenticationManager,
  jsonSchema: JsonSchemaLoader,
  authUserService: AuthUserService,
  applicationService: ApplicationService,
  cc: ControllerComponents
)
(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // admin authenticated composed action
  private val ownerAction = auth.RoleAction(Admin, HelpDeskOperator, Responsible)

  def patchApplications(id: Uuid): Action[JsValue] = ownerAction.async(parse.json) { request =>

    // validating by json schema
    jsonSchema.OwnerUserApplicationsPatch.validateObj[UserApplications](request.body) match {
      // json schema validation has been succeeded
      case s: JsSuccess[UserApplications] =>
        implicit val workspace: Workspace = request.workspace

        val body = s.value
        val appNames = body.applications.map(_.name)

        // verifying existence of system application
        applicationService.findAll flatMap { systemApps =>

          // all supplied applications are recognized
          if ((appNames diff systemApps) isEmpty) {
            val ownApps = request.authUser.applications.filter(_.permissions.contains(Owner)).map(_.name)
            val apps = appNames diff ownApps

            if (ownApps.forall(systemApps.contains)) {

              // verifying that all application are allowed
              if (request.authUser.roles.intersect(Admin :: HelpDeskOperator :: Nil).isEmpty && apps.nonEmpty) {
                if (ownApps.nonEmpty) successful(BadRequest(obj("message" -> s"allowed applications are: ${ownApps.mkString(",")}")))
                else successful(BadRequest(obj("message" -> "unable to assign applications")))
              }

              // saving new roles collection
              else authUserService.updateApplications(id, s.value.applications: _*) map {
                case Some(u) => Ok(toJson(UserApplications(u.applications)))
                case _ => NotFound
              }
            }

            else successful(BadRequest(obj("message" -> s"allowed applications are: ${appNames.mkString(",")}")))
          }

          // one or more specified applications cannot be assigned because
          // they are not registered as system applications
          else successful(BadRequest(obj("message" -> s"allowed applications are: ${appNames.mkString(",")}")))
        }

      // json schema validation has been failed
      case JsError(e) => successful(BadRequest(JsError.toJson(e)))
    }
  }
}