package io.xauth.config

import javax.inject.{Inject, Singleton}
import play.api.Environment

/**
  * Email configuration
  */
@Singleton
class ConfigurationLoader @Inject()(implicit env: Environment) {

  val AccountActivationConf: EmailConfiguration =
    new JsonResource[EmailConfiguration]("conf/email/account-activation.json").value

  val AccountDeletionConf: EmailConfiguration =
    new JsonResource[EmailConfiguration]("conf/email/account-deletion.json").value

  val ContactActivationConf: EmailConfiguration =
    new JsonResource[EmailConfiguration]("conf/email/contact-activation.json").value

  val PasswordResetConf: EmailConfiguration =
    new JsonResource[EmailConfiguration]("conf/email/password-reset.json").value

  val RegistrationInvitationConf: EmailConfiguration =
    new JsonResource[EmailConfiguration]("conf/email/registration-invitation.json").value
}

case class EmailConfiguration(name: String, from: String, subject: String, message: Seq[String])

object EmailConfiguration {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json._

  implicit val reads: Reads[EmailConfiguration] = (
    (__ \ "name").read[String]
      and (__ \ "from").read[String]
      and (__ \ "subject").read[String]
      and (__ \ "message").read[Seq[String]]
    ) (EmailConfiguration.apply _)

  implicit val writes: Writes[EmailConfiguration] = (
    (__ \ "name").write[String]
      and (__ \ "from").write[String]
      and (__ \ "subject").write[String]
      and (__ \ "message").write[Seq[String]]
    ) (unlift(EmailConfiguration.unapply))
}