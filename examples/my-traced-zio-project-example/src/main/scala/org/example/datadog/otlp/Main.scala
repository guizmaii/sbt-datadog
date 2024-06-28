package org.example.datadog.otlp

import com.guizmaii.datadog.zio.tracing.provider.{TracingConfig, TracingProvider}
import zio._

object Main extends ZIOAppDefault {
  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] =
    (
      for {
        result <- ZIO.serviceWithZIO[MyService](_.doSomething)
        _      <- ZIO.logInfo(s"Result: $result")
      } yield ()
    ).provide(
      MyService.live,
      TracingProvider.live(appName = "my-app", appVersion = Some("1.0.0")),
      TracingConfig.fromSystemEnv
    )
}