package io.xauth.service.auth

import akka.actor.ActorRef
import io.xauth.Uuid
import io.xauth.actor.AccountActivationActor.ActivateUserMessage
import io.xauth.model.ContactType.Email
import io.xauth.model.pagination.{PagedData, Pagination}
import io.xauth.model.{AppInfo, UserInfo}
import io.xauth.service.auth.model.AuthCodeType.{Activation, ContactTrust}
import io.xauth.service.auth.model.AuthRole.{AuthRole, User}
import io.xauth.service.auth.model.AuthStatus.{AuthStatus, Disabled, Enabled}
import io.xauth.service.auth.model.AuthUser
import io.xauth.service.auth.model.AuthUser.cryptWithSalt
import io.xauth.service.mongo.{MongoDbClient, WorkspaceCollection}
import io.xauth.service.workspace.model.Workspace
import io.xauth.util.Implicits.FormattedDate
import play.api.Logger
import play.api.libs.json.Json
import reactivemongo.api.{Collation, Cursor, ReadPreference, WriteConcern}
import reactivemongo.play.json.compat._
import reactivemongo.play.json.compat.json2bson.toDocumentWriter

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.Date
import javax.inject.{Inject, Named, Singleton}
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

  private val logger: Logger = Logger(this.getClass)

  /**
    * Searches and retrieves user by its username.
    *
    * @param username Username.
    * @return Returns non-empty [[Some(User)]] if the user was found.
    */
  def findByUsername(username: String)(implicit w: Workspace): Future[Option[AuthUser]] = {
    require(username.nonEmpty, "username must not be null")

    mongo.collection(WorkspaceCollection.AuthUser) flatMap {
      _.find(Json.obj("username" -> username), None).one[AuthUser]
    }
  }

  /**
    * Find all users and returns paged results.
    *
    * @param w Current workspace
    * @param p Pagination rules
    * @return Returns a future that boxes the paged result user list.
    */
  def findAll(implicit w: Workspace, p: Pagination): Future[PagedData[AuthUser]] = {
    val selector = Json.obj()
    val collection = mongo.collection(WorkspaceCollection.AuthUser)

    // counting matching documents
    val count = collection.flatMap(_.count(Some(selector)))

    // fetching matching documents
    val results = collection flatMap {
      _
        .find(Json.obj())
        .skip(p.offset)
        .cursor[AuthUser](ReadPreference.primary)
        .collect[Seq](p.size, Cursor.FailOnError[Seq[AuthUser]]())
    }

    for {
      tot <- count
      users <- results
    } yield p.paginate(users, tot.toInt)
  }

  def save(username: String, password: String, description: Option[String], parentId: Option[Uuid], userInfo: UserInfo)(implicit w: Workspace): Future[AuthUser] =
    save(username, password, description, parentId, userInfo, Disabled, Nil, User)

  /**
    * Creates new user.
    *
    * @param username Username.
    * @param password User password that it will encrypted.
    * @param userInfo User information.
    * @return Returns a [[Future]] that boxes just created user.
    */
  def save(username: String, password: String, description: Option[String], parentId: Option[Uuid], userInfo: UserInfo, status: AuthStatus, applications: List[AppInfo], roles: AuthRole*)(implicit w: Workspace): Future[AuthUser] = {
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
      parentId = parentId,
      roles = roles.toList,
      status = status,
      description = description,
      applications = applications,
      userInfo = userInfo,
      registeredAt = date,
      updatedAt = date
    )

    val writeRes = mongo.collection(WorkspaceCollection.AuthUser) flatMap {
      _.insert.one(user)
    }

    writeRes.failed.foreach {
      e => logger.error(s"unable to write user: ${e.getMessage}")
    }

    writeRes.map { _ =>
      // dispatching email/sms
      if (status == Disabled) trustAccount(user)

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
  def delete(id: Uuid)(implicit w: Workspace): Future[Boolean] = {
    require(id != null, "id must not be null")

    mongo.collection(WorkspaceCollection.AuthUser) flatMap {
      _.delete(ordered = false)
        .one(Json.obj("_id" -> id))
        .map(_.n > 0)
    }
  }

  def deleteByUsername(username: String)(implicit w: Workspace): Future[Boolean] = {
    require(username != null, "username must not be null")

    mongo.collection(WorkspaceCollection.AuthUser) flatMap {
      _.delete(ordered = false)
        .one(Json.obj("username" -> username))
        .map(_.n > 0)
    }
  }

  def updateUserInfoById(id: Uuid, userInfo: UserInfo)(implicit w: Workspace): Future[Option[AuthUser]] = {
    require(id != null, "id must not be null")
    require(userInfo != null, "user-info must not be null")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))

    mongo.collection(WorkspaceCollection.AuthUser) flatMap {
      _.findAndUpdate(
        Json.obj("_id" -> id),
        Json.obj("$set" -> Json.obj("userInfo" -> userInfo, "updatedAt" -> now.toIso8601)),
        fetchNewObject = true
      ) map {
        _.result[AuthUser]
      }
    }
  }

  def updateApplications(id: Uuid, applications: AppInfo*)(implicit w: Workspace): Future[Option[AuthUser]] = {
    require(id != null, "id must not be null")
    require(applications != null, "applications must not be null")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))

    mongo.collection(WorkspaceCollection.AuthUser) flatMap {
      _.findAndUpdate(
        Json.obj("_id" -> id),
        Json.obj("$set" -> Json.obj("applications" -> applications.toList, "updatedAt" -> now.toIso8601)),
        fetchNewObject = true
      ) map {
        _.result[AuthUser]
      }
    }
  }

  def updateRoles(id: Uuid, roles: AuthRole*)(implicit w: Workspace): Future[Option[AuthUser]] = {
    require(id != null, "id must not be null")
    require(roles != null, "roles must not be null")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))

    mongo.collection(WorkspaceCollection.AuthUser) flatMap {
      _.findAndUpdate(
        Json.obj("_id" -> id),
        Json.obj("$set" -> Json.obj("roles" -> roles.toList, "updatedAt" -> now.toIso8601)),
        fetchNewObject = true
      ) map {
        _.result[AuthUser]
      }
    }
  }

  def updateStatusById(id: Uuid, status: AuthStatus)(implicit w: Workspace): Future[Option[AuthUser]] = {
    require(id != null, "id must not be null")
    require(status != null, "status must not be null")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))

    mongo.collection(WorkspaceCollection.AuthUser) flatMap {
      _.findAndUpdate(
        Json.obj("_id" -> id),
        Json.obj("$set" -> Json.obj("status" -> status, "updatedAt" -> now.toIso8601)),
        fetchNewObject = true
      ) map {
        _.result[AuthUser]
      }
    }
  }

  def updateStatusByUsername(username: String, status: AuthStatus)(implicit w: Workspace): Future[Option[AuthUser]] = {
    require(username != null, "username must not be null")
    require(status != null, "status must not be null")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))

    mongo.collection(WorkspaceCollection.AuthUser) flatMap {
      _.findAndUpdate(
        Json.obj("username" -> username),
        Json.obj("$set" -> Json.obj("status" -> status, "updatedAt" -> now.toIso8601)),
      ) map {
        _.result[AuthUser]
      }
    }
  }

  def resetPassword(id: Uuid, password: String)(implicit w: Workspace): Future[Boolean] = {
    require(id != null, "id must not be null")
    require(password != null, "password must not be null")

    val encPass = cryptWithSalt(password)

    val f = for {
      u <- findById(id)
      c <- mongo.collection(WorkspaceCollection.AuthUser)
      r <- c.findAndUpdate(
        Json.obj("_id" -> id),
        u.get.copy(password = encPass._2, salt = encPass._1)
      )
    } yield r.result[AuthUser]

    f.map(_.isDefined)
  }

  /**
    * Returns a paged list o users that have the parent-child relationship with the given parent.
    *
    * @param id The parent's user identifier.
    * @param w  The workspace in which to do the research.
    * @param p  The pagination data object.
    * @return Returns a future that wraps the paged users.
    */
  def childrenOf(id: Uuid)(implicit w: Workspace, p: Pagination): Future[PagedData[AuthUser]] = {
    require(id != null, "id must not be null")

    val selector = Json.obj("parentId" -> id)
    val collection = mongo.collection(WorkspaceCollection.AuthUser)

    // counting matching documents
    val count = collection.flatMap(_.count(Some(selector)))

    // fetching matching documents
    val results = collection flatMap {
      _
        .find(selector)
        .skip(p.offset)
        .cursor[AuthUser](ReadPreference.primary)
        .collect[Seq](p.size, Cursor.FailOnError[Seq[AuthUser]]())
    }

    for {
      tot <- count
      users <- results
    } yield p.paginate(users, tot.toInt)
  }

  /**
    * Searches and retrieves from persistence system the
    * user referred to the given identifier.
    *
    * @param id User identifier.
    * @return Returns non-empty [[Some(AuthUser)]] if the user was found.
    */
  def findById(id: Uuid)(implicit w: Workspace): Future[Option[AuthUser]] = {
    require(id != null, "id must not be null")

    mongo.collection(WorkspaceCollection.AuthUser) flatMap {
      _.find(Json.obj("_id" -> id), None).one[AuthUser]
    }
  }

  /**
    * Activates the user account by an activation code.
    *
    * @param code The activation code.
    * @return Returns [[Future]] that boxes boolean if the operation has been
    *         completed without errors, boxes false otherwise.
    */
  def activate(code: String)(implicit w: Workspace): Future[Boolean] = {
    require(code != null, "code must not be null")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))

    authCodeService.find(code, Activation) flatMap {
      case Some(authCode) =>
        val userId = authCode.referenceId
        val userContact = authCode.userContact

        require(userContact nonEmpty, "user contact must be stored in auth code")

        mongo.collection(WorkspaceCollection.AuthUser) flatMap { c =>

          val selector = Json.obj("_id" -> userId)

          val update = Json.obj(
            "$set" -> Json.obj(
              "status" -> Enabled,
              "updatedAt" -> now.toIso8601,
              "userInfo.contacts.$[elem].trusted" -> true
            )
          )

          val filters = Json.obj("elem.value" -> Json.obj("$eq" -> userContact.get.value))

          val updateOp = c.updateModifier(update)

          val sort = None
          val fields = None

          c.findAndModify(selector, updateOp, sort, fields, bypassDocumentValidation = false,
            WriteConcern.Default,
            Option.empty[FiniteDuration],
            Option.empty[Collation],
            Seq(filters)
          ) map {
            _.result[AuthUser] match {
              case Some(_) =>
                // deleting code
                authCodeService.delete(code)
                true
              case _ => false
            }
          }
        }

      case _ =>
        logger.warn(s"invalid activation code: '$code'")
        successful(false)
    }
  }

  /**
    * Trusts the associated user re-sending the activation code.
    *
    * @param user Account user.
    */
  def trustAccount(user: AuthUser)(implicit w: Workspace): Unit = {
    require(user != null, "user must not be null")

    // dispatching email/sms
    user.userInfo.contacts.find(_.`type` == Email).fold(
      // here email must be present, this should not occurs
      logger.warn(s"email contact not present for user id ${user.id}")
    )(_ => accountActivatorActor ! ActivateUserMessage(user))
  }

  /**
    * Trusts the associated user contact by an contact trust code.
    *
    * @param code The contact trust code.
    * @return Returns [[Future]] that boxes boolean if the operation has been
    *         completed without errors, boxes false otherwise.
    */
  def trustContact(code: String)(implicit w: Workspace): Future[Boolean] = {
    require(code != null, "code must not be null")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))

    authCodeService.find(code, ContactTrust) flatMap {
      case Some(authCode) =>
        val userId = authCode.referenceId
        val userContact = authCode.userContact

        require(userContact nonEmpty, "user contact must be stored in auth code")

        // updating user account status
        mongo.collection(WorkspaceCollection.AuthUser) flatMap {
          _.findAndUpdate(
            Json.obj(
              "_id" -> userId,
              "userInfo.contacts.type" -> userContact.get.`type`,
              "userInfo.contacts.value" -> userContact.get.value
            ),
            Json.obj(
              "$set" -> Json.obj(
                "updatedAt" -> now.toIso8601,
                "userInfo.contacts.$.trusted" -> true
              )
            ),
          ) map {
            _.result[AuthUser] match {
              case Some(_) =>
                // deleting code
                authCodeService.delete(code)
                true
              case _ => false
            }
          }
        }
      case _ =>
        logger.warn(s"invalid contact trust code: '$code'")
        successful(false)
    }
  }
}
