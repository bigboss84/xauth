package io.xauth.model.serial

import it.russoft.xenum.Enum
import play.api.libs.json._

import scala.collection.Seq

/**
  * `Enum` reader helper that replaces default and allows
  * to read enumeration values from play-json library.
  * Written for play-json library.
  */
object EnumReads {
  def enumNameReads[E <: Enum](enum: E): Reads[E#EnumVal] = {
    case JsString(s) =>
      enum.values
        .find(_.toString == s)
        .map(JsSuccess(_))
        .getOrElse(JsError(Seq(JsPath -> Seq(JsonValidationError("error.expected.validenumvalue")))))
    case _ => JsError(Seq(JsPath -> Seq(JsonValidationError("error.expected.enumstring"))))
  }
}
