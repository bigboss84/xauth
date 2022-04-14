package io.xauth.actor

import akka.actor.{Actor, Props}
import io.xauth.config.{ConfigurationLoader, EmailConfiguration}
import io.xauth.model.ContactType.Email
import io.xauth.service.MessagingClient
import io.xauth.service.auth.AuthCodeService
import io.xauth.service.auth.model.AuthCodeType.PasswordReset
import io.xauth.service.auth.model.AuthUser
import io.xauth.service.workspace.model.Workspace
import play.api.Logger

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
 * Actor that dispatches email with password-reset code.
 */
class PasswordResetActor @Inject()
(
  authCodeService: AuthCodeService,
  messagingClient: MessagingClient,
  confLoader: ConfigurationLoader
)
(implicit ec: ExecutionContext) extends Actor {
  private val logger: Logger = Logger(this.getClass)

  private lazy val conf: EmailConfiguration = confLoader.PasswordResetConf

  import PasswordResetActor._

  def receive: Actor.Receive = {
    case m@PasswordResetMessage(u) =>

      u.userInfo.contacts.find(c => c.`type` == Email && c.trusted) match {
        case Some(contact) =>
          implicit val workspace: Workspace = m.workspace

          // generating new activation code
          authCodeService.save(PasswordReset, u.id, Some(contact)) onComplete {
            case Success(authCode) =>
              logger.info(s"sending password reset email to ${contact.value}")

              messagingClient.sendMail(
                conf.name, conf.from, contact.value, conf.subject,
                conf.message.map { s =>
                  s
                    .replace("{firstName}", u.userInfo.firstName)
                    .replace("{code}", authCode.code)
                    .replace("{codeExpiration}", authCode.expiresAt.toString)
                }.mkString("\n")
              )

            // code generation error
            case Failure(_) =>
              logger.error(s"unable to generate password reset code")
          }

        // no email contact
        case _ => logger.warn(s"no email contact for user ${u.id}")
      }
    // not handled message type
    case _ => logger.warn("unable to dispatch email: message not handled")
  }
}

object PasswordResetActor {
  def props: Props = Props[PasswordResetActor]

  case class PasswordResetMessage(user: AuthUser)(implicit val workspace: Workspace) extends WorkspaceMessage
}
