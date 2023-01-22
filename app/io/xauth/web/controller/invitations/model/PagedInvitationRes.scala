package io.xauth.web.controller.invitations.model

import io.xauth.model.pagination.PagedData
import io.xauth.service.invitation.model.Invitation
import io.xauth.web.controller.invitations.model.InvitationResource._
import play.api.libs.json.{Json, Writes}

case class PagedInvitationRes
(
  page: Int,
  pageSize: Int,
  pageCount: Int,
  totalPages: Int,
  totalCount: Int,
  elements: Seq[InvitationResource]
)

object PagedInvitationRes {
  implicit class PagedUserResConverter(p: PagedData[Invitation]) {
    def toResource: PagedInvitationRes =
      PagedInvitationRes(
        page = p.page,
        pageSize = p.pageSize,
        pageCount = p.pageCount,
        totalPages = p.totalPages,
        totalCount = p.totalCount,
        elements = p.elements.map(_.toResource)
      )
  }

  implicit val writes: Writes[PagedInvitationRes] = Json.writes[PagedInvitationRes]
}