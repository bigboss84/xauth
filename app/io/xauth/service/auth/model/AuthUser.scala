package io.xauth.service.auth.model

import io.xauth.Uuid
import io.xauth.model.{AppInfo, DataFormat, UserInfo}
import io.xauth.service.auth.model.AuthRole.AuthRole
import io.xauth.service.auth.model.AuthStatus.AuthStatus
import reactivemongo.api.bson.Macros.Annotations.Key
import reactivemongo.api.bson.{BSONDocumentHandler, Macros}

import java.util.Date

case class AuthUser
(
  @Key("_id")
  id: Uuid,
  username: String,
  password: String,
  salt: String,
  roles: List[AuthRole],
  applications: List[AppInfo] = Nil,
  status: AuthStatus,
  description: Option[String],
  userInfo: UserInfo,
  registeredAt: Date,
  updatedAt: Date
)

object AuthUser extends DataFormat {

  import com.lambdaworks.crypto.SCryptUtil._

  /**
   * Cyphers the string with a generated salt.
   *
   * @return Returns a [[Tuple2]] that contains generated salt and hashed string
   *         using `scrypt` encryption algorithm.
   */
  def cryptWithSalt(s: String): (String, String) = {
    import io.xauth.util.Implicits.RandomString
    val salt = (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')).random(79)
    val pass = scrypt(salt + s, 16, 16, 16) // 79 output bytes
    (salt, pass)
  }

  /**
   * Checks if hashed string has been encrypted for given salt and string.
   *
   * @param s  The salt.
   * @param ss The string to check with salt `s`.
   * @param hs The hashed string.
   * @return Returns `true` if `hs` has been encrypted by the given `s` salt and
   *         the the string `s`, returns false otherwise.
   */
  def checkWithSalt(s: String, ss: String, hs: String): Boolean = check(s + ss, hs)

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json.Writes._
  import play.api.libs.json._

  implicit val reads: Reads[AuthUser] = (
    (__ \ "id").read[Uuid]
      and (__ \ "username").read[String]
      and (__ \ "password").read[String]
      and (__ \ "salt").read[String]
      and (__ \ "roles").read[List[AuthRole]]
      and (__ \ "applications").read[List[AppInfo]]
      and (__ \ "status").read[AuthStatus]
      and (__ \ "description").readNullable[String]
      and (__ \ "userInfo").read[UserInfo]
      and (__ \ "registeredAt").read[Date]
      and (__ \ "updatedAt").read[Date]
    ) (AuthUser.apply _)

  implicit val write: Writes[AuthUser] = (
    (__ \ "id").write[Uuid]
      and (__ \ "username").write[String]
      and (__ \ "password").write[String]
      and (__ \ "salt").write[String]
      and (__ \ "roles").write[List[AuthRole]]
      and (__ \ "applications").write[List[AppInfo]]
      and (__ \ "status").write[AuthStatus]
      and (__ \ "description").writeNullable[String]
      and (__ \ "userInfo").write[UserInfo]
      and (__ \ "registeredAt").write(dateWrites(iso8601DateFormat))
      and (__ \ "updatedAt").write(dateWrites(iso8601DateFormat))
    ) (unlift(AuthUser.unapply))

  import io.xauth.service.mongo.BsonHandlers._
  implicit val bsonDocumentHandler: BSONDocumentHandler[AuthUser] = Macros.handler[AuthUser]
}