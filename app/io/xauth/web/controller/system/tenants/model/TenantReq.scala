package io.xauth.web.controller.system.tenants.model

import io.xauth.model.DataFormat

/**
  * Model that represents tenant in the ingoing request body.
  */
case class TenantReq
(
  slug: String,
  description: String
)

object TenantReq extends DataFormat {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[TenantReq] = (
    (__ \ "slug").read[String]
      and (__ \ "description").read[String]
    ) (TenantReq.apply _)

  implicit val write: Writes[TenantReq] = (
    (__ \ "slug").write[String]
      and (__ \ "description").write[String]
    ) (unlift(TenantReq.unapply))
}
