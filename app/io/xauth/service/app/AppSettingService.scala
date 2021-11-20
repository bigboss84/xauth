package io.xauth.service.app

import io.xauth.service.app.model.AppSetting
import io.xauth.service.app.model.AppKey.AppKey
import io.xauth.service.mongo.MongoDbClient
import javax.inject.{Inject, Singleton}
import play.api.Logger
import reactivemongo.bson.BSONDocument

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

  def save(key: AppKey, value: String): Future[AppSetting] = {
    require(key != null, "key must not be null")
    require(value != null, "value must not be null")

    val app = AppSetting(key, value)

    val writeRes = mongo.collections.app.flatMap {
      _.findAndUpdate(BSONDocument("_id" -> key), app, upsert = true) map {
        _.result[AppSetting]
      }
    }

    writeRes.failed.foreach {
      e => Logger.error(s"unable to write app conf: ${e.getMessage}")
    }

    writeRes.map { _ => app }
  }

  def find(key: AppKey): Future[Option[AppSetting]] = {
    require(key != null, "key must not be null")

    mongo.collections.app.flatMap {
      _.find(BSONDocument("_id" -> key), None).one[AppSetting]
    }
  }
}
