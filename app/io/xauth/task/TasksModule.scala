package io.xauth.task

import play.api.inject.{SimpleModule, bind}

/**
  * Loads actor tasks.
  */
class TasksModule extends SimpleModule(
  // cleans expired codes
  bind[CodeCleanTask].toSelf.eagerly,
  // cleans expired refresh tokens
  bind[TokenCleanTask].toSelf.eagerly,
)
