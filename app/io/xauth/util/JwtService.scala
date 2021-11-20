package io.xauth.util

import io.xauth.Uuid
import io.xauth.config.ApplicationConfiguration
import io.xauth.model.AppInfo
import io.xauth.service.auth.model.AuthRole
import io.xauth.service.auth.model.AuthRole.AuthRole
import io.xauth.util.Implicits._
import pdi.jwt.algorithms.{JwtAsymetricAlgorithm, JwtHmacAlgorithm}
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtHeader}
import play.api.libs.json.Json.{obj, toJson}
import play.api.libs.json.{JsArray, Json}

import java.net.InetAddress
import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.util.{Failure, Success}

@Singleton
class JwtService @Inject()(conf: ApplicationConfiguration) {

  // reading configuration
  private val jwtAlgorithm = conf.jwtAlgorithm
  private val jwtExpiration = conf.jwtExpirationAccessToken
  private val jwtAsymmetricAlgorithm = conf.jwtAsymmetricAlgorithm

  private val algorithm = JwtAlgorithm.fromString(jwtAlgorithm)

  val jwtTokenType: String = "bearer"
  val jwtExpirationInMinutes: Int = (jwtExpiration / 60).intValue

  def createToken(userId: Uuid, roles: List[AuthRole] = Nil, applications: List[AppInfo] = Nil): String = {
    val timestamp = Instant.now.getEpochSecond

    val claim = JwtClaim()
      .by(InetAddress.getLocalHost.getHostName)
      .issuedAt(timestamp)
      .expiresAt(timestamp + jwtExpiration)
      .about(userId.stringValue) + ("roles", roles) + obj("applications" -> toJson(applications)).toString

    // signing token with private key
    if (jwtAsymmetricAlgorithm) {
      val header = JwtHeader(
        algorithm = Some(algorithm),
        typ = Some("JWT"),
        keyId = Some(conf.jwtAsymmetricKey.get.name)
      )
      Jwt.encode(header, claim, conf.jwtAsymmetricKey.get.privateKeyBytes.toPrivateKey)
    }

    // signing token with secret key
    else Jwt.encode(claim, conf.jwtSymmetricKey.get.bytes.toSecretKey, algorithm.asInstanceOf[JwtHmacAlgorithm])
  }

  def decodeToken(token: String): Either[String, (Uuid, List[AuthRole])] = {
    if (jwtAsymmetricAlgorithm) {
      val publicKey = conf.jwtAsymmetricKey.get.publicKeyBytes.toPublicKey
      Jwt.decodeRawAll(token, publicKey, Seq(algorithm.asInstanceOf[JwtAsymetricAlgorithm]))
    }
    else {
      val secretKey = conf.jwtSymmetricKey.get.bytes.toSecretKey
      Jwt.decodeRawAll(token, secretKey, Seq(algorithm.asInstanceOf[JwtHmacAlgorithm]))
    }
  } match {
    case s: Success[(String, String, String)] =>
      Right {
        val json = Json.parse(s.value._2)
        (
          Uuid((json \ "sub").get.as[String]),
          (json \ "roles").get.as[JsArray].value.map(r => AuthRole.withName(r.as[String])).toList
        )
      }
    case Failure(e) => Left(e.getMessage)
  }

  def createRefreshToken: String = {
    import Implicits.RandomString
    (('a' to 'f') ++ ('0' to '9')).random(40)
  }

}

object JwtService {
  def isAsymmetricAlgorithm(s: String): Boolean = {
    val a = JwtAlgorithm.fromString(s)
    JwtAlgorithm.allAsymetric.contains(a)
  }
}