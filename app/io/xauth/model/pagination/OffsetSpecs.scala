package io.xauth.model.pagination

object OffsetSpecs {
  implicit object MongoOffsetSpec extends OffsetSpec {
    override def offset(page: Int): Int =
      if (page > 0) page - 1 else 0
  }
}
