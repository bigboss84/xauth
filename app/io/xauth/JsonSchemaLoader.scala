package io.xauth

import cats.data.Validated
import io.circe.schema.Schema
import io.xauth.config.ApplicationConfiguration
import play.api.Environment
import play.api.Mode.Test
import play.api.libs.json._

import javax.inject.{Inject, Singleton}
import scala.io.Source.{fromFile, fromResource}

/**
  * Route json schemas
  */
@Singleton
class JsonSchemaLoader @Inject()(implicit val conf: ApplicationConfiguration, implicit val env: Environment) {

  // app
  val Common: JsonSchema =
    new JsonSchema("public/schema/v1/common.json")

  val TimeZone: JsonSchema =
    new JsonSchema("public/schema/v1/timezone.json")

  // Init
  val InitConfigurationPost: JsonSchema =
    new JsonSchema("public/schema/v1/init/configuration.post.req.json")

  // System: Tenants
  val SystemTenantPost: JsonSchema =
    new JsonSchema("public/schema/v1/system/tenants/tenant.post.req.json")

  val SystemTenantPostRes: JsonSchema =
    new JsonSchema("public/schema/v1/system/tenants/tenant.post.res.json")

  val SystemTenantsGetRes: JsonSchema =
    new JsonSchema("public/schema/v1/system/tenants/tenants.get.res.json")

  val SystemTenantGetRes: JsonSchema =
    new JsonSchema("public/schema/v1/system/tenants/tenant.get.res.json")

  val SystemTenantPut: JsonSchema =
    new JsonSchema("public/schema/v1/system/tenants/tenant.put.req.json")

  val SystemTenantPutRes: JsonSchema =
    new JsonSchema("public/schema/v1/system/tenants/tenant.put.res.json")

  // System: Workspace
  val SystemWorkspaceCommon: JsonSchema =
    new JsonSchema("public/schema/v1/system/workspaces/common.json")

  val SystemWorkspacePost: JsonSchema =
    new JsonSchema("public/schema/v1/system/workspaces/workspace.post.req.json")

  val SystemWorkspacePostRes: JsonSchema =
    new JsonSchema("public/schema/v1/system/workspaces/workspace.post.res.json")

  val SystemWorkspacesGetRes: JsonSchema =
    new JsonSchema("public/schema/v1/system/workspaces/workspaces.get.res.json")

  val SystemWorkspaceGetRes: JsonSchema =
    new JsonSchema("public/schema/v1/system/workspaces/workspace.get.res.json")

  val SystemWorkspacePut: JsonSchema =
    new JsonSchema("public/schema/v1/system/workspaces/workspace.put.req.json")

  val SystemWorkspacePutRes: JsonSchema =
    new JsonSchema("public/schema/v1/system/workspaces/workspace.put.res.json")

  val SystemWorkspaceStatusPatch: JsonSchema =
    new JsonSchema("public/schema/v1/system/workspaces/status.patch.req.json")

  val SystemWorkspaceStatusPatchRes: JsonSchema =
    new JsonSchema("public/schema/v1/system/workspaces/status.patch.res.json")

  // Admin: Users, Clients and Application
  val AdminUsersPost: JsonSchema =
    new JsonSchema("public/schema/v1/admin/users/users.post.req.json")

  val AdminUserRolesPatch: JsonSchema =
    new JsonSchema("public/schema/v1/admin/users/roles.patch.req.json")

  val AdminUserRolesPatchRes: JsonSchema =
    new JsonSchema("public/schema/v1/admin/users/roles.patch.res.json")

  val AdminUserStatusPatch: JsonSchema =
    new JsonSchema("public/schema/v1/admin/users/status.patch.req.json")

  val AdminUserStatusPatchRes: JsonSchema =
    new JsonSchema("public/schema/v1/admin/users/status.patch.res.json")

  val AdminUserApplicationsPatch: JsonSchema =
    new JsonSchema("public/schema/v1/admin/users/applications.patch.req.json")

  val AdminUserApplicationsPatchRes: JsonSchema =
    new JsonSchema("public/schema/v1/admin/users/applications.patch.res.json")

  val AdminUserSearch: JsonSchema =
    new JsonSchema("public/schema/v1/admin/users/search.post.req.json")

  val AdminAccountTrustPostReq: JsonSchema =
    new JsonSchema("public/schema/v1/admin/account-trust.post.req.json")

  val AdminClientCommon: JsonSchema =
    new JsonSchema("public/schema/v1/admin/clients/common.json")

  val AdminClientPost: JsonSchema =
    new JsonSchema("public/schema/v1/admin/clients/client.post.req.json")

  val AdminClientPostRes: JsonSchema =
    new JsonSchema("public/schema/v1/admin/clients/client.post.res.json")

  val AdminClientPut: JsonSchema =
    new JsonSchema("public/schema/v1/admin/clients/client.put.req.json")

  val AdminClientPutRes: JsonSchema =
    new JsonSchema("public/schema/v1/admin/clients/client.put.res.json")

  val AdminRolePost: JsonSchema =
    new JsonSchema("public/schema/v1/admin/roles/roles.post.req.json")

  val AdminRolePostRes: JsonSchema =
    new JsonSchema("public/schema/v1/admin/roles/roles.post.res.json")

  val AdminSystemApplicationsPatch: JsonSchema =
    new JsonSchema("public/schema/v1/admin/applications/applications.patch.req.json")

  val AdminSystemApplicationsPatchRes: JsonSchema =
    new JsonSchema("public/schema/v1/admin/applications/applications.patch.res.json")

  // Owner
  val OwnerUserApplicationsPatch: JsonSchema =
    new JsonSchema("public/schema/v1/owner/users/applications.patch.req.json")

  val OwnerUserApplicationsPatchRes: JsonSchema =
    new JsonSchema("public/schema/v1/owner/users/applications.patch.req.json")

  // invitations/
  val InvitationsCommon: JsonSchema =
    new JsonSchema("public/schema/v1/invitations/common.json")

  val InvitationsPost: JsonSchema =
    new JsonSchema("public/schema/v1/invitations/invitations.post.req.json")

  val InvitationsPostRes: JsonSchema =
    new JsonSchema("public/schema/v1/invitations/invitations.post.res.json")

  val InvitationsCodePost: JsonSchema =
    new JsonSchema("public/schema/v1/invitations/code.post.req.json")

  val InvitationsCodePostRes: JsonSchema =
    new JsonSchema("public/schema/v1/invitations/code.post.res.json")

  // users/
  val UsersPost: JsonSchema =
    new JsonSchema("public/schema/v1/users/users.post.req.json")

  val UsersPostRes: JsonSchema =
    new JsonSchema("public/schema/v1/users/users.post.res.json")

  // auth/
  val AuthCommon: JsonSchema =
    new JsonSchema("public/schema/v1/auth/common.json")

  val TokenPost: JsonSchema =
    new JsonSchema("public/schema/v1/auth/token.post.req.json")

  val TokenPostRes: JsonSchema =
    new JsonSchema("public/schema/v1/auth/token.post.res.json")

  val CheckGetRes: JsonSchema =
    new JsonSchema("public/schema/v1/auth/check.get.res.json")

  val RefreshPost: JsonSchema =
    new JsonSchema("public/schema/v1/auth/refresh.post.req.json")

  val RefreshPostRes: JsonSchema =
    new JsonSchema("public/schema/v1/auth/refresh.post.res.json")

  val PasswordForgotten: JsonSchema =
    new JsonSchema("public/schema/v1/auth/password-forgotten.post.req.json")

  val PasswordReset: JsonSchema =
    new JsonSchema("public/schema/v1/auth/password-reset.post.req.json")

  val UserGetRes: JsonSchema =
    new JsonSchema("public/schema/v1/auth/user.get.res.json")

  val AuthActivationPost: JsonSchema =
    new JsonSchema("public/schema/v1/auth/activation.post.req.json")

  val AuthActivationPostRes: JsonSchema =
    new JsonSchema("public/schema/v1/auth/activation.post.res.json")

  val AuthAccountDeleteConfirmationPost: JsonSchema =
    new JsonSchema("public/schema/v1/auth/account-delete-confirmation.post.req.json")

  val AuthContactTrustPost: JsonSchema =
    new JsonSchema("public/schema/v1/auth/contact-trust.post.req.json")

  val AuthContactTrustPostRes: JsonSchema =
    new JsonSchema("public/schema/v1/auth/contact-trust.post.res.json")

  val AuthContactActivationPost: JsonSchema =
    new JsonSchema("public/schema/v1/auth/contact-activation.post.req.json")

  val AuthContactActivationPostRes: JsonSchema =
    new JsonSchema("public/schema/v1/auth/contact-activation.post.res.json")

  val values: Seq[JsonSchema] = Seq(
    // app
    Common,
    TimeZone,
    InvitationsCommon,

    // Init
    InitConfigurationPost,

    // system: tenant
    SystemTenantPost, SystemTenantPostRes,
    SystemTenantsGetRes,
    SystemTenantGetRes,
    SystemTenantPut, SystemTenantPutRes,

    // system: workspace
    SystemWorkspaceCommon,
    SystemWorkspacePost, SystemWorkspacePostRes,
    SystemWorkspacesGetRes,
    SystemWorkspaceGetRes,
    SystemWorkspacePut, SystemWorkspacePutRes,
    SystemWorkspaceStatusPatch, SystemWorkspaceStatusPatchRes,

    // admin: user
    AdminUsersPost,
    AdminUserRolesPatch, AdminUserRolesPatchRes,
    AdminUserStatusPatch, AdminUserStatusPatchRes,
    AdminUserApplicationsPatch, AdminUserApplicationsPatchRes,
    AdminUserSearch,
    AdminAccountTrustPostReq,

    // admin: client
    AdminClientCommon,
    AdminClientPost, AdminClientPostRes,
    AdminClientPut, AdminClientPutRes,

    // admin: client
    AdminRolePost, AdminRolePostRes,

    // admin: applications
    AdminSystemApplicationsPatch, AdminSystemApplicationsPatchRes,

    // owner: applications
    OwnerUserApplicationsPatch, OwnerUserApplicationsPatchRes,

    // invitations/
    InvitationsPost, InvitationsPostRes,
    InvitationsCodePost, InvitationsCodePostRes,

    // users/
    UsersPost, UsersPostRes,

    // auth/
    AuthCommon,
    TokenPost, TokenPostRes,
    CheckGetRes,
    RefreshPost, RefreshPostRes,
    PasswordForgotten,
    PasswordReset,
    UserGetRes,
    AuthActivationPost, AuthActivationPostRes,
    AuthAccountDeleteConfirmationPost,
    AuthContactTrustPost, AuthContactTrustPostRes,
    AuthContactActivationPost, AuthContactActivationPostRes
  )
}

class JsonSchema(resPath: String)(implicit val conf: ApplicationConfiguration, implicit val env: Environment) {

  val resourcePath: String = resPath

  lazy val value: String = {
    val source =
    // todo: setup properly resource classloader in test
      if (env.mode == Test) fromFile(env.getFile(resPath), "UTF-8")
      else fromResource(resPath, env.classLoader)
    source.getLines().map(_.trim).map(_.replace("{baseUrl}", conf.baseUrl)).mkString
  }

  private lazy val json = io.circe.parser.parse(value).getOrElse(io.circe.Json.Null)

  lazy val jsValue: JsValue = Json.parse(value)

  lazy val schema: Schema = Schema.load(json)

  private def components(s: String): Seq[String] =
    s.split("/").filterNot(_ == "#")

  private def path(cs: Seq[String]): JsPath =
    JsPath(cs.map(KeyPathNode).toList)

  private def lookup(cs: Seq[String]): JsLookupResult =
    cs.foldLeft(jsValue.result)((acc, c) => acc \ c)

  def validate(jsValue: => JsValue): JsResult[JsValue] = {
    // parsing json using circe
    val json = io.circe.parser.parse(jsValue.toString).getOrElse(io.circe.Json.Null)
    // validating json using play-json
    schema.validate(json) match {
      case Validated.Valid(_) => JsSuccess[JsValue](jsValue)
      case Validated.Invalid(e) =>

        val errors: Seq[(JsPath, Seq[JsonValidationError])] =
          e.map(o => {
            // computing path components
            val cs = components(o.location)
            // extracting data from json
            val data = lookup(cs)
            // making JsPath and error data
            path(cs) -> Seq(JsonValidationError(Seq(o.getMessage, s"definition at ${o.schemaLocation.getOrElse("")}"), data.getOrElse(JsString("n/a"))))
          }).toList

        JsError.apply(errors)
    }
  }

  def validateObj[A](jsValue: => JsValue)(implicit rds: Reads[A]): JsResult[A] = {
    validate(jsValue) match {
      case _: JsSuccess[_] => jsValue.validate[A]
      case e: JsError => e
    }
  }
}