package io.xauth.task

import akka.actor.ActorSystem
import io.xauth.config.ApplicationConfiguration
import io.xauth.service.auth.AuthCodeService
import javax.inject.{Inject, Singleton}
import play.api.Logger

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Task that cleans expired codes.
  */
@Singleton
class CodeCleanTask @Inject()
(
  actorSystem: ActorSystem,
  authCodeService: AuthCodeService,
  conf: ApplicationConfiguration
)(implicit executionContext: ExecutionContext) {

  actorSystem.scheduler.schedule(
    initialDelay = 0.microseconds,
    interval = conf.taskCodeCleanInterval.minutes
  ) {
    Logger.debug("cleaning expired codes")
    authCodeService.deleteAllExpired()
  }

}