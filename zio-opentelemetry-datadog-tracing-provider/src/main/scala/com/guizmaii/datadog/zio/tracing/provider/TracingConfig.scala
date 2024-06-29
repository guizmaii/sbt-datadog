package com.guizmaii.datadog.zio.tracing.provider

import zio.ZLayer
import zio.config.ConfigDescriptor.boolean
import zio.config.{ConfigDescriptor, ReadError, ZConfig}

sealed trait TracingConfig extends Product with Serializable
object TracingConfig {
  case object OPENTELEMETRY extends TracingConfig
  case object Disabled      extends TracingConfig

  val config: ConfigDescriptor[TracingConfig] =
    boolean("ZIO_OPENTELEMETRY_DATADOG_ENABLED")
      .default(false)
      .transformOrFailLeft(enabled => if (enabled) Right(OPENTELEMETRY) else Right(Disabled))(_ => false)

  /**
   * Provides a way to enable or disable the OpenTelemetry Tracing via the `ZIO_OPENTELEMETRY_DATADOG_ENABLED` environment variable.
   */
  val fromSystemEnv: ZLayer[Any, ReadError[String], TracingConfig] = ZConfig.fromSystemEnv(config)
}
