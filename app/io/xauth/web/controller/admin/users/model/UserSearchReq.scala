package io.xauth.web.controller.admin.users.model

case class UserSearchReq(username: String)

object UserSearchReq {

  import play.api.libs.json._

  implicit val reads: Reads[UserSearchReq] = Json.reads[UserSearchReq]
  implicit val writes: Writes[UserSearchReq] = Json.writes[UserSearchReq]
}