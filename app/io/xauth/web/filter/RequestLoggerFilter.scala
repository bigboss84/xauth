package io.xauth.web.filter

import akka.stream.Materializer
import javax.inject._
import play.api.Logger
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RequestLoggerFilter @Inject()
(
  implicit override val mat: Materializer,
  implicit val exec: ExecutionContext
) extends Filter {

  override def apply(nextFilter: RequestHeader => Future[Result])
                    (requestHeader: RequestHeader): Future[Result] = {

    val startTime = System.currentTimeMillis

    nextFilter(requestHeader) map { result =>

      val endTime = System.currentTimeMillis
      val requestTime = endTime - startTime

      Logger.info(s"request: method=${requestHeader.method}, uri=${requestHeader.uri}, time=$requestTime, status=${result.header.status}")

      result.withHeaders("Request-Time" -> requestTime.toString)
    }
  }

}