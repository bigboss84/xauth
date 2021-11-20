package io.xauth.web.controller.admin.users.model

import io.xauth.model.{DataFormat, UserInfo}
import io.xauth.model.UserInfo

/**
  * Model that represents user in the ingoing request body.
  */
case class UserReq
(
  password: String,
  passwordCheck: String,
  description: Option[String],
  userInfo: UserInfo
)

object UserReq extends DataFormat {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[UserReq] = (
      (__ \ "password").read[String]
      and (__ \ "passwordCheck").read[String]
      and (__ \ "description").readNullable[String]
      and (__ \ "userInfo").read[UserInfo]
    ) (UserReq.apply _)

  implicit val write: Writes[UserReq] = (
      (__ \ "password").write[String]
      and (__ \ "passwordCheck").write[String]
      and (__ \ "description").writeNullable[String]
      and (__ \ "userInfo").write[UserInfo]
    ) (unlift(UserReq.unapply))
}
