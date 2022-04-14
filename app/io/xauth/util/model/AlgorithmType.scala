package io.xauth.util.model

import io.xauth.model.serial.EnumReads.enumNameReads
import io.xauth.model.serial.EnumWrites.enumNameWrites
import io.xauth.service.mongo.BsonHandlers.enumBsonHandler
import it.russoft.xenum.Enum
import play.api.libs.json.{Reads, Writes}
import reactivemongo.api.bson.BSONHandler

/**
  * Defines algorithm type.
  */
object AlgorithmType extends Enum {
  type AlgorithmType = EnumVal

  /**
    * Defines the symmetric algorithm type.
    */
  val Symmetric: AlgorithmType = value("SYMMETRIC")

  /**
    * Defines the asymmetric  algorithm type.
    */
  val Asymmetric: AlgorithmType = value("ASYMMETRIC")

  // Json serialization
  implicit val reads: Reads[AlgorithmType] = enumNameReads(AlgorithmType)
  implicit val writes: Writes[AlgorithmType] = enumNameWrites

  // Bson serialization
  implicit val bsonHandler: BSONHandler[AlgorithmType] = enumBsonHandler(AlgorithmType)
}
