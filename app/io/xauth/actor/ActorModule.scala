package io.xauth.actor

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

/**
  * Model that configure actors using Guice for dependency injection.
  */
class ActorModule extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    bindActor[AccountActivationActor]("account-activator")
    bindActor[AccountDeletionActor]("account-deletion")
    bindActor[PasswordResetActor]("password-reset")
    bindActor[ContactTrustActor]("contact-trust")
    bindActor[RegistrationInvitationActor]("registration-invitation")
  }
}