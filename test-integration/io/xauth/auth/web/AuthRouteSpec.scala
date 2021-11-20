package io.xauth.auth.web

import java.lang.Thread.sleep
import java.text.SimpleDateFormat
import java.util.Date

import io.xauth.auth.config.ApplicationConfiguration
import io.xauth.auth.service.auth.model.AuthRole.User
import io.xauth.auth.service.auth.model.AuthStatus.{Disabled, Enabled}
import io.xauth.auth.service.mongo.MongoDbClient
import io.xauth.auth.web.Initializer.Data.{createClient, createUser, expireRefreshToken}
import org.specs2.specification.BeforeAfterAll
import play.api.Logger
import play.api.http.ContentTypes
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSAuthScheme.BASIC
import play.api.libs.ws.{BodyReadable, StandaloneWSResponse, WSResponse}
import play.api.test.{PlaySpecification, WithServer, WsTestClient}

import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

/**
  * Tests authentication routes.
  */
class AuthRouteSpec extends PlaySpecification with BeforeAfterAll {

  import Initializer._

  private implicit val ec: ExecutionContext = ExecutionContext.global

  private val mongodb = application.injector.instanceOf[MongoDbClient]
  private val conf = application.injector.instanceOf[ApplicationConfiguration]

  implicit val jsValueBodyReadable: BodyReadable[JsValue] =
    BodyReadable((r: StandaloneWSResponse) => Json.parse(r.body))

  object RegularExpression {
    val Uuid: Regex = "^[a-z\\d]{8}-[a-z\\d]{4}-[a-z\\d]{4}-[a-z\\d]{4}-[a-z\\d]{12}$".r
  }

  val iso8601DateFormat = "yyyy-MM-dd'T'HH:mm:ss.sss'Z'"
  val dateFormat = new SimpleDateFormat(iso8601DateFormat)

  override def beforeAll(): Unit = {
    createClient("trusted-client", "b8f19ff88bcdc0e6d4a91ae927cfc452")
    createClient("trusted-client-1", "b8f19ff88bcdc0e6d4a91ae927cfc452")
    createUser("david.bohm@xauth.com", "Th3H4ppyW0rld", Enabled, "David", "Bohm", User)
    createUser("peter.higgs@xauth.com", "Corr3ctPa55Word", Disabled, "Peter", "Higgs", User)
    createUser("karl.heisenberg@xauth.com", "MyPr1nc1pleIsF4lse", Enabled, "Karl", "Heisenberg", User)
  }

  override def afterAll(): Unit = Data.clean()

  //
  // BASIC AUTHENTICATED ROUTES
  //

  "/v1/auth/token" should {
    "needs basic authentication" in new WithServer {

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post("{}")
      }

      response.status must equalTo(FORBIDDEN)
      (response.body[JsValue] \ "message").as[String] must equalTo("client credentials are required")
    }

    "deny access for bad client credentials" in new WithServer {

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("bad-client-id", "secret", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post("{}")
      }

      response.status must equalTo(FORBIDDEN)
      (response.body[JsValue] \ "message").as[String] must equalTo("bad client credentials")
    }

    "deny access for invalid client credentials" in new WithServer {

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "invalid-secret", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post("{}")
      }

      response.status must equalTo(UNAUTHORIZED)
      (response.body[JsValue] \ "message").as[String] must equalTo("invalid client credentials")
    }

    "deny access for bad user" in new WithServer {

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "username": "albert.einstein@xauth.com",
              |  "password": "Corr3ctPa55Word"
              |}
            """.stripMargin
          )
      }

      response.status must equalTo(FORBIDDEN)
      (response.body[JsValue] \ "message").as[String] must equalTo("invalid user credentials")
    }

    "deny access for invalid user credentials" in new WithServer {

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "username": "david.bohm@xauth.com",
              |  "password": "WROng.pa55w0rd"
              |}
            """.stripMargin
          )
      }

      response.status must equalTo(FORBIDDEN)
      (response.body[JsValue] \ "message").as[String] must equalTo("invalid user credentials")
    }

    "deny access for disabled user" in new WithServer {

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/token")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "username": "peter.higgs@xauth.com",
              |  "password": "Corr3ctPa55Word"
              |}
            """.stripMargin
          )
      }

      response.status must equalTo(FORBIDDEN)
      (response.body[JsValue] \ "message").as[String] must equalTo("account is currently 'DISABLED'")
    }

    "grant access for valid user credentials" in new WithServer(application) {

      val response: WSResponse = await {
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

      response.status must equalTo(OK)

      val json: JsValue = response.body[JsValue]

      (json \ "tokenType").as[String] must equalTo("bearer")
      (json \ "accessToken").as[String].split("\\.") must have size 3
      (json \ "refreshToken").as[String] must have size 40
      (json \ "expiresIn").as[Int] must equalTo(conf.jwtExpirationAccessToken / 60)
    }

    s"blocks user after ${conf.maxLoginAttempts} login attempts" in new WithServer(application) {

      (1 to conf.maxLoginAttempts) foreach { i =>
        val response: WSResponse = await {
          WsTestClient.wsUrl("/v1/auth/token")
            .withAuth("trusted-client", "trusted-client", BASIC)
            .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
            .post(
              """
                |{
                |  "username": "karl.heisenberg@xauth.com",
                |  "password": "Wr0ng3ctPa55Word"
                |}
              """.stripMargin
            )
        }

        response.status must equalTo(FORBIDDEN)

        val json: JsValue = response.body[JsValue]

        (json \ "message").as[String] must equalTo("invalid user credentials")
      }

      sleep(3000) // waiting for blocked status

      val blockedResponse: WSResponse = await {
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

      blockedResponse.status must equalTo(FORBIDDEN)

      val json: JsValue = blockedResponse.body[JsValue]

      (json \ "message").as[String] must equalTo("account is currently 'BLOCKED'")
    }
  }

  "/v1/auth/check" should {
    "return valid status for valid token" in new WithServer {

      val tokenResponse: WSResponse = await {
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

      tokenResponse.status must equalTo(OK)

      val accessToken: String =
        (tokenResponse.body[JsValue] \ "accessToken").as[String]

      val checkResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/check")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer $accessToken"
          )
          .get
      }

      checkResponse.status must equalTo(OK)

      val json: JsValue = checkResponse.body[JsValue]

      (json \ "tokenStatus").as[String] must equalTo("VALID")
    }

    "return invalid status for invalid token" in new WithServer {
      val checkResponse: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/check")
          .withHttpHeaders(
            CONTENT_TYPE -> ContentTypes.JSON,
            AUTHORIZATION -> s"Bearer invalid-access-token"
          )
          .get
      }

      checkResponse.status must equalTo(OK)

      val json: JsValue = checkResponse.body[JsValue]

      (json \ "tokenStatus").as[String] must equalTo("INVALID")
    }
  }

  "/v1/auth/refresh" should {

    "deny token refresh for for invalid refresh token" in new WithServer {

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/refresh")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            """
              |{
              |  "refreshToken": "f063dbea8de64b601050687e0c18d9e59f081d1a"
              |}
            """.stripMargin
          )
      }

      response.status must equalTo(FORBIDDEN)

      val json: JsValue = response.body[JsValue]

      (json \ "message").as[String] must equalTo("invalid refresh token")
    }

    "deny token refresh for wrong client id" in new WithServer {

      val tokenResponse: WSResponse = await {
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

      tokenResponse.status must equalTo(OK)

      val refreshToken: String =
        (tokenResponse.body[JsValue] \ "refreshToken").as[String]

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/refresh")
          .withAuth("trusted-client-1", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            s"""
               |{
               |  "refreshToken": "$refreshToken"
               |}
            """.stripMargin
          )
      }

      response.status must equalTo(FORBIDDEN)

      val json: JsValue = response.body[JsValue]

      (json \ "message").as[String] must equalTo("wrong refresh token")
    }

    "return new access token" in new WithServer {

      val tokenResponse: WSResponse = await {
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

      tokenResponse.status must equalTo(OK)

      val tokenJson: JsValue = tokenResponse.body[JsValue]

      val accessToken: String = (tokenJson \ "accessToken").as[String]
      val refreshToken: String = (tokenJson \ "refreshToken").as[String]

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/refresh")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            s"""
               |{
               |  "refreshToken": "$refreshToken"
               |}
            """.stripMargin
          )
      }

      response.status must equalTo(OK)

      val json: JsValue = response.body[JsValue]

      (json \ "tokenType").as[String] must equalTo("bearer")
      (json \ "accessToken").as[String].split("\\.") must have size 3
      (json \ "refreshToken").as[String] must equalTo(refreshToken)
      (json \ "refreshToken").as[String] must have size 40
      (json \ "expiresIn").as[Int] must equalTo(30)
    }

    s"refresh token expires" in new WithServer(application) {

      val tokenResponse: WSResponse = await {
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

      tokenResponse.status must equalTo(OK)

      val refreshToken: String =
        (tokenResponse.body[JsValue] \ "refreshToken").as[String]

      // !!! forcing token expiration !!!
      expireRefreshToken(refreshToken)

      val ms: Long = (conf.taskTokenCleanInterval + 1) * 60 * 1000

      Logger.info(s"waiting $ms ms for refresh token expiration")
      sleep(ms)

      val response: WSResponse = await {
        WsTestClient.wsUrl("/v1/auth/refresh")
          .withAuth("trusted-client", "trusted-client", BASIC)
          .withHttpHeaders(CONTENT_TYPE -> ContentTypes.JSON)
          .post(
            s"""
               |{
               |  "refreshToken": "$refreshToken"
               |}
              """.stripMargin
          )
      }

      response.status mustEqual FORBIDDEN

      val json: JsValue = response.body[JsValue]

      (json \ "message").as[String] mustEqual "invalid refresh token"
    }

    //
    // UNAUTHENTICATED ROUTES
    //

    "/v1/auth/activation" should {
      // todo: error cases
      "enable user account" in new WithServer {}
    }

    "/v1/auth/password-forgotten" should {
      // todo: error cases
      "send code reset code" in new WithServer {}
    }

    "/v1/auth/password-reset" should {
      // todo: error cases
      "reset user password" in new WithServer {}
    }

    //
    // TOKEN AUTHENTICATED ROUTES
    //

    "/v1/auth/user" should {
      "return request user" in new WithServer {

        // AUTHENTICATION

        val tokenResponse: WSResponse = await {
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

        tokenResponse.status must equalTo(OK)

        val accessToken: String =
          (tokenResponse.body[JsValue] \ "accessToken").as[String]

        // CALLING TESTED ROUTE

        val response: WSResponse = await {
          WsTestClient.wsUrl("/v1/auth/user")
            .withHttpHeaders(
              CONTENT_TYPE -> ContentTypes.JSON,
              AUTHORIZATION -> s"Bearer $accessToken"
            )
            .get
        }

        response.status must equalTo(OK)

        val json: JsValue = response.body[JsValue]

        (json \ "id").as[String] must beMatching(RegularExpression.Uuid)
        (json \ "username").as[String] mustEqual "david.bohm"
        (json \ "password").isDefined must beFalse
        (json \ "roles").as[Array[String]] must have size 1
        (json \ "roles" \ 0).as[String] mustEqual "USER"
        (json \ "status").as[String] mustEqual "ENABLED"
        (json \ "userInfo" \ "firstName").as[String] mustEqual "David"
        (json \ "userInfo" \ "lastName").as[String] mustEqual "Bohm"
        (json \ "userInfo" \ "contacts").as[Array[JsValue]] must have size 2
        (json \ "userInfo" \ "contacts" \ 0 \ "type").as[String] mustEqual "EMAIL"
        (json \ "userInfo" \ "contacts" \ 0 \ "value").as[String] mustEqual "david.bohm@xauth.com"
        (json \ "userInfo" \ "contacts" \ 0 \ "trusted").as[Boolean] must beTrue
        (json \ "userInfo" \ "contacts" \ 1 \ "type").as[String] mustEqual "MOBILE_NUMBER"
        (json \ "userInfo" \ "contacts" \ 1 \ "value").as[String] mustEqual "+391234567890"
        (json \ "userInfo" \ "contacts" \ 1 \ "trusted").as[Boolean] must beFalse

        val registeredAt: Date = dateFormat.parse((json \ "registeredAt").as[String])
        val updatedAt: Date = dateFormat.parse((json \ "updatedAt").as[String])

        (registeredAt compareTo updatedAt) must beLessThanOrEqualTo(0)
      }

      s"access token expires after ${conf.jwtExpirationAccessToken} seconds" in new WithServer(application) {

        // AUTHENTICATION

        val tokenResponse: WSResponse = await {
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

        tokenResponse.status must equalTo(OK)

        val accessToken: String =
          (tokenResponse.body[JsValue] \ "accessToken").as[String]

        // CALLING JWT SECURED ROUTE

        sleep((conf.jwtExpirationAccessToken + 1) * 1000) // waiting for expiration time

        val response: WSResponse = await {
          WsTestClient.wsUrl("/v1/auth/user")
            .withHttpHeaders(
              CONTENT_TYPE -> ContentTypes.JSON,
              AUTHORIZATION -> s"Bearer $accessToken"
            )
            .get
        }

        response.status must equalTo(UNAUTHORIZED)

        val json: JsValue = response.body[JsValue]

        (json \ "message").as[String] mustEqual "invalid access token"
      }
    }

    "/v1/auth/contact-trust" should {
      "send user contact trust code" in new WithServer {}
    }

    "/v1/auth/contact-activation" should {
      "trust user contact" in new WithServer {}
    }
  }
}