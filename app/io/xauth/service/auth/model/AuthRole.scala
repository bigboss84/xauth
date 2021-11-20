package io.xauth.service.auth.model

import io.xauth.model.serial.EnumReads.enumNameReads
import io.xauth.model.serial.EnumWrites.enumNameWrites
import io.xauth.service.mongo.BsonHandlers.enumBsonHandler
import it.russoft.xenum.Enum
import play.api.libs.json.{Reads, Writes}
import reactivemongo.bson.{BSONHandler, BSONString}

/**
  * Defines all recognized and handled user roles.
  */
object AuthRole extends Enum {
  type AuthRole = EnumVal

  /**
    * Defines simple user role.
    */
  val User: AuthRole = value("USER")

  /**
    * Defines the human resource role.
    */
  val HumanResource: AuthRole = value("HR")

  /**
    * Defines the help desk operator role.
    */
  val HelpDeskOperator: AuthRole = value("HD_OPERATOR")

  /**
    * Defines the application responsible role.
    */
  val Responsible: AuthRole = value("RESPONSIBLE")

  /**
    * Defines the administrator role.
    */
  val Admin: AuthRole = value("ADMIN")

  // Json serialization
  implicit val reads: Reads[AuthRole] = enumNameReads(AuthRole)
  implicit val writes: Writes[AuthRole] = enumNameWrites

  // Bson serialization
  implicit val bsonHandler: BSONHandler[BSONString, AuthRole] = enumBsonHandler(AuthRole)
}
