package io.xauth.model

import io.xauth.model.serial.EnumReads.enumNameReads
import io.xauth.model.serial.EnumWrites.enumNameWrites
import io.xauth.service.mongo.BsonHandlers.enumBsonHandler
import it.russoft.xenum.Enum
import play.api.libs.json.{Reads, Writes}
import reactivemongo.api.bson.BSONHandler

/**
 * Defines all recognized and handled permission types.
 */
object Permission extends Enum {
  type Permission = EnumVal

  val Owner: Permission = value("OWNER")
  val Read: Permission = value("READ")
  val Write: Permission = value("WRITE")
  val Execution: Permission = value("EXECUTION")

  // Json serialization
  implicit val reads: Reads[Permission] = enumNameReads(Permission)
  implicit val writes: Writes[Permission] = enumNameWrites

  // Bson serialization
  implicit val bsonHandler: BSONHandler[Permission] = enumBsonHandler(Permission)
}
