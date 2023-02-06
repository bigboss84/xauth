package io.xauth.web.controller.init

import io.xauth.JsonSchemaLoader
import io.xauth.model.ContactType.Email
import io.xauth.model.{UserContact, UserInfo}
import io.xauth.service.app.AppSettingService
import io.xauth.service.app.model.AppKey.Init
import io.xauth.service.auth.model.AuthRole.{Admin, System, User}
import io.xauth.service.auth.model.AuthStatus.Enabled
import io.xauth.service.auth.{AuthClientService, AuthUserService}
import io.xauth.service.mongo.MongoDbClient
import io.xauth.service.tenant.TenantService
import io.xauth.service.workspace.WorkspaceService
import io.xauth.web.controller.init.model.InitReq
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful

/**
 * Handles all administrative actions for users.
 */
@Singleton
class InitController @Inject()
(
  jsonSchema: JsonSchemaLoader,
  mongoDbClient: MongoDbClient,
  appSettingService: AppSettingService,
  tenantService: TenantService,
  workspaceService: WorkspaceService,
  authClientService: AuthClientService,
  authUserService: AuthUserService,
  cc: ControllerComponents
)
(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val logger: Logger = Logger(this.getClass)

  /**
   * Initializes application creating system database.
   */
  def configuration: Action[JsValue] = Action.async(parse.json) {
    request =>

      // validating by json schema
      jsonSchema.InitConfigurationPost.validateObj[InitReq](request.body) match {

        // json schema validation has been succeeded
        case s: JsSuccess[InitReq] =>
          val o = s.value

          appSettingService.find(Init) flatMap {

            case Some(_) =>
              successful(BadRequest(Json.obj("message" -> "application already configured")))

            case _ =>
              val email = o.init.admin.username
              val pass = o.init.admin.password

              val userInfo = UserInfo(
                firstName = "unknown", // todo: read from app conf
                lastName = "unknown", // todo: read from app conf
                company = "this", // todo: read from app conf
                contacts = UserContact(Email, email, None, trusted = true) :: Nil
              )

              val result = for {
                // creating root tenant
                _ <- tenantService.createSystemTenant
                // creating root workspace
                w <- workspaceService.createSystemWorkspace(o.configuration.applications)
                // configuring client
                _ <- authClientService.create(o.init.client.id, o.init.client.secret)(w)
                // configuring system admin user
                _ <- authUserService.save(email, pass, Some("system administrator"), None, userInfo, Enabled, Nil, User, Admin, System)(w)
                // saving new application state
                _ <- appSettingService.save(Init, "1")
              } yield Ok

              result.recover {
                case e: Throwable =>
                  logger.error(s"initialization failure: ${e.getMessage}")
                  InternalServerError
              }
          }

        // json schema validation has been failed
        case JsError(e) => successful(BadRequest(JsError.toJson(e)))
      }
  }

}
