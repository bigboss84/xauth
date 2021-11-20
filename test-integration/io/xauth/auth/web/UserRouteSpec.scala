package io.xauth.auth.web

import io.xauth.auth.service.auth.model.AuthRole.{Admin, HumanResource}
import io.xauth.auth.service.auth.model.AuthStatus.Enabled
import io.xauth.auth.web.Initializer.Data.{clean, createClient, createUser}
import org.specs2.specification.BeforeAfterAll
import play.api.http.ContentTypes
import play.api.libs.json.JsValue
import play.api.libs.ws.WSAuthScheme.BASIC
import play.api.libs.ws.WSResponse
import play.api.test.{PlaySpecification, WithServer, WsTestClient}

import scala.concurrent.ExecutionContext

/**
  * Tests the user routes.
  */
class UserRouteSpec extends PlaySpecification with BeforeAfterAll {

  import Initializer._

  private implicit val ec: ExecutionContext = ExecutionContext.global

  override def beforeAll(): Unit = {
    // the root client must exists for the first login
    createClient("trusted-client", "b8f19ff88bcdc0e6d4a91ae927cfc452")
    createUser("david.bohm@xauth.com", "Th3H4ppyW0rld", Enabled, "David", "Bohm", Admin, HumanResource)
  }

  override def afterAll(): Unit = clean()

  "post /v1/users" should {
    "require at least one email contact" in new WithServer(application) {

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
              |  "email": "max.planck@fake.xauth.com"
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

      // REGISTRATION

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/users")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON
          )
          .post(
            s"""
              |{
              |  "invitationCode": "$code",
              |  "password": "MyS3cr37Pwd",
              |  "passwordCheck": "MyS3cr37Pwd",
              |  "userInfo": {
              |    "firstName": "Max",
              |    "lastName": "Planck",
              |    "contacts": [
              |      {
              |        "type": "MOBILE_NUMBER",
              |        "value": "00390123456789"
              |      }
              |    ]
              |  }
              |}
            """.stripMargin
          )
      }

      response.status mustEqual BAD_REQUEST

      val json: JsValue = response.body[JsValue]

      (json \ "message").as[String] shouldEqual "email contact not found"
    }

    "check password fields" in new WithServer(application) {

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/users")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON
          )
          .post(
            """
              |{
              |  "invitationCode": "lryswumhnv",
              |  "password": "MyS3cr37Pwd",
              |  "passwordCheck": "MyS3cr37PwdX",
              |  "userInfo": {
              |    "firstName": "Max",
              |    "lastName": "Planck",
              |    "contacts": [
              |      {
              |        "type": "EMAIL",
              |        "value": "max.plank@fake.xauth.com"
              |      }
              |    ]
              |  }
              |}
            """.stripMargin
          )
      }

      response.status mustEqual BAD_REQUEST

      val json: JsValue = response.body[JsValue]

      (json \ "message").as[String] shouldEqual "password fields are different"
    }

    "require a valid invitation code" in new WithServer(application) {

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/users")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON
          )
          .post(
            """
              |{
              |  "invitationCode": "lryswumhnv",
              |  "password": "MyS3cr37Pwd",
              |  "passwordCheck": "MyS3cr37Pwd",
              |  "userInfo": {
              |    "firstName": "Alessandro",
              |    "lastName": "Odasso",
              |    "contacts": [
              |      {
              |        "type": "EMAIL",
              |        "value": "alessandro.odasso@fake.xauth.com"
              |      }
              |    ]
              |  }
              |}
            """.stripMargin
          )
      }

      response.status mustEqual BAD_REQUEST

      val json: JsValue = response.body[JsValue]

      (json \ "message").as[String] shouldEqual "invitation code not found"
    }

    "require existing invitation" in new WithServer(application) {

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

      // DELETING INVITATION

      val delResponse: WSResponse = await {
        WsTestClient.wsUrl(s"/v1/invitations/$id")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $authAccessToken"
          )
          .delete
      }

      delResponse.status mustEqual NO_CONTENT

      // ATTEMPTING TO CREATE USER

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/users")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON
          )
          .post(
            s"""
               |{
               |  "invitationCode": "$code",
               |  "password": "MyS3cr37Pwd",
               |  "passwordCheck": "MyS3cr37Pwd",
               |  "userInfo": {
               |    "firstName": "Alessandro",
               |    "lastName": "Odasso",
               |    "contacts": [
               |      {
               |        "type": "EMAIL",
               |        "value": "fake@xauth.com"
               |      }
               |    ]
               |  }
               |}
            """.stripMargin
          )
      }

      response.status mustEqual BAD_REQUEST

      val json: JsValue = response.body[JsValue]

      (json \ "message").as[String] shouldEqual "invitation not found"
    }

    "create user" in new WithServer(application) {}
  }

  "get /v1/users/:id" should {
    "require admin authorization" in new WithServer(application) {}

    "handle not-found status" in new WithServer(application) {}

    "retrieve user" in new WithServer(application) {}
  }

  "delete /v1/users/:id" should {
    "require admin authorization" in new WithServer(application) {}
    "handle not-found status" in new WithServer(application) {}
    "delete user" in new WithServer(application) {}
  }
}
