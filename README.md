# sbt-datadog

This project provides three things:
1. `sbt-datadog`, an sbt plugin to easily add the Datadog APM for your project
2. `zio-opentelemetry-datadog-tracing-provider`, a library to help you easily configure zio-opentelemetry to send traces to Datadog via the Datadog APM you configured with the `sbt-datadog` plugin
3. An example ZIO app to show how to use the and the `zio-opentelemetry-datadog-tracing-provider` library

## `sbt-datadog` plugin documentation

The `sbt-datadog` documentation is available [here](SBT_DATADOG_README.md)

## `zio-opentelemetry-datadog-tracing-provider` library documentation

The `zio-opentelemetry-datadog-tracing-provider` documentation is available [here](ZIO_OTLP_DATADOG_PROVIDER_README.md)

## Example project

The example ZIO app can be found in the `example/my-traced-zio-project-example` directory
