package io.xauth.web.handler

import play.api.http.HttpErrorHandler
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result}

import javax.inject.Singleton
import scala.concurrent.Future
import scala.concurrent.Future.successful

/**
  * Custom http error handler
  */
@Singleton
class ErrorHandler extends HttpErrorHandler {

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    val m = statusCode match {
      case BAD_REQUEST => "bad request"
      case FORBIDDEN => "resource is forbidden"
      case NOT_FOUND => "resource not found"
      case clientError if statusCode >= 400 && statusCode < 500 => message
      case nonClientError =>
        throw new IllegalArgumentException(s"non client error status code $statusCode: $message")
    }

    successful(new Status(statusCode)(
      Json.toJson(Map("method" -> request.method, "uri" -> request.uri)))
    )
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    val x = Map("message" -> exception.getMessage)
    successful(InternalServerError(Json.toJson("" -> "")))
  }
}
