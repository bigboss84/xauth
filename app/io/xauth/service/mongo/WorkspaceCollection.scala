package io.xauth.service.mongo

import it.russoft.xenum.Enum

/**
 * Defines workspace persistence collections.
 */
object WorkspaceCollection extends Enum {
  type WorkspaceCollection = EnumVal

  val AuthAccessAttempt: WorkspaceCollection = value("w_auth_access_attempt")
  val AuthClient: WorkspaceCollection = value("w_auth_client")
  val AuthCode: WorkspaceCollection = value("w_auth_code")
  val AuthRefreshToken: WorkspaceCollection = value("w_auth_refresh_token")
  val AuthUser: WorkspaceCollection = value("w_auth_user")
  val Invitation: WorkspaceCollection = value("w_invitation")
}