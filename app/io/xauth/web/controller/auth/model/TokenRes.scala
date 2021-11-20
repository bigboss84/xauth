package io.xauth.web.controller.auth.model

/**
  * Authorization information response data.
  */
case class TokenRes
(
  tokenType: String,
  accessToken: String,
  expiresIn: Int,
  refreshToken: String
)

object TokenRes {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[TokenRes] = (
    (__ \ "tokenType").read[String]
      and (__ \ "accessToken").read[String]
      and (__ \ "expiresIn").read[Int]
      and (__ \ "refreshToken").read[String]
    ) (TokenRes.apply _)

  implicit val write: Writes[TokenRes] = (
    (__ \ "tokenType").write[String]
      and (__ \ "accessToken").write[String]
      and (__ \ "expiresIn").write[Int]
      and (__ \ "refreshToken").write[String]
    ) (unlift(TokenRes.unapply))
}
