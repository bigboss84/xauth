package io.xauth.service.workspace.model

import io.xauth.model.serial.EnumReads.enumNameReads
import io.xauth.model.serial.EnumWrites.enumNameWrites
import io.xauth.service.mongo.BsonHandlers.enumBsonHandler
import it.russoft.xenum.Enum
import play.api.libs.json.{Reads, Writes}
import reactivemongo.api.bson.BSONHandler

/**
 * Defines workspace status.
 */
object WorkspaceStatus extends Enum {
  type WorkspaceStatus = EnumVal

  /**
   * Defines the enabled status.
   */
  val Enabled: WorkspaceStatus = value("ENABLED")

  /**
   * Defines the disabled status.
   */
  val Disabled: WorkspaceStatus = value("DISABLED")

  // Json serialization
  implicit val reads: Reads[WorkspaceStatus] = enumNameReads(WorkspaceStatus)
  implicit val writes: Writes[WorkspaceStatus] = enumNameWrites

  // Bson serialization
  implicit val bsonHandler: BSONHandler[WorkspaceStatus] = enumBsonHandler(WorkspaceStatus)
}
