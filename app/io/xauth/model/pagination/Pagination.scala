package io.xauth.model.pagination

import play.api.mvc.Request

class Pagination(val page: Int, val size: Int, val offset: Int) {
  def paginate[T](s: Seq[T], totalCount: Int): PagedData[T] = {
    PagedData(
      page = page,
      pageSize = size, pageCount = s.size,
      totalPages = if (totalCount < size) 1 else Math.round(totalCount / size), totalCount = totalCount,
      elements = s
    )
  }
}

object Pagination {
  private val DefaultPage: Int = 1
  private val DefaultPageSize: Int = 10

  def apply(page: Int, size: Int)(implicit spec: OffsetSpec): Pagination = {
    new Pagination(
      page,
      size,
      (page - 1) * spec.offset(page) * size
    )
  }

  def fromRequest(r: Request[_])(implicit spec: OffsetSpec): Pagination = {
    val page = r
      .getQueryString("page")
      .map(_.toInt)
      .filter(_ > 0) getOrElse DefaultPage
    val size = r
      .getQueryString("size")
      .map(_.toInt)
      .filter(_ > 0) getOrElse DefaultPageSize
    apply(page, size)
  }

}