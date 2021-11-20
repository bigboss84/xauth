package io.xauth.web.controller.init

import io.xauth.JsonSchemaLoader
import io.xauth.model.ContactType.Email
import io.xauth.model.{UserContact, UserInfo}
import io.xauth.service.app.AppSettingService
import io.xauth.service.app.model.AppKey.Init
import io.xauth.service.auth.model.AuthRole.{Admin, User}
import io.xauth.service.auth.model.AuthStatus.Enabled
import io.xauth.service.auth.{AuthClientService, AuthUserService}
import io.xauth.service.mongo.MongoDbClient
import io.xauth.web.controller.init.model.InitReq
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful
import scala.util.{Failure, Success}

/**
  * Handles all administrative actions for users.
  */
@Singleton
class InitController @Inject()
(
  jsonSchema: JsonSchemaLoader,
  mongoDbClient: MongoDbClient,
  appSettingService: AppSettingService,
  authClientService: AuthClientService,
  authUserService: AuthUserService,
  cc: ControllerComponents
)
(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private def configureIndexes(): Unit = {
    //
    // k_auth_user
    //
    mongoDbClient.collections.authUser flatMap {
      // idx.username
      _.indexesManager.ensure(
        Index(
          key = ("username" -> IndexType.Ascending) :: Nil, unique = true
        )
      )
    } onComplete {
      case Success(b) if b => Logger.info("index k_auth_user.username")
      case Failure(e) => Logger.error(s"index k_auth_user.username (${e.getMessage})")
    }

    mongoDbClient.collections.authUser flatMap {
      // idx.contact
      _.indexesManager.ensure(
        Index(key = ("userInfo.contacts.value" -> IndexType.Ascending) :: Nil, unique = true)
      )
    } onComplete {
      case Success(b) if b => Logger.info("index k_auth_user.userInfo.contacts.value")
      case Failure(e) => Logger.error(s"index k_auth_user.userInfo.contacts.value (${e.getMessage})")
    }

    //
    // k_invitation
    //
    mongoDbClient.collections.invitation flatMap {
      // idx.email
      _.indexesManager.ensure(
        Index(key = ("email" -> IndexType.Ascending) :: Nil, unique = true)
      )
    } onComplete {
      case Success(b) if b => Logger.info("index k_invitation.email")
      case Failure(e) => Logger.error(s"index k_invitation.email (${e.getMessage})")
    }
  }

  def configuration: Action[JsValue] = Action.async(parse.json) {
    request =>

      // validating by json schema
      jsonSchema.InitConfigurationPost.validateObj[InitReq](request.body) match {

        // json schema validation has been succeeded
        case s: JsSuccess[InitReq] =>
          val init = s.value

          appSettingService.find(Init) flatMap {

            case Some(setting) =>
              successful(BadRequest(Json.obj("message" -> "application already configured")))

            case _ =>
              // configuring collection indexes
              configureIndexes()

              // client configuration
              val cf = authClientService.create(init.client.id, init.client.secret)

              cf onComplete {
                case Success(u) => Logger.info(s"client configuration")
                case Failure(e) => Logger.error(s"client configuration (${e.getMessage})")
              }

              val email = init.admin.username
              val pass = init.admin.password

              val userInfo = UserInfo(
                firstName = "brahma",
                lastName = "brahma",
                company = "universe",
                contacts = UserContact(Email, email, None, trusted = true) :: Nil
              )

              // admin configuration
              val af = authUserService.save(email, pass, Some("brahma is the creator"), userInfo, Enabled, Nil, User, Admin)

              af onComplete {
                case Success(u) => Logger.info(s"admin configuration")
                case Failure(e) => Logger.error(s"admin configuration (${e.getMessage})")
              }

              cf flatMap {
                case Right(v) => af transformWith {
                  case Success(x) =>
                    appSettingService.save(Init, "1")
                    successful(Ok)
                  case Failure(e) => successful(InternalServerError)
                }
                case Left(m) => successful(InternalServerError)
              }
          }

        // json schema validation has been failed
        case JsError(e) => successful(BadRequest(JsError.toJson(e)))
      }
  }

}
