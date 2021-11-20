package io.xauth.web.action.auth.model

import play.api.mvc.{Request, WrappedRequest}

/**
  * Represents an http basic authentication request with client info.
  */
case class BasicRequest[A]
(
  credentials: ClientCredentials, request: Request[A]
) extends WrappedRequest(request)