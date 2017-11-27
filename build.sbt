val akkaVersion = "2.4.7"
val auth0Version = "3.1.0"
val codecVersion = "1.10"
val configVersion = "1.3.1"
val derbyVersion = "10.13.1.1"
val eclipselinkVersion = "2.6.4"
val gsonJavatimeVersion = "1.1.1"
val gsonVersion = "2.8.0"
val h2Version = "1.4.193"
val jettyVersion = "9.4.1.v20170120"
val jsonVersion = "3.5.0"
val jtaVersion = "1.1"
val jtdsVersion = "1.3.1"
val junitVersion = "4.12"
val logbackVersion = "1.1.9"
val mssqlVersion = "6.1.0.jre8"
val scalatestVersion = "3.0.1"
val scalatraVersion = "2.5.0"
val servletVersion = "3.1.0"
val slf4jVersion = "1.7.22"
val vcr4jVersion = "3.0.1"


lazy val buildSettings = Seq(
  organization := "org.mbari.vars",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.12.2",
  crossScalaVersions := Seq("2.12.2"),
  organizationName := "Monterey Bay Aquarium Research Institute",
  startYear := Some(2017),
  licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
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
    "-encoding", "UTF-8", // yes, this is 2 args
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Xlint",
    "-Yno-adapted-args",
    "-Xfuture"),
  javacOptions ++= Seq("-target", "1.8", "-source", "1.8"),
  incOptions := incOptions.value.withNameHashing(true),
  updateOptions := updateOptions.value.withCachedResolution(true)
)

lazy val appSettings = buildSettings ++
  consoleSettings ++
  dependencySettings ++
  optionSettings ++ Seq(
  todosTags := Set("TODO", "FIXME", "WTF"),
  fork := true
)

lazy val apps = Seq("jetty-main")  // for sbt-pack

lazy val annosaurus = (project in file("."))
  .enablePlugins(JettyPlugin)
  .settings(appSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.auth0"                % "java-jwt"                       % auth0Version,
      "ch.qos.logback"           % "logback-classic"                % logbackVersion,
      "ch.qos.logback"           % "logback-core"                   % logbackVersion,
      "com.fatboyindustrial.gson-javatime-serialisers" % "gson-javatime-serialisers" % gsonJavatimeVersion,
      "com.google.code.gson"     % "gson"                           % gsonVersion,
      "com.h2database"           % "h2"                             % h2Version             % "test",
      "com.typesafe"             % "config"                         % configVersion,
      "commons-codec"            % "commons-codec"                  % codecVersion,
      "javax.servlet"            % "javax.servlet-api"              % servletVersion,
      "javax.transaction"        % "jta"                            % jtaVersion,
      "junit"                    % "junit"                          % junitVersion          % "test",
      "com.microsoft.sqlserver"  % "mssql-jdbc"                     % mssqlVersion,
      //"net.sourceforge.jtds"     % "jtds"                           % jtdsVersion,
      "org.apache.derby"         % "derby"                          % derbyVersion, //          % "test",
      "org.apache.derby"         % "derbyclient"                    % derbyVersion, //          % "test",
      "org.apache.derby"         % "derbynet"                       % derbyVersion, //          % "test",
      "org.eclipse.jetty"        % "jetty-server"                   % jettyVersion          % "compile;test",
      "org.eclipse.jetty"        % "jetty-servlets"                 % jettyVersion          % "compile;test",
      "org.eclipse.jetty"        % "jetty-webapp"                   % jettyVersion          % "compile;test",
      "org.eclipse.persistence"  % "eclipselink"                    % eclipselinkVersion,
      "org.json4s"              %% "json4s-jackson"                 % jsonVersion,
      "org.mbari.vcr4j"          % "vcr4j-core"                     % vcr4jVersion,
      "org.scalatest"           %% "scalatest"                      % scalatestVersion      % "test",
      "org.scalatra"            %% "scalatra"                       % scalatraVersion,
      "org.scalatra"            %% "scalatra-json"                  % scalatraVersion,
      "org.scalatra"            %% "scalatra-scalate"               % scalatraVersion,
      "org.scalatra"            %% "scalatra-scalatest"             % scalatraVersion,
      "org.scalatra"            %% "scalatra-slf4j"                 % scalatraVersion,
      "org.scalatra"            %% "scalatra-swagger"               % scalatraVersion,
      "org.scalatra"            %% "scalatra-swagger-ext"           % scalatraVersion,
      "org.slf4j"                % "log4j-over-slf4j"               % slf4jVersion,
      "org.slf4j"                % "slf4j-api"                      % slf4jVersion
    ).map(_.excludeAll(ExclusionRule("org.slf4j", "slf4j-jdk14"),
      ExclusionRule("org.slf4j", "slf4j-log4j12"),
      ExclusionRule("javax.servlet", "servlet-api"))),
    mainClass in assembly := Some("JettyMain")
  )
  .settings( // config sbt-pack
    packAutoSettings ++ Seq(
      packExtraClasspath := apps.map(_ -> Seq("${PROG_HOME}/conf")).toMap,
      packJvmOpts := apps.map(_ -> Seq("-Duser.timezone=UTC", "-Xmx4g")).toMap,
      packDuplicateJarStrategy := "latest",
      packJarNameConvention := "original"
    )
  )


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
