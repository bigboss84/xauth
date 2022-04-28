package io.xauth.service.workspace.model

import io.xauth.model.DataFormat
import io.xauth.service.mongo.BsonHandlers.zoneIdBsonHandler
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._
import play.api.libs.json._
import reactivemongo.api.bson.{BSONDocumentHandler, Macros}

import java.time.ZoneId

case class Expiration(accessToken: Int, refreshToken: Int)

object Expiration extends DataFormat {

  implicit val reads: Reads[Expiration] = (
    (__ \ "accessToken").read[Int]
      and (__ \ "refreshToken").read[Int]
    ) (Expiration.apply _)

  implicit val write: Writes[Expiration] = (
    (__ \ "accessToken").write[Int]
      and (__ \ "refreshToken").write[Int]
    ) (unlift(Expiration.unapply))

  implicit val bsonDocumentHandler: BSONDocumentHandler[Expiration] = Macros.handler[Expiration]
}

case class Encryption(algorithm: String)

object Encryption {
  implicit val reads: Reads[Encryption] = Json.reads[Encryption]
  implicit val writes: OWrites[Encryption] = Json.writes[Encryption]
  implicit val bsonDocumentHandler: BSONDocumentHandler[Encryption] = Macros.handler[Encryption]
}

case class Jwt(expiration: Expiration, encryption: Encryption)

object Jwt extends DataFormat {

  implicit val reads: Reads[Jwt] = (
    (__ \ "expiration").read[Expiration]
      and (__ \ "encryption").read[Encryption]
    ) (Jwt.apply _)

  implicit val write: Writes[Jwt] = (
    (__ \ "expiration").write[Expiration]
      and (__ \ "encryption").write[Encryption]
    ) (unlift(Jwt.unapply))

  implicit val bsonDocumentHandler: BSONDocumentHandler[Jwt] = Macros.handler[Jwt]
}

case class WorkspaceConfiguration(dbUri: String, jwt: Jwt, applications: List[String], zoneId: ZoneId)

object WorkspaceConfiguration extends DataFormat {

  implicit val reads: Reads[WorkspaceConfiguration] = (
    (__ \ "dbUri").read[String]
      and (__ \ "jwt").read[Jwt]
      and (__ \ "applications").read[List[String]]
      and (__ \ "zoneId").read(ZoneIdReads)
    ) (WorkspaceConfiguration.apply _)

  implicit val write: Writes[WorkspaceConfiguration] = (
    (__ \ "dbUri").write[String]
      and (__ \ "jwt").write[Jwt]
      and (__ \ "applications").write[List[String]]
      and (__ \ "zoneId").write(ZoneIdWrites)
    ) (unlift(WorkspaceConfiguration.unapply))

  implicit val bsonDocumentHandler: BSONDocumentHandler[WorkspaceConfiguration] = Macros.handler[WorkspaceConfiguration]
}