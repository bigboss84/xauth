package io.xauth.service.workspace

import io.xauth.Uuid
import io.xauth.config.ApplicationConfiguration
import io.xauth.model.ContactType.Email
import io.xauth.model.{UserContact, UserInfo}
import io.xauth.service.auth.model.AuthRole.{Admin, User}
import io.xauth.service.auth.model.AuthStatus
import io.xauth.service.auth.{AuthClientService, AuthUserService}
import io.xauth.service.mongo.{MongoDbClient, SystemCollection}
import io.xauth.service.tenant.TenantService
import io.xauth.service.workspace.model.WorkspaceStatus.{Enabled, WorkspaceStatus}
import io.xauth.service.workspace.model._
import play.api.Logger
import play.api.libs.json.Json
import reactivemongo.api.Cursor
import reactivemongo.play.json.compat._
import reactivemongo.play.json.compat.json2bson.toDocumentWriter

import java.nio.file.{Files, Path, Paths}
import java.time.ZoneOffset.UTC
import java.time.{LocalDateTime, ZoneId}
import java.util.Date
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process._

/**
  * Service that handles the workspace business logic.
  */
@Singleton
class WorkspaceService @Inject()
(
  authClientService: AuthClientService,
  authUserService: AuthUserService,
  tenantService: TenantService,
  mongo: MongoDbClient,
  configuration: ApplicationConfiguration
)(implicit ec: ExecutionContext) {

  private val logger: Logger = Logger(this.getClass)

  /**
    * Searches and retrieves from persistence system the
    * workspace referred to the given identifier.
    *
    * @param id Workspace identifier.
    * @return Returns non-empty [[Some(Workspace)]] if the workspace was found.
    */
  def findById(id: Uuid): Future[Option[Workspace]] = {
    require(id != null, "id must not be null")

    mongo.collection(SystemCollection.Workspace) flatMap {
      _.find(Json.obj("_id" -> id), None).one[Workspace]
    }
  }

  /**
    * Searches and retrieves from persistence system the
    * workspace referred to the given slug.
    *
    * @param slug Workspace slug.
    * @return Returns non-empty [[Some(Workspace)]] if the workspace was found.
    */
  def findBySlug(slug: String): Future[Option[Workspace]] = {
    require(slug != null, "slug must not be null")

    mongo.collection(SystemCollection.Workspace) flatMap {
      _.find(Json.obj("slug" -> slug), None).one[Workspace]
    }
  }

  def findAll: Future[List[Workspace]] =
    mongo.collection(SystemCollection.Workspace) flatMap {
      _
        .find(Json.obj(), None)
        .cursor[Workspace]()
        .collect[List](-1, Cursor.FailOnError[List[Workspace]]())
    }

  def generateKeyPair(workspaceId: Uuid, keyPath: String): Either[String, Path] = {
    // preparing file-system
    val keysPath = Paths.get(keyPath)
    val workspacePath = keysPath.resolve(workspaceId.stringValue)
    val keygenPath = keysPath.resolve("keygen.sh")

    // <key-path>/keys/<workspace-id>/
    Files.createDirectories(workspacePath)

    // creating private/public key pair by bash script
    s"${keygenPath.toString} -n ${workspaceId.stringValue} -p ${workspacePath.toString}".! match {
      case 0 => Right(workspacePath)
      case _ => Left(s"errors during keypair generation for workspace ${workspaceId.stringValue}")
    }
  }

  /**
    * Creates system default workspace.
    *
    * @return Returns a [[Future]] that boxes just created workspace.
    */
  def createSystemWorkspace(applications: List[String]): Future[Workspace] = {
    val now = LocalDateTime.now()
    val nowInstant = now.toInstant(UTC)
    val date = Date.from(nowInstant)

    val workspace = Workspace(
      id = Uuid.Zero,
      tenantId = Uuid.Zero,
      slug = "root",
      description = "system default",
      status = Enabled,
      configuration = WorkspaceConfiguration(
        dbUri = configuration.mongoDbUri,
        mail = MailConfiguration(
          name = configuration.mailName,
          from = configuration.mailFrom,
          smtp = SmtpConfiguration(
            host = configuration.mailSmtp.host,
            port = configuration.mailSmtp.port,
            user = configuration.mailSmtp.user,
            pass = configuration.mailSmtp.pass,
            channel = configuration.mailSmtp.channel,
            debug = configuration.mailSmtp.debug
          )
        ),
        jwt = Jwt(
          expiration = Expiration(
            accessToken = configuration.jwtExpirationAccessToken,
            refreshToken = configuration.jwtExpirationRefreshToken
          ),
          encryption = Encryption(configuration.jwtAlgorithm)
        ),
        applications = applications,
        zoneId = ZoneId.systemDefault()
      ),
      registeredAt = date,
      updatedAt = date
    )

    // writing workspace
    val result = for {
      c <- mongo.collection(SystemCollection.Workspace)
      r <- c.insert.one(workspace)
      // configuring workspace indexes
      _ <- mongo.configureIndexes(workspace)
    } yield r

    result.failed.foreach(
      e => logger.error(s"unable to create system default workspace: ${e.getMessage}")
    )

    result.map { _ =>
      // generating asymmetric key pair
      generateKeyPair(workspace.id, configuration.jwtSecretKeyPath) match {
        case Left(e) => logger.error(e)
        case Right(path) => logger.info(s"generated key pair for default system workspace at $path")
      }
      workspace
    }
  }

  /**
    * Creates new workspace.
    *
    * @param tenantId The tenant identifier.
    * @param slug     Simple name for the workspace.
    * @param desc     Brief description for the workspace.
    * @return Returns a [[Future]] that boxes just created workspace.
    */
  def save(tenantId: Uuid, slug: String, desc: String, conf: WorkspaceConfiguration, init: WorkspaceInit): Future[Workspace] = {
    require(tenantId != null, "tenant id must not be null")
    require(slug != null, "slug must not be null")
    require(desc != null, "description must not be null")
    require(conf != null, "configuration must not be null")

    val now = LocalDateTime.now()
    val nowInstant = now.toInstant(UTC)
    val date = Date.from(nowInstant)

    val workspace = Workspace(
      id = Uuid(),
      tenantId = tenantId,
      slug = slug,
      description = desc,
      status = Enabled,
      configuration = conf,
      registeredAt = date,
      updatedAt = date
    )

    val writeRes = for {
      c <- mongo.collection(SystemCollection.Workspace)
      // writing workspace
      r <- c.insert.one(workspace)
      // new pooled connection
      _ <- mongo.pooledConnection(workspace.configuration.dbUri, workspace.id)
    } yield r

    writeRes map { _ =>

      // generating asymmetric key pair
      generateKeyPair(workspace.id, configuration.jwtSecretKeyPath) match {
        case Left(e) => logger.error(e)
        case Right(path) => logger.info(s"generated key pair for new workspace at $path")
      }

      implicit val w: Workspace = workspace

      val configurationWriteResult = for {
        // configuring workspace indexes
        _ <- mongo.configureIndexes
        // configuring client
        _ <- authClientService.create(init.client.id, init.client.secret)
        // configuring admin user
        _ <- {
          val userInfo = UserInfo(
            firstName = "unknown",
            lastName = "unknown",
            company = "this",
            contacts = UserContact(Email, init.admin.username, None, trusted = true) :: Nil
          )
          authUserService.save(init.admin.username, init.admin.password, Some("workspace administrator"), userInfo, AuthStatus.Enabled, Nil, User, Admin)
        }
        // updating tenant adding current workspace id
        t <- tenantService.findById(workspace.tenantId)
        w <- tenantService.update(t.get.copy(
          workspaceIds = workspace.id :: t.get.workspaceIds
        ))
      } yield w

      configurationWriteResult.failed.foreach {
        e => logger.error(s"unable to complete workspace configuration: ${e.getMessage}")
      }
    }

    writeRes.failed.foreach {
      e => logger.error(s"unable to write workspace: ${e.getMessage}")
    }

    writeRes map { _ => workspace }
  }

  def update(w: Workspace): Future[Option[Workspace]] = {
    require(w.id != null, "workspace identifier must not be null")
    require(w.slug.nonEmpty, "slug length must not be empty")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))
    val updated = w.copy(updatedAt = now)

    val f = mongo.collection(SystemCollection.Workspace) flatMap {
      _.update.one(Json.obj("_id" -> w.id), updated)
    }

    f.filter(_.n == 1).map(_ => Some(updated))
  }

  def updateStatus(w: Workspace, status: WorkspaceStatus): Future[Option[WorkspaceStatus]] = {
    require(w.id != null, "workspace identifier must not be null")
    require(status != null, "status must not be null")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))
    val updated = w.copy(status = status, updatedAt = now)

    val f = mongo.collection(SystemCollection.Workspace) flatMap {
      _.update.one(Json.obj("_id" -> w.id), updated)
    }

    f.filter(_.n == 1).map(_ => Some(updated.status))
  }

  /**
    * Deletes workspace from persistence system.
    *
    * @param w The workspace.
    * @return Returns `true` if the requested workspace has been deleted,
    *         returns false otherwise.
    */
  def delete(w: Workspace): Future[Boolean] = {
    require(w != null, "workspace must not be null")

    // deleting workspace
    val f = mongo.collection(SystemCollection.Workspace) flatMap {
      _.delete(ordered = false)
        .one(Json.obj("_id" -> w.id))
        .map(_.n > 0)
    }

    f flatMap {
      case true =>
        // deleting private/public keypair
        val keysPath = Paths.get(configuration.jwtSecretKeyPath)
        val workspacePath = keysPath.resolve(w.id.stringValue)

        // deleting all keys
        Files.list(workspacePath).forEach(p => Files.delete(p))

        // deleting workspace directory
        Files.delete(workspacePath)

        // closing and purging workspace mongodb connection
        mongo.purgeConnection(w.id)

        // updating tenant removing current workspace id
        for {
          t <- tenantService.findById(w.tenantId)
          u <- tenantService.update(t.get.copy(
            workspaceIds = t.get.workspaceIds.filterNot(_ == w.id)
          ))
        } yield u.isDefined

      case false => successful(false)
    }
  }

}
