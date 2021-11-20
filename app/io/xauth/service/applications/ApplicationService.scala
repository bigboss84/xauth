package io.xauth.service.applications

import io.xauth.service.app.AppSettingService
import io.xauth.service.app.model.AppKey.Applications
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Handles application settings.
  */
@Singleton
class ApplicationService @Inject()
(
  appSettingService: AppSettingService
)
(implicit ec: ExecutionContext) {

  def findAll: Future[List[String]] =
    appSettingService.find(Applications) map {
      case Some(o) => o.value.split(",").toList
      case None => Nil
    }

  def save(applications: List[String]): Future[List[String]] =
    appSettingService.save(Applications, applications.mkString(",")) map {
      s => s.value.split(",").toList
    }
}
