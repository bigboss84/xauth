package io.xauth.web.controller.auth.model

import play.api.libs.json.{Json, Reads, Writes}

/**
  * Account deletion request body model.
  */
case class AccountDeleteConfirmationReq(code: String)

object AccountDeleteConfirmationReq {
  implicit val reads: Reads[AccountDeleteConfirmationReq] = Json.reads[AccountDeleteConfirmationReq]
  implicit val writes: Writes[AccountDeleteConfirmationReq] = Json.writes[AccountDeleteConfirmationReq]
}