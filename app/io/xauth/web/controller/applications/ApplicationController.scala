package io.xauth.web.controller.applications

import io.xauth.service.applications.ApplicationService
import io.xauth.service.auth.model.AuthRole.User
import io.xauth.web.action.auth.JwtAuthenticationAction
import io.xauth.web.action.auth.JwtAuthenticationAction.{roleAction, userAction}
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
  jwtAuthAction: JwtAuthenticationAction,
  applicationService: ApplicationService,
  cc: ControllerComponents
)
(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // user authenticated composed action
  private val authAction = jwtAuthAction andThen userAction andThen roleAction(User)

  def findAll: Action[AnyContent] = authAction.async {
    // finding system applications
    applicationService.findAll map { l =>
      Ok(obj("applications" -> l))
    }
  }

}