package io.xauth.service.app.model

import io.xauth.model.serial.EnumReads.enumNameReads
import io.xauth.model.serial.EnumWrites.enumNameWrites
import io.xauth.service.mongo.BsonHandlers._
import it.russoft.xenum.Enum
import play.api.libs.json.{Reads, Writes}
import reactivemongo.api.bson.BSONHandler

/**
 * Defines application configuration key.
 */
object AppKey extends Enum {
  type AppKey = EnumVal

  val Init: AppKey = value("app.init")
  val Applications: AppKey = value("app.applications")

  // Json serialization
  implicit val reads: Reads[AppKey] = enumNameReads(AppKey)
  implicit val writes: Writes[AppKey] = enumNameWrites

  // Bson serialization
  implicit val bsonHandler: BSONHandler[AppKey] = enumBsonHandler(AppKey)
}
