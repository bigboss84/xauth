package io.xauth.web.controller.applications

import io.xauth.web.action.auth.AuthenticationManager
import play.api.libs.json.Json.obj
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful

/**
  * Handles public actions for workspace applications.
  */
@Singleton
class ApplicationController @Inject()
(
  auth: AuthenticationManager,
  cc: ControllerComponents
)
(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def findAll: Action[AnyContent] = auth.WorkspaceAction.async { r =>
    // finding workspace applications
    successful(Ok(obj("applications" -> r.workspace.configuration.applications)))
  }

}