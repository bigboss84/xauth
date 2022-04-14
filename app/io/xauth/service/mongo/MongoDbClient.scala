package io.xauth.service.mongo

import io.xauth.Uuid
import io.xauth.config.ApplicationConfiguration
import io.xauth.service.mongo.SystemCollection.SystemCollection
import io.xauth.service.mongo.WorkspaceCollection.WorkspaceCollection
import io.xauth.service.workspace.model.Workspace
import play.Environment._
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import reactivemongo.api
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.{DB, MongoConnection}

import java.net.Socket
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * Client for MongoDb
  */
@Singleton
class MongoDbClient @Inject()
(
  conf: ApplicationConfiguration,
  al: ApplicationLifecycle
)
(implicit ec: ExecutionContext) {

  private val logger: Logger = Logger(this.getClass)

  private val uriRegex = "^.*/(?<db>[\\w\\d_-]+)(?:\\?.*)?$".r

  // mutable map for connection pools
  private val pools = scala.collection.mutable.Map[String, DB]()

  // todo: make checks to ensure uniqueness hosts in map

  def pooledConnection(uri: String, id: Uuid): Future[DB] = {
    val f = connect(uri) map { db =>
      pools += id.stringValue -> db
      db
    }

    f onComplete {
      case Failure(e) => println(s"datasource init error: ${e.getMessage}")
      case Success(db) => println(s"new datasource initialized: ${db.name}")
    }

    f
  }

  def purgeConnection(id: Uuid): Future[Unit] = {
    pools.get(id.stringValue) map { db =>
      logger.info(s"purging connection for id ${id.stringValue}")
      pools -= id.stringValue
      db.connection.close()(10 seconds) map {
        r =>
          logger.info(s"closed connection for id ${id.stringValue}: $r")
          logger.info(s"current managed connections: ${pools.size}")
      }
    } getOrElse successful(())
  }

  def connect(uri: String): Future[DB] = {
    val resolvedUri = resolveKeys(uri)

    logger.info(s"connecting to mongodb '$resolvedUri'")

    val f = for {
      parsedUri <- MongoConnection.fromString(resolvedUri)
      connection <- new api.AsyncDriver().connect(parsedUri)
    } yield connection

    val dbName = uriRegex.findFirstMatchIn(resolvedUri).map(_.group("db")).get

    f.flatMap(_.database(dbName))
  }

  def database(workspaceId: Uuid): Try[DB] = Try(pools(workspaceId.stringValue))

  def collection(t: SystemCollection): Future[BSONCollection] =
    Future.fromTry(database(Uuid.Zero).map(_.collection[BSONCollection](t.value)))

  def collection(t: WorkspaceCollection)(implicit w: Workspace): Future[BSONCollection] =
    Future.fromTry(database(w.id).map(_.collection[BSONCollection](t.value)))

  def collections(t: WorkspaceCollection, ws: Workspace*): Seq[Future[BSONCollection]] = ws.map { w =>
    Future.fromTry(database(w.id).map(_.collection[BSONCollection](t.value)))
  }

  def collections(t: WorkspaceCollection): Seq[(Uuid, BSONCollection)] =
    pools.toSeq.map(x => Uuid(x._1) -> x._2.collection[BSONCollection](t.value))

  private def resolveKeys(s: String): String = {
    val k = "keyStore"
    // when keyStore key is present, then convert it in absolute format
    s"(?<=$k\\=)(?<ks>.*)(?>&)".r
      .findFirstMatchIn(s)
      .map(_.group("ks"))
      .map(v => v -> s"file://${simple.rootPath.getAbsolutePath.replaceAll("\\/\\.$", "")}/conf/$v")
      .map(t => s.replace(s"$k=${t._1}", s"$k=${t._2}"))
      .getOrElse(s)
  }

  def lookup(uri: String): Future[Boolean] = {
    // todo: make unique, see top uriRegex
    val uriRegex = """^mongodb://(?:([\w-]+:[\w-]+)@)?(?<h>.*):(?<p>\d+)/(?<s>.*)\??.*$""".r
    uriRegex.findFirstMatchIn(uri) match {
      case Some(m) => Future {
        try {
          val sock = new Socket(m.group("h"), m.group("p").toInt)
          val connected = sock.isConnected
          sock.close()
          connected
        } catch {
          case e: Exception =>
            println(e.getMessage)
            false
        }
      }
      case _ => successful(false)
    }
  }

  def configureIndexes(implicit w: Workspace): Future[Boolean] = {

    def setup(cType: WorkspaceCollection, key: String, indexType: IndexType): Future[Boolean] = {
      val f = for {
        c <- collection(cType)
        b <- c.indexesManager.ensure(Index(key = (key -> indexType) :: Nil, unique = true))
      } yield b

      f onComplete {
        case Success(b) =>
          if (b) logger.info(s"created index ${cType.value}.$key")
          else logger.warn(s"index creation failed for ${cType.value}.$key")
        case Failure(e) => logger.error(s"index creation failed for ${cType.value}.$key (${e.getMessage})")
      }

      f
    }

    // creating all needed indexes
    for {
      f1 <- setup(WorkspaceCollection.AuthUser, "username", IndexType.Ascending)
      f2 <- setup(WorkspaceCollection.AuthUser, "userInfo.contacts.value", IndexType.Ascending)
      f3 <- setup(WorkspaceCollection.Invitation, "email", IndexType.Ascending)
    } yield f1 && f2 && f3
  }

  al.addStopHook { () =>
    logger.info(s"closing ${pools.size} mongodb connections...")
    Future.sequence {
      pools.map(_._2.connection.close()(2 seconds))
    }
  }
}
