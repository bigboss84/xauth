package io.xauth.web.controller.schema

import io.xauth.{JsonSchema, JsonSchemaLoader}
import javax.inject.{Inject, Singleton}
import play.api.mvc._

/**
  * Controller that exposes json schemas.
  */
@Singleton
class JsonSchemaController @Inject()
(
  cc: ControllerComponents, jsonSchema: JsonSchemaLoader
) extends AbstractController(cc) {

  /**
    * Retrieves the requested json schema.
    */
  def find(path: String) = Action { implicit request =>
    // todo: replace all $ref
    jsonSchema.values.find(_.resourcePath.endsWith(path)) match {
      case s: Some[JsonSchema] => Ok(s.get.jsValue)
      case None => NotFound
    }
  }

}