package io.xauth.web.controller

import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

/**
  * Temporary test controller to manually trigger some actions.
  */
@Singleton
class TestController @Inject()
(
  cc: ControllerComponents
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def test = Action { request =>
    Ok
  }
}
