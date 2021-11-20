package io.xauth.web.action.auth.model

import io.xauth.service.auth.model.AuthUser
import play.api.mvc.{Request, WrappedRequest}

/**
  * Represents a user authentication request with user info.
  */
case class UserRequest[A]
(
  authUser: AuthUser, request: Request[A]
) extends WrappedRequest(request)