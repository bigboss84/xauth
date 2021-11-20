package io.xauth.util

import java.security.interfaces.RSAPublicKey

import com.nimbusds.jose.JWSAlgorithm.RS256
import com.nimbusds.jose.jwk.KeyUse.SIGNATURE
import com.nimbusds.jose.jwk.{JWK, RSAKey}
import io.xauth.config.ApplicationConfiguration
import io.xauth.util.Implicits._
import javax.inject.{Inject, Singleton}
import play.api.Environment

@Singleton
class JwkService @Inject()(conf: ApplicationConfiguration)(implicit val env: Environment) {

  val jwk: Option[JWK] =
    conf.jwtAsymmetricKey map { k =>
      new RSAKey.Builder(k.publicKeyBytes.toPublicKey.asInstanceOf[RSAPublicKey])
        .privateKey(k.privateKeyBytes.toPrivateKey)
        .keyUse(SIGNATURE)
        .keyID(k.name)
        .algorithm(RS256)
        .build
    }

}
