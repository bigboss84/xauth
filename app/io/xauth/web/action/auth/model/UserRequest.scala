package io.xauth.web.action.auth.model

import io.xauth.service.auth.model.AuthUser
import io.xauth.service.workspace.model.Workspace
import play.api.mvc.{Request, WrappedRequest}

/**
  * Represents a user authentication request with user info.
  */
case class UserRequest[A]
(
  authUser: AuthUser, workspace: Workspace, request: Request[A]
) extends WrappedRequest(request)