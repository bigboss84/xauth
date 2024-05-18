package io.xauth.service.auth

import io.xauth.Uuid
import io.xauth.model.UserContact
import io.xauth.service.auth.model.AuthCode
import io.xauth.service.auth.model.AuthCode.generateCode
import io.xauth.service.auth.model.AuthCodeType.AuthCodeType
import io.xauth.service.mongo.{MongoDbClient, WorkspaceCollection}
import io.xauth.service.workspace.model.Workspace
import play.api.Logger
import play.api.libs.json.Json
import reactivemongo.play.json.compat._
import reactivemongo.play.json.compat.json2bson.toDocumentWriter

import java.time.Duration.ofHours
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.time.temporal.TemporalAmount
import java.util.Date
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Service that handles for authentication codes.
 */
@Singleton
class AuthCodeService @Inject()
(
  mongo: MongoDbClient
)
(implicit ec: ExecutionContext) {

  private val logger: Logger = Logger(this.getClass)

  def find(code: String)(implicit w: Workspace): Future[Option[AuthCode]] = {
    require(code.nonEmpty, "code must not be null")

    mongo.collection(WorkspaceCollection.AuthCode) flatMap {
      _.find(Json.obj("_id" -> code), None).one[AuthCode]
    }
  }

  def find(code: String, codeType: AuthCodeType)(implicit w: Workspace): Future[Option[AuthCode]] = {
    require(code.nonEmpty, "code must not be null")
    require(codeType != null, "code-type must not be null")

    mongo.collection(WorkspaceCollection.AuthCode) flatMap {
      _.find(Json.obj("_id" -> code, "type" -> codeType), None).one[AuthCode]
    }
  }

  def find(referenceId: Uuid, codeType: AuthCodeType)(implicit w: Workspace): Future[Option[AuthCode]] = {
    require(referenceId != null, "reference-id must not be null")
    require(codeType != null, "code-type must not be null")

    mongo.collection(WorkspaceCollection.AuthCode) flatMap {
      _.find(Json.obj("referenceId" -> referenceId, "type" -> codeType), None).one[AuthCode]
    }
  }

  def delete(code: String)(implicit w: Workspace): Unit = {
    require(code.nonEmpty, "code must not be null")

    mongo.collection(WorkspaceCollection.AuthCode) flatMap {
      _.delete(ordered = false)
        .one(Json.obj("_id" -> code))
        .map(_ => {})
    }
  }

  def deleteAllExpired(implicit w: Workspace): Unit = {
    val now = LocalDateTime.now.toInstant(UTC)

    mongo.collection(WorkspaceCollection.AuthCode) flatMap {
      _.delete(ordered = false)
        .one(Json.obj("expiresAt" -> Json.obj("$lt" -> now.toString)))
        .map(_ => {})
    }
  }

  def save(codeType: AuthCodeType, referenceId: Uuid, userContact: Option[UserContact])(implicit w: Workspace): Future[AuthCode] =
    save(codeType, generateCode, referenceId, userContact, Left(ofHours(1)))

  /**
   * Generates a shared code.
   *
   * @param codeType    the type of code.
   * @param referenceId the reference id that the generated code refers to.
   * @return Returns a [[Future]] that will contains just created activation code.
   */
  def save(codeType: AuthCodeType, code: String, referenceId: Uuid, userContact: Option[UserContact], validity: Either[TemporalAmount, Date])(implicit w: Workspace): Future[AuthCode] = {
    require(codeType != null, "codeType must not be null")
    require(referenceId != null, "referenceId must not be null")

    val registeredAt = LocalDateTime.now.toInstant(UTC)

    val expiresAt = validity match {
      case Left(a) => Date.from(registeredAt plus a)
      case Right(d) => d
    }

    val authCode = AuthCode(
      code,
      codeType,
      referenceId,
      userContact,
      expiresAt,
      Date.from(registeredAt)
    )

    val writeRes = mongo.collection(WorkspaceCollection.AuthCode) flatMap {
      _.insert.one(authCode)
    }

    writeRes.failed.foreach {
      e => logger.error(s"unable to write auth code: ${e.getMessage}")
    }

    writeRes.map { _ => authCode }
  }

}