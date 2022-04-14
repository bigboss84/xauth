package io.xauth.model

import io.xauth.model.ContactType.ContactType
import reactivemongo.api.bson.{BSONDocumentHandler, Macros}

/**
 * Defines minimum information of a contact
 */
case class UserContact
(
  `type`: ContactType,
  value: String,
  description: Option[String],
  trusted: Boolean
)

object UserContact {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json._

  implicit val reads: Reads[UserContact] = (
    (__ \ "type").read[ContactType]
      and (__ \ "value").read[String]
      and (__ \ "description").readNullable[String](maxLength[String](128))
      and (__ \ "trusted").readWithDefault[Boolean](false)
    ) (UserContact.apply _)

  implicit val writes: Writes[UserContact] = Json.writes[UserContact]

  implicit val bsonDocumentHandler: BSONDocumentHandler[UserContact] = Macros.handler[UserContact]
}
