Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / organization  := "com.guizmaii"
ThisBuild / homepage      := Some(url("https://github.com/guizmaii-opensource/sbt-datadog"))
ThisBuild / licenses      := Seq("MIT" -> url("https://opensource.org/license/MIT"))
ThisBuild / versionScheme := Some("semver-spec")
ThisBuild / developers    :=
  List(
    Developer(
      "guizmaii",
      "Jules Ivanic",
      "jules.ivanic@gmail.com",
      url("https://x.com/guizmaii"),
    )
  )

addCommandAlias("releaseSbtDatadog", "project sbt-datadog;clean;Test/compile;ci-release")
addCommandAlias(
  "releaseZioOpentelemetryDatadogTracingProvider",
  "project zio-opentelemetry-datadog-tracing-provider;clean;+Test/compile;ci-release",
)

val scala212 = "2.12.19"
val scala213 = "2.13.14"
val scala3   = "3.3.3"

lazy val root =
  project
    .in(file("."))
    .settings(
      publish / skip     := true,
      crossScalaVersions := Nil, // https://www.scala-sbt.org/1.x/docs/Cross-Build.html#Cross+building+a+project+statefully,
    )
    .aggregate(
      `sbt-datadog`,
      `zio-opentelemetry-datadog-tracing-provider`,
      `my-traced-zio-project-example`,
    )

lazy val `sbt-datadog` =
  project
    .in(file("sbt-datadog"))
    .settings(
      name         := "sbt-datadog",
      scalaVersion := scala212,
      sbtPlugin    := true,
      addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.10.0" % "provided"),
    )

lazy val `zio-opentelemetry-datadog-tracing-provider` =
  project
    .in(file("zio-opentelemetry-datadog-tracing-provider"))
    .settings(
      name               := "zio-opentelemetry-datadog-tracing-provider",
      scalaVersion       := scala213,
      crossScalaVersions := Seq(scala212, scala213, scala3),
      libraryDependencies ++= Seq(
        "dev.zio"         %% "zio"               % "2.1.5" % "provided",
        "dev.zio"         %% "zio-config"        % "4.0.2" % "provided",
        "dev.zio"         %% "zio-opentelemetry" % "2.0.3",
        "io.opentelemetry" % "opentelemetry-api" % "1.39.0",
      ),
    )

lazy val `my-traced-zio-project-example` =
  project
    .in(file("examples/my-traced-zio-project-example"))
    .dependsOn(`zio-opentelemetry-datadog-tracing-provider`)
    .settings(publish / skip := true)
    .settings(
      name         := "my-traced-zio-project-example",
      scalaVersion := scala213,
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio"        % "2.1.5",
        "dev.zio" %% "zio-config" % "4.0.2",
      ),
      //
      // This is an example of configuration you need to add in your project to configure sbt-datadog and the Datadog APM Agent
      //
      // Seq(
      //  datadogServiceName := "my-app",
      //  datadogApmVersion  := "1.36.0", // See https://github.com/DataDog/dd-trace-java/releases
      //  datadogGlobalTags  := Map("version" -> version.value),
      //  bashScriptExtraDefines ++= Seq(
      //    "-XX:FlightRecorderOptions=stackdepth=256",
      //    "-Ddd.appsec.enabled=true",
      //    "-Ddd.dynamic.instrumentation.enabled=true",
      //    "-Ddd.trace.otel.enabled=true",                             // https://docs.datadoghq.com/tracing/trace_collection/custom_instrumentation/java/otel/#setup
      //    "-Ddd.logs.injection=true",
      //    "-Ddd.iast.enabled=true",                                   // code vulnerability detection
      //    "-Ddd.trace.sample.rate=1",
      //    "-Ddd.profiling.enabled=true",                              // https://docs.datadoghq.com/profiler/enabling/java/?tab=datadog
      //    "-Ddd.profiling.allocation.enabled=true",                   // https://docs.datadoghq.com/tracing/profiler/profiler_troubleshooting/#enabling-the-allocation-profiler
      //    "-Ddd.profiling.heap.enabled=true",                         // https://docs.datadoghq.com/tracing/profiler/profiler_troubleshooting/#enabling-the-heap-profiler
      //    "-Ddd.profiling.jfr-template-override-file=comprehensive",  // https://docs.datadoghq.com/tracing/profiler/profiler_troubleshooting/#increase-profiler-information-granularity
      //    // https://docs.datadoghq.com/tracing/trace_collection/library_config/java/
      //    "-Ddd.service.mapping=postgresql:my-app-postgresql",
      //    "-Ddd.integration.okhttp.enabled=false",                    // needs to disable tracing of datadog agent
      //    "-Ddd.integration.throwables.enabled=false",                // https://twitter.com/ghostdogpr/status/1602243328119971841
      //    "-Ddd.integration.opentelemetry.experimental.enabled=true", // https://github.com/DataDog/dd-trace-java/pull/4669
      //    "-Ddd.integration.zio.experimental.enabled=true",           // https://github.com/DataDog/dd-trace-java/pull/4848
      //  ).map(config => s"""addJava "$config""""),
    )
