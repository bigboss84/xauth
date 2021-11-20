package io.xauth.web.controller.health.model

import io.xauth.web.controller.health.model.HealthStatus.HealthStatus
import play.api.libs.json.{Json, Reads, Writes}

/**
  * Service for health check.
  */
case class Service(name: String, status: HealthStatus, trace: Option[String] = None)

object Service {
  implicit val reads: Reads[Service] = Json.reads[Service]
  implicit val writes: Writes[Service] = Json.writes[Service]
}