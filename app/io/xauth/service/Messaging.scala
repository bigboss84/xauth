package io.xauth.service

import io.xauth.service.workspace.model.{MailConfiguration, Workspace}
import org.apache.commons.mail.{Email, SimpleEmail}
import play.api.Logger

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.Try

/**
  * Messaging service that makes workspace clients.
  */
@Singleton
class Messaging @Inject()(implicit ec: ExecutionContext) {

  private val logger: Logger = Logger(this.getClass)

  class EmailService(conf: MailConfiguration) {
    private val mailer: Email = {
      val email = new SimpleEmail

      email.setHostName(conf.smtp.host)
      email.setSmtpPort(conf.smtp.port)
      email.setAuthentication(conf.smtp.user, conf.smtp.pass)

      email.setStartTLSEnabled(conf.smtp.channel == "STARTTLS")
      email.setSSLOnConnect(conf.smtp.channel == "SSL")

      email.setFrom(conf.from, conf.name)

      email.setDebug(conf.smtp.debug)
      email
    }

    def send(to: String, subject: String, message: String): Unit = Try {
      // todo: handle retries
      logger.info(s"sending email to $to...")
      mailer.addTo(to)
      mailer.setSubject(subject)
      mailer.setMsg(message)
      mailer.send()
      logger.info(s"email sent to $to")
    }
  }

  import cats.data.Reader

  private val reader: Reader[MailConfiguration, EmailService] = Reader(new EmailService(_))

  def mailer(implicit w: Workspace): EmailService = {
    logger.info(s"making email service for workspace ${w.slug}...")
    reader.run(w.configuration.mail)
  }

}
