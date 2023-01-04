package io.xauth.service.auth

import io.xauth.Uuid
import io.xauth.config.ApplicationConfiguration
import io.xauth.service.auth.model.AuthRefreshToken
import io.xauth.service.mongo.{MongoDbClient, WorkspaceCollection}
import io.xauth.service.workspace.model.Workspace
import play.api.Logger
import play.api.libs.json.Json
import reactivemongo.play.json.compat._
import reactivemongo.play.json.compat.json2bson.toDocumentWriter

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.Date
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handles business logic for refresh tokens.
 */
@Singleton
class AuthRefreshTokenService @Inject()
(
  mongo: MongoDbClient,
  conf: ApplicationConfiguration
)
(implicit ec: ExecutionContext) {

  private val logger: Logger = Logger(this.getClass)

  /**
   * Finds refresh token.
   *
   * @param token Token to search.
   * @return Returns [[Some(AuthRefreshToken)]] if the token was found.
   */
  def find(token: String)(implicit w: Workspace): Future[Option[AuthRefreshToken]] = {
    require(token.nonEmpty, "token must not be empty")

    mongo.collection(WorkspaceCollection.AuthRefreshToken) flatMap {
      _.find(Json.obj("_id" -> token), None).one[AuthRefreshToken]
    }
  }

  def delete(token: String)(implicit w: Workspace): Unit = {
    require(token.nonEmpty, "token must not be empty")

    mongo.collection(WorkspaceCollection.AuthRefreshToken) flatMap {
      _.findAndRemove(Json.obj("token" -> token)).map(_.result[AuthRefreshToken])
    }
  }

  def deleteAllExpired(implicit w: Workspace): Unit = {
    val now = Date.from(LocalDateTime.now.toInstant(UTC))

    mongo.collection(WorkspaceCollection.AuthRefreshToken) flatMap {
      _.delete(ordered = false)
        .one(Json.obj("expiresAt" -> Json.obj("$lt" -> now)))
    }
  }

  def save(token: String, clientId: String, userId: Uuid)(implicit w: Workspace): Unit = {
    require(token.nonEmpty, "token must not be empty")
    require(clientId.nonEmpty, "client-id must not be empty")
    require(userId != null, "user-id must not be null")

    val now = LocalDateTime.now()
    val registeredAt = now.toInstant(UTC)
    val expiresAt = now.plusSeconds(w.configuration.jwt.expiration.refreshToken).toInstant(UTC)

    val authRefreshToken = AuthRefreshToken(
      token = token,
      clientId = clientId,
      userId = userId,
      expiresAt = Date.from(expiresAt),
      registeredAt = Date.from(registeredAt)
    )

    val writeRes = mongo.collection(WorkspaceCollection.AuthRefreshToken) flatMap {
      _.insert.one(authRefreshToken)
    }

    writeRes.failed.foreach {
      e => logger.error(s"unable to write refresh token: ${e.getMessage}")
    }

    writeRes.map(_ => {})
  }
}
