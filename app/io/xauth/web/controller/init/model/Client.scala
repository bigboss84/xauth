package io.xauth.web.controller.init.model

/**
  * Initial client for http-basic authentication.
  */
case class Client(id: String, secret: String)

object Client {
  import play.api.libs.json._

  implicit val reads: Reads[Client] = Json.reads[Client]
  implicit val writes: Writes[Client] = Json.writes[Client]
}