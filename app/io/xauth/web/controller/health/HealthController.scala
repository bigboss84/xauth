package io.xauth.web.controller.health

import java.time.LocalDateTime.now
import java.time.ZoneOffset.UTC
import java.util.Date

import io.xauth.service.app.model.AppKey.Init
import io.xauth.service.app.model.AppSetting
import io.xauth.service.mongo.MongoDbClient
import io.xauth.web.controller.health.model.HealthStatus.{Down, Up}
import io.xauth.web.controller.health.model.{Health, Service}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json.toJson
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import reactivemongo.bson.BSONDocument

import scala.concurrent.ExecutionContext
import scala.concurrent.Future.{sequence, successful}
import scala.util.{Failure, Success}

/**
  * Application health controller.
  */
@Singleton
class HealthController @Inject()
(
  mongoDbClient: MongoDbClient,
  cc: ControllerComponents
)
(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def health: Action[AnyContent] = Action.async { r =>

    //
    // mongodb connection
    //
    val mongodb = mongoDbClient.collections.app flatMap {
      _.find(BSONDocument("_id" -> Init), None).one[AppSetting]
    } transformWith {
      case Success(v) => successful(Service("mongodb", Up))
      case Failure(e) => successful(Service("mongodb", Down, Some(e.getMessage)))
    }

    sequence(mongodb :: Nil) transformWith {
      case Success(v) => successful {

        val updatedAt = Date.from(now.toInstant(UTC))

        Ok {
          toJson(
            Health(
              status = if (v exists (_.status == Down)) Down else Up,
              services = v,
              updatedAt = updatedAt
            )
          )
        }
      }
      case Failure(e) => successful {
        InternalServerError(toJson("message" -> e.getMessage))
      }
    }
  }
}
