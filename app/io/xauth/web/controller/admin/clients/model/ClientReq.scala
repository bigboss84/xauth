package io.xauth.web.controller.admin.clients.model

/**
  * Client creation request.
  */
case class ClientReq(id: String, secret: String)

object ClientReq {

  import play.api.libs.json._

  implicit val reads: Reads[ClientReq] = Json.reads[ClientReq]
  implicit val writes: Writes[ClientReq] = Json.writes[ClientReq]
}