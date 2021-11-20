package io.xauth.web.action.auth

import java.util.Base64

import io.xauth.service.auth.AuthClientService
import io.xauth.util.Implicits._
import io.xauth.web.action.auth.BasicAuthenticationAction._
import io.xauth.web.action.auth.model.{BasicRequest, ClientCredentials}
import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Results.{Forbidden, Unauthorized}
import play.api.mvc._

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

/**
  * Implements logic to perform a basic authentication.
  */
class BasicAuthenticationAction @Inject()
(
  authClientService: AuthClientService,
  parser: BodyParsers.Default
)(implicit ec: ExecutionContext) extends ActionBuilderImpl(parser) {

  private def unauthorized(m: String) = successful(Unauthorized(Json.obj("message" -> m)))

  private def forbidden(m: String) = successful(Forbidden(Json.obj("message" -> m)))

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {

    request.clientCredentials match {
      case Some(c) =>
        // getting basic authentication info
        authClientService.find(c.id).flatMap {
          case Some(cc) =>
            // verifying client credentials
            if (c.id == cc.id && c.secret.md5 == cc.secret) {
              block(BasicRequest(c, request)) // client credentials are valid
            } else {
              Logger.warn(s"invalid client credentials for ${c.id}:${c.secret}")
              unauthorized("invalid client credentials")
            }
          case None =>
            Logger.warn(s"bad client credentials for ${c.id}:${c.secret}")
            forbidden("bad client credentials")
        }
      case _ => forbidden("client credentials are required")
    }
  }
}

object BasicAuthenticationAction {

  private val AuthHeader = "Authorization"
  private val AuthBasicRegex = "^Basic\\s+(?<auth>.*)".r

  implicit class AuthRequest(r: Request[_]) {
    /**
      * Extracts client credentials from request [[AuthHeader]] header.
      *
      * @return Returns a [[ Some[ClientCredentials] ]] object that contains
      *         client credentials if these are present into the request,
      *         returns [[None]] otherwise.
      */
    def clientCredentials: Option[ClientCredentials] = {
      r.headers.get(AuthHeader) match {
        case s: Some[String] =>
          AuthBasicRegex.findFirstMatchIn(s.value) map { m =>
            val auth = m.group("auth")
            val ss = new String(Base64.getDecoder.decode(auth)).split(":", 2)
            ClientCredentials(ss(0), ss(1))
          }
        case _ => None
      }
    }
  }

}