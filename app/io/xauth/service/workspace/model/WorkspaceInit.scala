package io.xauth.service.workspace.model

/**
  * Workspace initialization.
  */
case class WorkspaceInit(client: Client, admin: Admin)

object WorkspaceInit {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json._

  implicit val reads: Reads[WorkspaceInit] = (
    (__ \ "client").read[Client]
      and (__ \ "admin").read[Admin]
    ) (WorkspaceInit.apply _)

  implicit val write: Writes[WorkspaceInit] = (
    (__ \ "client").write[Client]
      and (__ \ "admin").write[Admin]
    ) (unlift(WorkspaceInit.unapply))
}

/**
  * Initial client for http-basic authentication.
  */
case class Client(id: String, secret: String)

object Client {
  import play.api.libs.json._

  implicit val reads: Reads[Client] = Json.reads[Client]
  implicit val writes: Writes[Client] = Json.writes[Client]
}

/**
  * Initial workspace administrator.
  */
case class Admin(username: String, password: String)

object Admin {
  import play.api.libs.json._

  implicit val reads: Reads[Admin] = Json.reads[Admin]
  implicit val writes: Writes[Admin] = Json.writes[Admin]
}