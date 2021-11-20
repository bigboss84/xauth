package io.xauth.actor

import akka.actor.Actor
import io.xauth.config.{ConfigurationLoader, EmailConfiguration}
import io.xauth.model.ContactType.Email
import io.xauth.service.MessagingClient
import io.xauth.service.auth.AuthCodeService
import io.xauth.service.auth.model.AuthCodeType.Deletion
import io.xauth.service.auth.model.AuthUser
import javax.inject.Inject
import play.api.Logger

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
  * Actor that dispatches email with account-deletion code.
  */
class AccountDeletionActor @Inject()
(
  authCodeService: AuthCodeService,
  messagingClient: MessagingClient,
  confLoader: ConfigurationLoader
)
(implicit ec: ExecutionContext) extends Actor {

  private lazy val conf: EmailConfiguration = confLoader.AccountDeletionConf

  def receive: Actor.Receive = {
    case u: AuthUser =>

      u.userInfo.contacts.find(c => c.`type` == Email && c.trusted) match {
        case Some(contact) =>

          // generating new deletion code
          authCodeService.save(Deletion, u.id, Some(contact)) onComplete {
            case Success(authCode) =>
              Logger.info(s"sending deletion email to ${contact.value}")

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
              Logger.error(s"unable to generate deletion code")
          }

        // no email contact
        case _ => Logger.warn(s"no email contact for user ${u.id}")
      }
    // not handled message type
    case _ => Logger.warn("unable to dispatch email: message not handled")
  }
}


