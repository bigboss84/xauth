package io.xauth.model

import io.xauth.model.Permission.Permission
import reactivemongo.api.bson.{BSONDocumentHandler, Macros}

/**
 * Defines application information
 */
case class AppInfo
(
  name: String,
  permissions: Set[Permission]
)

object AppInfo {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json._

  implicit val reads: Reads[AppInfo] = (
    (__ \ "name").read[String](maxLength[String](128))
      and (__ \ "permissions").read[Set[Permission]]
    ) (AppInfo.apply _)

  implicit val writes: Writes[AppInfo] = Json.writes[AppInfo]

  implicit val bsonDocumentHandler: BSONDocumentHandler[AppInfo] = Macros.handler[AppInfo]
}
