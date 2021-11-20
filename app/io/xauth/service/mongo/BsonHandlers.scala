package io.xauth.service.mongo

import io.xauth.Uuid
import io.xauth.UuidType.{Anonymous, UuidType}
import it.russoft.xenum.Enum
import reactivemongo.bson.{BSONBinary, BSONHandler, BSONString}

/**
  * Handles serialization for enumeration from/to bson format.
  */
object BsonHandlers {
  def uuidBsonHandler(implicit idType: UuidType = Anonymous): BSONHandler[BSONBinary, Uuid] =
    new BSONHandler[BSONBinary, Uuid] {
      def read(bson: BSONBinary): Uuid = Uuid(bson.byteArray)
      def write(uuid: Uuid): BSONBinary = BSONBinary(uuid.value)
    }

  def enumBsonHandler[T <: Enum](enum: T): BSONHandler[BSONString, T#EnumVal] =
    new BSONHandler[BSONString, T#EnumVal] {
      def read(bson: BSONString): T#EnumVal = enum.withName(bson.value)
      def write(enumVal: T#EnumVal): BSONString = BSONString(enumVal.value)
    }
}
