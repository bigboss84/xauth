package io.xauth.web.controller.admin.users.model

import io.xauth.service.auth.model.AuthRole.AuthRole

/**
  * User id and roles model
  */
case class UserRoles(roles: List[AuthRole])

object UserRoles {

  import play.api.libs.json._

  implicit val reads: Reads[UserRoles] = Json.reads[UserRoles]
  implicit val writes: Writes[UserRoles] = Json.writes[UserRoles]
}
