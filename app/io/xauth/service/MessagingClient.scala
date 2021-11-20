package io.xauth.service

import io.xauth.config.ApplicationConfiguration
import io.xauth.config.MailServiceType.WebService
import javax.inject.{Inject, Singleton}
import org.apache.commons.mail.SimpleEmail
import play.api.Logger
import play.api.libs.ws.WSAuthScheme.BASIC
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Messaging client.
  */
@Singleton
class MessagingClient @Inject()
(
  ws: WSClient, conf: ApplicationConfiguration
)
(implicit ec: ExecutionContext) {

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
            Logger.info(s"email dispatched for $to")
            Logger.info(s"email service response: ${s.statusText}")
          case Failure(e) => Logger.error(s"error during sending email: ${e.getMessage}")
        }
    }

    // SMTP
    else Future {
      Logger.info(s"sending email to $to...")

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

      Logger.info(s"email sent to $to")
    }
  }

}
