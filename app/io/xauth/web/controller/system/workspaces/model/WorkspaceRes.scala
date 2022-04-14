package io.xauth.web.controller.system.workspaces.model

import io.xauth.Uuid
import io.xauth.model.DataFormat
import io.xauth.service.workspace.model.WorkspaceStatus.WorkspaceStatus
import io.xauth.service.workspace.model.{Workspace, WorkspaceConfiguration}

import java.util.Date

case class WorkspaceRes
(
  id: Uuid,
  tenantId: Uuid,
  slug: String,
  description: String,
  status: WorkspaceStatus,
  configuration: WorkspaceConfiguration,
  registeredAt: Date,
  updatedAt: Date
)

object WorkspaceRes extends DataFormat {
  implicit class WorkspaceResourceConverter(w: Workspace) {
    def toResource: WorkspaceRes = {
      WorkspaceRes(
        id = w.id,
        tenantId = w.tenantId,
        slug = w.slug,
        description = w.description,
        status = w.status,
        configuration = w.configuration,
        registeredAt = w.registeredAt,
        updatedAt = w.updatedAt
      )
    }
  }

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[WorkspaceRes] = (
    (__ \ "id").read[Uuid]
      and (__ \ "tenantId").read[Uuid]
      and (__ \ "slug").read[String]
      and (__ \ "description").read[String]
      and (__ \ "status").read[WorkspaceStatus]
      and (__ \ "configuration").read[WorkspaceConfiguration]
      and (__ \ "registeredAt").read[Date]
      and (__ \ "updatedAt").read[Date]
    ) (WorkspaceRes.apply _)

  implicit val write: Writes[WorkspaceRes] = (
    (__ \ "id").write[Uuid]
      and (__ \ "tenantId").write[Uuid]
      and (__ \ "slug").write[String]
      and (__ \ "description").write[String]
      and (__ \ "status").write[WorkspaceStatus]
      and (__ \ "configuration").write[WorkspaceConfiguration]
      and (__ \ "registeredAt").write(dateWrites(iso8601DateFormat))
      and (__ \ "updatedAt").write(dateWrites(iso8601DateFormat))
    ) (unlift(WorkspaceRes.unapply))
}