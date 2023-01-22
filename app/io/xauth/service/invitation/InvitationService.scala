package io.xauth.service.invitation

import akka.actor.ActorRef
import io.xauth.Uuid
import io.xauth.actor.RegistrationInvitationActor.InvitationMessage
import io.xauth.config.ApplicationConfiguration
import io.xauth.config.InvitationCodeNotification.Auto
import io.xauth.model.ContactType.Email
import io.xauth.model.pagination.{PagedData, Pagination}
import io.xauth.service.invitation.model.Invitation
import io.xauth.service.mongo.{MongoDbClient, WorkspaceCollection}
import io.xauth.service.workspace.model.Workspace
import play.api.Logger
import play.api.libs.json.Json
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.play.json.compat._
import reactivemongo.play.json.compat.json2bson.toDocumentWriter

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.Date
import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.Future.{failed, successful}
import scala.concurrent.{ExecutionContext, Future}

/**
  * Service that handles the business logic for registration invitation data.
  */
@Singleton
class InvitationService @Inject()
(
  mongo: MongoDbClient,
  @Named("registration-invitation") invitationActor: ActorRef,
  conf: ApplicationConfiguration
)
(implicit ec: ExecutionContext) {

  private val logger: Logger = Logger(this.getClass)

  /**
    * Find all invitations and returns paged results.
    *
    * @param w Current workspace
    * @param p Pagination rules
    * @return Returns a future that boxes the paged result invitation list.
    */
  def findAll(q: Option[String])(implicit w: Workspace, p: Pagination): Future[PagedData[Invitation]] = {
    val conditions = q map { s =>
      Json.obj(
        "$or" -> Json.arr(
          Json.obj("_id" -> Json.obj("$eq" -> s)),
          Json.obj("description" -> Json.obj("$regex" -> s".*$s.*", "$options" -> "i")),
          Json.obj("userInfo.firstName" -> Json.obj("$regex" -> s".*$s.*", "$options" -> "i")),
          Json.obj("userInfo.lastName" -> Json.obj("$regex" -> s".*$s.*", "$options" -> "i")),
          Json.obj("userInfo.company" -> Json.obj("$regex" -> s".*$s.*", "$options" -> "i")),
          Json.obj("userInfo.contacts.value" -> Json.obj("$regex" -> s".*$s.*", "$options" -> "i")),
          Json.obj("userInfo.contacts.description" -> Json.obj("$regex" -> s".*$s.*", "$options" -> "i"))
        )
      )
    }

    val selector = conditions getOrElse Json.obj()
    val collection = mongo.collection(WorkspaceCollection.Invitation)

    // counting matching documents
    val count = collection.flatMap(_.count(Some(selector)))

    // fetching matching documents
    val results = collection flatMap {
      _
        .find(selector)
        .skip(p.offset)
        .cursor[Invitation](ReadPreference.primary)
        .collect[Seq](p.size, Cursor.FailOnError[Seq[Invitation]]())
    }

    for {
      tot <- count
      users <- results
    } yield p.paginate(users, tot.toInt)
  }

  /**
    * Retrieves the invitation related to the supplied `email` address.
    *
    * @param email Email address of the invited user.
    * @return Returns a [[Future]] that it will boxes found [[Invitation]] object
    */
  def findByEmail(email: String)(implicit w: Workspace): Future[Option[Invitation]] = {
    require(email != null, "email must not be null")

    mongo.collection(WorkspaceCollection.Invitation) flatMap {
      _
        .find(Json.obj(
          "userInfo.contacts.type" -> Email,
          "userInfo.contacts.value" -> email
        ), None)
        .one[Invitation]
    }
  }

  /**
    * Creates new registration invitation.
    *
    * @param invitation Invitation object to store.
    * @return Returns a [[Future]] that will boxes the created invitation.
    */
  def create(invitation: Invitation)(implicit w: Workspace): Future[Invitation] = {
    require(invitation != null, "invitation must not be null")

    // retrieving user information from invitation
    val now = Date.from(LocalDateTime.now.toInstant(UTC))

    val inv = invitation.copy(
      id = Some(Uuid()),
      registeredAt = Some(now),
      updatedAt = Some(now)
    )

    val writeRes = mongo.collection(WorkspaceCollection.Invitation) flatMap {
      _.insert.one(inv)
    }

    writeRes.failed.foreach {
      e => logger.error(s"unable to write invitation: ${e.getMessage}")
    }

    writeRes flatMap { r =>
      if (r.n == 1) {
        // sending invitation email
        if (conf.invitationCodeNotification == Auto) invitationActor ! InvitationMessage(inv)
        successful(inv)
      }
      else failed(new RuntimeException(r.writeErrors.map(_.errmsg).mkString(", ")))
    }
  }

  /**
    * Searches and retrieves from persistence system the
    * invitation referred to the given identifier.
    *
    * @param id Invitation identifier.
    * @return Returns [[Future]] that will boxes the [[Invitation]].
    */
  def find(id: Uuid)(implicit w: Workspace): Future[Option[Invitation]] = {
    require(id != null, "id must not be null")

    mongo.collection(WorkspaceCollection.Invitation) flatMap {
      _.find(Json.obj("_id" -> id), None).one[Invitation]
    }
  }

  /**
    * Deletes invitation from persistence system.
    *
    * @param id Invitation identifier.
    * @return Returns a [[Future]] that boxes `true` in case of delete success,
    *         boxes `false` otherwise.
    */
  def delete(id: Uuid)(implicit w: Workspace): Future[Boolean] = {
    require(id != null, "id must not be null")

    mongo.collection(WorkspaceCollection.Invitation) flatMap {
      _.delete(ordered = false)
        .one(Json.obj("_id" -> id))
        .map(_.n > 0)
    }
  }
}
