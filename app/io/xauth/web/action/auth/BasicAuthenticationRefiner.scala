package io.xauth.web.action.auth

import io.xauth.service.auth.AuthClientService
import io.xauth.service.workspace.model.Workspace
import io.xauth.web.action.auth.model.{BasicRequest, ClientCredentials, WorkspaceRequest}
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.typedmap.TypedKey
import play.api.mvc.Results.{Forbidden, Unauthorized}
import play.api.mvc.{ActionRefiner, BodyParsers, Request, Result}

import java.util.Base64
import javax.inject.Inject
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

/**
  * Implements logic to perform a basic authentication.
  */
class BasicAuthenticationRefiner @Inject()
(
  authClientService: AuthClientService,
  parser: BodyParsers.Default
)(implicit ec: ExecutionContext) extends ActionRefiner[WorkspaceRequest, BasicRequest] {

  private val logger: Logger = Logger(this.getClass)

  private def unauthorized(m: String) = Left(Unauthorized(Json.obj("message" -> m)))

  private def forbidden(m: String) = Left(Forbidden(Json.obj("message" -> m)))

  override protected def executionContext: ExecutionContext = ec

  override protected def refine[A](request: WorkspaceRequest[A]): Future[Either[Result, BasicRequest[A]]] = {
    import BasicAuthenticationRefiner._
    import io.xauth.util.Implicits._

    implicit val workspace: Workspace = request.workspace

    request.clientCredentials match {
      case Some(c) =>
        // getting basic authentication info
        authClientService.find(c.id) map {
          case Some(cc) =>
            // verifying client credentials
            if (c.id == cc.id && c.secret.md5 == cc.secret) {
              Right(BasicRequest(c, workspace, request)) // client credentials are valid
            } else {
              logger.warn(s"invalid client credentials for ${c.id}:${c.secret}")
              unauthorized("invalid client credentials")
            }
          case None =>
            logger.warn(s"bad client credentials for ${c.id}:${c.secret}")
            forbidden("bad client credentials")
        }
      case _ => successful(forbidden("client credentials are required"))
    }
  }
}

object BasicAuthenticationRefiner {

  val ClientCredentialsKey: TypedKey[ClientCredentials] = TypedKey[ClientCredentials]

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