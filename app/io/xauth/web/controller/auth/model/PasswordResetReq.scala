package io.xauth.web.controller.auth.model

import play.api.libs.json.{Json, Reads, Writes}

/**
  * Password reset request body model.
  */
case class PasswordResetReq
(
  code: String,
  password: String,
  passwordCheck: String
)

object PasswordResetReq {
  implicit val reads: Reads[PasswordResetReq] = Json.reads[PasswordResetReq]
  implicit val writes: Writes[PasswordResetReq] = Json.writes[PasswordResetReq]
}