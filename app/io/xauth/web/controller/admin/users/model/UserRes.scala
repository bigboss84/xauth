package io.xauth.web.controller.admin.users.model

import java.util.Date

import io.xauth.Uuid
import io.xauth.model.{DataFormat, UserInfo}
import io.xauth.service.auth.model.AuthRole.AuthRole
import io.xauth.service.auth.model.AuthStatus.AuthStatus
import io.xauth.service.auth.model.AuthUser

/**
  * Represents a the exposed user resource.
  */
case class UserRes
(
  id: Uuid,
  username: String,
  roles: List[AuthRole],
  status: AuthStatus,
  description: Option[String],
  userInfo: UserInfo,
  registeredAt: Date,
  updatedAt: Date
)

object UserRes extends DataFormat {

  implicit class UserResConverter(u: AuthUser) {
    def toResource: UserRes = {
      UserRes(
        id = u.id,
        username = u.username,
        roles = u.roles,
        status = u.status,
        description = u.description,
        userInfo = u.userInfo,
        registeredAt = u.registeredAt,
        updatedAt = u.updatedAt
      )
    }
  }

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[UserRes] = (
    (__ \ "id").read[Uuid]
      and (__ \ "username").read[String]
      and (__ \ "roles").read[List[AuthRole]]
      and (__ \ "status").read[AuthStatus]
      and (__ \ "description").readNullable[String]
      and (__ \ "userInfo").read[UserInfo]
      and (__ \ "registeredAt").read[Date]
      and (__ \ "updatedAt").read[Date]
    ) (UserRes.apply _)

  implicit val write: Writes[UserRes] = (
    (__ \ "id").write[Uuid]
      and (__ \ "username").write[String]
      and (__ \ "roles").write[List[AuthRole]]
      and (__ \ "status").write[AuthStatus]
      and (__ \ "description").writeNullable[String]
      and (__ \ "userInfo").write[UserInfo]
      and (__ \ "registeredAt").write(dateWrites(iso8601DateFormat))
      and (__ \ "updatedAt").write(dateWrites(iso8601DateFormat))
    ) (unlift(UserRes.unapply))
}
