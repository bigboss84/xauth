package io.xauth.service.auth.model

import io.xauth.model.serial.EnumReads.enumNameReads
import io.xauth.model.serial.EnumWrites.enumNameWrites
import io.xauth.service.mongo.BsonHandlers.enumBsonHandler
import it.russoft.xenum.Enum
import play.api.libs.json.{Reads, Writes}
import reactivemongo.api.bson.BSONHandler

/**
 * Defines all recognized and handled user statuses.
 */
object AuthStatus extends Enum {
  type AuthStatus = EnumVal

  /**
   * Defines the disabled status.
   */
  val Disabled: AuthStatus = value("DISABLED")

  /**
   * Defines the enabled status for the working user.
   */
  val Enabled: AuthStatus = value("ENABLED")

  /**
   * Defines the status for the blocked user.
   */
  val Blocked: AuthStatus = value("BLOCKED")

  // Json serialization
  implicit val reads: Reads[AuthStatus] = enumNameReads(AuthStatus)
  implicit val writes: Writes[AuthStatus] = enumNameWrites

  // Bson serialization
  implicit val bsonHandler: BSONHandler[AuthStatus] = enumBsonHandler(AuthStatus)
}
