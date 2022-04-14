package io.xauth.service.mongo

import it.russoft.xenum.Enum

/**
 * Defines all basic persistence collections.
 */
object SystemCollection extends Enum {
  type SystemCollection = EnumVal

  val App: SystemCollection = value("s_app")
  val Tenant: SystemCollection = value("s_tenant")
  val Workspace: SystemCollection = value("s_workspace")
}
