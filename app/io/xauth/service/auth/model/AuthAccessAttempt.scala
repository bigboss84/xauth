package io.xauth.service.auth.model

import java.util.Date

import io.xauth.model.DataFormat
import reactivemongo.bson.{BSONDocumentHandler, Macros}

case class AuthAccessAttempt
(
  username: String,
  clientId: String,
  remoteAddress: String,
  registeredAt: Date
)

object AuthAccessAttempt extends DataFormat {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[AuthAccessAttempt] = (
    (__ \ "username").read[String]
      and (__ \ "clientId").read[String]
      and (__ \ "remoteAddress").read[String]
      and (__ \ "registeredAt").read[Date]
    ) (AuthAccessAttempt.apply _)

  implicit val write: Writes[AuthAccessAttempt] = (
    (__ \ "username").write[String]
      and (__ \ "clientId").write[String]
      and (__ \ "remoteAddress").write[String]
      and (__ \ "registeredAt").write(dateWrites(iso8601DateFormat))
    ) (unlift(AuthAccessAttempt.unapply))

  implicit val bsonDocumentHandler: BSONDocumentHandler[AuthAccessAttempt] = Macros.handler[AuthAccessAttempt]
}