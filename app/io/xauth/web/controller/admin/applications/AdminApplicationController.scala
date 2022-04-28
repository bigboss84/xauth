package io.xauth.web.controller.admin.applications

import io.xauth.JsonSchemaLoader
import io.xauth.service.auth.model.AuthRole.Admin
import io.xauth.service.workspace.WorkspaceService
import io.xauth.web.action.auth.AuthenticationManager
import io.xauth.web.controller.admin.applications.model.WorkspaceApplications
import play.api.libs.json.Json.{obj, toJson}
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful

/**
  * Handles all administrative actions for system applications.
  */
@Singleton
class AdminApplicationController @Inject()
(
  auth: AuthenticationManager,
  jsonSchema: JsonSchemaLoader,
  workspaceService: WorkspaceService,
  cc: ControllerComponents
)
(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // admin authenticated composed action
  private val adminAction = auth.RoleAction(Admin)

  def findAll: Action[AnyContent] = adminAction.async { r =>
    // finding workspace applications
    successful(Ok(obj("applications" -> r.workspace.configuration.applications)))
  }

  def patch: Action[JsValue] = adminAction.async(parse.json) { r =>
    // validating by json schema
    jsonSchema.AdminSystemApplicationsPatch.validateObj[WorkspaceApplications](r.body) match {
      // json schema validation has been succeeded
      case s: JsSuccess[WorkspaceApplications] =>

        val w = r.workspace
        val c = w.configuration.copy(applications = s.value.applications)

        // saving new roles collection
        workspaceService.update(w.copy(configuration = c)) map {
          case Some(o) => Ok(toJson(o.configuration.applications))
          case None => NotFound
        }

      // json schema validation has been failed
      case JsError(e) => successful(BadRequest(JsError.toJson(e)))
    }
  }
}