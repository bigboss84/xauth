package io.xauth.auth.web

import java.text.SimpleDateFormat
import java.util.Date

import io.xauth.auth.service.auth.model.AuthRole.{Admin, HumanResource, User}
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
  * Tests the invitation client routes.
  */
class InvitationRouteSpec extends PlaySpecification with BeforeAfterAll {

  import Initializer._

  private implicit val ec: ExecutionContext = ExecutionContext.global

  val iso8601DateFormat = "yyyy-MM-dd'T'HH:mm:ss.sss'Z'"
  val dateFormat = new SimpleDateFormat(iso8601DateFormat)

  override def beforeAll(): Unit = {
    // the root client must exists for the first login
    createClient("trusted-client", "b8f19ff88bcdc0e6d4a91ae927cfc452")
    createUser("david.bohm@xauth.com", "Th3H4ppyW0rld", Enabled, "David", "Bohm", Admin, HumanResource)
    createUser("karl.heisenberg@xauth.com", "MyPr1nc1pleIsF4lse", Enabled, "Karl", "Heisenberg", User)
  }

  override def afterAll(): Unit = clean()

  "get /v1/invitations" should {
    "require hr authorization" in new WithServer(application) {

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
        WsTestClient.wsUrl("/v1/invitations")
          .addQueryStringParameters(("invitationCode", "qevcj2wlqm"))
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .get
      }

      response.status mustEqual FORBIDDEN

      val json: JsValue = response.body[JsValue]

      (json \ "message").as[String] mustEqual "access restricted to: HR"
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

      val authAccessToken: String = (authJson \ "accessToken").as[String]

      // MAKING REQUEST

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/invitations")
          .addQueryStringParameters(("invitationCode", "not-existing-code"))
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .get
      }

      response.status mustEqual NOT_FOUND

      val json: JsValue = response.body[JsValue]

      (json \ "message").as[String] mustEqual "Invitation code not found"
    }

    "find invitation by code" in new WithServer(application) {

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

      // CREATING INVITATION TO SEARCH

      val invResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/invitations")
          .addQueryStringParameters(("invitationCode", "not-existing-code"))
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .post(
            """
              |{
              |  "email": "15406918"
              |}
            """.stripMargin
          )
      }

      invResponse.status mustEqual CREATED

      val id: String = (invResponse.body[JsValue] \ "id").as[String]

      // CREATING INVITATION CODE TO SEARCH

      val codeResponse: WSResponse = await {
        WsTestClient.wsUrl(s"/v1/invitations/$id/code")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .post("{}")
      }

      codeResponse.status mustEqual CREATED

      val code: String = (codeResponse.body[JsValue] \ "invitationCode").as[String]

      // SEARCHING INVITATION BY CODE

      val response: WSResponse = await {
        WsTestClient.wsUrl(s"/v1/invitations")
          .addQueryStringParameters(("invitationCode", code))
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .get
      }

      response.status mustEqual OK

      val json: JsValue = response.body[JsValue]

      (json \ "id").as[String] mustEqual id
    }
  }

  "post /v1/invitations" should {
    "require hr authorization" in new WithServer(application) {

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
        WsTestClient.wsUrl("/v1/invitations")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .post("{}")
      }

      response.status mustEqual FORBIDDEN

      val json: JsValue = response.body[JsValue]

      (json \ "message").as[String] mustEqual "access restricted to: HR"
    }

    "create invitation with email" in new WithServer(application) {

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

      // CREATING INVITATION

      val invResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/invitations")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .post(
            """
              |{
              |  "email": "15406918"
              |}
            """.stripMargin
          )
      }

      invResponse.status mustEqual CREATED

      val json: JsValue = invResponse.body[JsValue]

      (json \ "id").as[String] mustNotEqual null
      (json \ "email").as[String] mustEqual "15406918"
      (json \ "userInfo" \ "firstName").as[String] mustEqual "ALESSANDRO"
      (json \ "userInfo" \ "lastName").as[String] mustEqual "ODASSO"
      (json \ "userInfo" \ "userType").as[String] mustEqual "BLUE_COLLAR"
      (json \ "userInfo" \ "country").as[String] mustEqual "IT"
      (json \ "userInfo" \ "company").as[String] mustEqual "Pir. Sistemi Informativi"
      (json \ "userInfo" \ "contacts").as[Seq[JsObject]] must have size 0

      val registeredAt: Date = dateFormat.parse((json \ "registeredAt").as[String])
      val updatedAt: Date = dateFormat.parse((json \ "updatedAt").as[String])

      (registeredAt compareTo updatedAt) must beLessThanOrEqualTo(0)
    }

    "not create invitation for the same email" in new WithServer(application) {

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

      // CREATING INVITATION

      val invResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/invitations")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .post(
            """
              |{
              |  "email": "15406918"
              |}
            """.stripMargin
          )
      }

      invResponse.status mustEqual CREATED

      // CREATING INVITATION AGAIN WITH THE SAME EMAIL

      val invResponse2: WSResponse = await {
        WsTestClient.wsUrl("/v1/invitations")
          .addQueryStringParameters(("invitationCode", "not-existing-code"))
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .post(
            """
              |{
              |  "email": "15406918"
              |}
            """.stripMargin
          )
      }

      invResponse2.status mustEqual BAD_REQUEST

      val json: JsValue = authResponse.body[JsValue]

      (json \ "message").as[String] shouldEqual "an existing invitation already corresponds to email '15406918'"
    }
  }

  "post /v1/invitations/:id/code" should {
    "require hr authorization" in new WithServer(application) {

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
        WsTestClient.wsUrl("/v1/invitations/id/code")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .post("{}")
      }

      response.status mustEqual FORBIDDEN

      val json: JsValue = response.body[JsValue]

      (json \ "message").as[String] mustEqual "access restricted to: HR"
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

      val authAccessToken: String = (authJson \ "accessToken").as[String]

      // MAKING FORBIDDEN POST

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/invitations/not-existing-id/code")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .post("{}")
      }

      response.status mustEqual NOT_FOUND
    }

    "create invitation code" in new WithServer(application) {

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

      // CREATING INVITATION

      val invResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/invitations")
          .addQueryStringParameters(("invitationCode", "not-existing-code"))
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .post(
            """
              |{
              |  "email": "15406918"
              |}
            """.stripMargin
          )
      }

      invResponse.status mustEqual CREATED

      val invJson: JsValue = invResponse.body[JsValue]

      val id: String = (invJson \ "id").as[String]

      // CREATING INVITATION CODE

      val codeResponse: WSResponse = await {
        WsTestClient.wsUrl(s"/v1/invitations/$id/code")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .post("{}")
      }

      codeResponse.status mustEqual CREATED

      val json: JsValue = codeResponse.body[JsValue]

      (json \ "invitationCode").as[String] must have size 10
      (json \ "expiresAt").as[String] mustNotEqual null
    }
  }

}
