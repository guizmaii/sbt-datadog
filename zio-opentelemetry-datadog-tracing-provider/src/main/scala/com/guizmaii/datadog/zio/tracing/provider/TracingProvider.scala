package com.guizmaii.datadog.zio.tracing.provider

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.TracerProvider
import zio.telemetry.opentelemetry.Tracing
import zio.{TaskLayer, ZIO, ZLayer}

object TracingProvider {

  /**
   * Useful for tests
   */
  def noOp: TaskLayer[Tracing] = ZLayer(ZIO.attempt(TracerProvider.noop().get("noOp"))) >>> Tracing.live

  /**
   * Provides a zio-opentelemetry `Tracing` instance with the OpenTelemetry `Tracer` configured by the Datadog APM Agent
   *
   * Don't forget to enable the OpenTelemetry tracing in the Datadog APM Agent.
   * You can enable it by setting the `DD_TRACE_OTEL_ENABLED` environment variable to `true`
   * or by setting the `dd.trace.otel.enabled` property to `true`.
   *
   * See related Datadog documentation:
   * - https://docs.datadoghq.com/tracing/trace_collection/custom_instrumentation/java/otel/
   */
  def live(appName: String, appVersion: Option[String] = None): ZLayer[TracingConfig, Throwable, Tracing] =
    ZLayer.fromZIO {
      for {
        config <- ZIO.service[TracingConfig]
        _      <- ZIO.logInfo(s"OpenTelemetry Tracing config: $config - appName: $appName - appVersion: $appVersion")
      } yield config match {
        case TracingConfig.Disabled      => noOp
        case TracingConfig.Opentelemetry =>
          // Magically get the OpenTelemetry `Tracer` configured in the Datadog APM Agent
          // that you configured in your application thanks the `sbt-datadog` plugin
          val tracer =
            ZIO.attempt {
              appVersion match {
                case Some(version) => GlobalOpenTelemetry.getTracer(appName, version)
                case None          => GlobalOpenTelemetry.getTracer(appName)
              }
            }

          ZLayer(tracer) >>> Tracing.propagating
      }
    }.flatten
}
