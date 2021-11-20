package io.xauth.auth.web

import java.lang.Thread.sleep
import java.text.SimpleDateFormat
import java.util.Date

import io.xauth.auth.service.auth.model.AuthRole.{Admin, User}
import io.xauth.auth.service.auth.model.AuthStatus.Enabled
import io.xauth.auth.web.Initializer.Data.{clean, createClient, createUser}
import org.specs2.specification.BeforeAfterAll
import play.api.http.ContentTypes
import play.api.libs.json.{JsObject, JsValue}
import play.api.libs.ws.WSAuthScheme.BASIC
import play.api.libs.ws.WSResponse
import play.api.test.{PlaySpecification, WithServer, WsTestClient}

import scala.concurrent.ExecutionContext

/**
  * Tests the admin client routes.
  */
class AdminClientRouteSpec extends PlaySpecification with BeforeAfterAll {

  import Initializer._

  private implicit val ec: ExecutionContext = ExecutionContext.global

  val iso8601DateFormat = "yyyy-MM-dd'T'HH:mm:ss.sss'Z'"
  val dateFormat = new SimpleDateFormat(iso8601DateFormat)

  override def beforeAll(): Unit = {

    // the root client must exists for the first login
    createClient("trusted-client", "b8f19ff88bcdc0e6d4a91ae927cfc452")
    createUser("david.bohm@xauth.com", "Th3H4ppyW0rld", Enabled, "David", "Bohm", Admin)
    createUser("karl.heisenberg@xauth.com", "MyPr1nc1pleIsF4lse", Enabled, "Karl", "Heisenberg", User)
  }

  override def afterAll(): Unit = clean()

  "post /v1/admin/clients" should {
    "requires admin authorization" in new WithServer(application) {

      val authResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "username": "karl.heisenberg@xauth.com",
              |  "password": "MyPr1nc1pleIsF4lse"
              |}
            """.stripMargin
          )
      }

      authResponse.status mustEqual OK

      val authJson: JsValue = authResponse.body[JsValue]

      val authAccessToken: String = (authJson \ "accessToken").as[String]

      // MAKING FORBIDDEN POST

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .post(
            """
              |{
              |  "id": "my-trusted-client"
              |}
            """.stripMargin
          )
      }

      response.status mustEqual FORBIDDEN

      val json: JsValue = response.body[JsValue]

      (json \ "message").as[String] mustEqual "access restricted to: ADMIN"
    }

    "rejects bad id" in new WithServer(application) {

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

      val authAccessToken: String = (authJson \ "accessToken").as[String]

      // MAKING BAD REQUEST (BAD ID)

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .post(
            """
              |{
              |  "id": "- bad-client-id -"
              |}
            """.stripMargin
          )
      }

      response.status mustEqual BAD_REQUEST

      val json: JsValue = response.body[JsValue]

      // actual json-schema validator uses different implementation for
      // regular expressions and not supports negative lookhead and look behind,
      // then the better format like '^(?!-)[a-z0-9-]{10,32}(?<!-)$' is not currently supported
      // evaluate to check for newer version of the library
      (json \ "obj.id" \ 0 \ "msg" \ 0).as[String] mustEqual "'- bad-client-id -' does not match pattern '^[a-z0-9][a-z0-9-]{8,30}[a-z0-9]$'."
      (json \ "obj.id" \ 0 \ "args" \ 0 \ "keyword").as[String] mustEqual "pattern"
      (json \ "obj.id" \ 0 \ "args" \ 0 \ "schemaPath").as[String] mustEqual "#/definitions/ClientId"
      (json \ "obj.id" \ 0 \ "args" \ 0 \ "value").as[String] mustEqual "- bad-client-id -"
      (json \ "obj.id" \ 0 \ "args" \ 0 \ "instancePath").as[String] mustEqual "/id"
      (json \ "obj.id" \ 0 \ "args" \ 0 \ "errors").as[JsObject].keys.size mustEqual 0
    }

    "rejects bad secret" in new WithServer(application) {

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

      val authAccessToken: String = (authJson \ "accessToken").as[String]

      // MAKING BAD REQUEST (BAD SECRET)

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .post(
            """
              |{
              |  "id": "my-trusted-client",
              |  "secret": "- bad secret -"
              |}
            """.stripMargin
          )
      }

      response.status mustEqual BAD_REQUEST

      val json: JsValue = response.body[JsValue]

      (json \ "obj.secret" \ 0 \ "msg" \ 0).as[String] mustEqual "'- bad secret -' does not match pattern '^[a-zA-Z0-9]{10,32}$'."
      (json \ "obj.secret" \ 0 \ "args" \ 0 \ "keyword").as[String] mustEqual "pattern"
      (json \ "obj.secret" \ 0 \ "args" \ 0 \ "schemaPath").as[String] mustEqual "#/definitions/ClientSecret"
      (json \ "obj.secret" \ 0 \ "args" \ 0 \ "value").as[String] mustEqual "- bad secret -"
      (json \ "obj.secret" \ 0 \ "args" \ 0 \ "instancePath").as[String] mustEqual "/secret"
      (json \ "obj.secret" \ 0 \ "args" \ 0 \ "errors").as[JsObject].keys.size mustEqual 0
    }

    "creates client" in new WithServer(application) {

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

      val authAccessToken: String = (authJson \ "accessToken").as[String]

      // CREATING NEW CLIENT

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .post(
            """
              |{
              |  "id": "my-trusted-client",
              |  "secret": "mySecureSecret"
              |}
            """.stripMargin
          )
      }

      response.status mustEqual CREATED

      val json: JsValue = response.body[JsValue]

      (json \ "id").as[String] mustEqual "my-trusted-client"
      (json \ "secret").as[String] mustEqual "1eb836a3bf3d19676b7392a72b1e2cab"

      val registeredAt: Date = dateFormat.parse((json \ "registeredAt").as[String])
      val updatedAt: Date = dateFormat.parse((json \ "updatedAt").as[String])

      (registeredAt compareTo updatedAt) must beEqualTo(0)
    }
  }

  "get /v1/admin/clients" should {
    "requires admin authorization" in new WithServer(application) {

      val authResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "username": "karl.heisenberg@xauth.com",
              |  "password": "MyPr1nc1pleIsF4lse"
              |}
            """.stripMargin
          )
      }

      authResponse.status mustEqual OK

      val authJson: JsValue = authResponse.body[JsValue]

      val authAccessToken: String = (authJson \ "accessToken").as[String]

      // MAKING FORBIDDEN GET

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .get
      }

      response.status mustEqual FORBIDDEN

      val json: JsValue = response.body[JsValue]

      (json \ "message").as[String] mustEqual "access restricted to: ADMIN"
    }

    "returns all clients" in new WithServer(application) {

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

      val authAccessToken: String = (authJson \ "accessToken").as[String]

      // CREATING NEW CLIENT

      val creationResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .post(
            """
              |{
              |  "id": "my-trusted-client",
              |  "secret": "MySecureSecret"
              |}
            """.stripMargin
          )
      }

      creationResponse.status mustEqual CREATED

      // GETTING ALL CLIENTS

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .get
      }

      response.status mustEqual OK

      val json: JsValue = response.body[JsValue]

      json.as[Seq[JsObject]] must have size 2

      (json \ 0 \ "id").as[String] mustEqual "trusted-client"
      (json \ 1 \ "id").as[String] mustEqual "my-trusted-client"
    }
  }

  "get /v1/admin/clients/:id" should {
    "requires admin authorization" in new WithServer(application) {

      val authResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "username": "karl.heisenberg@xauth.com",
              |  "password": "MyPr1nc1pleIsF4lse"
              |}
            """.stripMargin
          )
      }

      authResponse.status mustEqual OK

      val authJson: JsValue = authResponse.body[JsValue]

      val authAccessToken: String = (authJson \ "accessToken").as[String]

      // MAKING FORBIDDEN GET

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients/trusted-client")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .get
      }

      response.status mustEqual FORBIDDEN

      val json: JsValue = response.body[JsValue]

      (json \ "message").as[String] mustEqual "access restricted to: ADMIN"
    }

    "handles not-found status" in new WithServer(application) {

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

      val authAccessToken: String = (authJson \ "accessToken").as[String]

      // MAKING GET

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients/not-existing-client")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .get
      }

      response.status mustEqual NOT_FOUND
    }

    "returns client" in new WithServer(application) {

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

      val authAccessToken: String = (authJson \ "accessToken").as[String]

      // MAKING GET

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients/trusted-client")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .get
      }

      response.status mustEqual OK

      val json: JsValue = response.body[JsValue]

      (json \ "id").as[String] mustEqual "trusted-client"
      (json \ "secret").as[String] mustEqual "b8f19ff88bcdc0e6d4a91ae927cfc452"

      val registeredAt: Date = dateFormat.parse((json \ "registeredAt").as[String])
      val updatedAt: Date = dateFormat.parse((json \ "updatedAt").as[String])

      (registeredAt compareTo updatedAt) must beEqualTo(0)
    }
  }

  "put /v1/admin/clients/:id" should {
    "requires admin authorization" in new WithServer(application) {

      val authResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "username": "karl.heisenberg@xauth.com",
              |  "password": "MyPr1nc1pleIsF4lse"
              |}
            """.stripMargin
          )
      }

      authResponse.status mustEqual OK

      val authJson: JsValue = authResponse.body[JsValue]

      val authAccessToken: String = (authJson \ "accessToken").as[String]

      // MAKING FORBIDDEN PUT

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients/my-trusted-client")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .put(
            """
              |{
              |  "id": "my-trusted-client",
              |  "secret": "new-secure-secret",
              |  "registeredAt": "2018-11-19T15:51:15.151",
              |  "updatedAt": "2018-11-19T15:51:15.151"
              |}
            """.stripMargin
          )
      }

      response.status mustEqual FORBIDDEN

      val json: JsValue = response.body[JsValue]

      (json \ "message").as[String] mustEqual "access restricted to: ADMIN"
    }

    "handles not-found status" in new WithServer(application) {

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

      val authAccessToken: String = (authJson \ "accessToken").as[String]

      // MAKING FORBIDDEN PUT

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients/not-existing-client")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .put(
            """
              |{
              |  "id": "not-existing-client",
              |  "secret": "newSecureSecret",
              |  "registeredAt": "2018-11-19T15:51:15.151Z",
              |  "updatedAt": "2018-11-19T15:51:15.151Z"
              |}
            """.stripMargin
          )
      }

      response.status mustEqual NOT_FOUND
    }

    "rejects bad id" in new WithServer(application) {

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

      val authAccessToken: String = (authJson \ "accessToken").as[String]

      // MAKING BAD REQUEST (BAD ID)

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients/bad-client-id")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .put(
            """
              |{
              |  "id": "- bad-client-id -",
              |  "secret": "newSecureSecret",
              |  "registeredAt": "2018-11-19T15:51:15.151Z",
              |  "updatedAt": "2018-11-19T15:51:15.151Z"
              |}
            """.stripMargin
          )
      }

      response.status mustEqual BAD_REQUEST

      val json: JsValue = response.body[JsValue]

      // actual json-schema validator uses different implementation for
      // regular expressions and not supports negative lookhead and lookbehind,
      // then the better format like '^(?!-)[a-z0-9-]{10,32}(?<!-)$' is not currently supported
      // evaluate to check for newer version of the library
      (json \ "obj.id" \ 0 \ "msg" \ 0).as[String] mustEqual "'- bad-client-id -' does not match pattern '^[a-z0-9][a-z0-9-]{8,30}[a-z0-9]$'."
      (json \ "obj.id" \ 0 \ "args" \ 0 \ "keyword").as[String] mustEqual "pattern"
      (json \ "obj.id" \ 0 \ "args" \ 0 \ "schemaPath").as[String] mustEqual "#/definitions/ClientId"
      (json \ "obj.id" \ 0 \ "args" \ 0 \ "value").as[String] mustEqual "- bad-client-id -"
      (json \ "obj.id" \ 0 \ "args" \ 0 \ "instancePath").as[String] mustEqual "/id"
      (json \ "obj.id" \ 0 \ "args" \ 0 \ "errors").as[JsObject].keys.size mustEqual 0
    }

    "rejects bad secret" in new WithServer(application) {

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

      val authAccessToken: String = (authJson \ "accessToken").as[String]

      // MAKING BAD REQUEST (BAD SECRET)

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients/my-trusted-client")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .put(
            """
              |{
              |  "id": "my-trusted-client",
              |  "secret": "- bad secret -",
              |  "registeredAt": "2018-11-19T15:51:15.151",
              |  "updatedAt": "2018-11-19T15:51:15.151"
              |}
            """.stripMargin
          )
      }

      response.status mustEqual BAD_REQUEST

      val json: JsValue = response.body[JsValue]

      (json \ "obj.secret" \ 0 \ "msg" \ 0).as[String] mustEqual "'- bad secret -' does not match pattern '^[a-zA-Z0-9]{10,32}$'."
      (json \ "obj.secret" \ 0 \ "args" \ 0 \ "keyword").as[String] mustEqual "pattern"
      (json \ "obj.secret" \ 0 \ "args" \ 0 \ "schemaPath").as[String] mustEqual "#/definitions/ClientSecret"
      (json \ "obj.secret" \ 0 \ "args" \ 0 \ "value").as[String] mustEqual "- bad secret -"
      (json \ "obj.secret" \ 0 \ "args" \ 0 \ "instancePath").as[String] mustEqual "/secret"
      (json \ "obj.secret" \ 0 \ "args" \ 0 \ "errors").as[JsObject].keys.size mustEqual 0
    }

    "checks identifiers" in new WithServer(application) {

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

      val authAccessToken: String = (authJson \ "accessToken").as[String]

      // MAKING BAD REQUEST (BAD SECRET)

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients/trusted-client-id")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .put(
            """
              |{
              |  "id": "different-trusted-client-id",
              |  "secret": "secureSecret",
              |  "registeredAt": "2018-11-19T15:51:15.151Z",
              |  "updatedAt": "2018-11-19T15:51:15.151Z"
              |}
            """.stripMargin
          )
      }

      response.status mustEqual BAD_REQUEST

      val json: JsValue = response.body[JsValue]

      (json \ "message").as[String] mustEqual "inconsistent update request: different identifiers"
    }

    "updates client" in new WithServer(application) {

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

      val authAccessToken: String = (authJson \ "accessToken").as[String]

      // CREATING NEW CLIENT SECRET

      val postResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .post(
            """
              |{
              |  "id": "quantum-client",
              |  "secret": "mySecureSecret"
              |}
            """.stripMargin
          )
      }

      postResponse.status mustEqual CREATED

      val postResponseJson: JsValue = postResponse.body[JsValue]
      val postRegisteredAt: String = (postResponseJson \ "registeredAt").as[String]
      val postUpdatedAt: String = (postResponseJson \ "updatedAt").as[String]

      // UPDATING CLIENT

      sleep(1000)

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients/quantum-client")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .put(
            s"""
               |{
               |  "id": "quantum-client",
               |  "secret": "updatedSecret",
               |  "registeredAt": "$postRegisteredAt",
               |  "updatedAt": "$postUpdatedAt"
               |}
            """.stripMargin
          )
      }

      response.status mustEqual OK

      val json: JsValue = response.body[JsValue]

      (json \ "id").as[String] mustEqual "quantum-client"
      (json \ "secret").as[String] mustEqual "da50bded1dbb04bbf7d1ee785437e723"
      (json \ "registeredAt").as[String] mustEqual postRegisteredAt

      val registeredAt: Date = dateFormat.parse((json \ "registeredAt").as[String])
      val updatedAt: Date = dateFormat.parse((json \ "updatedAt").as[String])

      (registeredAt compareTo updatedAt) must beEqualTo(-1)
    }
  }

  "delete /v1/admin/clients/:id" should {
    "requires admin authorization" in new WithServer(application) {

      val authResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "username": "karl.heisenberg@xauth.com",
              |  "password": "MyPr1nc1pleIsF4lse"
              |}
            """.stripMargin
          )
      }

      authResponse.status mustEqual OK

      val authJson: JsValue = authResponse.body[JsValue]

      val authAccessToken: String = (authJson \ "accessToken").as[String]

      // MAKING FORBIDDEN DELETE

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients/client-to-delete")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .delete
      }

      response.status mustEqual FORBIDDEN

      val json: JsValue = response.body[JsValue]

      (json \ "message").as[String] mustEqual "access restricted to: ADMIN"
    }

    "handles not-found status" in new WithServer(application) {

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

      val authAccessToken: String = (authJson \ "accessToken").as[String]

      // MAKING DELETE

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients/not-existing-client")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .delete
      }

      response.status mustEqual NOT_FOUND
    }

    "deletes client" in new WithServer(application) {

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

      val authAccessToken: String = (authJson \ "accessToken").as[String]

      // CREATING NEW CLIENT

      val postResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .post(
            """
              |{
              |  "id": "client-to-delete",
              |  "secret": "mySecureSecret"
              |}
            """.stripMargin
          )
      }

      postResponse.status mustEqual CREATED

      // GETTING JUST CREATED CLIENT

      val getResponse1: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients/client-to-delete")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .get
      }

      getResponse1.status mustEqual OK

      val getResponseJson1: JsValue = getResponse1.body[JsValue]

      (getResponseJson1 \ "id").as[String] mustEqual "client-to-delete"

      // DELETING CLIENT

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients/client-to-delete")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .delete
      }

      response.status mustEqual NO_CONTENT

      // TRYING TO GET JUST DELETED CLIENT

      val getResponse2: WSResponse = await {
        WsTestClient.wsUrl("/v1/admin/clients/client-to-delete")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .get
      }

      getResponse2.status mustEqual NOT_FOUND
    }
  }
}
