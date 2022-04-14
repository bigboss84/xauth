package io.xauth.service.invitation.model

import io.xauth.Uuid
import io.xauth.model.{AppInfo, DataFormat, UserInfo}
import reactivemongo.api.bson.Macros.Annotations.Key
import reactivemongo.api.bson.{BSONDocumentHandler, Macros}

import java.util.Date

/**
 * Registration invitation model.
 *
 * @param id           Identifier
 * @param description  Simple and brief description that explains invitation
 * @param userInfo     Pre-filled user information
 * @param validFrom    If defined specifies the invitation validity start date
 * @param validTo      If defined specifies the invitation validity end date
 * @param registeredAt Invitation registration date
 * @param updatedAt    Invitation last update date
 */
case class Invitation
(
  @Key("_id")
  id: Option[Uuid] = None,
  description: Option[String] = None,
  applications: List[AppInfo] = Nil,
  userInfo: UserInfo,
  validFrom: Option[Date] = None,
  validTo: Option[Date] = None,
  registeredAt: Option[Date] = None,
  updatedAt: Option[Date] = None
)

object Invitation extends DataFormat {

  def generateCode: String = {
    import io.xauth.util.Implicits.RandomString
    (('a' to 'z') ++ ('0' to '9')) random 10
  }

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[Invitation] = (
    (__ \ "id").readNullable[Uuid]
      and (__ \ "description").readNullable[String]
      and (__ \ "applications").read[List[AppInfo]]
      and (__ \ "userInfo").read[UserInfo]
      and (__ \ "validFrom").readNullable[Date]
      and (__ \ "validTo").readNullable[Date]
      and (__ \ "registeredAt").readNullable[Date]
      and (__ \ "updatedAt").readNullable[Date]
    ) (Invitation.apply _)

  implicit val write: Writes[Invitation] = (
    (__ \ "id").writeNullable[Uuid]
      and (__ \ "description").writeNullable[String]
      and (__ \ "applications").write[List[AppInfo]]
      and (__ \ "userInfo").write[UserInfo]
      and (__ \ "validFrom").writeNullable(dateWrites(iso8601DateFormat))
      and (__ \ "validTo").writeNullable(dateWrites(iso8601DateFormat))
      and (__ \ "registeredAt").writeNullable(dateWrites(iso8601DateFormat))
      and (__ \ "updatedAt").writeNullable(dateWrites(iso8601DateFormat))
    ) (unlift(Invitation.unapply))

  import io.xauth.service.mongo.BsonHandlers._
  implicit val bsonDocumentHandler: BSONDocumentHandler[Invitation] = Macros.handler[Invitation]
}
