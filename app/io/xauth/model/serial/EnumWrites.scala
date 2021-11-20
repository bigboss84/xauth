package io.xauth.model.serial

import it.russoft.xenum.Enum
import play.api.libs.json.{JsString, Writes}

/**
  * `Enum` writer helper that replaces default and allows
  * to write enumeration values.
  * Written for play-json library.
  */
object EnumWrites {
  implicit def enumNameWrites[E <: Enum]: Writes[E#EnumVal] =
    Writes[E#EnumVal] { value: E#EnumVal => JsString(value.toString) }
}
