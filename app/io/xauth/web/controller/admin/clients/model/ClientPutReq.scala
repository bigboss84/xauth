package io.xauth.web.controller.admin.clients.model

import io.xauth.model.DataFormat

import java.util.Date

/**
  * Client update request.
  */
case class ClientPutReq(id: String, secret: String, registeredAt: Date, updatedAt: Date)

object ClientPutReq extends DataFormat {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[ClientPutReq] = (
    (__ \ "id").read[String]
      and (__ \ "secret").read[String]
      and (__ \ "registeredAt").read[Date]
      and (__ \ "updatedAt").read[Date]
    ) (ClientPutReq.apply _)

  implicit val write: Writes[ClientPutReq] = (
    (__ \ "id").write[String]
      and (__ \ "secret").write[String]
      and (__ \ "registeredAt").write(dateWrites(iso8601DateFormat))
      and (__ \ "updatedAt").write(dateWrites(iso8601DateFormat))
    ) (unlift(ClientPutReq.unapply))
}