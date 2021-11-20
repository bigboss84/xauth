package io.xauth.service.mongo

import java.util.concurrent.TimeUnit.SECONDS

import io.xauth.config.ApplicationConfiguration
import javax.inject.{Inject, Singleton}
import play.Environment._
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

/**
  * Client for MongoDb
  */
@Singleton
class MongoDbClient @Inject()
(
  conf: ApplicationConfiguration, al: ApplicationLifecycle
)
(implicit ec: ExecutionContext) {

  private val uriRegex = "^.*/(?<db>[\\w\\d_]+)(?:\\?.*)?$".r

  private val driver = new MongoDriver
  private val uri = parseUri(conf.mongoDbUri)
  private val parsedUri = MongoConnection.parseURI(uri)
  private val conn = parsedUri.map(driver.connection)
  private val futureConnection = Future.fromTry(conn)
  private val dbName = uriRegex.findFirstMatchIn(uri).map(_.group("db")).get

  private def parseUri(s: String) = {
    val k = "keyStore"
    // when keyStore key is present, then convert it in absolute format
    s"(?<=$k\\=)(?<ks>.*)(?>&)".r
      .findFirstMatchIn(s)
      .map(_.group("ks"))
      .map(v => v -> s"file://${simple.rootPath.getAbsolutePath.replaceAll("\\/\\.$", "")}/conf/$v")
      .map(t => s.replace(s"$k=${t._1}", s"$k=${t._2}"))
      .getOrElse(s)
  }

  Logger.info(s"using mongodb uri '$uri'")
  Logger.info(s"using database '$dbName'")

  def db: Future[DefaultDB] = futureConnection flatMap {
    _.database(dbName)
  }

  def writeConcern: reactivemongo.api.commands.WriteConcern = conn.get.options.writeConcern

  val collections: Collections = new Collections

  class Collections {
    // @formatter:off
    def authAccessAttempt: Future[BSONCollection] = db.map(_.collection("k_auth_access_attempt"))
    def authCode: Future[BSONCollection] = db.map(_.collection("k_auth_code"))
    def authClient: Future[BSONCollection] = db.map(_.collection("k_auth_client"))
    def authRefreshToken: Future[BSONCollection] = db.map(_.collection("k_auth_refresh_token"))
    def authUser: Future[BSONCollection] = db.map(_.collection("k_auth_user"))
    def invitation: Future[BSONCollection] = db.map(_.collection("k_invitation"))
    def app: Future[BSONCollection] = db.map(_.collection("k_app"))
    // @formatter:on
  }

  al.addStopHook { () =>
    Logger.info("closing mongodb connection...")
    futureConnection.flatMap {
      _.askClose()(FiniteDuration(60, SECONDS))
    }
  }
}
