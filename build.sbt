val akkaVersion         = "2.4.7"
val auth0Version        = "3.10.1"
val codecVersion        = "1.14"
val configVersion       = "1.4.0"
val derbyVersion        = "10.15.2.0"
val eclipselinkVersion  = "2.7.6"
val gsonJavatimeVersion = "1.1.1"
val gsonVersion         = "2.8.6"
val h2Version           = "1.4.200"
val hikariVersion       = "3.4.2"
val jansiVersion        = "1.18"
val javamelodyVersion   = "1.82.0"
val jettyVersion        = "9.4.27.v20200227"
val jsonVersion         = "3.6.7"
val jtaVersion          = "1.1"
val jtdsVersion         = "1.3.1"
val junitVersion        = "4.13"
val logbackVersion      = "1.2.3"
val oracleVersion       = "19.3.0.0"
val postgresqlVersion   = "42.2.11"
val rxjavaVersion       = "2.2.17"
val scalatestVersion    = "3.0.8"
val scalatraVersion     = "2.7.0"
val scilubeVersion      = "2.0.7.jre11"
val servletVersion      = "4.0.1"
val slf4jVersion        = "1.7.30"
val sqlserverVersion    = "8.2.1.jre11"
val uuidgenVersion      = "0.1.3"
val vcr4jVersion        = "4.0.2"
val zeromqVersion       = "0.5.2"

Global / onChangedBuildSource := ReloadOnSourceChanges

scalafixDependencies in ThisBuild += "org.scalatest" %% "autofix" % "3.1.0.0"

addCompilerPlugin(scalafixSemanticdb) // enable SemanticDB

lazy val buildSettings = Seq(
  organization := "org.mbari.vars",
  version := "0.6.3",
  scalaVersion in ThisBuild := "2.13.1",
  crossScalaVersions := Seq("2.13.1"),
  organizationName := "Monterey Bay Aquarium Research Institute",
  startYear := Some(2017),
  licenses += ("Apache-2.0", new URL(
    "https://www.apache.org/licenses/LICENSE-2.0.txt"
  ))
)

lazy val consoleSettings = Seq(
  shellPrompt := { state =>
    val user = System.getProperty("user.name")
    user + "@" + Project.extract(state).currentRef.project + ":sbt> "
  },
  initialCommands in console :=
    """
      |import java.time.Instant
      |import java.util.UUID
    """.stripMargin
)

lazy val dependencySettings = Seq(
  resolvers ++= Seq(
    Resolver.mavenLocal,
    Resolver.bintrayRepo("hohonuuli", "maven")
  )
)

lazy val optionSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8", // yes, this is 2 args
    "-feature",
    "-language:existentials",
    "-language:implicitConversions",
    "-unchecked",
    "-Xlint"
  ),
  javacOptions ++= Seq("-target", "11", "-source", "11"),
  updateOptions := updateOptions.value.withCachedResolution(true)
)

lazy val appSettings = buildSettings ++
  consoleSettings ++
  dependencySettings ++
  optionSettings ++ Seq(
  fork := true
)

lazy val apps = Map("jetty-main" -> "JettyMain") // for sbt-pack

lazy val annosaurus = (project in file("."))
  .enablePlugins(JettyPlugin)
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(PackPlugin)
  // .enablePlugins(EclipseLinkStaticWeaver)
  // .settings(staticWeaverLogLevel := 0)
  .settings(appSettings)
  .settings(
    libraryDependencies ++= Seq(
      "ch.qos.logback"                                 % "logback-classic"                   % logbackVersion,
      "ch.qos.logback"                                 % "logback-core"                      % logbackVersion,
      "com.auth0"                                      % "java-jwt"                          % auth0Version,
      "com.fatboyindustrial.gson-javatime-serialisers" % "gson-javatime-serialisers"         % gsonJavatimeVersion,
      "com.google.code.gson"                           % "gson"                              % gsonVersion,
      "com.h2database"                                 % "h2"                                % h2Version % "test",
      "com.microsoft.sqlserver"                        % "mssql-jdbc"                        % sqlserverVersion,
      "com.oracle.ojdbc"                               % "ojdbc8"                            % oracleVersion,
      "com.typesafe"                                   % "config"                            % configVersion,
      "com.zaxxer"                                     % "HikariCP"                          % hikariVersion,
      "commons-codec"                                  % "commons-codec"                     % codecVersion,
      "io.reactivex.rxjava2"                           % "rxjava"                            % rxjavaVersion,
      "javax.servlet"                                  % "javax.servlet-api"                 % servletVersion,
      "javax.transaction"                              % "jta"                               % jtaVersion,
      "junit"                                          % "junit"                             % junitVersion % "test",
      "net.bull.javamelody"                            % "javamelody-core"                   % javamelodyVersion,
      "org.apache.derby"                               % "derby"                             % derbyVersion, //          % "test",
      "org.apache.derby"                               % "derbyclient"                       % derbyVersion, //          % "test",
      "org.apache.derby"                               % "derbynet"                          % derbyVersion, //          % "test",
      "org.apache.derby"                               % "derbyshared"                       % derbyVersion,
      "org.apache.derby"                               % "derbytools"                        % derbyVersion,
      "org.eclipse.jetty"                              % "jetty-server"                      % jettyVersion % "compile;test",
      "org.eclipse.jetty"                              % "jetty-servlets"                    % jettyVersion % "compile;test",
      "org.eclipse.jetty"                              % "jetty-webapp"                      % jettyVersion % "compile;test",
      "org.eclipse.persistence"                        % "org.eclipse.persistence.extension" % eclipselinkVersion,
      "org.eclipse.persistence"                        % "org.eclipse.persistence.jpa"       % eclipselinkVersion,
      "org.fusesource.jansi"                           % "jansi"                             % jansiVersion % "runtime",
      "org.json4s"                                     %% "json4s-jackson"                   % jsonVersion,
      "org.mbari.uuid"                                 % "uuid-gen"                          % uuidgenVersion,
      "org.mbari.vcr4j"                                % "vcr4j-core"                        % vcr4jVersion,
      "org.postgresql"                                 % "postgresql"                        % postgresqlVersion,
      "org.scalatest"                                  %% "scalatest"                        % scalatestVersion % "test",
      "org.scalatra"                                   %% "scalatra-json"                    % scalatraVersion,
      "org.scalatra"                                   %% "scalatra-scalate"                 % scalatraVersion,
      "org.scalatra"                                   %% "scalatra-scalatest"               % scalatraVersion,
      "org.scalatra"                                   %% "scalatra"                         % scalatraVersion,
      "org.slf4j"                                      % "log4j-over-slf4j"                  % slf4jVersion,
      "scilube"                                        %% "scilube-core"                     % scilubeVersion,
      //"net.sourceforge.jtds"     % "jtds"                           % jtdsVersion,
      "org.slf4j"  % "slf4j-api" % slf4jVersion,
      "org.zeromq" % "jeromq"    % zeromqVersion
    ).map(
      _.excludeAll(
        ExclusionRule("org.slf4j", "slf4j-jdk14"),
        ExclusionRule("org.slf4j", "slf4j-log4j12"),
        ExclusionRule("javax.servlet", "servlet-api")
      )
    )
//    mainClass in assembly := Some("JettyMain")
  )
  .settings( // config sbt-pack
    packMain := apps,
    packExtraClasspath := apps
      .keys
      .map(k => k -> Seq("${PROG_HOME}/conf"))
      .toMap,
    packJvmOpts := apps
      .keys
      .map(k => k -> Seq("-Duser.timezone=UTC", "-Xmx4g"))
      .toMap,
    packDuplicateJarStrategy := "latest",
    packJarNameConvention := "original"
  )

// Aliases
addCommandAlias("cleanall", ";clean;clean-files")
