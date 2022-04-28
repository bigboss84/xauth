package io.xauth.web.controller.init.model

import play.api.libs.json.{Json, OWrites, Reads}

case class Init(client: Client, admin: Admin)

object Init {
  implicit val reads: Reads[Init] = Json.reads[Init]
  implicit val writes: OWrites[Init] = Json.writes[Init]
}