package io.xauth.service.app

import io.xauth.service.app.model.AppKey.AppKey
import io.xauth.service.app.model.AppSetting
import io.xauth.service.mongo.{MongoDbClient, SystemCollection}
import play.api.Logger
import play.api.libs.json.Json
import reactivemongo.play.json.compat._
import reactivemongo.play.json.compat.json2bson.toDocumentWriter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Handles application settings.
 */
@Singleton
class AppSettingService @Inject()
(
  mongo: MongoDbClient
)
(implicit ec: ExecutionContext) {

  private val logger: Logger = Logger(this.getClass)

  def save(key: AppKey, value: String): Future[AppSetting] = {
    require(key != null, "key must not be null")
    require(value != null, "value must not be null")

    val app = AppSetting(key, value)

    val writeRes = mongo.collection(SystemCollection.App) flatMap {
      _.findAndUpdate(Json.obj("_id" -> key), app, upsert = true) map {
        _.result[AppSetting]
      }
    }

    writeRes.failed.foreach {
      e => logger.error(s"unable to write app conf: ${e.getMessage}")
    }

    writeRes.map { _ => app }
  }

  def find(key: AppKey): Future[Option[AppSetting]] = {
    require(key != null, "key must not be null")

    mongo.collection(SystemCollection.App) flatMap {
      _.find(Json.obj("_id" -> key), None).one[AppSetting]
    }
  }
}
