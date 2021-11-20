package io.xauth.web.controller.auth.model

/**
  * Refreshed authorization information response data.
  */
case class RefreshRes
(
  tokenType: String,
  accessToken: String,
  expiresIn: Int,
  refreshToken: String
)

object RefreshRes {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[RefreshRes] = (
    (__ \ "tokenType").read[String]
      and (__ \ "accessToken").read[String]
      and (__ \ "expiresIn").read[Int]
      and (__ \ "refreshToken").read[String]
    ) (RefreshRes.apply _)

  implicit val write: Writes[RefreshRes] = (
    (__ \ "tokenType").write[String]
      and (__ \ "accessToken").write[String]
      and (__ \ "expiresIn").write[Int]
      and (__ \ "refreshToken").write[String]
    ) (unlift(RefreshRes.unapply))
}