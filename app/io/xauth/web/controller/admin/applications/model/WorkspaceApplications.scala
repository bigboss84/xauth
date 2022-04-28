package io.xauth.web.controller.admin.applications.model

/**
  * Defines workspace applications model.
  */
case class WorkspaceApplications(applications: List[String])

object WorkspaceApplications {

  import play.api.libs.json._

  implicit val reads: Reads[WorkspaceApplications] = Json.reads[WorkspaceApplications]
  implicit val writes: Writes[WorkspaceApplications] = Json.writes[WorkspaceApplications]
}
