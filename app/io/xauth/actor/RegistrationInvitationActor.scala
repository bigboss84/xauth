package io.xauth.actor

import java.time.Duration

import akka.actor.{Actor, Props}
import io.xauth.config.{ConfigurationLoader, EmailConfiguration}
import io.xauth.model.ContactType.Email
import io.xauth.service.MessagingClient
import io.xauth.service.auth.AuthCodeService
import io.xauth.service.auth.model.AuthCodeType
import io.xauth.service.invitation.model.Invitation
import io.xauth.service.invitation.model.Invitation.generateCode
import javax.inject.Inject
import play.api.Logger

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
  * Registration Invitation Email dispatcher actor.
  */
class RegistrationInvitationActor @Inject()
(
  authCodeService: AuthCodeService,
  messagingClient: MessagingClient,
  confLoader: ConfigurationLoader
)
(implicit ec: ExecutionContext) extends Actor {

  private lazy val conf: EmailConfiguration = confLoader.RegistrationInvitationConf

  def receive: Actor.Receive = {

    // invitation email
    case i: Invitation if i.id.isDefined =>
      val u = i.userInfo

      // exists email contact
      u.contacts.find(_.`type` == Email) match {
        case Some(contact) =>

          // invitation code validity
          val validity = i.validTo match {
            case Some(d) => Right(d)
            case _ => Left(Duration.ofDays(5))
          }

          // generating new invitation code
          authCodeService.save(AuthCodeType.Invitation, generateCode, i.id.get, Some(contact), validity) onComplete {
            case Success(authCode) =>
              Logger.info(s"sending invitation email to ${contact.value}")

              messagingClient.sendMail(
                conf.name, conf.from, contact.value, conf.subject,
                conf.message.map { s =>
                  s
                    .replace("{firstName}", u.firstName)
                    .replace("{code}", authCode.code)
                    .replace("{codeExpiration}", authCode.expiresAt.toString)
                }.mkString("\n")
              )

            // code generation error
            case Failure(e) =>
              Logger.error(s"unable to generate invitation code")
          }

        // missing email contact
        case None => Logger.warn("unable to dispatch invitation code: missing email contact")
      }

    // not handled message type
    case _ => Logger.warn("unable to dispatch invitation email: message not handled")
  }
}

object RegistrationInvitationActor {
  def props: Props = Props[RegistrationInvitationActor]
}
