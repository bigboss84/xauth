package io.xauth.auth.web

import io.xauth.auth.service.auth.model.AuthRole.{Admin, User}
import io.xauth.auth.service.auth.model.AuthStatus.{Blocked, Disabled, Enabled}
import io.xauth.auth.service.auth.model.AuthUser
import io.xauth.auth.web.Initializer.Data.{createClient, createUser}
import org.specs2.specification.BeforeAfterAll
import play.api.http.ContentTypes
import play.api.libs.json.{JsObject, JsValue}
import play.api.libs.ws.WSAuthScheme.BASIC
import play.api.libs.ws.WSResponse
import play.api.test.{PlaySpecification, WithServer, WsTestClient}

import scala.concurrent.Await.result
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Promise}

/**
  * Tests the admin user routes
  */
class AdminUserRouteSpec extends PlaySpecification with BeforeAfterAll {

  import Initializer._

  private implicit val ec: ExecutionContext = ExecutionContext.global

  private val userToUnblockPromise: Promise[AuthUser] = Promise()
  private val userToPatchPromise: Promise[AuthUser] = Promise()

  override def beforeAll(): Unit = {
    createClient("trusted-client", "b8f19ff88bcdc0e6d4a91ae927cfc452")
    createUser("david.bohm@xauth.com", "Th3H4ppyW0rld", Enabled, "David", "Bohm", Admin)
    createUser("albert.einstein@xauth.com", "Th3L1mi7IsTheModel", Enabled, "Albert", "Einstein", User)

    // creating user to unblock
    userToUnblockPromise completeWith {
      createUser("harry.potter@xauth.com", "Qu4n7umEfF3ct5", Blocked, "Harry", "Potter", User)
    }

    // creating user to update
    userToPatchPromise completeWith {
      createUser("karl.heisenberg@xauth.com", "MyPr1nc1pleIsF4lse", Disabled, "Karl", "Heisenberg", User)
    }
  }

  override def afterAll(): Unit = Data.clean()

  "post /v1/admin/users/:id/unblock" should {
    "requires admin authorization" in new WithServer(application) {

      val authResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "username": "albert.einstein@xauth.com",
              |  "password": "Th3L1mi7IsTheModel"
              |}
            """.stripMargin
          )
      }

      authResponse.status mustEqual OK

      val authJson: JsValue = authResponse.body[JsValue]

      val accessToken: String = (authJson \ "accessToken").as[String]

      // GETTING USER TO TRY TO UNBLOCK

      val user: AuthUser = result(
        userToUnblockPromise.future, 3 seconds
      )

      // ATTEMPTING TO UNBLOCK

      val response: WSResponse = await {
        WsTestClient.wsUrl(s"/v1/admin/users/${user.id.stringValue}/unblock")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $accessToken"
          )
          .post("")
      }

      response.status mustEqual FORBIDDEN

      val json: JsValue = response.body[JsValue]

      (json \ "message").as[String] mustEqual "access restricted to: ADMIN"
    }

    "reject request for non-blocked users" in new WithServer(application) {

      val authResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "username": "david.bohm@xauth.com",
              |  "password": "Th3H4ppyW0rld"
              |}
            """.stripMargin
          )
      }

      authResponse.status mustEqual OK

      val authJson: JsValue = authResponse.body[JsValue]

      val accessToken: String = (authJson \ "accessToken").as[String]

      // GETTING USER TO TRY TO UNBLOCK

      val user: AuthUser = result(
        userToPatchPromise.future, 3 seconds
      )

      // ATTEMPTING TO UNBLOCK

      val response: WSResponse = await {
        WsTestClient.wsUrl(s"/v1/admin/users/${user.id.stringValue}/unblock")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $accessToken"
          )
          .post("")
      }

      response.status mustEqual BAD_REQUEST
    }

    "handle not-found status" in new WithServer(application) {

      val authResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "username": "david.bohm@xauth.com",
              |  "password": "Th3H4ppyW0rld"
              |}
            """.stripMargin
          )
      }

      authResponse.status mustEqual OK

      val authJson: JsValue = authResponse.body[JsValue]

      val accessToken: String = (authJson \ "accessToken").as[String]

      // ATTEMPTING TO UNBLOCK NOT EXISTING USER

      val notExistingUserId: String = "7de1d96d-7894-45d5-b161-acd1c0fad0a6"

      val response: WSResponse = await {
        WsTestClient.wsUrl(s"/v1/admin/users/$notExistingUserId/unblock")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.TEXT,
            AUTHORIZATION -> s"Bearer $accessToken"
          )
          .post("")
      }

      response.status mustEqual NOT_FOUND
    }

    "unblock blocked user" in new WithServer(application) {

      val authResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "username": "david.bohm@xauth.com",
              |  "password": "Th3H4ppyW0rld"
              |}
            """.stripMargin
          )
      }

      authResponse.status mustEqual OK

      val authJson: JsValue = authResponse.body[JsValue]

      val accessToken: String = (authJson \ "accessToken").as[String]

      // GETTING USER TO UNBLOCK

      val user: AuthUser = result(
        userToUnblockPromise.future, 3 seconds
      )

      // UNBLOCKING USER

      val response: WSResponse = await {
        WsTestClient.wsUrl(s"/v1/admin/users/${user.id.stringValue}/unblock")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $accessToken"
          )
          .post("")
      }

      response.status mustEqual OK
    }
  }

  "patch /v1/admin/users/:id/roles" should {
    "require admin authorization" in new WithServer(application) {

      val authResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "username": "albert.einstein@xauth.com",
              |  "password": "Th3L1mi7IsTheModel"
              |}
            """.stripMargin
          )
      }

      authResponse.status mustEqual OK

      val authJson: JsValue = authResponse.body[JsValue]

      val accessToken: String = (authJson \ "accessToken").as[String]

      // GETTING USER TO TRY TO PATCH

      val user: AuthUser = result(
        userToPatchPromise.future, 3 seconds
      )

      // ATTEMPTING TO PATCH USER

      val response: WSResponse = await {
        WsTestClient.wsUrl(s"/v1/admin/users/${user.id.stringValue}/roles")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $accessToken"
          )
          .patch(
            """
              |{
              |  "roles": [
              |    "ADMIN",
              |    "HR",
              |    "USER"
              |  ]
              |}
            """.stripMargin
          )
      }

      response.status mustEqual FORBIDDEN

      val json: JsValue = response.body[JsValue]

      (json \ "message").as[String] mustEqual "access restricted to: ADMIN"
    }

    "handle not-found status" in new WithServer(application) {

      val authResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "username": "david.bohm@xauth.com",
              |  "password": "Th3H4ppyW0rld"
              |}
            """.stripMargin
          )
      }

      authResponse.status mustEqual OK

      val authJson: JsValue = authResponse.body[JsValue]

      val accessToken: String = (authJson \ "accessToken").as[String]

      // ATTEMPTING TO PATCH NOT EXISTING USER

      val notExistingUser: String = "20b3ca8f-47f1-40f0-bf9a-cc0989747c09"

      val response: WSResponse = await {
        WsTestClient.wsUrl(s"/v1/admin/users/$notExistingUser/roles")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $accessToken"
          )
          .patch(
            """
              |{
              |  "roles": [
              |    "ADMIN",
              |    "HR",
              |    "USER"
              |  ]
              |}
            """.stripMargin
          )
      }

      response.status mustEqual NOT_FOUND
    }

    "not recognize invalid status" in new WithServer(application) {

      val authResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "username": "david.bohm@xauth.com",
              |  "password": "Th3H4ppyW0rld"
              |}
            """.stripMargin
          )
      }

      authResponse.status mustEqual OK

      val authJson: JsValue = authResponse.body[JsValue]

      val accessToken: String = (authJson \ "accessToken").as[String]

      // ATTEMPTING TO PATCH USER

      val notExistingUser: String = "20b3ca8f-47f1-40f0-bf9a-cc0989747c09"

      val response: WSResponse = await {
        WsTestClient.wsUrl(s"/v1/admin/users/$notExistingUser/roles")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $accessToken"
          )
          .patch(
            """
              |{
              |  "roles": [
              |    "ADMIN",
              |    "INVALID"
              |  ]
              |}
            """.stripMargin
          )
      }

      response.status mustEqual BAD_REQUEST

      val json: JsValue = response.body[JsValue]

      (json \ "obj.roles.1" \ 0 \ "msg").as[Array[String]] should have size 1
      (json \ "obj.roles.1" \ 0 \ "msg" \ 0).as[String] shouldEqual "Instance is invalid enum value."
      (json \ "obj.roles.1" \ 0 \ "args").as[Array[JsObject]] should have size 1
      (json \ "obj.roles.1" \ 0 \ "args" \ 0 \ "keyword").as[String] shouldEqual "enum"
      (json \ "obj.roles.1" \ 0 \ "args" \ 0 \ "schemaPath").as[String] shouldEqual "#/definitions/AuthRole"
      (json \ "obj.roles.1" \ 0 \ "args" \ 0 \ "instancePath").as[String] shouldEqual "/roles/1"
      (json \ "obj.roles.1" \ 0 \ "args" \ 0 \ "value").as[String] shouldEqual "INVALID"
      (json \ "obj.roles.1" \ 0 \ "args" \ 0 \ "errors" \ "enum").as[Array[String]] should have size 3
      (json \ "obj.roles.1" \ 0 \ "args" \ 0 \ "errors" \ "enum" \ 0).as[String] shouldEqual "USER"
      (json \ "obj.roles.1" \ 0 \ "args" \ 0 \ "errors" \ "enum" \ 1).as[String] shouldEqual "HR"
      (json \ "obj.roles.1" \ 0 \ "args" \ 0 \ "errors" \ "enum" \ 2).as[String] shouldEqual "ADMIN"
    }

    "patch user roles" in new WithServer(application) {

      val authResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "username": "david.bohm@xauth.com",
              |  "password": "Th3H4ppyW0rld"
              |}
            """.stripMargin
          )
      }

      authResponse.status mustEqual OK

      val authJson: JsValue = authResponse.body[JsValue]

      val accessToken: String = (authJson \ "accessToken").as[String]

      // GETTING USER TO TRY TO PATCH

      val user: AuthUser = result(
        userToPatchPromise.future, 3 seconds
      )

      // PATCHING USER

      val response: WSResponse = await {
        WsTestClient.wsUrl(s"/v1/admin/users/${user.id.stringValue}/roles")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $accessToken"
          )
          .patch(
            """
              |{
              |  "roles": [
              |    "USER",
              |    "HR"
              |  ]
              |}
            """.stripMargin
          )
      }

      response.status mustEqual OK

      val json: JsValue = response.body[JsValue]

      (json \ "roles").as[Array[String]] should have size 2
      (json \ "roles" \ 0).as[String] shouldEqual "USER"
      (json \ "roles" \ 1).as[String] shouldEqual "HR"
    }
  }

  "patch /v1/admin/users/:id/status" should {
    "require admin authorization" in new WithServer(application) {

      val authResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "username": "albert.einstein@xauth.com",
              |  "password": "Th3L1mi7IsTheModel"
              |}
            """.stripMargin
          )
      }

      authResponse.status mustEqual OK

      val authJson: JsValue = authResponse.body[JsValue]

      val accessToken: String = (authJson \ "accessToken").as[String]

      // GETTING USER TO PATCH

      val user: AuthUser = result(
        userToPatchPromise.future, 3 seconds
      )

      // ATTEMPTING TO PATCH USER STATUS

      val response: WSResponse = await {
        WsTestClient.wsUrl(s"/v1/admin/users/${user.id.stringValue}/status")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $accessToken"
          )
          .patch(
            """
              |{
              |  "status": "ENABLED"
              |}
            """.stripMargin
          )
      }

      response.status mustEqual FORBIDDEN

      val json: JsValue = response.body[JsValue]

      (json \ "message").as[String] mustEqual "access restricted to: ADMIN"
    }

    "accept only valid statuses" in new WithServer(application) {

      val authResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "username": "david.bohm@xauth.com",
              |  "password": "Th3H4ppyW0rld"
              |}
            """.stripMargin
          )
      }

      authResponse.status mustEqual OK

      val authJson: JsValue = authResponse.body[JsValue]

      val accessToken: String = (authJson \ "accessToken").as[String]

      // GETTING USER TO PATCH

      val user: AuthUser = result(
        userToPatchPromise.future, 3 seconds
      )

      // ATTEMPTING TO PATCH USER STATUS WITH AN INVALID VALUE

      val response: WSResponse = await {
        WsTestClient.wsUrl(s"/v1/admin/users/${user.id.stringValue}/status")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $accessToken"
          )
          .patch(
            """
              |{
              |  "status": "INVALID_STATUS"
              |}
            """.stripMargin
          )
      }

      response.status mustEqual BAD_REQUEST

      val json: JsValue = response.body[JsValue]

      (json \ "obj.status" \ 0 \ "msg" \ 0).as[String] mustEqual "Instance is invalid enum value."
      (json \ "obj.status" \ 0 \ "args" \ 0 \ "value").as[String] mustEqual "INVALID_STATUS"
      (json \ "obj.status" \ 0 \ "args" \ 0 \ "errors" \ "enum").as[Seq[String]] must have size 3
      (json \ "obj.status" \ 0 \ "args" \ 0 \ "errors" \ "enum" \ 0).as[String] mustEqual "DISABLED"
      (json \ "obj.status" \ 0 \ "args" \ 0 \ "errors" \ "enum" \ 1).as[String] mustEqual "ENABLED"
      (json \ "obj.status" \ 0 \ "args" \ 0 \ "errors" \ "enum" \ 2).as[String] mustEqual "BLOCKED"
    }

    "handle not-found status" in new WithServer(application) {

      val authResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "username": "david.bohm@xauth.com",
              |  "password": "Th3H4ppyW0rld"
              |}
            """.stripMargin
          )
      }

      authResponse.status mustEqual OK

      val authJson: JsValue = authResponse.body[JsValue]

      val accessToken: String = (authJson \ "accessToken").as[String]

      // ATTEMPTING TO PATCH NON EXISTING USER

      val notExistingUser: String = "20b3ca8f-47f1-40f0-bf9a-cc0989747c09"

      val response: WSResponse = await {
        WsTestClient.wsUrl(s"/v1/admin/users/$notExistingUser/status")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $accessToken"
          )
          .patch(
            """
              |{
              |  "status": "ENABLED"
              |}
            """.stripMargin
          )
      }

      response.status mustEqual NOT_FOUND
    }

    "patch user status" in new WithServer(application) {

      val authResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "username": "david.bohm@xauth.com",
              |  "password": "Th3H4ppyW0rld"
              |}
            """.stripMargin
          )
      }

      authResponse.status mustEqual OK

      val authJson: JsValue = authResponse.body[JsValue]

      val accessToken: String = (authJson \ "accessToken").as[String]

      // GETTING USER TO PATCH

      val user: AuthUser = result(
        userToPatchPromise.future, 3 seconds
      )

      // PATCHING USER STATUS

      val response: WSResponse = await {
        WsTestClient.wsUrl(s"/v1/admin/users/${user.id.stringValue}/status")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $accessToken"
          )
          .patch(
            """
              |{
              |  "status": "ENABLED"
              |}
            """.stripMargin
          )
      }

      response.status mustEqual OK

      val json: JsValue = response.body[JsValue]

      (json \ "status").as[String] mustEqual "ENABLED"
    }
  }
}
