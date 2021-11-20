package io.xauth.service.auth.model

import java.util.Date

import io.xauth.Uuid
import io.xauth.model.DataFormat
import reactivemongo.bson.Macros.Annotations.Key
import reactivemongo.bson.{BSONDocumentHandler, Macros}

case class AuthRefreshToken
(
  @Key("_id")
  token: String,
  clientId: String,
  userId: Uuid,
  expiresAt: Date,
  registeredAt: Date
)

object AuthRefreshToken extends DataFormat {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  implicit val reads: Reads[AuthRefreshToken] = (
    (__ \ "token").read[String]
      and (__ \ "clientId").read[String]
      and (__ \ "userId").read[Uuid]
      and (__ \ "expiresAt").read[Date]
      and (__ \ "registeredAt").read[Date]
    ) (AuthRefreshToken.apply _)

  implicit val write: Writes[AuthRefreshToken] = (
    (__ \ "token").write[String]
      and (__ \ "clientId").write[String]
      and (__ \ "userId").write[Uuid]
      and (__ \ "expiresAt").write[Date]
      and (__ \ "registeredAt").write[Date]
    ) (unlift(AuthRefreshToken.unapply))

  implicit val bsonDocumentHandler: BSONDocumentHandler[AuthRefreshToken] = Macros.handler[AuthRefreshToken]
}