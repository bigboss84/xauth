package io.xauth.web.controller.health

import io.xauth.service.auth.model.AuthUser
import io.xauth.service.mongo.{MongoDbClient, WorkspaceCollection}
import io.xauth.web.controller.health.model.HealthStatus.{Down, Up}
import io.xauth.web.controller.health.model.{Health, Service}
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import reactivemongo.play.json.compat._
import reactivemongo.play.json.compat.json2bson.toDocumentWriter

import java.time.LocalDateTime.now
import java.time.ZoneOffset.UTC
import java.util.Date
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.sequence
import scala.concurrent.{ExecutionContext, Future}
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

  def health: Action[AnyContent] = Action.async { _ =>

    // workspace mongodb connections
    val services: Seq[Future[Service]] =
      mongoDbClient.collections(WorkspaceCollection.AuthUser) map { t =>
        t._2.find(Json.obj(), None).requireOne[AuthUser] transform {
          case Success(_) => Success(Service(s"mongodb (${t._1.stringValue})", Up))
          case Failure(e) => Success(Service(s"mongodb (${t._1.stringValue})", Down, Some(e.getMessage)))
        }
      }

    val data = sequence(services.toList) map { vs =>
      val updatedAt = Date.from(now.toInstant(UTC))

      Ok {
        toJson(
          Health(
            status = if (vs exists (_.status == Down)) Down else Up,
            services = vs,
            updatedAt = updatedAt
          )
        )
      }
    }

    data.recover {
      case e: Throwable =>
        InternalServerError(toJson("message" -> e.getMessage))
    }
  }
}
