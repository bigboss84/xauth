package io.xauth.service.auth

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.Date

import io.xauth.Uuid
import io.xauth.config.ApplicationConfiguration
import io.xauth.service.auth.model.AuthRefreshToken
import io.xauth.service.mongo.MongoDbClient
import javax.inject.{Inject, Singleton}
import play.api.Logger
import reactivemongo.bson.BSONDocument

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

  /**
    * Finds refresh token.
    *
    * @param token Token to search.
    * @return Returns [[Some(AuthRefreshToken)]] if the token was found.
    */
  def find(token: String): Future[Option[AuthRefreshToken]] = {
    require(token.length > 0, "token length must not be empty")

    mongo.collections.authRefreshToken.flatMap {
      _.find(BSONDocument("_id" -> token), None).one[AuthRefreshToken]
    }
  }

  def delete(token: String): Unit = {
    require(token.length > 0, "token length must not be empty")

    mongo.collections.authRefreshToken.flatMap {
      _.findAndRemove(BSONDocument("token" -> token)).map(_.result[AuthRefreshToken])
    }
  }

  def deleteAllExpired(): Unit = {
    val now = Date.from(LocalDateTime.now.toInstant(UTC))

    mongo.collections.authRefreshToken.flatMap {
      _.delete(ordered = false)
        .one(BSONDocument("expiresAt" -> BSONDocument("$lt" -> now)))
        .map(_ => {})
    }
  }

  def save(token: String, clientId: String, userId: Uuid): Unit = {
    require(token.length > 0, "token length must not be empty")
    require(clientId.length > 0, "client-id length must not be empty")
    require(userId != null, "user-id must not be null")

    val now = LocalDateTime.now()
    val registeredAt = now.toInstant(UTC)
    val expiresAt = now.plusHours(12).toInstant(UTC)

    val authRefreshToken = AuthRefreshToken(
      token = token,
      clientId = clientId,
      userId = userId,
      expiresAt = Date.from(expiresAt),
      registeredAt = Date.from(registeredAt)
    )

    val writeRes = mongo.collections.authRefreshToken.flatMap {
      _.insert(authRefreshToken)
    }

    writeRes.failed.foreach {
      e => Logger.error(s"unable to write refresh token: ${e.getMessage}")
    }

    writeRes.map(_ => {})
  }
}
