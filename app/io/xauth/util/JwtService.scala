package io.xauth.util

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.KeyUse.SIGNATURE
import com.nimbusds.jose.jwk.{JWK, RSAKey}
import io.xauth.Uuid
import io.xauth.config.{ApplicationConfiguration, AsymmetricKey, SymmetricKey}
import io.xauth.model.AppInfo
import io.xauth.service.auth.model.AuthRole
import io.xauth.service.auth.model.AuthRole.AuthRole
import io.xauth.service.workspace.model.Workspace
import io.xauth.util.Implicits._
import io.xauth.util.JwtService.toAlgorithmType
import io.xauth.util.model.AlgorithmType.{AlgorithmType, Asymmetric, Symmetric}
import pdi.jwt.algorithms.{JwtAsymetricAlgorithm, JwtHmacAlgorithm}
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtHeader}
import play.api.libs.json.Json.{obj, toJson}
import play.api.libs.json.{JsArray, Json}

import java.io.File
import java.net.InetAddress
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.util.{Failure, Success, Try}

@Singleton
class JwtService @Inject()(conf: ApplicationConfiguration) {

  def createToken(userId: Uuid, workspaceId: Uuid, roles: List[AuthRole] = Nil, applications: List[AppInfo] = Nil)(implicit w: Workspace): String = {
    val timestamp = Instant.now.getEpochSecond

    val claim = JwtClaim()
      .by(InetAddress.getLocalHost.getHostName)
      .issuedAt(timestamp)
      .expiresAt(timestamp + w.configuration.jwt.expiration.accessToken)
      .about(userId.stringValue) + ("workspaceId", workspaceId.stringValue) + ("roles", roles) + obj("applications" -> toJson(applications)).toString

    // reading algorithm from workspace configuration
    val wAlg = w.configuration.jwt.encryption.algorithm
    val algorithm: JwtAlgorithm = JwtAlgorithm.fromString(wAlg)
    val aType = toAlgorithmType(wAlg)

    aType match {
      case Asymmetric =>
        // signing token with private key
        val key = asymmetricKey
        val header = JwtHeader(
          algorithm = Some(algorithm),
          typ = Some("JWT"),
          keyId = Some(key.name)
        )
        Jwt.encode(header, claim, key.privateKeyBytes.toPrivateKey)
      case Symmetric =>
        // signing token with secret key
        val key = symmetricKey
        Jwt.encode(claim, key.bytes.toSecretKey, algorithm.asInstanceOf[JwtHmacAlgorithm])
    }
  }

  def decodeToken(token: String)(implicit w: Workspace): Either[String, (Uuid, Uuid, List[AuthRole])] = {
    // reading algorithm from workspace configuration
    val wAlg = w.configuration.jwt.encryption.algorithm
    val algorithm: JwtAlgorithm = JwtAlgorithm.fromString(wAlg)
    val aType = toAlgorithmType(wAlg)
    val decoded: Try[(String, String, String)] = aType match {
      case Asymmetric =>
        val key = asymmetricKey
        val publicKey = key.publicKeyBytes.toPublicKey
        Jwt.decodeRawAll(token, publicKey, Seq(algorithm.asInstanceOf[JwtAsymetricAlgorithm]))
      case Symmetric =>
        val secretKey = symmetricKey.bytes.toSecretKey
        Jwt.decodeRawAll(token, secretKey, Seq(algorithm.asInstanceOf[JwtHmacAlgorithm]))
    }

    // handling decoding result
    decoded match {
      case s: Success[(String, String, String)] =>
        Right {
          val json = Json.parse(s.value._2)
          (
            Uuid((json \ "sub").get.as[String]),
            Uuid((json \ "workspaceId").get.as[String]),
            (json \ "roles").get.as[JsArray].value.map(r => AuthRole.withName(r.as[String])).toList
          )
        }
      case Failure(e) => Left(e.getMessage)
    }
  }

  def jwk(implicit w: Workspace): Option[JWK] = {
    val wAlg = w.configuration.jwt.encryption.algorithm
    toAlgorithmType(wAlg) match {
      case Asymmetric => Some {
        val algorithm = JWSAlgorithm.parse(wAlg)
        val key = asymmetricKey
        new RSAKey.Builder(key.publicKeyBytes.toPublicKey.asInstanceOf[RSAPublicKey])
          .privateKey(key.privateKeyBytes.toPrivateKey)
          .keyUse(SIGNATURE)
          .keyID(s"${w.slug}-default")
          .algorithm(algorithm)
          .build
      }
      case Symmetric => None
    }
  }

  def asymmetricKey(implicit w: Workspace): AsymmetricKey = {
    val path = conf.jwtSecretKeyPath
    val pvtBytes = new File(path, s"${w.id.stringValue}/${w.id.stringValue}-rsa.private.der").bytes
    val pubBytes = new File(path, s"${w.id.stringValue}/${w.id.stringValue}-rsa.public.der").bytes
    AsymmetricKey(w.id.stringValue, pvtBytes, pubBytes)
  }

  def symmetricKey(implicit w: Workspace): SymmetricKey = {
    val path = conf.jwtSecretKeyPath
    val key = new File(path, s"${w.id.stringValue}/${w.id.stringValue}-rsa.private.der")
    SymmetricKey(w.id.stringValue, key.bytes)
  }
}

object JwtService {
  val TokenType: String = "bearer"

  def isAsymmetricAlgorithm(s: String): Boolean = {
    val a = JwtAlgorithm.fromString(s)
    JwtAlgorithm.allAsymetric.contains(a)
  }

  def toAlgorithmType(algorithm: String): AlgorithmType = {
    val a = JwtAlgorithm.fromString(algorithm)
    if (JwtAlgorithm.allAsymetric().contains(a)) Asymmetric else Symmetric
  }

  def createRefreshToken: String = {
    import Implicits.RandomString
    (('a' to 'f') ++ ('0' to '9')).random(40)
  }
}