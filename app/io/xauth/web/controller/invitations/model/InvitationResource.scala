package io.xauth.web.controller.invitations.model

import io.xauth.Uuid
import io.xauth.model.{AppInfo, DataFormat, UserInfo}
import io.xauth.service.invitation.model.Invitation

import java.util.Date

/**
  * Represents the invitation request body.
  */
case class InvitationResource
(
  id: Option[Uuid],
  description: Option[String] = None,
  applications: List[AppInfo] = Nil,
  userInfo: UserInfo,
  validFrom: Option[Date] = None,
  validTo: Option[Date] = None,
  registeredAt: Option[Date] = None,
  updatedAt: Option[Date] = None
)

object InvitationResource extends DataFormat {

  implicit class InvitationResourceConverter(i: Invitation) {
    def toResource: InvitationResource = {
      InvitationResource(
        id = i.id,
        description = i.description,
        applications = i.applications,
        userInfo = i.userInfo,
        validFrom = i.validFrom,
        validTo = i.validTo,
        registeredAt = Option(i.registeredAt),
        updatedAt = Option(i.updatedAt)
      )
    }
  }

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[InvitationResource] = (
    (__ \ "id").readNullable[Uuid]
      and (__ \ "description").readNullable[String]
      and (__ \ "applications").read[List[AppInfo]]
      and (__ \ "userInfo").read[UserInfo]
      and (__ \ "validFrom").readNullable[Date]
      and (__ \ "validTo").readNullable[Date]
      and (__ \ "registeredAt").readNullable[Date]
      and (__ \ "updatedAt").readNullable[Date]
    ) (InvitationResource.apply _)

  implicit val write: Writes[InvitationResource] = (
    (__ \ "id").writeNullable[Uuid]
      and (__ \ "description").writeNullable[String]
      and (__ \ "applications").write[List[AppInfo]]
      and (__ \ "userInfo").write[UserInfo]
      and (__ \ "validFrom").writeNullable(dateWrites(iso8601DateFormat))
      and (__ \ "validTo").writeNullable(dateWrites(iso8601DateFormat))
      and (__ \ "registeredAt").writeNullable(dateWrites(iso8601DateFormat))
      and (__ \ "updatedAt").writeNullable(dateWrites(iso8601DateFormat))
    ) (unlift(InvitationResource.unapply))
}