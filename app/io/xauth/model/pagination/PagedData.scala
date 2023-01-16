package io.xauth.model.pagination

case class PagedData[T]
(
  page: Int,
  pageSize: Int,
  pageCount: Int,
  totalPages: Int,
  totalCount: Int,
  elements: Seq[T]
)