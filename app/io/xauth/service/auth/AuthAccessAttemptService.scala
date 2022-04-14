package io.xauth.service.auth

import io.xauth.service.auth.model.AuthAccessAttempt
import io.xauth.service.mongo.{MongoDbClient, WorkspaceCollection}
import io.xauth.service.workspace.model.Workspace
import play.api.Logger
import play.api.libs.json.Json
import reactivemongo.api.ReadConcern
import reactivemongo.play.json.compat._
import reactivemongo.play.json.compat.json2bson.toDocumentWriter

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.Date
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handles the business logic for authentication attempts.
 */
@Singleton
class AuthAccessAttemptService @Inject()
(
  mongo: MongoDbClient
)
(implicit ec: ExecutionContext) {

  private val logger: Logger = Logger(this.getClass)

  def save(username: String, clientId: String, remoteAddress: String)(implicit w: Workspace): Future[AuthAccessAttempt] = {
    require(username != null, "username must not be null")
    require(clientId != null, "clientId must not be null")
    require(remoteAddress != null, "remoteAddress must not be null")

    val now = LocalDateTime.now().toInstant(UTC)
    val attempt = AuthAccessAttempt(username, clientId, remoteAddress, Date.from(now))

    val writeRes = mongo.collection(WorkspaceCollection.AuthAccessAttempt) flatMap {
      _.insert.one(attempt)
    }

    writeRes.failed.foreach {
      e => logger.error(s"unable to write access attempt: ${e.getMessage}")
    }

    writeRes map { _ => attempt }
  }

  def findAllByUsername(username: String)(implicit w: Workspace): Future[Int] = {
    require(username != null, "username must not be null")

    mongo.collection(WorkspaceCollection.AuthAccessAttempt) flatMap {
      _
        .count(Some(Json.obj("username" -> username)), None, 0, None, ReadConcern.Available)
        .map(_.toInt)
    }
  }

  def deleteByUsername(username: String)(implicit w: Workspace): Unit = {
    require(username != null, "username must not be null")

    mongo.collection(WorkspaceCollection.AuthAccessAttempt) flatMap {
      _.delete(ordered = false)
        .one(Json.obj("username" -> username))
        .map(_ => {})
    }
  }
}
