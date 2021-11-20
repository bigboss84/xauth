package io.xauth.web.controller.owner.users.model

import io.xauth.model.AppInfo

/**
  * User applications model
  */
case class UserApplications(applications: List[AppInfo] = Nil)

object UserApplications {

  import play.api.libs.json._

  implicit val reads: Reads[UserApplications] = Json.reads[UserApplications]
  implicit val writes: Writes[UserApplications] = Json.writes[UserApplications]
}
