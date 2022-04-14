package io.xauth.web.controller.system.workspaces.model

import io.xauth.model.DataFormat
import io.xauth.service.workspace.model.WorkspaceConfiguration

/**
  * Model that represents workspace in the ingoing request body.
  */
case class WorkspacePutReq
(
  slug: String,
  description: String,
  configuration: WorkspaceConfiguration // no information hiding is currently needed
)

object WorkspacePutReq extends DataFormat {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[WorkspacePutReq] = (
    (__ \ "slug").read[String]
      and (__ \ "description").read[String]
      and (__ \ "configuration").read[WorkspaceConfiguration]
    ) (WorkspacePutReq.apply _)

  implicit val write: Writes[WorkspacePutReq] = (
    (__ \ "slug").write[String]
      and (__ \ "description").write[String]
      and (__ \ "configuration").write[WorkspaceConfiguration]
    ) (unlift(WorkspacePutReq.unapply))
}
