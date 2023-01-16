package io.xauth.model.pagination

trait OffsetSpec {
  def offset(page: Int): Int
}