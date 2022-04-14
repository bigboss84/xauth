package io.xauth.web.controller.applications

import io.xauth.service.applications.ApplicationService
import io.xauth.service.auth.model.AuthRole.User
import io.xauth.web.action.auth.AuthenticationManager
import play.api.libs.json.Json.obj
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

/**
  * Handles public actions for system applications.
  */
@Singleton
class ApplicationController @Inject()
(
  auth: AuthenticationManager,
  applicationService: ApplicationService,
  cc: ControllerComponents
)
(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // user authenticated composed action
  private val userAction = auth.RoleAction(User)

  def findAll: Action[AnyContent] = userAction.async {
    // finding system applications
    applicationService.findAll map { l =>
      Ok(obj("applications" -> l))
    }
  }

}