package io.xauth.service.auth

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.Date

import io.xauth.service.auth.model.AuthClient
import io.xauth.util.Implicits.Md5String
import io.xauth.service.mongo.MongoDbClient
import javax.inject.{Inject, Singleton}
import reactivemongo.api.Cursor
import reactivemongo.bson.BSONDocument

import scala.concurrent.{ExecutionContext, Future}

/**
  * Service that handles trusted clients business logic.
  */
@Singleton
class AuthClientService @Inject()
(
  mongo: MongoDbClient
)
(implicit ec: ExecutionContext) {

  def find(id: String): Future[Option[AuthClient]] = {
    require(id.length > 0, "id length must not be empty")

    mongo.collections.authClient.flatMap {
      _.find(BSONDocument("_id" -> id), None).one[AuthClient]
    }
  }

  def findAll: Future[List[AuthClient]] = {
    mongo.collections.authClient.flatMap {
      _
        .find(BSONDocument(), None)
        .cursor[AuthClient]()
        .collect[List](-1, Cursor.FailOnError[List[AuthClient]]())
    }
  }

  def create(id: String, secret: String): Future[Either[String, AuthClient]] = {
    require(id.length > 0, "id must not be empty")
    require(secret.length > 0, "secret must not be empty")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))
    val c = AuthClient(id, secret.md5, now, now)

    val f = mongo.collections.authClient.flatMap {
      _.insert(c)
    }

    f map { w => if (w.ok) Right(c) else Left(s"unable to write client '$id'") }
  }

  def update(c: AuthClient): Future[Option[AuthClient]] = {
    require(c.id.length > 0, "id length must not be empty")
    require(c.secret.length >= 10, "id length must be greater than ten")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))

    mongo.collections.authClient.flatMap {
      _.findAndUpdate(
        BSONDocument("_id" -> c.id),
        BSONDocument("$set" -> BSONDocument("secret" -> c.secret.md5, "updatedAt" -> now)),
        fetchNewObject = true
      ) map {
        _.result[AuthClient]
      }
    }
  }

  def delete(id: String): Future[Boolean] = {
    require(id.length > 0, "id length must not be empty")

    mongo.collections.authClient.flatMap {
      _
        .delete(ordered = false)
        .one(BSONDocument("_id" -> id))
        .map(_.n > 0)
    }
  }
}