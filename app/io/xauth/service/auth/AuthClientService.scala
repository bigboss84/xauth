package io.xauth.service.auth

import io.xauth.service.auth.model.AuthClient
import io.xauth.service.mongo.{MongoDbClient, WorkspaceCollection}
import io.xauth.service.workspace.model.Workspace
import io.xauth.util.Implicits.Md5String
import play.api.libs.json.Json
import reactivemongo.api.Cursor
import reactivemongo.play.json.compat._
import reactivemongo.play.json.compat.json2bson.toDocumentWriter

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.Date
import javax.inject.{Inject, Singleton}
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

  def find(id: String)(implicit w: Workspace): Future[Option[AuthClient]] = {
    require(id.nonEmpty, "id length must not be empty")

    mongo.collection(WorkspaceCollection.AuthClient) flatMap {
      _.find(Json.obj("_id" -> id), None).one[AuthClient]
    }
  }

  def findAll(implicit w: Workspace): Future[List[AuthClient]] = {
    mongo.collection(WorkspaceCollection.AuthClient) flatMap {
      _
        .find(Json.obj(), None)
        .cursor[AuthClient]()
        .collect[List](-1, Cursor.FailOnError[List[AuthClient]]())
    }
  }

  def create(id: String, secret: String)(implicit w: Workspace): Future[Either[String, AuthClient]] = {
    require(id.nonEmpty, "id must not be empty")
    require(secret.nonEmpty, "secret must not be empty")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))
    val client = AuthClient(id, secret.md5, now, now)

    for {
      collection <- mongo.collection(WorkspaceCollection.AuthClient)
      result <- collection.insert.one(client)
    } yield if (result.n == 1) Right(client) else Left(s"unable to write client '$id'")
  }

  def update(c: AuthClient)(implicit w: Workspace): Future[Option[AuthClient]] = {
    require(c.id.nonEmpty, "id length must not be empty")
    require(c.secret.length >= 10, "id length must be greater than ten")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))

    mongo.collection(WorkspaceCollection.AuthClient) flatMap {
      _.findAndUpdate(
        Json.obj("_id" -> c.id),
        Json.obj("$set" -> Json.obj("secret" -> c.secret.md5, "updatedAt" -> now)),
        fetchNewObject = true
      ) map {
        _.result[AuthClient]
      }
    }
  }

  def delete(id: String)(implicit w: Workspace): Future[Boolean] = {
    require(id.nonEmpty, "id must not be empty")

    mongo.collection(WorkspaceCollection.AuthClient) flatMap {
      _
        .delete(ordered = false)
        .one(Json.obj("_id" -> id))
        .map(_.n > 0)
    }
  }
}