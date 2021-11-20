package io.xauth.actor

import akka.actor._
import io.xauth.config.{ConfigurationLoader, EmailConfiguration}
import io.xauth.model.ContactType.Email
import io.xauth.service.MessagingClient
import io.xauth.service.auth.AuthCodeService
import io.xauth.service.auth.model.AuthCodeType.Activation
import io.xauth.service.auth.model.AuthUser
import javax.inject.Inject
import play.api.Logger

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
  * Activation Email dispatcher actor.
  */
class AccountActivationActor @Inject()
(
  authCodeService: AuthCodeService,
  messagingClient: MessagingClient,
  confLoader: ConfigurationLoader
)
(implicit ec: ExecutionContext) extends Actor {

  private lazy val conf: EmailConfiguration = confLoader.AccountActivationConf

  def receive: Actor.Receive = {

    // activation email
    case u: AuthUser =>

      u.userInfo.contacts.find(_.`type` == Email) match {
        case Some(contact) =>

          // generating new activation code
          authCodeService.save(Activation, u.id, Some(contact)) onComplete {
            case Success(authCode) =>
              Logger.info(s"sending activation email to ${contact.value}")

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
              Logger.error(s"unable to generate activation code")
          }

        // no email contact
        case _ => Logger.warn(s"no email contact for user ${u.id}")
      }

    // not handled message type
    case _ => Logger.warn("unable to dispatch email: message not handled")
  }
}

object AccountActivationActor {
  def props: Props = Props[AccountActivationActor]
}
