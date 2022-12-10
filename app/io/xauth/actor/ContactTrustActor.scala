package io.xauth.actor

import akka.actor.Actor
import io.xauth.config.{ConfigurationLoader, EmailConfiguration}
import io.xauth.model.ContactType.Email
import io.xauth.model.UserContact
import io.xauth.service.Messaging
import io.xauth.service.auth.AuthCodeService
import io.xauth.service.auth.model.AuthCodeType.ContactTrust
import io.xauth.service.auth.model.AuthUser
import io.xauth.service.workspace.model.Workspace
import play.api.Logger

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
 * Actor that dispatches email with contact trust code.
 */
class ContactTrustActor @Inject()
(
  authCodeService: AuthCodeService,
  messaging: Messaging,
  confLoader: ConfigurationLoader
)
(implicit ec: ExecutionContext) extends Actor {

  private val logger: Logger = Logger(this.getClass)

  private lazy val conf: EmailConfiguration = confLoader.ContactActivationConf

  import ContactTrustActor._

  def receive: Actor.Receive = {
    case m@ContactTrustMessage(u, c) =>

      if (c.`type` == Email) {
        u.userInfo.contacts.find(uc => uc.`type` == c.`type` && !uc.trusted && uc.value == c.value) match {
          case Some(contact) =>
            implicit val workspace: Workspace = m.workspace

            // generating new trust code
            authCodeService.save(ContactTrust, u.id, Some(c)) onComplete {
              case Success(authCode) =>
                logger.info(s"sending trust code to ${contact.value}")

                val link = workspace.configuration.frontEnd.baseUrl +
                  workspace.configuration.frontEnd.routes.contactTrust.replace("{code}", authCode.code)

                messaging.mailer.send(
                  contact.value, conf.subject,
                  conf.message.map { s =>
                    s
                      .replace("{firstName}", u.userInfo.firstName)
                      .replace("{code}", authCode.code)
                      .replace("{link}", link)
                      .replace("{codeExpiration}", authCode.expiresAt.toString)
                  }.mkString("\n")
                )

              // code generation error
              case Failure(_) =>
                logger.error(s"unable to generate contact trust code")
            }

          // no email contact
          case _ => logger.warn(s"no untrusted email contact for user ${u.id}")
        }
      }

      // not handled contact type
      else logger.warn(s"${c.value} is not of type email")

    // not handled message type
    case _ => logger.warn("unable to dispatch email: message not handled")
  }
}

object ContactTrustActor {
  case class ContactTrustMessage(user: AuthUser, contact: UserContact)
                                (implicit val workspace: Workspace) extends WorkspaceMessage
}


