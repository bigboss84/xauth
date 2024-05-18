package io.xauth.config

import play.api.Environment
import play.api.libs.json.Json.parse
import play.api.libs.json.Reads

import scala.io.Source.fromFile

/**
  * Json Resource Loader
  */
class JsonResource[T](resPath: String)
                     (implicit env: Environment, reads: Reads[T]) {

  val resourcePath: String = resPath

  val value: T = {
    val source = fromFile(env.getFile(resPath))

    try {
      parse(source.getLines().map(_.trim).mkString).as[T]
    } finally {
      source.close()
    }
  }
}
