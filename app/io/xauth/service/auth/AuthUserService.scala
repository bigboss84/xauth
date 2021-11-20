package io.xauth.service.auth

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.Date

import akka.actor.ActorRef
import io.xauth.Uuid
import io.xauth.model.ContactType.Email
import io.xauth.model.{AppInfo, UserInfo}
import io.xauth.service.auth.model.AuthCodeType.{Activation, ContactTrust}
import io.xauth.service.auth.model.AuthRole.{AuthRole, User}
import io.xauth.service.auth.model.AuthStatus.{AuthStatus, Disabled, Enabled}
import io.xauth.service.auth.model.AuthUser
import io.xauth.service.auth.model.AuthUser.cryptWithSalt
import io.xauth.service.mongo.MongoDbClient
import javax.inject.{Inject, Named, Singleton}
import play.api.Logger
import reactivemongo.api.commands.Collation
import reactivemongo.bson.BSONDocument

import scala.concurrent.Future.successful
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

/**
  * Service that handles the user business logic.
  */
@Singleton
class AuthUserService @Inject()
(
  @Named("account-activator") accountActivatorActor: ActorRef,
  @Named("password-reset") passwordResetActor: ActorRef,
  mongo: MongoDbClient,
  authCodeService: AuthCodeService
)(implicit ec: ExecutionContext) {

  /**
    * Searches and retrieves from persistence system the
    * user referred to the given identifier.
    *
    * @param id User identifier.
    * @return Returns non-empty [[Some(AuthUser)]] if the user was found.
    */
  def findById(id: Uuid): Future[Option[AuthUser]] = {
    require(id != null, "id must not be null")

    mongo.collections.authUser.flatMap {
      _.find(BSONDocument("_id" -> id), None).one[AuthUser]
    }
  }

  /**
    * Searches and retrieves user by its username.
    *
    * @param username Username.
    * @return Returns non-empty [[Some(User)]] if the user was found.
    */
  def findByUsername(username: String): Future[Option[AuthUser]] = {
    require(username != null, "username must not be null")

    mongo.collections.authUser.flatMap {
      _.find(BSONDocument("username" -> username), None).one[AuthUser]
    }
  }

  def save(username: String, password: String, description: Option[String], userInfo: UserInfo): Future[AuthUser] =
    save(username, password, description, userInfo, Disabled, Nil, User)

  /**
    * Creates new user.
    *
    * @param username Username.
    * @param password User password that it will encrypted.
    * @param userInfo User information.
    * @return Returns a [[Future]] that boxes just created user.
    */
  def save(username: String, password: String, description: Option[String], userInfo: UserInfo, status: AuthStatus, applications: List[AppInfo], roles: AuthRole*): Future[AuthUser] = {
    require(username != null, "username must not be null")
    require(password != null, "password must not be null")
    require(userInfo != null, "user-info must not be null")

    val now = LocalDateTime.now()
    val nowInstant = now.toInstant(UTC)
    val date = Date.from(nowInstant)

    val encPass = cryptWithSalt(password)

    val user = AuthUser(
      id = Uuid(),
      username = username,
      password = encPass._2,
      salt = encPass._1,
      roles = roles.toList,
      status = status,
      description = description,
      applications = applications,
      userInfo = userInfo,
      registeredAt = date,
      updatedAt = date
    )

    val writeRes = mongo.collections.authUser.flatMap {
      _.insert(user)
    }

    writeRes.failed.foreach {
      e => Logger.error(s"unable to write user: ${e.getMessage}")
    }

    writeRes.map { _ =>

      if (status == Disabled) {
        // dispatching email/sms
        user.userInfo.contacts.find(_.`type` == Email).fold(
          // here email must be present, this should not occurs
          Logger.warn(s"email contact not present for user id ${user.id}")
        )(_ => accountActivatorActor ! user)
      }

      user
    }
  }

  /**
    * Deletes user from persistence system.
    *
    * @param id User identifier.
    * @return Returns `true` if the requested user has been deleted,
    *         returns false otherwise.
    */
  def delete(id: Uuid): Future[Boolean] = {
    require(id != null, "id must not be null")

    mongo.collections.authUser.flatMap {
      _.delete(ordered = false)
        .one(BSONDocument("_id" -> id))
        .map(_.n > 0)
    }
  }

  def deleteByUsername(username: String): Future[Boolean] = {
    require(username != null, "username must not be null")

    mongo.collections.authUser.flatMap {
      _.delete(ordered = false)
        .one(BSONDocument("username" -> username))
        .map(_.n > 0)
    }
  }

  def updateUserInfoById(id: Uuid, userInfo: UserInfo): Future[Option[AuthUser]] = {
    require(id != null, "id must not be null")
    require(userInfo != null, "user-info must not be null")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))

    mongo.collections.authUser.flatMap {
      _.findAndUpdate(
        BSONDocument("_id" -> id),
        BSONDocument("$set" -> BSONDocument("userInfo" -> userInfo, "updatedAt" -> now)),
        fetchNewObject = true
      ) map {
        _.result[AuthUser]
      }
    }
  }

  def updateApplications(id: Uuid, applications: AppInfo*): Future[Option[AuthUser]] = {
    require(id != null, "id must not be null")
    require(applications != null, "applications must not be null")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))

    mongo.collections.authUser.flatMap {
      _.findAndUpdate(
        BSONDocument("_id" -> id),
        BSONDocument("$set" -> BSONDocument("applications" -> applications.toList, "updatedAt" -> now)),
        fetchNewObject = true
      ) map {
        _.result[AuthUser]
      }
    }
  }

  def updateRoles(id: Uuid, roles: AuthRole*): Future[Option[AuthUser]] = {
    require(id != null, "id must not be null")
    require(roles != null, "roles must not be null")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))

    mongo.collections.authUser.flatMap {
      _.findAndUpdate(
        BSONDocument("_id" -> id),
        BSONDocument("$set" -> BSONDocument("roles" -> roles.toList, "updatedAt" -> now)),
        fetchNewObject = true
      ) map {
        _.result[AuthUser]
      }
    }
  }

  def updateStatusById(id: Uuid, status: AuthStatus): Future[Option[AuthUser]] = {
    require(id != null, "id must not be null")
    require(status != null, "status must not be null")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))

    mongo.collections.authUser.flatMap {
      _.findAndUpdate(
        BSONDocument("_id" -> id),
        BSONDocument("$set" -> BSONDocument("status" -> status, "updatedAt" -> now)),
        fetchNewObject = true
      ) map {
        _.result[AuthUser]
      }
    }
  }

  def updateStatusByUsername(username: String, status: AuthStatus): Future[Option[AuthUser]] = {
    require(username != null, "username must not be null")
    require(status != null, "status must not be null")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))

    mongo.collections.authUser.flatMap {
      _.findAndUpdate(
        BSONDocument("username" -> username),
        BSONDocument("$set" -> BSONDocument("status" -> status, "updatedAt" -> now)),
      ) map {
        _.result[AuthUser]
      }
    }
  }

  def resetPassword(id: Uuid, password: String): Future[Boolean] = {
    require(id != null, "id must not be null")
    require(password != null, "password must not be null")

    val encPass = cryptWithSalt(password)

    findById(id).flatMap { user =>
      mongo.collections.authUser.flatMap {
        _.update(
          BSONDocument("_id" -> id),
          user.get.copy(password = encPass._2, salt = encPass._1)
        )
      }
    } map {
      _.ok
    }
  }

  /**
    * Activates the user account by an activation code.
    *
    * @param code The activation code.
    * @return Returns [[Future]] that boxes boolean if the operation has been
    *         completed without errors, boxes false otherwise.
    */
  def activate(code: String): Future[Boolean] = {
    require(code != null, "code must not be null")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))

    authCodeService.find(code, Activation) flatMap {
      case Some(authCode) =>
        val userId = authCode.referenceId
        val userContact = authCode.userContact

        require(userContact nonEmpty, "user contact must be stored in auth code")

        mongo.collections.authUser.flatMap { c =>

          val selector = BSONDocument("_id" -> userId)

          val update = BSONDocument(
            "$set" -> BSONDocument(
              "status" -> Enabled,
              "updatedAt" -> now,
              "userInfo.contacts.$[elem].trusted" -> true
            )
          )

          val filters = BSONDocument("elem.value" -> BSONDocument("$eq" -> userContact.get.value)) :: Nil

          val updateOp = c.updateModifier(update)

          val sort = None
          val fields = None

          c.findAndModify(selector, updateOp, sort, fields, bypassDocumentValidation = false,
            mongo.writeConcern,
            Option.empty[FiniteDuration],
            Option.empty[Collation],
            filters
          ) map {
            _.result[AuthUser] match {
              case Some(u) =>
                // deleting code
                authCodeService.delete(code)
                true
              case _ => false
            }
          }
        }

      case _ =>
        Logger.warn(s"invalid activation code: '$code'")
        successful(false)
    }
  }

  /**
    * Trusts the associated user re-sending the activation code.
    *
    * @param user Account user.
    */
  def trustAccount(user: AuthUser): Unit = {
    require(user != null, "user must not be null")

    // dispatching email/sms
    user.userInfo.contacts.find(_.`type` == Email).fold(
      // here email must be present, this should not occurs
      Logger.warn(s"email contact not present for user id ${user.id}")
    )(_ => accountActivatorActor ! user)
  }

  /**
    * Trusts the associated user contact by an contact trust code.
    *
    * @param code The contact trust code.
    * @return Returns [[Future]] that boxes boolean if the operation has been
    *         completed without errors, boxes false otherwise.
    */
  def trustContact(code: String): Future[Boolean] = {
    require(code != null, "code must not be null")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))

    authCodeService.find(code, ContactTrust) flatMap {
      case Some(authCode) =>
        val userId = authCode.referenceId
        val userContact = authCode.userContact

        require(userContact nonEmpty, "user contact must be stored in auth code")

        // updating user account status
        mongo.collections.authUser.flatMap {
          _.findAndUpdate(
            BSONDocument(
              "_id" -> userId,
              "userInfo.contacts.type" -> userContact.get.`type`,
              "userInfo.contacts.value" -> userContact.get.value
            ),
            BSONDocument(
              "$set" -> BSONDocument(
                "updatedAt" -> now,
                "userInfo.contacts.$.trusted" -> true
              )
            ),
          ) map {
            _.result[AuthUser] match {
              case Some(u) =>
                // deleting code
                authCodeService.delete(code)
                true
              case _ => false
            }
          }
        }
      case _ =>
        Logger.warn(s"invalid contact trust code: '$code'")
        successful(false)
    }
  }
}
