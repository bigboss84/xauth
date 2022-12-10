package io.xauth.actor

import akka.actor.{Actor, Props}
import io.xauth.config.{ConfigurationLoader, EmailConfiguration}
import io.xauth.model.ContactType.Email
import io.xauth.service.Messaging
import io.xauth.service.auth.AuthCodeService
import io.xauth.service.auth.model.AuthCodeType
import io.xauth.service.invitation.model.Invitation
import io.xauth.service.invitation.model.Invitation.generateCode
import io.xauth.service.workspace.model.Workspace
import play.api.Logger

import java.time.Duration
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
 * Registration Invitation Email dispatcher actor.
 */
class RegistrationInvitationActor @Inject()
(
  authCodeService: AuthCodeService,
  messaging: Messaging,
  confLoader: ConfigurationLoader
)
(implicit ec: ExecutionContext) extends Actor {

  private val logger: Logger = Logger(this.getClass)

  private lazy val conf: EmailConfiguration = confLoader.RegistrationInvitationConf

  import RegistrationInvitationActor._

  def receive: Actor.Receive = {
    // invitation email
    case m@InvitationMessage(i) if i.id.isDefined =>
      implicit val workspace: Workspace = m.workspace
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
              logger.info(s"sending invitation email to ${contact.value}")

              val link = workspace.configuration.frontEnd.baseUrl +
                workspace.configuration.frontEnd.routes.registrationInvitation.replace("{code}", authCode.code)

              messaging.mailer.send(
                contact.value, conf.subject,
                conf.message.map { s =>
                  s
                    .replace("{firstName}", u.firstName)
                    .replace("{code}", authCode.code)
                    .replace("{link}", link)
                    .replace("{codeExpiration}", authCode.expiresAt.toString)
                }.mkString("\n")
              )

            // code generation error
            case Failure(_) =>
              logger.error(s"unable to generate invitation code")
          }

        // missing email contact
        case None => logger.warn("unable to dispatch invitation code: missing email contact")
      }

    // not handled message type
    case _ => logger.warn("unable to dispatch invitation email: message not handled")
  }
}

object RegistrationInvitationActor {
  def props: Props = Props[RegistrationInvitationActor]

  case class InvitationMessage(invitation: Invitation)(implicit val workspace: Workspace) extends WorkspaceMessage
}
