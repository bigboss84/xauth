package io.xauth.web.controller.init.model

import play.api.libs.json.{Json, OWrites, Reads}

case class Configuration(applications: List[String])

object Configuration {
  implicit val reads: Reads[Configuration] = Json.reads[Configuration]
  implicit val writes: OWrites[Configuration] = Json.writes[Configuration]
}