package io.xauth.task

import akka.actor.ActorSystem
import io.xauth.config.ApplicationConfiguration
import io.xauth.service.auth.AuthRefreshTokenService
import io.xauth.service.workspace.WorkspaceService
import play.api.Logger

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * Task that cleans expired refresh tokens.
 */
@Singleton
class TokenCleanTask @Inject()
(
  actorSystem: ActorSystem,
  workspaceService: WorkspaceService,
  authRefreshTokenService: AuthRefreshTokenService,
  conf: ApplicationConfiguration
) {

  private val logger: Logger = Logger(this.getClass)

  private implicit val ec: ExecutionContext = actorSystem.dispatchers.defaultGlobalDispatcher

  actorSystem.scheduler.scheduleWithFixedDelay(
    initialDelay = 60 seconds,
    delay = conf.taskTokenCleanInterval minutes
  ) { () =>
    workspaceService.findAll.onComplete {
      case Failure(e) => logger.error(s"unable to clean tokens: ${e.getMessage}")
      case Success(ws) => ws.foreach { implicit w =>
        logger.debug(s"cleaning expired refresh tokens for workspace ${w.id}")
        authRefreshTokenService.deleteAllExpired
      }
    }
  }

}