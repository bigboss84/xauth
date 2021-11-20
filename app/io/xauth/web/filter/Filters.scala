package io.xauth.web.filter

import javax.inject._
import play.api.http.HttpFilters
import play.api.mvc._

/**
  * Registers application filters.
  */
@Singleton
class Filters @Inject() (requestLoggerFilter: RequestLoggerFilter) extends HttpFilters {

  override val filters: Seq[EssentialFilter] = {
    requestLoggerFilter :: Nil
  }

}