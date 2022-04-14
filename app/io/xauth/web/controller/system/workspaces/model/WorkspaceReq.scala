package io.xauth.web.controller.system.workspaces.model

import io.xauth.Uuid
import io.xauth.model.DataFormat
import io.xauth.service.workspace.model.{WorkspaceConfiguration, WorkspaceInit}

/**
  * Model that represents workspace in the ingoing request body.
  */
case class WorkspaceReq
(
  tenantId: Uuid,
  slug: String,
  description: String,
  configuration: WorkspaceConfiguration, // no information hiding is currently needed
  init: WorkspaceInit
)

object WorkspaceReq extends DataFormat {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[WorkspaceReq] = (
    (__ \ "tenantId").read[Uuid]
      and (__ \ "slug").read[String]
      and (__ \ "description").read[String]
      and (__ \ "configuration").read[WorkspaceConfiguration]
      and (__ \ "init").read[WorkspaceInit]
    ) (WorkspaceReq.apply _)

  implicit val write: Writes[WorkspaceReq] = (
    (__ \ "tenantId").write[Uuid]
      and (__ \ "slug").write[String]
      and (__ \ "description").write[String]
      and (__ \ "configuration").write[WorkspaceConfiguration]
      and (__ \ "init").write[WorkspaceInit]
    ) (unlift(WorkspaceReq.unapply))
}
