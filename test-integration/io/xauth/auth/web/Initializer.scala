package io.xauth.auth.web

import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.Date

import io.xauth.auth.Uuid
import io.xauth.auth.model.ContactType.{Email, MobileNumber}
import io.xauth.auth.model.{UserContact, UserInfo}
import io.xauth.auth.service.auth.model.AuthRole.AuthRole
import io.xauth.auth.service.auth.model.AuthStatus.AuthStatus
import io.xauth.auth.service.auth.model.AuthUser.cryptWithSalt
import io.xauth.auth.service.auth.model.{AuthClient, AuthUser}
import io.xauth.auth.service.mongo.MongoDbClient
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory.load
import play.api.Mode.Test
import play.api._
import play.api.inject.guice.GuiceApplicationBuilder
import reactivemongo.bson.BSONDocument

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object Initializer {

  val conf: Config = load("application-test-integration.conf")
  val env = new Environment(new File("."), this.getClass.getClassLoader, Test)

  def application: Application = GuiceApplicationBuilder()
    .loadConfig(e => {
      Logger.info(s"root path: ${e.rootPath}")
      Configuration(load(e.classLoader, conf))
    })
    .build

  //
  // DATA INITIALIZATION
  //

  object Data {

    private implicit val executionContext: ExecutionContext = ExecutionContext.global

    private val mongodb = application.injector.instanceOf[MongoDbClient]

    def createClient(id: String, secret: String): Future[AuthClient] = {
      val now = new Date
      val client = AuthClient(
        id = id,
        secret = secret,
        registeredAt = now,
        updatedAt = now
      )

      val f = mongodb.collections.authClient.flatMap {
        _.insert(client)
      }

      f onComplete {
        case Success(r) => Logger.info(s"${client.id} client created")
        case Failure(e) => Logger.error(s"unable to write client: ${e.getMessage}")
      }

      f map { _ => client }
    }

    def createUser(u: String, p: String, s: AuthStatus, fn: String, ln: String, r: AuthRole*): Future[AuthUser] = {
      val date: Date = Date.from(LocalDateTime.now.toInstant(UTC))
      val encPass: (String, String) = cryptWithSalt(p)

      val user = AuthUser(
        id = Uuid(),
        username = u,
        password = encPass._2,
        salt = encPass._1,
        roles = List(r: _*),
        status = s,
        description = Some("registered in integration test init"),
        userInfo = UserInfo(
          firstName = fn,
          lastName = ln,
          contacts = List(
            UserContact(
              `type` = Email,
              value = s"$fn.$ln@xauth.com".toLowerCase,
              description = Some("X-Auth email"),
              trusted = true
            ),
            UserContact(
              `type` = MobileNumber,
              value = "+391234567890",
              description = Some("X-Auth number"),
              trusted = false
            )
          )
        ),
        registeredAt = date,
        updatedAt = date
      )

      val f = mongodb.collections.authUser.flatMap {
        _.insert(user)
      }

      f onComplete {
        case Success(w) => Logger.info(s"${user.username} user created with result: $w")
        case Failure(e) => Logger.error(s"unable to write user: ${e.getMessage}")
      }

      f map { _ => user }
    }

    def expireRefreshToken(t: String): Unit = {
      val expiresAt = Date.from(LocalDateTime.now.minusDays(1).toInstant(UTC))
      mongodb.collections.authRefreshToken.flatMap {
        _.findAndUpdate(
          BSONDocument("_id" -> t),
          BSONDocument("$set" -> BSONDocument("expiresAt" -> expiresAt))
        ) map {
          _.result
        }
      }
    }

    def clean(): Unit = {
      mongodb.collections.authAccessAttempt.flatMap(_.drop(failIfNotFound = false))
      mongodb.collections.authCode.flatMap(_.drop(failIfNotFound = false))
      mongodb.collections.authClient.flatMap(_.drop(failIfNotFound = false))
      mongodb.collections.authRefreshToken.flatMap(_.drop(failIfNotFound = false))
      mongodb.collections.authUser.flatMap(_.drop(failIfNotFound = false))
      mongodb.collections.invitation.flatMap(_.drop(failIfNotFound = false))
    }
  }

}
