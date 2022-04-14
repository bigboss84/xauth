package io.xauth.service

import io.xauth.config.ApplicationConfiguration
import io.xauth.config.MailServiceType.WebService
import org.apache.commons.mail.SimpleEmail
import play.api.Logger
import play.api.libs.ws.WSAuthScheme.BASIC
import play.api.libs.ws.WSClient

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/**
 * Messaging client.
 */
@Singleton
class MessagingClient @Inject()
(
  ws: WSClient, conf: ApplicationConfiguration
)
(implicit ec: ExecutionContext) {

  private val logger: Logger = Logger(this.getClass)

  private val serviceBaseUrl = s"${conf.mailService.schema}://${conf.mailService.host}"

  def sendMail(name: String, from: String, to: String, subject: String, message: String): Unit = {

    // Internal service
    if (conf.mailServiceType == WebService) {
      ws.url(s"$serviceBaseUrl/send-simple-mail")
        .withAuth(conf.mailService.user, conf.mailService.pass, BASIC)
        .withHttpHeaders(
          "Content-Length" -> "0"
        )
        .addQueryStringParameters(
          "fromName" -> name,
          "fromEmail" -> from,
          "toEmail" -> to,
          "subject" -> subject,
          "contentHtml" -> message
        )
        .post("")
        .onComplete {
          case Success(s) =>
            logger.info(s"email dispatched for $to")
            logger.info(s"email service response: ${s.statusText}")
          case Failure(e) => logger.error(s"error during sending email: ${e.getMessage}")
        }
    }

    // SMTP
    // todo: handle retries
    else Try {
      logger.info(s"sending email to $to...")

      val email = new SimpleEmail

      email.setHostName(conf.mailSmtp.host)
      email.setSmtpPort(conf.mailSmtp.port)
      email.setAuthentication(conf.mailSmtp.user, conf.mailSmtp.pass)

      email.setStartTLSEnabled(conf.mailSmtp.channel == "starttls")
      email.setSSLOnConnect(conf.mailSmtp.channel == "ssl")

      email.setFrom(from, name)
      email.addTo(to)
      email.setSubject(subject)
      email.setMsg(message)

      email.setDebug(conf.mailDebug)
      email.send

      logger.info(s"email sent to $to")
    }
  }

}
