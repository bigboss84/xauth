package io.xauth.service.auth.model

import java.util.Date
import io.xauth.model.DataFormat
import reactivemongo.api.bson.Macros.Annotations.Key
import reactivemongo.api.bson.{BSONDocumentHandler, Macros}

case class AuthClient
(
  @Key("_id")
  id: String,
  secret: String,
  registeredAt: Date,
  updatedAt: Date
)

object AuthClient extends DataFormat {

  def generateSecret: String = {
    import io.xauth.util.Implicits.RandomString
    (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')) random 10
  }

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[AuthClient] = (
    (__ \ "id").read[String]
      and (__ \ "secret").read[String]
      and (__ \ "registeredAt").read[Date](IsoDateReads)
      and (__ \ "updatedAt").read[Date](IsoDateReads)
    ) (AuthClient.apply _)

  implicit val write: Writes[AuthClient] = (
    (__ \ "id").write[String]
      and (__ \ "secret").write[String]
      and (__ \ "registeredAt").write(dateWrites(iso8601DateFormat))
      and (__ \ "updatedAt").write(dateWrites(iso8601DateFormat))
    ) (unlift(AuthClient.unapply))

  import io.xauth.service.mongo.BsonHandlers._
  implicit val bsonDocumentHandler: BSONDocumentHandler[AuthClient] = Macros.handler[AuthClient]
}