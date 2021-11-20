package io.xauth.auth.web

import play.api.libs.ws.WSResponse
import play.api.test.{WsTestClient, _}

/**
  * Tests the application startup.
  */
class ReachabilitySpec extends PlaySpecification {

  "Application" should {
    "be reachable" in new WithServer {
      val response: WSResponse = await(WsTestClient.wsUrl("/").get())

      response.status must equalTo(OK)
      response.body must containing("It works!")
    }
  }

}
