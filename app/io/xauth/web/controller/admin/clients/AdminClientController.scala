package io.xauth.web.controller.admin.clients

import io.xauth.JsonSchemaLoader
import io.xauth.service.auth.AuthClientService
import io.xauth.service.auth.model.AuthRole.Admin
import io.xauth.web.action.auth.JwtAuthenticationAction
import io.xauth.web.action.auth.JwtAuthenticationAction.{roleAction, userAction}
import io.xauth.web.controller.admin.clients.model.ClientRes._
import io.xauth.web.controller.admin.clients.model.{ClientPutReq, ClientReq}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json.{obj, toJson}
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful

/**
  * Handles all administrative actions for users.
  */
@Singleton
class AdminClientController @Inject()
(
  jwtAuthAction: JwtAuthenticationAction,
  jsonSchema: JsonSchemaLoader,
  authClientService: AuthClientService,
  cc: ControllerComponents
)
(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // admin authenticated composed action
  private val adminAction =
    jwtAuthAction andThen userAction andThen roleAction(Admin)

  def create: Action[JsValue] = adminAction.async(parse.json) {
    request =>
      // validating by json schema
      jsonSchema.AdminClientPost.validateObj[ClientReq](request.body) match {
        // json schema validation has been succeeded
        case s: JsSuccess[ClientReq] =>
          val client = s.value

          authClientService.find(client.id) flatMap {
            case Some(_) => successful(
              BadRequest(obj("message" -> s"Client '${client.id}' already exists"))
            )
            case _ =>
              authClientService.create(client.id, client.secret) map {
                case Left(e) => InternalServerError(obj("message" -> "An internal error has occurred"))
                case Right(c) => Created(toJson(c.toResource))
              }
          }

        // json schema validation has been failed
        case JsError(e) => successful(BadRequest(JsError.toJson(e)))
      }
  }

  def findAll: Action[AnyContent] = adminAction.async {
    _ =>
      authClientService.findAll map { l =>
        Ok(toJson(l.map(_.toResource)))
      }
  }

  def find(id: String): Action[AnyContent] = adminAction.async {
    _ =>
      authClientService.find(id) map {
        case Some(c) => Ok(toJson(c.toResource))
        case _ => NotFound
      }
  }

  def update(id: String): Action[JsValue] = adminAction.async(parse.json) {
    request =>
      // validating by json schema
      jsonSchema.AdminClientPut.validateObj[ClientPutReq](request.body) match {
        // json schema validation has been succeeded
        case s: JsSuccess[ClientPutReq] =>
          val client = s.value

          if (id == client.id)
            authClientService.find(id) flatMap {
              case Some(v) => authClientService.update(v.copy(secret = client.secret)) map {
                case Some(vv) => Ok(toJson(vv.toResource))
                case _ => InternalServerError
              }
              case _ => successful(NotFound)
            }
          else successful {
            BadRequest(obj("message" -> "inconsistent update request: different identifiers"))
          }

        // json schema validation has been failed
        case JsError(e) => successful(BadRequest(JsError.toJson(e)))
      }
  }

  def delete(id: String): Action[AnyContent] = adminAction.async {
    request =>
      authClientService.find(id) flatMap {
        case Some(c) => authClientService.delete(id) map {
          if (_) NoContent else InternalServerError
        }
        case _ => successful(NotFound)
      }
  }

}