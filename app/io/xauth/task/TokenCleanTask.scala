package io.xauth.task

import akka.actor.ActorSystem
import io.xauth.config.ApplicationConfiguration
import io.xauth.service.auth.AuthRefreshTokenService
import javax.inject.{Inject, Singleton}
import play.api.Logger

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Task that cleans expired refresh tokens.
  */
@Singleton
class TokenCleanTask @Inject()
(
  actorSystem: ActorSystem,
  authRefreshTokenService: AuthRefreshTokenService,
  conf: ApplicationConfiguration
)(implicit executionContext: ExecutionContext) {

  actorSystem.scheduler.schedule(
    initialDelay = 0.microseconds,
    interval = conf.taskTokenCleanInterval.minutes
  ) {
    Logger.debug("cleaning refresh token")
    authRefreshTokenService.deleteAllExpired()
  }

}