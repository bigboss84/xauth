package io.xauth.actor

import akka.actor.Actor
import io.xauth.config.{ConfigurationLoader, EmailConfiguration}
import io.xauth.model.ContactType.Email
import io.xauth.model.UserContact
import io.xauth.service.MessagingClient
import io.xauth.service.auth.AuthCodeService
import io.xauth.service.auth.model.AuthCodeType.ContactTrust
import io.xauth.service.auth.model.AuthUser
import javax.inject.Inject
import play.api.Logger

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
  * Actor that dispatches email with contact trust code.
  */
class ContactTrustActor @Inject()
(
  authCodeService: AuthCodeService,
  messagingClient: MessagingClient,
  confLoader: ConfigurationLoader
)
(implicit ec: ExecutionContext) extends Actor {

  private lazy val conf: EmailConfiguration = confLoader.ContactActivationConf

  def receive: Actor.Receive = {
    case (u: AuthUser, c: UserContact) =>

      if (c.`type` == Email) {
        u.userInfo.contacts.find(uc => uc.`type` == c.`type` && !uc.trusted && uc.value == c.value) match {
          case Some(contact) =>

            // generating new trust code
            authCodeService.save(ContactTrust, u.id, Some(c)) onComplete {
              case Success(authCode) =>
                Logger.info(s"sending trust code to ${contact.value}")

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
              case Failure(e) =>
                Logger.error(s"unable to generate contact trust code")
            }

          // no email contact
          case _ => Logger.warn(s"no untrusted email contact for user ${u.id}")
        }
      }

      // not handled contact type
      else Logger.warn(s"${c.value} is not of type email")

    // not handled message type
    case _ => Logger.warn("unable to dispatch email: message not handled")
  }
}


