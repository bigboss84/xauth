package io.xauth.service.workspace.model

import io.xauth.Uuid
import io.xauth.service.mongo.BsonHandlers._
import io.xauth.service.workspace.model.WorkspaceStatus.WorkspaceStatus
import reactivemongo.api.bson.Macros.Annotations.Key
import reactivemongo.api.bson.{BSONDocumentHandler, Macros}

import java.util.Date

case class Workspace
(
  @Key("_id")
  id: Uuid,
  tenantId: Uuid,
  slug: String,
  description: String,
  status: WorkspaceStatus,
  configuration: WorkspaceConfiguration,
  registeredAt: Date,
  updatedAt: Date
)

object Workspace {
  implicit val bsonDocumentHandler: BSONDocumentHandler[Workspace] = Macros.handler[Workspace]
}