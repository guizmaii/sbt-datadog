# zio-opentelemetry-datadog-tracing-provider

## Two words about Datadog

### Terminology

- **Datadog APM Agent**: is the Java agent added by `sbt-datadog` in your project.    
  It's job is to monitor your application and send traces to the Datadog Agent.   
  See also: https://github.com/DataDog/dd-trace-java   
- **Datadog Agent**: is the agent that receives traces from the Datadog APM Agent and sends them to the Datadog backend.
  It must be installed in your infrastructure so that your application can send traces to it.    
  See also: https://github.com/DataDog/datadog-agent   

### The DataDog APM Agent magic

The Datadog APM agent you configured with `sbt-datadog` adds a pre-configured OpenTelemetry `Tracer` to your project.    
This OpenTelemetry `Tracer` is used to send traces to the Datadog Agent configured in your infrastructure.   
The zio-opentelemtry `Tracing` is a kind of wrapper around OpenTelemetry `Tracer`.   
This is this `Tracing` abstraction that you'll need to use in your ZIO project.

This library is here to help you get a configured `Tracing` instance in your ZIO project.

## zio-opentelemetry-datadog-tracing-provider

### Installation

In your build.sbt file:
```scala
libraryDependencies += "com.guizmaii" %% "zio-opentelemetry-datadog-tracing-provider" % "x.x.x"
```
(To find the latest vesrion, see the [releases](https://github.com/guizmaii-opensource/sbt-datadog/releases))

### Documentation

This library provides you two things:
1. The [TracingProvider](zio-opentelemetry-datadog-tracing-provider/src/main/scala/com/guizmaii/datadog/zio/tracing/provider/TracingProvider.scala) object to help you easily get a zio-opentelemetry `Tracing` instance configured to send traces to Datadog via the Datadog APM you configured with the sbt plugin.
2. The [TracingConfig](zio-opentelemetry-datadog-tracing-provider/src/main/scala/com/guizmaii/datadog/zio/tracing/provider/TracingConfig.scala) object to help you easily enable or disable zio-opentelemetry `Tracing` via .

### TracingProvider

#### `TracingProvider.live` layer

This layer is the one you'll want to use in your app.    
It'll provide you a `Tracing` instance that'll send traces to Datadog via the Datadog APM you configured with the sbt plugin.

You have an example of how to use it in [examples/my-traced-zio-project-example/src/main/scala/org/example/datadog/old/Main](examples/my-traced-zio-project-example/src/main/scala/org/example/datadog/otlp/Main.scala).

#### `TracingProvider.noOp` layer

This layer is useful when you want to disable tracing in your app.   
It's for example useful in your tests.   
The `Tracing` instance that it'll provide does nothing. 

## The "Traced Service Pattern"

We all know the ZIO "service pattern".      
Well, I "extended" it with what I call the "traced service pattern".    
I give an example of this pattern in [examples/my-traced-zio-project-example/src/main/scala/org/example/datadog/old/MyService](examples/my-traced-zio-project-example/src/main/scala/org/example/datadog/otlp/MyService.scala).