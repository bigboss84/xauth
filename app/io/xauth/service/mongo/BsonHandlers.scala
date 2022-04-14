package io.xauth.service.mongo

import io.xauth.Uuid
import io.xauth.model.DataFormat
import it.russoft.xenum.Enum
import reactivemongo.api.bson.{BSONHandler, BSONString, BSONValue}

import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Date
import scala.util.{Success, Try}

/**
 * Handles serialization for enumeration from/to bson format.
 */
object BsonHandlers extends DataFormat {

  implicit val uuidBsonHandler: BSONHandler[Uuid] = new BSONHandler[Uuid] {
    override def readTry(b: BSONValue): Try[Uuid] = b.asTry[BSONString].map(b => Uuid(b.value))
    override def writeTry(u: Uuid): Try[BSONValue] = Success(BSONString(u.stringValue))
  }

  def enumBsonHandler[T <: Enum](enum: T): BSONHandler[T#EnumVal] = new BSONHandler[T#EnumVal] {
    override def readTry(b: BSONValue): Try[T#EnumVal] = b.asTry[BSONString].map(s => enum.withName(s.value))
    override def writeTry(e: T#EnumVal): Try[BSONValue] = Success(BSONString(e.value))
  }

  implicit val dateBsonHandler: BSONHandler[Date] = new BSONHandler[Date] {
    override def readTry(b: BSONValue): Try[Date] =
      b.asTry[BSONString].map(s => new SimpleDateFormat(iso8601DateFormat).parse(s.value))
    override def writeTry(d: Date): Try[BSONValue] =
      Success(BSONString(new SimpleDateFormat(iso8601DateFormat).format(d)))
  }

  implicit val zoneIdBsonHandler: BSONHandler[ZoneId] = new BSONHandler[ZoneId] {
    override def readTry(b: BSONValue): Try[ZoneId] = b.asTry[BSONString].map(s => ZoneId.of(s.value))
    override def writeTry(z: ZoneId): Try[BSONValue] = Success(BSONString(z.getId))
  }
}
