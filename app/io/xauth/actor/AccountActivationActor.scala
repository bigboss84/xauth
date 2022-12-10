package io.xauth.actor

import akka.actor._
import io.xauth.config.{ConfigurationLoader, EmailConfiguration}
import io.xauth.model.ContactType.Email
import io.xauth.service.Messaging
import io.xauth.service.auth.AuthCodeService
import io.xauth.service.auth.model.AuthCodeType.Activation
import io.xauth.service.auth.model.AuthUser
import io.xauth.service.workspace.model.Workspace
import play.api.Logger

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
  * Activation Email dispatcher actor.
  */
class AccountActivationActor @Inject()
(
  authCodeService: AuthCodeService,
  messaging: Messaging,
  confLoader: ConfigurationLoader
)
(implicit ec: ExecutionContext) extends Actor {

  private val logger: Logger = Logger(this.getClass)

  private lazy val conf: EmailConfiguration = confLoader.AccountActivationConf

  import AccountActivationActor._

  def receive: Actor.Receive = {

    // activation email
    case m@ActivateUserMessage(u) =>

      u.userInfo.contacts.find(_.`type` == Email) match {
        case Some(contact) =>
          implicit val workspace: Workspace = m.workspace

          // generating new activation code
          authCodeService.save(Activation, u.id, Some(contact)) onComplete {
            case Success(authCode) =>
              logger.info(s"sending activation email to ${contact.value}")

              val link = workspace.configuration.frontEnd.baseUrl +
                  workspace.configuration.frontEnd.routes.activation.replace("{code}", authCode.code)

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
              logger.error(s"unable to generate activation code")
          }

        // no email contact
        case _ => logger.warn(s"no email contact for user ${u.id}")
      }

    // not handled message type
    case _ => logger.warn("unable to dispatch email: message not handled")
  }
}

object AccountActivationActor {
  def props: Props = Props[AccountActivationActor]

  case class ActivateUserMessage(user: AuthUser)
                                (implicit val workspace: Workspace) extends WorkspaceMessage
}
