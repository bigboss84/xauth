package io.xauth.config

import io.xauth.config.InvitationCodeNotification.{Auto, InvitationCodeNotification, Manual}
import play.api.{Configuration, Logger}

import javax.inject.{Inject, Singleton}

@Singleton
class ApplicationConfiguration @Inject()(conf: Configuration) {

  private val logger: Logger = Logger(this.getClass)

  val baseUrl: String = conf.get[String]("baseUrl")

  // Task
  val taskCodeCleanInterval: Int = conf.get[Int]("task.codeClean.interval")
  val taskTokenCleanInterval: Int = conf.get[Int]("task.tokenClean.interval")

  // Json Web Token
  val jwtSecretKeyPath: String = conf.get[String]("jwt.secretKey.path")
  val jwtExpirationAccessToken: Int = conf.get[Int]("jwt.expiration.accessToken")

  val jwtAlgorithm: String = conf.get[String]("jwt.secretKey.algorithm")

  val jwtExpirationRefreshToken: Int = conf.get[Int]("jwt.expiration.refreshToken")

  val mongoDbUri: String = conf.get[String]("mongodb.uri")

  import MailServiceType.MailServiceType

  val mailDebug: Boolean = conf.get[Boolean]("mail.debug")

  val mailServiceType: MailServiceType = conf.get[String]("mail.service") match {
    case "smtp" => MailServiceType.Smtp
    case _ => MailServiceType.WebService
  }

  val mailService: MailService = MailService(
    conf.get[String]("mail.ws.schema"),
    conf.get[String]("mail.ws.host"),
    conf.get[String]("mail.ws.user"),
    conf.get[String]("mail.ws.pass")
  )

  val mailSmtp: MailSmtp = MailSmtp(
    conf.get[String]("mail.smtp.host"),
    conf.get[Int]("mail.smtp.port"),
    conf.get[String]("mail.smtp.user"),
    conf.get[String]("mail.smtp.pass"),
    conf.get[String]("mail.smtp.channel")
  )

  val maxLoginAttempts: Int = conf.get[Int]("auth.maxLoginAttempts")

  val invitationCodeNotification: InvitationCodeNotification =
    if ("manual" == conf.get[String]("invitation.code.notification")) Manual else Auto
}

//
// Configuration models
//

case class SymmetricKey
(
  name: String,
  bytes: Array[Byte],
)

case class AsymmetricKey
(
  name: String,
  privateKeyBytes: Array[Byte],
  publicKeyBytes: Array[Byte]
)

object MailServiceType extends Enumeration {
  type MailServiceType = Value

  val WebService: MailServiceType = Value
  val Smtp: MailServiceType = Value
}

case class MailService(schema: String, host: String, user: String, pass: String)

case class MailSmtp(host: String, port: Int, user: String, pass: String, channel: String)

object InvitationCodeNotification extends Enumeration {
  type InvitationCodeNotification = Value

  val Auto: InvitationCodeNotification = Value
  val Manual: InvitationCodeNotification = Value
}
