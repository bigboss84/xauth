package io.xauth.web.controller.auth.model

/**
  * Contact trust request information.
  */
case class ContactTrustReq(contact: String)

object ContactTrustReq {

  import play.api.libs.json.Reads._
  import play.api.libs.json._

  implicit val reads: Reads[ContactTrustReq] =
    (__ \ "contact").read[String].map(ContactTrustReq.apply)

  implicit val write: Writes[ContactTrustReq] =
    Writes[ContactTrustReq](b => JsString(b.contact))
}

