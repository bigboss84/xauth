package io.xauth.web.controller.auth

import akka.actor.ActorRef
import io.xauth.JsonSchemaLoader
import io.xauth.config.ApplicationConfiguration
import io.xauth.model.ContactType.Email
import io.xauth.service.auth.model.AuthCodeType
import io.xauth.service.auth.model.AuthStatus.{Blocked, Enabled}
import io.xauth.service.auth.model.AuthUser.checkWithSalt
import io.xauth.service.auth.{AuthAccessAttemptService, AuthCodeService, AuthRefreshTokenService, AuthUserService}
import io.xauth.util.Implicits._
import io.xauth.util.{JwkService, JwtService}
import io.xauth.web.action.auth.JwtAuthenticationAction._
import io.xauth.web.action.auth.model.{BasicRequest, UserRequest}
import io.xauth.web.action.auth.{BasicAuthenticationAction, JwtAuthenticationAction}
import io.xauth.web.controller.auth.model.TokenStatus.{Invalid, TokenStatus, Valid}
import io.xauth.web.controller.auth.model._
import io.xauth.web.controller.users.model.UserRes._
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.Date
import javax.inject._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful
import scala.util.{Failure, Success}

@Singleton
class AuthController @Inject()
(
  authService: AuthUserService,
  authAccessAttemptService: AuthAccessAttemptService,
  authCodeService: AuthCodeService,
  authRefreshTokenService: AuthRefreshTokenService,
  basicAuthAction: BasicAuthenticationAction,
  jwtAuthAction: JwtAuthenticationAction,
  jwtHelper: JwtService,
  jwkService: JwkService,
  jsonSchema: JsonSchemaLoader,
  conf: ApplicationConfiguration,
  @Named("account-deletion") accountDeletionActor: ActorRef,
  @Named("password-reset") passwordResetActor: ActorRef,
  @Named("contact-trust") contactTrustActor: ActorRef,
  cc: ControllerComponents
)
(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val logger = Logger(this.getClass)

  private val jwtUserAction = jwtAuthAction andThen userAction

  private def status(s: Status)(m: String) = s(Json.obj("message" -> m))

  private def forbidden(m: String) = status(Forbidden)(m)

  private def badRequest(m: String) = status(BadRequest)(m)

  def jwk: Action[AnyContent] = Action.async {
    r =>
      def keys = (ks: List[JsValue]) => Json.obj("keys" -> JsArray(ks))

      jwkService.jwk match {
        case Some(jwk) => successful(Ok(keys(Json.parse(jwk.toJSONString) :: Nil)))
        case _ => successful(badRequest("No JWKs available"))
      }
  }

  def token: Action[JsValue] = basicAuthAction.async(parse.json) {
    r =>
      val request = r.asInstanceOf[BasicRequest[JsValue]]

      // json schema validation
      jsonSchema.TokenPost.validate(request.body) match {
        case s: JsSuccess[JsValue] =>
          val body = s.value.as[TokenReq]

          authService.findByUsername(body.username).map {
            case Some(authUser) =>
              // verifying current user status
              if (authUser.status == Enabled) {
                // check user and password
                if (checkWithSalt(authUser.salt, body.password, authUser.password)) {
                  // cleaning user authentication attempts
                  authAccessAttemptService.deleteByUsername(body.username)

                  // basic user information to store into token
                  // todo: store token date
                  // creating new bearer token

                  val tokenRes = TokenRes(
                    jwtHelper.jwtTokenType,
                    jwtHelper.createToken(authUser.id, authUser.roles, authUser.applications),
                    jwtHelper.jwtExpirationInMinutes,
                    jwtHelper.createRefreshToken
                  )

                  logger.info(s"created access-token: ${tokenRes.accessToken}")
                  logger.info(s"created refresh-token: ${tokenRes.refreshToken}")

                  // saving refresh token
                  authRefreshTokenService.save(
                    tokenRes.refreshToken,
                    request.credentials.id,
                    authUser.id
                  )

                  // todo: store access log
                  // returning access token to the client
                  Ok(Json.toJson(tokenRes))
                } else {
                  // wrong password
                  // storing login attempt
                  authAccessAttemptService.save(body.username, request.credentials.id, request.remoteAddress).map { a =>
                    authAccessAttemptService.findAllByUsername(body.username) onComplete {
                      case Success(n) if n >= conf.maxLoginAttempts =>
                        authService.updateStatusByUsername(body.username, Blocked)
                      case Success(n) =>
                        Logger.warn(s"access attempt number $n for user ${body.username}")
                      case Failure(e) =>
                        Logger.error(s"error counting access attempts: ${e.getMessage}")
                    }
                  }

                  forbidden("invalid user credentials")
                }
              }
              // user is not enabled
              else forbidden(s"account is currently '${authUser.status.value}'")

            case None => forbidden("invalid user credentials")
          }

        // bad request body
        case JsError(e) => successful(BadRequest(JsError.toJson(e)))
      }
  }

  def check: Action[AnyContent] = Action { request =>
    def status = (s: TokenStatus) => Json.obj("tokenStatus" -> s)

    request.token.map {
      token =>
        jwtHelper.decodeToken(token).fold(
          _ => Ok(status(Invalid)),
          _ => Ok(status(Valid))
        )
    } getOrElse BadRequest
  }

  def refresh: Action[JsValue] = basicAuthAction.async(parse.json) {
    r =>
      val request = r.asInstanceOf[BasicRequest[JsValue]]

      // json schema validation
      jsonSchema.RefreshPost.validate(request.body) match {
        case s: JsSuccess[JsValue] =>
          val body = s.value.as[RefreshReq]

          authRefreshTokenService.find(body.refreshToken).map {
            case Some(authRefreshToken) =>
              // checking client id
              if (request.credentials.id == authRefreshToken.clientId) {
                // checking expiration date
                val now = Date.from(LocalDateTime.now.toInstant(UTC))
                // verifying existence of refresh token
                if (authRefreshToken.expiresAt.after(now)) {
                  // verifying current user status
                  authService.findById(authRefreshToken.userId).map {
                    case Some(authUser) =>
                      // verifying if user is currently enabled
                      if (authUser.status == Enabled) {
                        // generating new access token
                        Ok(Json.toJson(TokenRes(
                          jwtHelper.jwtTokenType,
                          jwtHelper.createToken(authRefreshToken.userId, authUser.roles, authUser.applications),
                          jwtHelper.jwtExpirationInMinutes,
                          authRefreshToken.token
                        )))
                      }
                      // user is not enabled
                      else forbidden(s"account is currently '${authUser.status.value}'")
                    // user not exists
                    case None => forbidden("user not found")
                  }
                } else {
                  // refresh token has been expired, deleting...
                  authRefreshTokenService.delete(authRefreshToken.token)
                  successful(forbidden("refresh token has been expired"))
                }
              } else {
                // refresh token not belongs to the client
                successful(forbidden("wrong refresh token"))
              }
            case None => successful(forbidden("invalid refresh token"))
          }.flatten

        // bad request body
        case JsError(e) => successful(BadRequest(JsError.toJson(e)))
      }
  }

  def passwordForgotten: Action[JsValue] = Action.async(parse.json) { r =>
    // json schema validation
    jsonSchema.PasswordForgotten.validateObj[PasswordForgottenReq](r.body) match {
      case s: JsSuccess[PasswordForgottenReq] =>
        val reset = s.value
        authService.findByUsername(reset.username) map {
          case Some(u) =>
            if (u.status == Enabled) {
              // account is enabled and the user can reset password
              u.userInfo.contacts.find(c => c.`type` == Email && c.trusted) match {
                case Some(email) =>
                  // generating reset code
                  passwordResetActor ! u
                  status(Accepted)(s"reset code will send to '${email.value.mask}'")
                case _ =>
                  status(BadRequest)("no trusted email was found")
              }
            }
            // account is not enabled and the user can not reset password
            else status(BadRequest)("account is not enabled")
          // user was not found
          case _ =>
            status(NotFound)(s"user '${reset.username}' not found")
        }
      case JsError(e) => successful(BadRequest(JsError.toJson(e)))
    }
  }

  def passwordReset: Action[JsValue] = Action.async(parse.json) { r =>
    // json schema validation
    jsonSchema.PasswordReset.validateObj[PasswordResetReq](r.body) match {
      case s: JsSuccess[PasswordResetReq] =>
        val reset = s.value
        if (reset.password == reset.passwordCheck) {
          authCodeService.find(reset.code) flatMap {
            case Some(authCode) if authCode.`type` == AuthCodeType.PasswordReset =>
              authService.resetPassword(authCode.referenceId, reset.password) map { b =>
                if (b) {
                  authCodeService.delete(authCode.code)
                  Ok
                }
                else status(InternalServerError)("an error has been occurred, please contact us")
              }
            case None =>
              successful(status(NotFound)(s"reset code '${reset.code}' was not found"))
          }
        }
        // password and password-check fields are not equal to each other
        else successful(
          status(BadRequest)(
            "password and passwordCheck fields are not equal to each other"
          )
        )
      case JsError(e) => successful(BadRequest(JsError.toJson(e)))
    }
  }

  /**
    * Performs a user account deletion request sending confirmation code.
    */
  def accountDeleteRequest: Action[AnyContent] = jwtUserAction.async { r =>
    // here the user must be enabled because
    r.authUser.userInfo.contacts.find(c => c.`type` == Email && c.trusted) match {
      case Some(email) =>
        // generating reset code
        accountDeletionActor ! r.authUser
        successful(status(Accepted)(s"deletion code will send to '${email.value.mask}'"))
      case _ =>
        successful(status(BadRequest)("no trusted email was found"))
    }
  }

  /**
    * Deletes definitively the user account.
    */
  def accountDeleteConfirmation: Action[JsValue] = jwtUserAction.async(parse.json) { r =>
    // here the user must be enabled because
    // json schema validation
    jsonSchema.AuthAccountDeleteConfirmationPost.validateObj[AccountDeleteConfirmationReq](r.body) match {
      case s: JsSuccess[AccountDeleteConfirmationReq] =>
        val delete = s.value
        authCodeService.find(delete.code) flatMap {
          case Some(authCode) if authCode.`type` == AuthCodeType.Deletion =>
            authService.delete(authCode.referenceId) map { b =>
              if (b) {
                authCodeService.delete(authCode.code)
                NoContent
              }
              else status(InternalServerError)("an error has been occurred, please contact us")
            }
          case None =>
            successful(status(NotFound)(s"reset code '${delete.code}' was not found"))
        }
      case JsError(e) => successful(BadRequest(JsError.toJson(e)))
    }
  }

  def user: Action[AnyContent] = jwtAuthAction.async { r =>
    val request = r.asInstanceOf[UserRequest[_]]

    authService.findById(request.authUser.id).map {
      case Some(u) => Ok(Json.toJson(u.toResource))
      case _ => NotFound
    }
  }

  def activation: Action[JsValue] = Action.async(parse.json) { request =>
    // json schema validation
    jsonSchema.AuthActivationPost.validate(request.body) match {
      case JsSuccess(value, _) =>
        val code = (value \\ "code").head.asInstanceOf[JsString].value
        authCodeService.find(code).map {
          case Some(c) =>
            val now = Date.from(LocalDateTime.now.toInstant(UTC))
            if (c.expiresAt.after(now)) {
              authService.activate(code).map { b =>
                Ok(Json.obj("result" -> (if (b) "ACTIVATED" else "NOT_ACTIVATED")))
              }
            } else successful(badRequest("activation code has been expired"))
          case _ => successful(NotFound)
        }.flatten
      // bad request body
      case JsError(e) => successful(BadRequest(JsError.toJson(e)))
    }
  }

  /**
    * Accepts trust requests for the user contact.
    */
  def contactTrust: Action[JsValue] = jwtAuthAction.async(parse.json) { r =>
    // json schema validation
    jsonSchema.AuthContactTrustPost.validateObj[ContactTrustReq](r.body) match {
      case s: JsSuccess[ContactTrustReq] =>
        val request = r.asInstanceOf[UserRequest[_]]
        val u = request.authUser
        u.userInfo.contacts.find(_.value == s.value.contact) match {
          case Some(c) =>
            if (c.trusted) successful(BadRequest(Json.obj("message" -> "contact is already trusted")))
            else {
              // generating trust code
              contactTrustActor ! (u, c)
              successful {
                status(Accepted)(s"trust code will send to '${c.value.mask}'")
              }
            }
          case None => successful(NotFound)
        }
      // bad request body
      case JsError(e) => successful(BadRequest(JsError.toJson(e)))
    }
  }

  /**
    * Trusts requests for mark user contact as trusted.
    */
  def contactActivation: Action[JsValue] = jwtAuthAction.async(parse.json) { r =>
    // json schema validation
    jsonSchema.AuthContactActivationPost.validate(r.body) match {
      case JsSuccess(value, _) =>
        val code = (value \\ "code").head.asInstanceOf[JsString].value
        authCodeService.find(code).map {
          case Some(c) =>
            val now = Date.from(LocalDateTime.now.toInstant(UTC))
            if (c.expiresAt.after(now)) {
              authService.trustContact(code).map { b =>
                Ok(Json.obj("result" -> (if (b) "TRUSTED" else "NOT_TRUSTED")))
              }
            } else successful(badRequest("trust code has been expired"))
          case _ => successful(NotFound)
        }.flatten
      // bad request body
      case JsError(e) => successful(BadRequest(JsError.toJson(e)))
    }
  }

}
