package io.xauth.web.controller.auth.model

/**
  * Login information supplied for sign-in.
  */
case class TokenReq(username: String, password: String)

object TokenReq {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[TokenReq] = (
    (__ \ "username").read[String] and (__ \ "password").read[String]
    ) (TokenReq.apply _)

  implicit val write: Writes[TokenReq] = (
    (__ \ "username").write[String] and (__ \ "password").write[String]
    ) (unlift(TokenReq.unapply))
}