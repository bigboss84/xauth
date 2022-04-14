package io.xauth.web.action.auth.model

import io.xauth.service.workspace.model.Workspace
import play.api.mvc.{Request, WrappedRequest}

/**
 * Represents an http basic authentication request with client info
 * and workspace.
 */
case class BasicRequest[A]
(
  credentials: ClientCredentials, workspace: Workspace, request: Request[A]
) extends WrappedRequest(request)