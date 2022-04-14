package io.xauth.web.controller.system.workspaces.model

import io.xauth.service.workspace.model.WorkspaceStatus.WorkspaceStatus

/**
  * Workspace status model
  */
case class WorkspaceStatusPatch(status: WorkspaceStatus)

object WorkspaceStatusPatch {

  import play.api.libs.json._

  implicit val reads: Reads[WorkspaceStatusPatch] = Json.reads[WorkspaceStatusPatch]
  implicit val writes: Writes[WorkspaceStatusPatch] = Json.writes[WorkspaceStatusPatch]
}