package io.xauth.web.controller.admin.roles

import io.xauth.JsonSchemaLoader
import io.xauth.service.auth.AuthUserService
import io.xauth.service.auth.model.AuthRole.Admin
import io.xauth.web.action.auth.AuthenticationManager
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful

/**
  * Handles all administrative actions for authentication roles.
  */
@Singleton
class RoleController @Inject()
(
  auth: AuthenticationManager,
  jsonSchema: JsonSchemaLoader,
  authUserService: AuthUserService,
  cc: ControllerComponents
)
(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // admin authenticated composed action
  private val adminAction = auth.RoleAction(Admin)

  def create: Action[JsValue] = adminAction.async(parse.json) { request =>
    // json schema validation
    jsonSchema.AdminRolePost.validate(request.body) match {
      case s: JsSuccess[JsValue] =>
        val usr = s.value
        ???
      // json schema validation has been failed
      case e: JsError => successful(BadRequest(JsError.toJson(e)))
    }
  }

}