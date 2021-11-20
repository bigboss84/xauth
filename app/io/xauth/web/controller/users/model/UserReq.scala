package io.xauth.web.controller.users.model

import io.xauth.model.{DataFormat, UserInfo}

/**
  * Model that represents user in the ingoing request body.
  */
case class UserReq
(
  invitationCode: Option[String],
  username: String,
  password: String,
  passwordCheck: String,
  description: Option[String],
  userInfo: UserInfo,
  privacy: Boolean
)

object UserReq extends DataFormat {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[UserReq] = (
    (__ \ "invitationCode").readNullable[String]
      and (__ \ "username").readWithDefault[String]("")
      and (__ \ "password").read[String]
      and (__ \ "passwordCheck").read[String]
      and (__ \ "description").readNullable[String]
      and (__ \ "userInfo").read[UserInfo]
      and (__ \ "privacy").read[Boolean]
    ) (UserReq.apply _)

  implicit val write: Writes[UserReq] = (
    (__ \ "invitationCode").writeNullable[String]
      and (__ \ "username").write[String]
      and (__ \ "password").write[String]
      and (__ \ "passwordCheck").write[String]
      and (__ \ "description").writeNullable[String]
      and (__ \ "userInfo").write[UserInfo]
      and (__ \ "privacy").write[Boolean]
    ) (unlift(UserReq.unapply))
}
