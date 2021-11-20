package io.xauth.web.controller.auth.model

import io.xauth.model.serial.EnumReads.enumNameReads
import io.xauth.model.serial.EnumWrites.enumNameWrites
import it.russoft.xenum.Enum
import play.api.libs.json.{Reads, Writes}

/**
  * Defines all possible token statuses.
  */
object TokenStatus extends Enum {
  type TokenStatus = EnumVal

  val Valid: TokenStatus = value("VALID")
  val Expired: TokenStatus = value("EXPIRED")
  val Invalid: TokenStatus = value("INVALID")

  implicit val reads: Reads[TokenStatus] = enumNameReads(TokenStatus)
  implicit val writes: Writes[TokenStatus] = enumNameWrites
}
