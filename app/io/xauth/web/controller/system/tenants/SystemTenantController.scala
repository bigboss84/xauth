package io.xauth.web.controller.system.tenants

import io.xauth.service.auth.model.AuthRole.System
import io.xauth.service.tenant.TenantService
import io.xauth.web.action.auth.AuthenticationManager
import io.xauth.web.controller.system.tenants.model.TenantReq
import io.xauth.web.controller.system.tenants.model.TenantRes._
import io.xauth.{JsonSchemaLoader, Uuid}
import play.api.libs.json.Json.{obj, toJson}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful

/**
  * Handles all system administrative actions for tenants.
  */
@Singleton
class SystemTenantController @Inject()
(
  auth: AuthenticationManager,
  jsonSchema: JsonSchemaLoader,
  tenantService: TenantService,
  cc: ControllerComponents
)
(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // system authenticated composed action
  private val systemAction = auth.RoleAction(System)

  def create: Action[JsValue] = systemAction.async(parse.json) { request =>
    // json schema validation
    jsonSchema.SystemTenantPost.validateObj[TenantReq](request.body) match {
      case s: JsSuccess[TenantReq] =>
        val t = s.value

        // check slug existence
        tenantService.findBySlug(t.slug) flatMap {
          case Some(_) => successful(BadRequest(obj("message" -> "tenant already registered with the given slug")))
          case _ => tenantService.save(t.slug, t.description) flatMap {
            t => successful(Created(toJson(t.toResource)))
          }
        }

      // json schema validation has been failed
      case e: JsError => successful(BadRequest(JsError.toJson(e)))
    }
  }

  def find(id: Uuid): Action[AnyContent] = systemAction.async { _ =>
    tenantService.findById(id) map {
      case Some(t) => Ok(Json.toJson(t.toResource))
      case _ => NotFound
    }
  }

  def findAll: Action[AnyContent] = systemAction.async {
    _ =>
      tenantService.findAll map { l =>
        Ok(toJson(l.map(_.toResource)))
      }
  }

  def update(id: Uuid): Action[JsValue] = systemAction.async(parse.json) {
    request =>
      // validating by json schema
      jsonSchema.SystemTenantPut.validateObj[TenantReq](request.body) match {
        // json schema validation has been succeeded
        case s: JsSuccess[TenantReq] =>
          val t = s.value

          // check id existence
          tenantService.findById(id) flatMap {
            case Some(ct) =>

              // check slug existence
              tenantService.findBySlug(t.slug) flatMap {
                case Some(ot) if ot.id != id =>
                  successful(BadRequest(obj("message" -> "slug already belongs to another tenant")))
                case _ =>
                  val tenantToSave = ct.copy(slug = t.slug, description = t.description)
                  tenantService.update(tenantToSave) map {
                    case Some(ut) => Ok(toJson(ut.toResource))
                    case _ => InternalServerError
                  }
              }

            case _ =>
              successful(NotFound(obj("message" -> "tenant not found for the given id")))
          }

        // json schema validation has been failed
        case JsError(e) => successful(BadRequest(JsError.toJson(e)))
      }
  }

  def delete(id: Uuid): Action[AnyContent] = systemAction.async {
    _ =>
      tenantService.findById(id) flatMap {
        case Some(t) if t.workspaceIds.nonEmpty =>
          successful(BadRequest(Json.obj("message" -> "some workspaces refer to the tenant")))
        case Some(_) => tenantService.delete(id) map {
          if (_) NoContent else InternalServerError
        }
        case _ => successful(NotFound)
      }
  }

}
