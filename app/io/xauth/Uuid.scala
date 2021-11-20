package io.xauth

import io.xauth.UuidType.{Anonymous, UuidType}
import io.xauth.service.mongo.BsonHandlers.uuidBsonHandler
import it.russoft.xenum.Enum
import play.api.libs.json._
import play.api.mvc.PathBindable
import reactivemongo.bson.{BSONBinary, BSONHandler}

import java.nio.ByteBuffer
import java.util.UUID
import scala.util.{Failure, Success, Try}

case class Uuid(uuid: Option[UUID] = None) {

  val value: UUID = uuid match {
    case Some(s) => s
    case None => UUID.randomUUID
  }

  val idType: UuidType = Anonymous

  val stringValue: String = value.toString

  // uuid(<id-type>, db9d9c40-e2eb-43de-910d-6a24114f11c5)
  override def toString: String = s"uuid(${idType.value.toLowerCase}, $stringValue)"
}

object Uuid {

  def apply(uuid: String): Uuid = new Uuid(Some(UUID.fromString(uuid)))

  def apply(bytes: Array[Byte]): Uuid = {
    new Uuid(Some(new UUID(
      ByteBuffer.wrap(bytes.slice(0, 8)).getLong,
      ByteBuffer.wrap(bytes.slice(8, 16)).getLong
    )))
  }

  // Json serialization
  implicit object Format extends Format[Uuid] {
    def reads(json: JsValue): JsResult[Uuid] =
      Try(Uuid(json.as[String])) match {
        case Success(v) => JsSuccess(v)
        case Failure(e) => JsError(e.getMessage)
      }

    def writes(uuid: Uuid): JsValue = JsString(uuid.stringValue)
  }

  // Bson serialization
  implicit val bsonHandler: BSONHandler[BSONBinary, Uuid] = uuidBsonHandler

  // Path binder
  implicit object UuidPathBindable extends PathBindable[Uuid] {
    def bind(key: String, value: String): Either[String, Uuid] = {
      if (value.length != 36) Left(s"Cannot parse parameter $key with value '$value' as Uuid: $key must be exactly 36 digits in length.")
      else Right(Uuid(value))
    }

    def unbind(key: String, value: Uuid): String = value.stringValue
  }
}

object UuidType extends Enum {
  type UuidType = EnumVal

  /**
    * Defines an anonymous uuid type.
    */
  val Anonymous: UuidType = value("ANONYMOUS")

  /**
    * Defines the uuid type for invitation type.
    */
  val Invitation: UuidType = value("INVITATION")

  /**
    * Defines the uuid type for user type.
    */
  val User: UuidType = value("USER")
}
