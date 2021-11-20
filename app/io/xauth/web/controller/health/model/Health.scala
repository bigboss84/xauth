package io.xauth.web.controller.health.model

import java.util.Date

import io.xauth.model.DataFormat
import io.xauth.web.controller.health.model.HealthStatus.HealthStatus

/**
  * Global application health summary.
  */
case class Health
(
  status: HealthStatus,
  services: List[Service],
  updatedAt: Date
)

object Health extends DataFormat {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[Health] = (
    (__ \ "status").read[HealthStatus]
      and (__ \ "services").read[List[Service]]
      and (__ \ "updatedAt").read[Date]
    ) (Health.apply _)

  implicit val write: Writes[Health] = (
    (__ \ "status").write[HealthStatus]
      and (__ \ "services").write[List[Service]]
      and (__ \ "updatedAt").write(dateWrites(iso8601DateFormat))
    ) (unlift(Health.unapply))
}