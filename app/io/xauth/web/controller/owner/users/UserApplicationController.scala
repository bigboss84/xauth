package io.xauth.web.controller.owner.users

import io.xauth.model.Permission.Owner
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
  cc: ControllerComponents
)
(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // admin authenticated composed action
  private val ownerAction = auth.RoleAction(Admin, HelpDeskOperator, Responsible)

  @deprecated("implement entire crud to avoid other owners data loss")
  def patchApplications(id: Uuid): Action[JsValue] = ownerAction.async(parse.json) { request =>

    // validating by json schema
    jsonSchema.OwnerUserApplicationsPatch.validateObj[UserApplications](request.body) match {
      // json schema validation has been succeeded
      case s: JsSuccess[UserApplications] =>
        implicit val workspace: Workspace = request.workspace

        // ingoing application names
        val inAppNames = s.value.applications.map(_.name)
        // current workspace applications
        val workspaceApps = workspace.configuration.applications
        // current owner valid application names
        val ownAppsNames = request.authUser.applications
          .filter(_.permissions.contains(Owner))
          .filter(workspaceApps.contains)
          .map(_.name)

        // verifying existence of workspace application
        // all supplied applications are recognized
        if (inAppNames.forall(workspaceApps.contains)) {
          // verifying that all application are allowed
          if ((inAppNames diff ownAppsNames).nonEmpty) {
            if (ownAppsNames.nonEmpty) successful(BadRequest(obj("message" -> s"allowed applications are: ${ownAppsNames.mkString(",")}")))
            else successful(BadRequest(obj("message" -> "unable to assign applications")))
          }
          // all ingoing applications are valid
          else authUserService.updateApplications(id, s.value.applications: _*) map {
            case Some(u) => Ok(toJson(UserApplications(u.applications)))
            case _ => NotFound
          }
        }

        // one or more specified applications cannot be assigned because
        // they are not registered as workspace applications
        else successful(BadRequest(obj("message" -> s"allowed applications are: ${ownAppsNames.mkString(",")}")))

      // json schema validation has been failed
      case JsError(e) => successful(BadRequest(JsError.toJson(e)))
    }
  }
}