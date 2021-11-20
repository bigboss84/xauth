package io.xauth.service.invitation

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.Date

import akka.actor.ActorRef
import io.xauth.Uuid
import io.xauth.config.ApplicationConfiguration
import io.xauth.config.InvitationCodeNotification.Auto
import io.xauth.model.ContactType.Email
import io.xauth.service.invitation.model.Invitation
import io.xauth.service.mongo.MongoDbClient
import javax.inject.{Inject, Named, Singleton}
import play.api.Logger
import reactivemongo.bson.BSONDocument

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

  /**
    * Retrieves the invitation related to the supplied `email` address.
    *
    * @param email Email address of the invited user.
    * @return Returns a [[Future]] that it will boxes found [[Invitation]] object
    */
  def findByEmail(email: String): Future[Option[Invitation]] = {
    require(email != null, "email must not be null")

    mongo.collections.invitation.flatMap {
      _
        .find(BSONDocument(
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
  def create(invitation: Invitation): Future[Invitation] = {
    require(invitation != null, "invitation must not be null")

    // retrieving user information from invitation
    val now = Date.from(LocalDateTime.now.toInstant(UTC))

    val inv = invitation.copy(
      id = Some(Uuid()),
      registeredAt = Some(now),
      updatedAt = Some(now)
    )

    val writeRes = mongo.collections.invitation.flatMap {
      _.insert(inv)
    }

    writeRes.failed.foreach {
      e => Logger.error(s"unable to write invitation: ${e.getMessage}")
    }

    writeRes flatMap { r =>
      if (r.ok) {
        // sending invitation email
        if (conf.invitationCodeNotification == Auto) invitationActor ! inv
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
  def find(id: Uuid): Future[Option[Invitation]] = {
    require(id != null, "id must not be null")

    mongo.collections.invitation.flatMap {
      _.find(BSONDocument("_id" -> id), None).one[Invitation]
    }
  }

  /**
    * Deletes invitation from persistence system.
    *
    * @param id Invitation identifier.
    * @return Returns a [[Future]] that boxes `true` in case of delete success,
    *         boxes `false` otherwise.
    */
  def delete(id: Uuid): Future[Boolean] = {
    require(id != null, "id must not be null")

    mongo.collections.invitation.flatMap {
      _.delete(ordered = false)
        .one(BSONDocument("_id" -> id))
        .map(_.n > 0)
    }
  }
}
