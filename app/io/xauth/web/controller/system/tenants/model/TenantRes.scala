package io.xauth.web.controller.system.tenants.model

import io.xauth.Uuid
import io.xauth.model.DataFormat
import io.xauth.service.tenant.model.Tenant

import java.util.Date

/**
  * Tenant creation response.
  */
case class TenantRes
(
  id: Uuid,
  slug: String,
  description: String,
  workspaceIds: List[Uuid] = Nil,
  registeredAt: Date,
  updatedAt: Date
)

object TenantRes extends DataFormat {

  implicit class TenantResourceConverter(t: Tenant) {
    def toResource: TenantRes = {
      TenantRes(
        id = t.id,
        slug = t.slug,
        description = t.description,
        workspaceIds = t.workspaceIds,
        registeredAt = t.registeredAt,
        updatedAt = t.updatedAt
      )
    }
  }

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[TenantRes] = (
    (__ \ "id").read[Uuid]
      and (__ \ "slug").read[String]
      and (__ \ "description").read[String]
      and (__ \ "workspaceIds").read[List[Uuid]]
      and (__ \ "registeredAt").read[Date]
      and (__ \ "updatedAt").read[Date]
    ) (TenantRes.apply _)

  implicit val write: Writes[TenantRes] = (
    (__ \ "id").write[Uuid]
      and (__ \ "slug").write[String]
      and (__ \ "description").write[String]
      and (__ \ "workspaceIds").write[List[Uuid]]
      and (__ \ "registeredAt").write(dateWrites(iso8601DateFormat))
      and (__ \ "updatedAt").write(dateWrites(iso8601DateFormat))
    ) (unlift(TenantRes.unapply))
}