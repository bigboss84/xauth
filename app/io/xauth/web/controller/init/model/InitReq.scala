package io.xauth.web.controller.init.model

import play.api.libs.json.{Json, OWrites, Reads}

/**
  * Application initialization request.
  */
case class InitReq(init: Init, configuration: Configuration)

object InitReq {
  implicit val reads: Reads[InitReq] = Json.reads[InitReq]
  implicit val writes: OWrites[InitReq] = Json.writes[InitReq]
}