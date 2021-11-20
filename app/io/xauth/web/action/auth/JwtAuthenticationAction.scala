package io.xauth.web.action.auth

import io.xauth.service.auth.AuthUserService
import io.xauth.service.auth.model.AuthRole.AuthRole
import io.xauth.service.auth.model.AuthStatus.Enabled
import io.xauth.util.JwtService
import io.xauth.web.action.auth.JwtAuthenticationAction._
import io.xauth.web.action.auth.model.UserRequest
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Results.{Forbidden, Unauthorized}
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

/**
  * Implements logic to perform authentication through a JWT token
  * in `Authorization` header in request.
  *
  * @param authService User service that have access to the user data.
  */
class JwtAuthenticationAction @Inject()
(
  authService: AuthUserService,
  jwtHelper: JwtService,
  parser: BodyParsers.Default
)(implicit ec: ExecutionContext) extends ActionBuilderImpl(parser) {

  private val logger = Logger(this.getClass)

  private def forbidden(m: String) = successful(Forbidden(Json.obj("message" -> m)))

  private def unauthorized(m: String) = successful(Unauthorized(Json.obj("message" -> m)))

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    request.token.map { token =>
      jwtHelper.decodeToken(token) match {
        case Right((userId, _)) =>
          authService.findById(userId).flatMap {
            // token is still valid
            case Some(user) =>
              if (user.status == Enabled) block(UserRequest(user, request))
              else forbidden(s"account is currently '${user.status}'")
            case None =>
              logger.warn(s"invalid access token for user $userId from ${request.remoteAddress}")
              unauthorized("invalid access token")
          }
        case Left(e) => unauthorized("invalid access token")
      }
    } getOrElse unauthorized("authentication token not found in request header")
  }
}

object JwtAuthenticationAction {

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

  def userAction()(implicit ec: ExecutionContext): ActionRefiner[Request, UserRequest] =
    new ActionRefiner[Request, UserRequest] {
      def executionContext: ExecutionContext = ec

      def refine[A](request: Request[A]): Future[Either[Result, UserRequest[A]]] = successful {
        request match {
          case u: UserRequest[A] => Right(u)
          case _ => Left(Forbidden)
        }
      }
    }

  def roleAction(roles: AuthRole*)(implicit ec: ExecutionContext): ActionFilter[UserRequest] =
    new ActionFilter[UserRequest] {
      def executionContext: ExecutionContext = ec

      def filter[B](request: UserRequest[B]): Future[Option[Result]] = successful {
        if (roles.exists(request.authUser.roles.contains(_))) None
        else Some(Forbidden(Json.obj("message" -> s"access restricted to: ${roles.mkString(", ")}")))
      }
    }

}