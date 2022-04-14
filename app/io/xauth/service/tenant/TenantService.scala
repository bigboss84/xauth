package io.xauth.service.tenant

import io.xauth.Uuid
import io.xauth.service.mongo.{MongoDbClient, SystemCollection}
import io.xauth.service.tenant.model.Tenant
import play.api.Logger
import play.api.libs.json.Json
import reactivemongo.api.Cursor
import reactivemongo.play.json.compat._
import reactivemongo.play.json.compat.json2bson.toDocumentWriter

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.Date
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Service that handles the tenant business logic.
 */
@Singleton
class TenantService @Inject()
(
  mongo: MongoDbClient,
)(implicit ec: ExecutionContext) {

  private val logger: Logger = Logger(this.getClass)

  /**
   * Searches and retrieves from persistence system the
   * tenant referred to the given identifier.
   *
   * @param id Tenant identifier.
   * @return Returns non-empty [[Some(Tenant)]] if the tenant was found.
   */
  def findById(id: Uuid): Future[Option[Tenant]] = {
    require(id != null, "id must not be null")

    mongo.collection(SystemCollection.Tenant) flatMap {
      _.find(Json.obj("_id" -> id), None).one[Tenant]
    }
  }

  /**
   * Searches and retrieves from persistence system the
   * tenant referred to the given slug.
   *
   * @param slug Tenant slug.
   * @return Returns non-empty [[Some(Tenant)]] if the tenant was found.
   */
  def findBySlug(slug: String): Future[Option[Tenant]] = {
    require(slug.nonEmpty, "slug must not be null")

    mongo.collection(SystemCollection.Tenant) flatMap {
      _.find(Json.obj("slug" -> slug), None).one[Tenant]
    }
  }

  def findAll: Future[List[Tenant]] =
    mongo.collection(SystemCollection.Tenant) flatMap {
      _
        .find(Json.obj(), None)
        .cursor[Tenant]()
        .collect[List](-1, Cursor.FailOnError[List[Tenant]]())
    }

  /**
   * Creates system default tenant.
   *
   * @return Returns a [[Future]] that boxes just created tenant.
   */
  def createSystemTenant: Future[Tenant] = {
    val now = LocalDateTime.now()
    val nowInstant = now.toInstant(UTC)
    val date = Date.from(nowInstant)

    val tenant = Tenant(
      id = Uuid.Zero,
      slug = "root",
      description = "system default",
      workspaceIds = Uuid.Zero :: Nil,
      registeredAt = date,
      updatedAt = date
    )

    val result = for {
      c <- mongo.collection(SystemCollection.Tenant)
      r <- c.insert.one(tenant)
    } yield r

    result.failed.foreach(e => logger.error(s"unable to write tenant: ${e.getMessage}"))

    result.map(_ => tenant)
  }

  /**
   * Creates new tenant.
   *
   * @param slug        String that represents the tenant unique name.
   * @param description Brief description for tenant.
   * @return Returns a [[Future]] that boxes just created tenant.
   */
  def save(slug: String, description: String): Future[Tenant] = {
    require(slug != null, "slug must not be null")
    require(description.nonEmpty, "description must not be null")

    val now = LocalDateTime.now()
    val nowInstant = now.toInstant(UTC)
    val date = Date.from(nowInstant)

    val tenant = Tenant(
      id = Uuid(),
      slug = slug,
      description = description,
      registeredAt = date,
      updatedAt = date
    )

    val writeRes = mongo.collection(SystemCollection.Tenant) flatMap {
      _.insert.one(tenant)
    }

    writeRes.failed.foreach {
      e => logger.error(s"unable to write tenant: ${e.getMessage}")
    }

    writeRes map { _ => tenant }
  }

  def update(t: Tenant): Future[Option[Tenant]] = {
    require(t.id != null, "tenant identifier must not be null")
    require(t.slug.nonEmpty, "slug length must be greater than eight")

    val now = Date.from(LocalDateTime.now.toInstant(UTC))
    val updated = t.copy(updatedAt = now)

    val f = mongo.collection(SystemCollection.Tenant) flatMap {
      _.update.one(Json.obj("_id" -> t.id), updated)
    }

    f.filter(_.n == 1).map(_ => Some(updated))
  }

  /**
   * Deletes tenant from persistence system.
   *
   * @param id Tenant identifier.
   * @return Returns `true` if the requested tenant has been deleted,
   *         returns false otherwise.
   */
  def delete(id: Uuid): Future[Boolean] = {
    require(id != null, "id must not be null")

    mongo.collection(SystemCollection.Tenant) flatMap {
      _.delete(ordered = false)
        .one(Json.obj("_id" -> id))
        .map(_.n > 0)
    }
  }

}
