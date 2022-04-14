package io.xauth.web.action.auth

import io.xauth.Uuid
import io.xauth.service.workspace.WorkspaceService
import io.xauth.service.workspace.model.Workspace
import io.xauth.service.workspace.model.WorkspaceStatus.Enabled
import io.xauth.web.action.auth.WorkspaceResolverAction._
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.typedmap.TypedKey
import play.api.mvc.Results.{Forbidden, Unauthorized}
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

/**
 * Implements logic to resolve workspace through the `X-Workspace-Id` http header in request.
 *
 * @param service Workspace service that access to the stored data.
 */
class WorkspaceResolverAction @Inject()
(
  service: WorkspaceService,
  parser: BodyParsers.Default
)(implicit ec: ExecutionContext) extends ActionBuilderImpl(parser) {

  private val logger = Logger(this.getClass)

  private def forbidden(m: String) = successful(Forbidden(Json.obj("message" -> m)))

  private def unauthorized(m: String) = successful(Unauthorized(Json.obj("message" -> m)))

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    request.workspaceId map { id =>
      service.findById(id) flatMap {
        case Some(w) =>
          if (w.status == Enabled) {
            logger.info(s"resolved workspace ${id.stringValue}")
            val newRequest = request.addAttr(WorkspaceKey, w)
            block(newRequest)
          }
          else forbidden(s"workspace is currently '${w.status}'")
        case None =>
          unauthorized("invalid workspace id")
      }
    } getOrElse unauthorized("workspace id not found in request header")
  }
}

object WorkspaceResolverAction {

  val WorkspaceKey: TypedKey[Workspace] = TypedKey[Workspace]

  implicit class WsRequest(r: Request[_]) {
    private val WorkspaceHeader = "X-Workspace-Id"
    private val UuidRegex = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$".r

    /**
     * Extracts workspace id from request X-Workspace-Id header.
     *
     * @return Returns a [[Some[Uuid]] object that contains
     *         the workspace identifier, returns [[None]] otherwise.
     */
    def workspaceId: Option[Uuid] =
      r.headers.get(WorkspaceHeader) flatMap { v =>
        UuidRegex.findFirstMatchIn(v) map (m => Uuid(m.group(0)))
      }
  }
}