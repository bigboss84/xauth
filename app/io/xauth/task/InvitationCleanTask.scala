package io.xauth.task

import akka.actor.ActorSystem
import io.xauth.config.ApplicationConfiguration
import io.xauth.service.invitation.InvitationService
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
class InvitationCleanTask @Inject()
(
  actorSystem: ActorSystem,
  workspaceService: WorkspaceService,
  invitationService: InvitationService,
  conf: ApplicationConfiguration
) {

  private val logger: Logger = Logger(this.getClass)

  private implicit val ec: ExecutionContext = actorSystem.dispatchers.defaultGlobalDispatcher

  actorSystem.scheduler.scheduleWithFixedDelay(
    initialDelay = 190 seconds,
    delay = conf.taskInvitationCleanInterval minutes
  ) { () =>
    workspaceService.findAll.onComplete {
      case Failure(e) => logger.error(s"unable to clean invitations: ${e.getMessage}")
      case Success(ws) => ws.foreach { implicit w =>
        logger.debug(s"cleaning expired invitations for workspace ${w.id}")
        invitationService.deleteAllExpired
      }
    }
  }

}