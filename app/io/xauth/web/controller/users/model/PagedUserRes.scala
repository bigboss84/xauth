package io.xauth.web.controller.users.model

import io.xauth.model.pagination.PagedData
import io.xauth.service.auth.model.AuthUser
import io.xauth.web.controller.users.model.UserRes.UserResConverter
import play.api.libs.json.{Json, Writes}

case class PagedUserRes
(
  page: Int,
  pageSize: Int,
  pageCount: Int,
  totalPages: Int,
  totalCount: Int,
  elements: Seq[UserRes]
)

object PagedUserRes {
  implicit class PagedUserResConverter(p: PagedData[AuthUser]) {
    def toResource: PagedUserRes =
      PagedUserRes(
        page = p.page,
        pageSize = p.pageSize,
        pageCount = p.pageCount,
        totalPages = p.totalPages,
        totalCount = p.totalCount,
        elements = p.elements.map(_.toResource)
      )
  }

  implicit val writes: Writes[PagedUserRes] = Json.writes[PagedUserRes]
}