package io.xauth.config

import io.xauth.config.InvitationCodeNotification.{Auto, InvitationCodeNotification, Manual}
import play.api.Configuration

import javax.inject.{Inject, Singleton}

@Singleton
class ApplicationConfiguration @Inject()(conf: Configuration) {

  val baseUrl: String = conf.get[String]("baseUrl")

  // Task
  val taskCodeCleanInterval: Int = conf.get[Int]("task.codeClean.interval")
  val taskInvitationCleanInterval: Int = conf.get[Int]("task.invitationClean.interval")
  val taskTokenCleanInterval: Int = conf.get[Int]("task.tokenClean.interval")

  // Json Web Token
  val jwtExpirationAccessToken: Int = conf.get[Int]("workspace.jwt.expiration.accessToken")
  val jwtExpirationRefreshToken: Int = conf.get[Int]("workspace.jwt.expiration.refreshToken")
  val jwtSecretKeyPath: String = conf.get[String]("workspace.jwt.secretKey.path")
  val jwtAlgorithm: String = conf.get[String]("workspace.jwt.secretKey.algorithm")

  val mongoDbUri: String = conf.get[String]("mongodb.uri")

  // Front-End
  val frontEnd: FrontEnd = FrontEnd(
    conf.get[String]("workspace.frontEnd.baseUrl"),
    Routes(
      conf.get[String]("workspace.frontEnd.routes.activation"),
      conf.get[String]("workspace.frontEnd.routes.deletion"),
      conf.get[String]("workspace.frontEnd.routes.contactTrust"),
      conf.get[String]("workspace.frontEnd.routes.passwordReset"),
      conf.get[String]("workspace.frontEnd.routes.registrationInvitation"),
    )
  )

  // Mail
  val mailName: String = conf.get[String]("workspace.mail.name")
  val mailFrom: String = conf.get[String]("workspace.mail.from")
  val mailSmtp: MailSmtp = MailSmtp(
    conf.get[String]("workspace.mail.smtp.host"),
    conf.get[Int]("workspace.mail.smtp.port"),
    conf.get[String]("workspace.mail.smtp.user"),
    conf.get[String]("workspace.mail.smtp.pass"),
    conf.get[String]("workspace.mail.smtp.channel"),
    conf.get[Boolean]("workspace.mail.smtp.debug")
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

case class Routes(activation: String, deletion: String, contactTrust: String, passwordReset: String, registrationInvitation: String)
case class FrontEnd(baseUrl: String, routes: Routes)

case class MailSmtp(host: String, port: Int, user: String, pass: String, channel: String, debug: Boolean)

object InvitationCodeNotification extends Enumeration {
  type InvitationCodeNotification = Value

  val Auto: InvitationCodeNotification = Value
  val Manual: InvitationCodeNotification = Value
}
