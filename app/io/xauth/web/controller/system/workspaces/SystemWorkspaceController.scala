package io.xauth.web.controller.system.workspaces

import io.xauth.service.Messaging
import io.xauth.service.auth.model.AuthRole.System
import io.xauth.service.mongo.MongoDbClient
import io.xauth.service.tenant.TenantService
import io.xauth.service.workspace.WorkspaceService
import io.xauth.service.workspace.model.Workspace
import io.xauth.web.action.auth.AuthenticationManager
import io.xauth.web.controller.system.workspaces.model.WorkspaceRes.WorkspaceResourceConverter
import io.xauth.web.controller.system.workspaces.model.{WorkspacePutReq, WorkspaceReq, WorkspaceStatusPatch}
import io.xauth.{JsonSchemaLoader, Uuid}
import play.api.libs.json.Json.{obj, toJson}
import play.api.libs.json.{JsDefined, JsError, JsSuccess, JsUndefined, JsValue, Json}
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful

/**
  * Handles all system administrative actions for workspaces.
  */
@Singleton
class SystemWorkspaceController @Inject()
(
  auth: AuthenticationManager,
  jsonSchema: JsonSchemaLoader,
  tenantService: TenantService,
  workspaceService: WorkspaceService,
  messaging: Messaging,
  mongoDbClient: MongoDbClient,
  cc: ControllerComponents
)
(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // admin authenticated composed action
  private val systemAction = auth.RoleAction(System)

  def create: Action[JsValue] = systemAction.async(parse.json) { request =>
    // json schema validation
    jsonSchema.SystemWorkspacePost.validateObj[WorkspaceReq](request.body) match {
      case s: JsSuccess[WorkspaceReq] =>
        val w = s.value

        // looking up persistence system connection
        mongoDbClient.lookup(w.configuration.dbUri).flatMap {
          case true => // connection verified
            // check tenant existence
            tenantService.findById(w.tenantId) flatMap {
              case Some(_) =>
                // check slug existence
                workspaceService.findBySlug(w.slug) flatMap {
                  case Some(_) => successful(BadRequest(obj("message" -> "workspace already registered with the given slug")))
                  case _ =>
                    workspaceService.save(w.tenantId, w.slug, w.description, w.configuration, w.init) map {
                      w => Created(toJson(w.toResource))
                    }
                }
              case _ => successful(BadRequest(obj("message" -> "no tenant found for the given identifier")))
            }
          case false => // connection failed
            successful(BadRequest(obj("message" -> "database lookup failed for the given uri")))
        }

      // json schema validation has been failed
      case e: JsError => successful(BadRequest(JsError.toJson(e)))
    }
  }

  def find(id: Uuid): Action[AnyContent] = systemAction.async { _ =>
    workspaceService.findById(id) map {
      case Some(t) => Ok(Json.toJson(t.toResource))
      case _ => NotFound
    }
  }

  def findAll: Action[AnyContent] = systemAction.async { _ =>
    workspaceService.findAll map { l =>
      Ok(toJson(l.map(_.toResource)))
    }
  }

  def update(id: Uuid): Action[JsValue] = systemAction.async(parse.json) {
    request =>
      // validating by json schema
      jsonSchema.SystemWorkspacePut.validateObj[WorkspacePutReq](request.body) match {
        // json schema validation has been succeeded
        case s: JsSuccess[WorkspacePutReq] =>
          val w = s.value

          // check id existence
          workspaceService.findById(id) flatMap {
            case Some(cw) =>
              // check database uri immutability
              if (w.configuration.dbUri != cw.configuration.dbUri)
                successful(BadRequest(obj("message" -> "database uri cannot be changed")))
              // check slug existence
              else workspaceService.findBySlug(w.slug) flatMap {
                case Some(ow) if ow.id != id =>
                  successful(BadRequest(obj("message" -> "slug already belongs to another workspace")))
                case _ =>
                  val workspaceToSave = cw.copy(slug = w.slug, description = w.description, configuration = w.configuration)
                  workspaceService.update(workspaceToSave) map {
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

  def delete(id: Uuid): Action[AnyContent] = systemAction.async { _ =>
    workspaceService.findById(id) flatMap {
      case Some(w) => workspaceService.delete(w) map {
        if (_) NoContent else InternalServerError
      }
      case _ => successful(NotFound)
    }
  }

  def patchStatus(id: Uuid): Action[JsValue] = systemAction.async(parse.json) { r =>
    // validating by json schema
    jsonSchema.SystemWorkspaceStatusPatch.validateObj[WorkspaceStatusPatch](r.body) match {
      // json schema validation has been succeeded
      case s: JsSuccess[WorkspaceStatusPatch] =>

        workspaceService.findById(id) flatMap {
          // saving new workspace status
          case Some(w) => workspaceService.updateStatus(w, s.value.status) map {
            case Some(u) => Ok(toJson(WorkspaceStatusPatch(u)))
            case _ => NotFound
          }
          case _ => successful(NotFound)
        }

      // json schema validation has been failed
      case JsError(e) => successful(BadRequest(JsError.toJson(e)))
    }
  }

  def messagingTest: Action[JsValue] = systemAction.async(parse.json) { r =>
    r.body \ "email" match {
      case JsDefined(email) =>
        implicit val w: Workspace = r.workspace
        messaging.mailer.send(email.as[String], "messaging-test", s"Messaging test for workspace: ${w.slug}")
        successful(Accepted)
      case _ => successful(BadRequest)
    }
  }

}
