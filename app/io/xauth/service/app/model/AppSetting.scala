package io.xauth.service.app.model

import io.xauth.service.app.model.AppKey.AppKey
import reactivemongo.bson.Macros.Annotations.Key
import reactivemongo.bson.{BSONDocumentHandler, Macros}

/**
  * Defines application configuration.
  */
case class AppSetting(@Key("_id") key: AppKey, value: String)

object AppSetting {
  implicit val bsonDocumentHandler: BSONDocumentHandler[AppSetting] = Macros.handler[AppSetting]
}
