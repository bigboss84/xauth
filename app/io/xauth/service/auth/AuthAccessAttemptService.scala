package io.xauth.service.auth

import io.xauth.service.auth.model.AuthAccessAttempt
import io.xauth.service.mongo.MongoDbClient
import play.api.Logger
import reactivemongo.api.ReadConcern
import reactivemongo.bson.BSONDocument

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

  def save(username: String, clientId: String, remoteAddress: String): Future[AuthAccessAttempt] = {
    require(username != null, "username must not be null")
    require(clientId != null, "clientId must not be null")
    require(remoteAddress != null, "remoteAddress must not be null")

    val now = LocalDateTime.now().toInstant(UTC)
    val attempt = AuthAccessAttempt(username, clientId, remoteAddress, Date.from(now))

    val writeRes = mongo.collections.authAccessAttempt.flatMap {
      _.insert(attempt)
    }

    writeRes.failed.foreach {
      e => Logger.error(s"unable to write access attempt: ${e.getMessage}")
    }

    writeRes map { _ => attempt }
  }

  def findAllByUsername(username: String): Future[Int] = {
    require(username != null, "username must not be null")

    mongo.collections.authAccessAttempt.flatMap {
      _
        .count(Some(BSONDocument("username" -> username)), None, 0, None, ReadConcern.Available)
        .map(_.toInt)
    }
  }

  def deleteByUsername(username: String): Unit = {
    require(username != null, "username must not be null")

    mongo.collections.authAccessAttempt.flatMap {
      _.delete(ordered = false)
        .one(BSONDocument("username" -> username))
        .map(_ => {})
    }
  }
}
