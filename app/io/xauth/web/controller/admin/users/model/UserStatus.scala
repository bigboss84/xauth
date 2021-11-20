package io.xauth.web.controller.admin.users.model

import io.xauth.service.auth.model.AuthStatus.AuthStatus

/**
  * User status model
  */
case class UserStatus(status: AuthStatus)

object UserStatus {

  import play.api.libs.json._

  implicit val reads: Reads[UserStatus] = Json.reads[UserStatus]
  implicit val writes: Writes[UserStatus] = Json.writes[UserStatus]
}