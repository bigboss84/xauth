package io.xauth.web.filter

import javax.inject._
import play.api.http.HttpFilters
import play.api.mvc._
import play.filters.cors.CORSFilter

/**
  * Registers application filters.
  */
@Singleton
class Filters @Inject() (corsFilter: CORSFilter, requestLoggerFilter: RequestLoggerFilter) extends HttpFilters {

  override val filters: Seq[EssentialFilter] = {
    corsFilter :: requestLoggerFilter :: Nil
  }

}