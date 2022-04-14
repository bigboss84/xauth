package io.xauth.web.action.auth.model

import io.xauth.service.workspace.model.Workspace
import play.api.mvc.{Request, WrappedRequest}

/**
  * Represents a request with workspace info.
  */
case class WorkspaceRequest[A]
(
  workspace: Workspace, request: Request[A]
) extends WrappedRequest(request)