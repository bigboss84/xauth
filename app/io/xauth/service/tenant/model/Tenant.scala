package io.xauth.service.tenant.model

import io.xauth.Uuid
import io.xauth.model.DataFormat
import reactivemongo.api.bson.Macros.Annotations.Key
import reactivemongo.api.bson.{BSONDocumentHandler, Macros}

import java.util.Date

case class Tenant
(
  @Key("_id")
  id: Uuid,
  slug: String,
  description: String,
  workspaceIds: List[Uuid] = Nil,
  registeredAt: Date,
  updatedAt: Date
)

object Tenant extends DataFormat {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[Tenant] = (
    (__ \ "id").read[Uuid]
      and (__ \ "slug").read[String]
      and (__ \ "description").read[String]
      and (__ \ "workspaceIds").read[List[Uuid]]
      and (__ \ "registeredAt").read[Date]
      and (__ \ "updatedAt").read[Date]
    ) (Tenant.apply _)

  implicit val write: Writes[Tenant] = (
    (__ \ "id").write[Uuid]
      and (__ \ "slug").write[String]
      and (__ \ "description").write[String]
      and (__ \ "workspaceIds").write[List[Uuid]]
      and (__ \ "registeredAt").write(dateWrites(iso8601DateFormat))
      and (__ \ "updatedAt").write(dateWrites(iso8601DateFormat))
    ) (unlift(Tenant.unapply))

  import io.xauth.service.mongo.BsonHandlers._
  implicit val bsonDocumentHandler: BSONDocumentHandler[Tenant] = Macros.handler[Tenant]
}