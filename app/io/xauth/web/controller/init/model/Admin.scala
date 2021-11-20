package io.xauth.web.controller.init.model

/**
  * Initial administrator.
  */
case class Admin(username: String, password: String)

object Admin {
  import play.api.libs.json._

  implicit val reads: Reads[Admin] = Json.reads[Admin]
  implicit val writes: Writes[Admin] = Json.writes[Admin]
}