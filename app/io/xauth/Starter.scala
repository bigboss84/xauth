package io.xauth

import io.xauth.config.ApplicationConfiguration
import io.xauth.service.mongo.MongoDbClient
import io.xauth.service.workspace.WorkspaceService
import play.api.Logger

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
  * Application lifecycle handler.
  */
class Starter @Inject()
(
  mongo: MongoDbClient,
  workspaceService: WorkspaceService,
  conf: ApplicationConfiguration
)
(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  // main datasource initialization
  mongo.pooledConnection(conf.mongoDbUri, Uuid.Zero) onComplete {
    case Failure(e) => logger.error(s"unable to establish database connection: ${e.getMessage}")
    case Success(_) =>
      for {
        ws <- workspaceService.findAll
      } yield ws.filterNot(_.id == Uuid.Zero) foreach { w =>
        // workspace datasource initialization
        mongo.pooledConnection(w.configuration.dbUri, w.id)
      }
  }

  // todo: reads configuration changes and update system default workspace
}