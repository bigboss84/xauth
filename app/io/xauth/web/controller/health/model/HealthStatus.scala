package io.xauth.web.controller.health.model

import io.xauth.model.serial.EnumReads.enumNameReads
import io.xauth.model.serial.EnumWrites.enumNameWrites
import it.russoft.xenum.Enum
import play.api.libs.json.{Reads, Writes}

/**
  * Health status.
  */
object HealthStatus extends Enum {
  type HealthStatus = EnumVal

  val Up: HealthStatus = value("UP")
  val Down: HealthStatus = value("DOWN")

  // Json serialization
  implicit val reads: Reads[HealthStatus] = enumNameReads(HealthStatus)
  implicit val writes: Writes[HealthStatus] = enumNameWrites
}