package io.xauth.web.controller.admin.clients.model

import java.util.Date

import io.xauth.model.DataFormat
import io.xauth.service.auth.model.AuthClient

/**
  * Client creation response.
  */
case class ClientRes(id: String, secret: String, registeredAt: Date, updatedAt: Date)

object ClientRes extends DataFormat {

  implicit class AuthClientResourceConverter(c: AuthClient) {
    def toResource: ClientRes = {
      ClientRes(
        id = c.id,
        secret = c.secret,
        registeredAt = c.registeredAt,
        updatedAt = c.updatedAt
      )
    }
  }

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[ClientRes] = (
    (__ \ "id").read[String]
      and (__ \ "secret").read[String]
      and (__ \ "registeredAt").read[Date]
      and (__ \ "updatedAt").read[Date]
    ) (ClientRes.apply _)

  implicit val write: Writes[ClientRes] = (
    (__ \ "id").write[String]
      and (__ \ "secret").write[String]
      and (__ \ "registeredAt").write(dateWrites(iso8601DateFormat))
      and (__ \ "updatedAt").write(dateWrites(iso8601DateFormat))
    ) (unlift(ClientRes.unapply))
}