package io.xauth.web.controller.auth.model

import play.api.libs.json.{Json, Reads, Writes}

/**
  * Password forgotten request body model.
  */
case class PasswordForgottenReq(username: String)

object PasswordForgottenReq {
  implicit val reads: Reads[PasswordForgottenReq] = Json.reads[PasswordForgottenReq]
  implicit val writes: Writes[PasswordForgottenReq] = Json.writes[PasswordForgottenReq]
}