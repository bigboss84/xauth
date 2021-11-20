package io.xauth.web.controller.admin.applications

import io.xauth.JsonSchemaLoader
import io.xauth.service.applications.ApplicationService
import io.xauth.service.auth.model.AuthRole.Admin
import io.xauth.web.action.auth.JwtAuthenticationAction
import io.xauth.web.action.auth.JwtAuthenticationAction.{roleAction, userAction}
import io.xauth.web.controller.admin.applications.model.SystemApplications
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json.{obj, toJson}
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful

/**
  * Handles all administrative actions for system applications.
  */
@Singleton
class AdminApplicationController @Inject()
(
  jwtAuthAction: JwtAuthenticationAction,
  jsonSchema: JsonSchemaLoader,
  applicationService: ApplicationService,
  cc: ControllerComponents
)
(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // admin authenticated composed action
  private val adminAction =
    jwtAuthAction andThen userAction andThen roleAction(Admin)

  def findAll: Action[AnyContent] = adminAction.async {
    // finding system applications
    applicationService.findAll map { l =>
      Ok(obj("applications" -> l))
    }
  }

  def patch: Action[JsValue] = adminAction.async(parse.json) {
    request =>

      // validating by json schema
      jsonSchema.AdminSystemApplicationsPatch.validateObj[SystemApplications](request.body) match {
        // json schema validation has been succeeded
        case s: JsSuccess[SystemApplications] =>

          // saving new roles collection
          applicationService.save(s.value.applications) map {
            l => Ok(toJson(l))
          }

        // json schema validation has been failed
        case JsError(e) => successful(BadRequest(JsError.toJson(e)))
      }
  }
}