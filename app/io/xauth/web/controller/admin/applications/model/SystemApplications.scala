package io.xauth.web.controller.admin.applications.model

/**
  * Defines system applications model.
  */
case class SystemApplications(applications: List[String])

object SystemApplications {

  import play.api.libs.json._

  implicit val reads: Reads[SystemApplications] = Json.reads[SystemApplications]
  implicit val writes: Writes[SystemApplications] = Json.writes[SystemApplications]
}
