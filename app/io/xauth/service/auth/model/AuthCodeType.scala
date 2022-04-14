package io.xauth.service.auth.model

import io.xauth.model.serial.EnumReads.enumNameReads
import io.xauth.model.serial.EnumWrites.enumNameWrites
import io.xauth.service.mongo.BsonHandlers.enumBsonHandler
import it.russoft.xenum.Enum
import play.api.libs.json.{Reads, Writes}
import reactivemongo.api.bson.BSONHandler

/**
 * Defines authentication code types.
 */
object AuthCodeType extends Enum {
  type AuthCodeType = EnumVal

  /**
   * Defines the type for account activation code.
   */
  val Activation: AuthCodeType = value("ACTIVATION")

  /**
   * Defines the type for account deletion code.
   */
  val Deletion: AuthCodeType = value("DELETION")

  /**
   * Defines the type for user contact trust code.
   */
  val ContactTrust: AuthCodeType = value("CONTACT_TRUST")

  /**
   * Defines the type for password reset code.
   */
  val PasswordReset: AuthCodeType = value("PASSWORD_RESET")

  /**
   * Defines the type for invitation registration code.
   */
  val Invitation: AuthCodeType = value("INVITATION")

  // Json serialization
  implicit val reads: Reads[AuthCodeType] = enumNameReads(AuthCodeType)
  implicit val writes: Writes[AuthCodeType] = enumNameWrites

  // Bson serialization
  implicit val bsonHandler: BSONHandler[AuthCodeType] = enumBsonHandler(AuthCodeType)
}
