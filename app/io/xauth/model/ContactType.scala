package io.xauth.model

import io.xauth.model.serial.EnumReads.enumNameReads
import io.xauth.model.serial.EnumWrites.enumNameWrites
import io.xauth.service.mongo.BsonHandlers.enumBsonHandler
import it.russoft.xenum.Enum
import play.api.libs.json.{Reads, Writes}
import reactivemongo.api.bson.BSONHandler

/**
 * Defines all recognized and handled contact types.
 */
object ContactType extends Enum {
  type ContactType = EnumVal

  val Email: ContactType = value("EMAIL")
  val MobileNumber: ContactType = value("MOBILE_NUMBER")

  // Json serialization
  implicit val reads: Reads[ContactType] = enumNameReads(ContactType)
  implicit val writes: Writes[ContactType] = enumNameWrites

  // Bson serialization
  implicit val bsonHandler: BSONHandler[ContactType] = enumBsonHandler(ContactType)
}
