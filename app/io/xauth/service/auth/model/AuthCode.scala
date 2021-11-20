package io.xauth.service.auth.model

import java.util.Date

import io.xauth.Uuid
import io.xauth.model.{DataFormat, UserContact}
import io.xauth.service.auth.model.AuthCodeType.AuthCodeType
import reactivemongo.bson.Macros.Annotations.Key
import reactivemongo.bson.{BSONDocumentHandler, Macros}

case class AuthCode
(
  @Key("_id")
  code: String,
  `type`: AuthCodeType,
  referenceId: Uuid,
  userContact: Option[UserContact],
  expiresAt: Date,
  registeredAt: Date
)

object AuthCode extends DataFormat {

  def generateCode: String = {
    import io.xauth.util.Implicits.RandomString
    (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')).random(32)
  }

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[AuthCode] = (
    (__ \ "code").read[String]
      and (__ \ "type").read[AuthCodeType]
      and (__ \ "referenceId").read[Uuid]
      and (__ \ "userContact").readNullable[UserContact]
      and (__ \ "expiresAt").read[Date]
      and (__ \ "registeredAt").read[Date]
    ) (AuthCode.apply _)

  implicit val write: Writes[AuthCode] = (
    (__ \ "code").write[String]
      and (__ \ "type").write[AuthCodeType]
      and (__ \ "referenceId").write[Uuid]
      and (__ \ "userContact").writeNullable[UserContact]
      and (__ \ "expiresAt").write(dateWrites(iso8601DateFormat))
      and (__ \ "registeredAt").write(dateWrites(iso8601DateFormat))
    ) (unlift(AuthCode.unapply))

  implicit val bsonDocumentHandler: BSONDocumentHandler[AuthCode] = Macros.handler[AuthCode]
}
