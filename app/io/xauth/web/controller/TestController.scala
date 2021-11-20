package io.xauth.web.controller

import akka.actor.ActorRef
import io.xauth.service.auth.AuthUserService
import javax.inject.{Inject, Named, Singleton}
import play.api.mvc.{AbstractController, ControllerComponents}

/**
  * Temporary test controller to manually trigger some actions.
  */
@Singleton
class TestController @Inject()
(
  @Named("account-activator") accountActivatorActor: ActorRef,
  authService: AuthUserService,
  cc: ControllerComponents
) extends AbstractController(cc) {

  def test = Action { request =>
    Ok
  }
}
