package io.xauth.web.action.auth

import io.xauth.service.auth.model.AuthRole.AuthRole
import io.xauth.service.workspace.model.Workspace
import io.xauth.web.action.auth.WorkspaceResolverAction.WorkspaceKey
import io.xauth.web.action.auth.model.{BasicRequest, UserRequest, WorkspaceRequest}
import play.api.libs.json.Json
import play.api.mvc.Results.Forbidden
import play.api.mvc.{ActionBuilder, ActionFilter, ActionTransformer, AnyContent, Request, Result}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticationManager @Inject()
(
  workspaceResolverAction: WorkspaceResolverAction,
  basicAuthRefiner: BasicAuthenticationRefiner,
  userAuthRefiner: UserAuthenticationRefiner
)
(implicit ec: ExecutionContext) {

  /*
    - workspaceAction
    - workspaceAction => basicAuthRefiner
    - workspaceAction => userAuthRefiner
   */

  val WorkspaceAction: ActionBuilder[WorkspaceRequest, AnyContent] = workspaceResolverAction andThen toWorkspaceRequest
  val BasicAction: ActionBuilder[BasicRequest, AnyContent] = WorkspaceAction andThen basicAuthRefiner
  val UserAction: ActionBuilder[UserRequest, AnyContent] = WorkspaceAction andThen userAuthRefiner

  def RoleAction(roles: AuthRole*)(implicit ec: ExecutionContext): ActionBuilder[UserRequest, AnyContent] =
    UserAction andThen new ActionFilter[UserRequest] {
      def executionContext: ExecutionContext = ec
      def filter[A](request: UserRequest[A]): Future[Option[Result]] = successful {
        if (roles.exists(request.authUser.roles.contains(_))) None
        else Some(Forbidden(Json.obj("message" -> s"access restricted to: ${roles.mkString(", ")}")))
      }
    }

  /**
    * Transforms Request to WorkspaceRequest.
    */
  private def toWorkspaceRequest(implicit ec: ExecutionContext): ActionTransformer[Request, WorkspaceRequest] =
    new ActionTransformer[Request, WorkspaceRequest] {
      override protected def executionContext: ExecutionContext = ec
      override protected def transform[A](request: Request[A]): Future[WorkspaceRequest[A]] = {
        val workspace: Workspace = request.attrs(WorkspaceKey)
        successful(WorkspaceRequest[A](workspace, request))
      }
    }
}
