package io.xauth

import play.api.inject.{SimpleModule, bind}

/**
  * Loads actor tasks.
  */
class StarterModule extends SimpleModule(
  bind[Starter].toSelf.eagerly,
)
