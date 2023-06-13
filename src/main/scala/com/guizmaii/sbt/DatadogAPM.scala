package com.guizmaii.sbt

import com.typesafe.sbt.SbtNativePackager.*
import com.typesafe.sbt.packager.archetypes.scripts.BashStartScriptPlugin.autoImport.bashScriptExtraDefines
import com.typesafe.sbt.packager.archetypes.scripts.{BashStartScriptPlugin, BatStartScriptPlugin}
import sbt.*
import sbt.Keys.*
import sbt.librarymanagement.DependencyFilter

/**
 * Eagerly inspired by https://github.com/gilt/sbt-newrelic
 */
object DatadogAPM extends AutoPlugin {

  /**
   * See:
   *  - https://docs.datadoghq.com/tracing/setup_overview/setup/java/?tab=containers
   *  - https://docs.datadoghq.com/agent/kubernetes/apm/?tab=ipport#configure-your-application-pods-in-order-to-communicate-with-the-datadog-agent
   */
  sealed trait TraceAgentUrl
  object TraceAgentUrl {
    final case class TraceAgentHttpUrl(host: String, port: String) extends TraceAgentUrl
    final case class TraceAgentUnixSocketUrl(socket: String)       extends TraceAgentUrl

    final val defaultHttpUrl: TraceAgentUrl       = TraceAgentHttpUrl(host = "localhost", port = "8126")
    final val defaultUnixSocketUrl: TraceAgentUrl = TraceAgentUnixSocketUrl(socket = "/var/run/datadog/apm.socket")
  }

  object autoImport {
    lazy val datadogApmVersion = settingKey[String]("Datadog APM agent version")

    lazy val datadogJavaAgent = taskKey[File]("Datadog agent jar location")

    lazy val datadogApmEnabled = settingKey[Boolean](
      "Datadog APM agent enabled. Default: `DD_TRACE_ENABLED` envvar value if present, 'true' otherwise"
    )

    lazy val datadogProfilingEnabled = settingKey[Boolean](
      "Datadog Profiling. See https://docs.datadoghq.com/profiler/enabling/java/?tab=commandarguments. Default: 'true'. Deactivated if `datadogApmEnabled` is `false`"
    )

    lazy val datadogAllocationProfilingEnabled = settingKey[Boolean](
      "Datadog Allocations Profiling. See https://docs.datadoghq.com/profiler/enabling/java/?tab=commandarguments. Default: 'true'. Deactivated if `datadogApmEnabled` is `false`"
    )

    lazy val datadogServiceName = settingKey[String](
      "The name of a set of processes that do the same job. Used for grouping stats for your application. Default value is the sbt project name"
    )

    lazy val datadogAgentTraceUrl = settingKey[TraceAgentUrl](
      "Configures how the APM communicates with the Datadog agent. By default it uses the default Datadog Unix Socket `/var/run/datadog/apm.socket`. More information, see https://docs.datadoghq.com/agent/kubernetes/apm/?tab=ipport#configure-your-application-pods-in-order-to-communicate-with-the-datadog-agent"
    )

    lazy val datadogEnableDebug =
      settingKey[Boolean]("To return debug level application logs, enable debug mode. Default value: false")

    lazy val datadogGlobalTags = settingKey[Map[String, String]](
      "A list of default tags to be added to every span and every JMX metric. Default value: Empty List"
    )
  }
  import autoImport.*

  override def requires = BashStartScriptPlugin && BatStartScriptPlugin

  val DatadogConfig = config("dd-java-agent").hide

  override lazy val projectSettings = Seq(
    ivyConfigurations += DatadogConfig,
    datadogApmVersion                 := "1.15.3",
    datadogJavaAgent                  := findDatadogJavaAgent(update.value),
    datadogApmEnabled                 := true,
    datadogProfilingEnabled           := true,
    datadogAllocationProfilingEnabled := true,
    datadogServiceName                := name.value,
    datadogAgentTraceUrl              := TraceAgentUrl.defaultUnixSocketUrl,
    datadogEnableDebug                := false,
    datadogGlobalTags                 := Map.empty,
    libraryDependencies ++=
      (
        if (datadogApmEnabled.value) Seq("com.datadoghq" % "dd-java-agent" % datadogApmVersion.value % DatadogConfig)
        else Seq.empty
      ),
    Universal / mappings ++=
      (
        if (datadogApmEnabled.value) Seq(datadogJavaAgent.value -> "datadog/dd-java-agent.jar")
        else Seq.empty
      ),
    bashScriptExtraDefines ++=
      (
        if (datadogApmEnabled.value) {
          Seq(
            """addJava "-javaagent:${app_home}/../datadog/dd-java-agent.jar"""",
            s"""addJava "-Ddd.service.name=${datadogServiceName.value}"""",
            datadogAgentTraceUrl.value match {
              case TraceAgentUrl.TraceAgentHttpUrl(host, port)   => s"""addJava "-Ddd.trace.agent.url=http://$host:$port""""
              case TraceAgentUrl.TraceAgentUnixSocketUrl(socket) => s"""addJava "-Ddd.trace.agent.url=unix://$socket""""
            },
            // https://docs.datadoghq.com/profiler/enabling/java/?tab=commandarguments
            // We have to check `datadogApmEnabled` to enable profiling because if we activate the profiling but deactivate the APM, the APM will start anyway.
            // I'm clearly not an expert in Bash... At least, it's explicit...
            s"""
               |if [ "$${DD_TRACE_ENABLED}" == "true" ]; then
               |  export __ENABLE_TRACES__=true
               |else
               |  export __ENABLE_TRACES__=${datadogApmEnabled.value}
               |fi
               |if [ "$${__ENABLE_TRACES__}" == "true" ]; then
               |  export __ENABLE_PROFILING__=${datadogProfilingEnabled.value}
               |else
               |  export __ENABLE_PROFILING__=false
               |fi
               |if [ "$${__ENABLE_TRACES__}" == "true" ]; then
               |  export __ENABLE_ALLOCATION_PROFILING__=${datadogAllocationProfilingEnabled.value}
               |else
               |  export __ENABLE_ALLOCATION_PROFILING__=false
               |fi
               |addJava "-Ddd.trace.enabled=$${__ENABLE_TRACES__}"
               |addJava "-Ddd.profiling.enabled=$${__ENABLE_PROFILING__}"
               |addJava "-Ddd.profiling.allocation.enabled=$${__ENABLE_ALLOCATION_PROFILING__}"
               |""".stripMargin.trim,
          ) ++ {
            val debugEnabled = datadogEnableDebug.value
            if (debugEnabled) Seq(s"""addJava "-Ddatadog.slf4j.simpleLogger.defaultLogLevel=debug"""")
            else Seq.empty
          } ++ {
            val globalTags = datadogGlobalTags.value
            if (globalTags.nonEmpty) {
              val tags = globalTags.map { case (key, value) => s"$key:$value" }.mkString(",")
              Seq(s"""addJava "-Ddd.trace.global.tags=$tags"""")
            } else Seq.empty
          }
        } else Seq.empty
      ),
  )

  private[this] def findDatadogJavaAgent(report: UpdateReport) = report.matching(datadogFilter).head

  private[this] val datadogFilter: DependencyFilter =
    configurationFilter("dd-java-agent") && artifactFilter(`type` = "jar")

}
