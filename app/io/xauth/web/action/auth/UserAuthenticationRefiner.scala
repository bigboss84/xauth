package io.xauth.web.action.auth

import io.xauth.service.auth.AuthUserService
import io.xauth.service.auth.model.AuthStatus.Enabled
import io.xauth.service.auth.model.AuthUser
import io.xauth.service.workspace.model.Workspace
import io.xauth.util.JwtService
import io.xauth.web.action.auth.UserAuthenticationRefiner.AuthRequest
import io.xauth.web.action.auth.model.{UserRequest, WorkspaceRequest}
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.typedmap.TypedKey
import play.api.mvc.Results.{BadRequest, Forbidden, Unauthorized}
import play.api.mvc.{ActionRefiner, Request, Result}

import javax.inject.Inject
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

/**
  * Implements logic to perform authentication through a JWT token
  * in `Authorization` header in request.
  *
  * @param authService User service that have access to the user data.
  */
class UserAuthenticationRefiner @Inject()
(
  authService: AuthUserService,
  jwtHelper: JwtService
)
(implicit ec: ExecutionContext) extends ActionRefiner[WorkspaceRequest, UserRequest] {

  private val logger: Logger = Logger(this.getClass)

  private def badRequest(m: String) = BadRequest(Json.obj("message" -> m))

  private def forbidden(m: String) = Forbidden(Json.obj("message" -> m))

  private def unauthorized(m: String) = Unauthorized(Json.obj("message" -> m))

  override protected def executionContext: ExecutionContext = ec

  override protected def refine[A](request: WorkspaceRequest[A]): Future[Either[Result, UserRequest[A]]] = {
    implicit val workspace: Workspace = request.workspace

    request.token.map { token =>
      jwtHelper.decodeToken(token) match {
        case Right((userId, workspaceId, _)) =>
          if (workspace.id == workspaceId)
            authService.findById(userId) map {
              // token is still valid
              case Some(user) =>
                if (user.status == Enabled) Right(UserRequest(user, workspace, request))
                else Left(forbidden(s"account is currently '${user.status}'"))
              case None =>
                logger.warn(s"invalid access token for user $userId from ${request.remoteAddress}")
                Left(unauthorized("invalid access token"))
            }
          else successful(Left(badRequest("inconsistent workspace request")))
        case Left(_) => successful(Left(unauthorized("invalid access token")))
      }
    } getOrElse successful(Left(unauthorized("authentication token not found in request header")))
  }
}

object UserAuthenticationRefiner {
  val UserKey: TypedKey[AuthUser] = TypedKey[AuthUser]

  implicit class AuthRequest(r: Request[_]) {
    private val AuthHeader = "Authorization"
    private val AuthBearerRegex = "^Bearer\\s+(?<auth>.*)".r

    /**
      * Extracts bearer token from request [[AuthHeader]] header.
      *
      * @return Returns a [[Some[String]] object that contains
      *         the JWT bearer token if it is present into the
      *         request, returns [[None]] otherwise.
      */
    def token: Option[String] =
      r.headers.get(AuthHeader) match {
        case s: Some[String] =>
          AuthBearerRegex.findFirstMatchIn(s.value) map {
            _.group("auth")
          }
        case _ => None
      }
  }
}
