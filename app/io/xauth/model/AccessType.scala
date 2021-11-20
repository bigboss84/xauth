package io.xauth.model

import io.xauth.model.serial.EnumReads.enumNameReads
import io.xauth.model.serial.EnumWrites.enumNameWrites
import it.russoft.xenum.Enum
import play.api.libs.json._

/**
  * Defines all recognized and handled access types.
  */
object AccessType extends Enum {
  type AccessType = EnumVal

  val Application: AccessType = value("APPLICATION")
  val User: AccessType = value("USER")

  implicit val reads: Reads[AccessType] = enumNameReads(AccessType)
  implicit val writes: Writes[AccessType] = enumNameWrites
}
