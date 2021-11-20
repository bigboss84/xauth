package io.xauth.web.controller.auth.model

/**
  * Refresh token information supplied to obtain new access token.
  */
case class RefreshReq(refreshToken: String)

object RefreshReq {

  import play.api.libs.json.Reads._
  import play.api.libs.json._

  implicit val reads: Reads[RefreshReq] =
    (__ \ "refreshToken").read[String].map(RefreshReq.apply)

  implicit val write: Writes[RefreshReq] =
    Writes[RefreshReq](b => JsString(b.refreshToken))
}
