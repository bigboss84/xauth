package io.xauth.task

import akka.actor.ActorSystem
import io.xauth.config.ApplicationConfiguration
import io.xauth.service.auth.AuthCodeService
import io.xauth.service.workspace.WorkspaceService
import play.api.Logger

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * Task that cleans expired codes.
 */
@Singleton
class CodeCleanTask @Inject()
(
  actorSystem: ActorSystem,
  workspaceService: WorkspaceService,
  authCodeService: AuthCodeService,
  conf: ApplicationConfiguration
) {

  private val logger: Logger = Logger(this.getClass)

  private implicit val ec: ExecutionContext = actorSystem.dispatchers.defaultGlobalDispatcher

  actorSystem.scheduler.scheduleWithFixedDelay(
    initialDelay = 90 seconds,
    delay = conf.taskCodeCleanInterval minutes
  ) { () =>
    workspaceService.findAll.onComplete {
      case Failure(e) => logger.error(s"unable to clean codes: ${e.getMessage}")
      case Success(ws) => ws.foreach { implicit w =>
        logger.debug(s"cleaning expired codes for workspace ${w.id}")
        authCodeService.deleteAllExpired
      }
    }
  }

}