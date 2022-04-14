package io.xauth.model

import reactivemongo.api.bson.{BSONDocumentHandler, Macros}

/**
 * Defines user information.
 */
case class UserInfo
(
  firstName: String,
  lastName: String,
  company: String,
  contacts: List[UserContact]
)

object UserInfo {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json._

  implicit val reads: Reads[UserInfo] = (
    (__ \ "firstName").readWithDefault[String]("n/a")(maxLength[String](128))
      and (__ \ "lastName").readWithDefault[String]("n/a")(maxLength[String](128))
      and (__ \ "company").readWithDefault[String]("n/a")(maxLength[String](128))
      and (__ \ "contacts").read[List[UserContact]]
    ) (UserInfo.apply _)

  implicit val writes: Writes[UserInfo] = Json.writes[UserInfo]

  implicit val bsonDocumentHandler: BSONDocumentHandler[UserInfo] = Macros.handler[UserInfo]
}
