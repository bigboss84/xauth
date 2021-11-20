package io.xauth.web.controller.admin.users.model

/**
  * Model that represents account activation request.
  */
case class AccountTrustReq
(
  username: String
)

object AccountTrustReq {

  import play.api.libs.json._

  implicit val reads: Reads[AccountTrustReq] = Json.reads[AccountTrustReq]
  implicit val writes: Writes[AccountTrustReq] = Json.writes[AccountTrustReq]
}
