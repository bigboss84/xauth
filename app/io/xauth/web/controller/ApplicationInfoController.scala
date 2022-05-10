package io.xauth.web.controller

import io.xauth.generated.ApplicationInfo._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

/**
  * Controller that exposes public application and build information.
  */
@Singleton
class ApplicationInfoController @Inject()
(cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def info: Action[AnyContent] = Action { _ =>
    Ok(Json.obj(
      "name" -> Name,
      "version" -> Version,
      "builtAt" -> BuiltAt
    ))
  }
}
