lazy val versionReport = taskKey[String]("version-report")
lazy val gitHeadCommitSha = settingKey[String]("git-head")
lazy val makeVersionProperties = taskKey[Seq[File]]("make-version-props")

// PROJECT PROPERTIES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
lazy val commonSettings = Seq(
  organization := "org.mbari.vars",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.8",
  libraryDependencies ++= {
    val slf4jVersion = "1.7.21"
    val logbackVersion = "1.1.7"
    Seq(
      "ch.qos.logback"  % "logback-classic"  % logbackVersion,
      "ch.qos.logback"  % "logback-core"     % logbackVersion,
      "com.typesafe"    % "config"           % "1.3.0",
      "junit"           % "junit"            % "4.12"          % "test",
      "org.scalatest"   %% "scalatest"       % "3.0.0"         % "test",
      "org.slf4j"       % "log4j-over-slf4j" % slf4jVersion,
      "org.slf4j"       % "slf4j-api"        % slf4jVersion)
  },
  resolvers ++= Seq(Resolver.mavenLocal,
    "hohonuuli-bintray" at "http://dl.bintray.com/hohonuuli/maven"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",       // yes, this is 2 args
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Xfuture"),
  javacOptions ++= Seq("-target", "1.8", "-source","1.8"),
  incOptions := incOptions.value.withNameHashing(true),
  updateOptions := updateOptions.value.withCachedResolution(true),
  shellPrompt := { state =>
    val user = System.getProperty("user.name")
    user + "@" + Project.extract(state).currentRef.project + ":sbt> "
  },
  versionReport <<= (externalDependencyClasspath in Compile, streams) map {
    (cp: Seq[Attributed[File]], streams) =>
      val report = cp.map {
        attributed =>
          attributed.get(Keys.moduleID.key) match {
            case Some(moduleId) => "%40s %20s %10s %10s".format(
              moduleId.organization,
              moduleId.name,
              moduleId.revision,
              moduleId.configurations.getOrElse("")
            )
            case None =>
              // unmanaged JAR, just
              attributed.data.getAbsolutePath
          }
      }.sortBy(a => a.trim.split("\\\\s+").map(_.toUpperCase).take(2).last).mkString("\n")
      streams.log.info(report)
      report
  },
  gitHeadCommitSha := scala.util.Try(Process("git rev-parse HEAD").lines.head).getOrElse(""),
  makeVersionProperties := {
    val propFile = (resourceManaged in Compile).value / "version.properties"
    val content = "version=%s" format (gitHeadCommitSha.value)
    IO.write(propFile, content)
    Seq(propFile)
  },
  resourceGenerators in Compile <+= makeVersionProperties,
  scalastyleFailOnError := true,
  // default tags are TODO, FIXME, WIP and XXX. I want the following instead
  todosTags := Set("TODO", "FIXME", "WTF?"),
  fork := true,
  initialCommands in console :=
      """
        |import java.util.Date
      """.stripMargin
)

lazy val root = (project in file("."))
        .settings(commonSettings: _*)
        .settings(
          name := "annosaurus",
          libraryDependencies ++= {
            val akkaVersion = "2.4.7"
            val derbyVersion = "10.12.1.1"
            val eclipselinkVersion = "2.6.3"
            val gsonJavatimeVersion = "1.1.1"
            val gsonVersion = "2.7"
            val h2Version = "1.4.192"
            val jettyVersion = "9.3.11.v20160721"
            val jtaVersion = "1.1"
            val jtdsVersion = "1.3.1"
            val logbackVersion = "1.1.7"
            val scalaTestVersion = "3.0.0"
            val scalatraVersion = "2.4.1"
            val slf4jVersion = "1.7.21"
            val vcr4jVersion = "3.0.0"
            Seq(
              "com.fatboyindustrial.gson-javatime-serialisers" % "gson-javatime-serialisers" % gsonJavatimeVersion,
              "com.google.code.gson"     % "gson"                           % gsonVersion,
              "com.h2database"           % "h2"                             % h2Version             % "test",
              "commons-codec"            % "commons-codec"                  % "1.10",
              "javax.servlet"            % "javax.servlet-api"              % "3.1.0",
              "javax.transaction"        % "jta"                            % jtaVersion,
              "net.databinder.dispatch" %% "dispatch-core"                  % "0.11.3",
              "org.apache.derby"         % "derby"                          % derbyVersion, //          % "test",
              "org.apache.derby"         % "derbyclient"                    % derbyVersion, //          % "test",
              "org.apache.derby"         % "derbynet"                       % derbyVersion, //          % "test",
              "org.eclipse.jetty"        % "jetty-server"                   % jettyVersion          % "compile;test",
              "org.eclipse.jetty"        % "jetty-servlets"                 % jettyVersion          % "compile;test",
              "org.eclipse.jetty"        % "jetty-webapp"                   % jettyVersion          % "compile;test",
              "org.eclipse.persistence"  % "eclipselink"                    % eclipselinkVersion,
              "org.json4s"              %% "json4s-jackson"                 % "3.4.0",
              "org.mbari.vcr4j"          % "vcr4j-core"                     % vcr4jVersion,
              "org.scalatest"           %% "scalatest"                      % scalaTestVersion      % "test",
              "org.scalatra"            %% "scalatra"                       % scalatraVersion,
              "org.scalatra"            %% "scalatra-json"                  % scalatraVersion,
              "org.scalatra"            %% "scalatra-scalate"               % scalatraVersion,
              "org.scalatra"            %% "scalatra-slf4j"                 % scalatraVersion,
              "org.scalatra"            %% "scalatra-swagger"               % scalatraVersion,
              "org.scalatra"            %% "scalatra-swagger-ext"           % scalatraVersion,
              "org.scalatra"            %% "scalatra-scalatest"            % scalatraVersion)
                .map(_.excludeAll(ExclusionRule("org.slf4j", "slf4j-jdk14"),
                  ExclusionRule("org.slf4j", "slf4j-log4j12"),
                  ExclusionRule("javax.servlet", "servlet-api")))
          },
          mainClass in assembly := Some("JettyMain")
        )


// OTHER SETTINGS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// -- sbt-native-packager
enablePlugins(JavaServerAppPackaging)

// -- xsbt-web-plugin
enablePlugins(JettyPlugin)



// -- SBT-PACK
// For sbt-pack
packAutoSettings

// For sbt-pack
val apps = Seq("jetty-main")

packAutoSettings ++ Seq(packExtraClasspath := apps.map(_ -> Seq("${PROG_HOME}/conf")).toMap,
  packJvmOpts := apps.map(_ -> Seq("-Duser.timezone=UTC", "-Xmx4g")).toMap,
  packDuplicateJarStrategy := "latest",
  packJarNameConvention := "original")


// -- SCALARIFORM
// Format code on save with scalariform
import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform

SbtScalariform.scalariformSettings

SbtScalariform.ScalariformKeys.preferences := SbtScalariform.ScalariformKeys.preferences.value
  .setPreference(IndentSpaces, 2)
  .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, false)
  .setPreference(DoubleIndentClassDeclaration, true)

// Aliases
addCommandAlias("cleanall", ";clean;clean-files")
