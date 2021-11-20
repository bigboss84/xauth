package io.xauth.web.controller.init.model

/**
  * Application initialization request.
  */
case class InitReq(client: Client, admin: Admin)

object InitReq {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json._

  implicit val reads: Reads[InitReq] = (
    (__ \ "client").read[Client]
      and (__ \ "admin").read[Admin]
    ) (InitReq.apply _)

  implicit val write: Writes[InitReq] = (
    (__ \ "client").write[Client]
      and (__ \ "admin").write[Admin]
    ) (unlift(InitReq.unapply))
}