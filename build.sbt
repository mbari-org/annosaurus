val akkaVersion         = "2.4.7"
val auth0Version        = "3.19.4"
val circeVersion        = "0.14.3"
val codecVersion        = "1.15"
val configVersion       = "1.4.2"
val derbyVersion        = "10.16.1.1"
val eclipselinkVersion  = "2.7.11" // 2.7.8
val gsonJavatimeVersion = "1.1.2"
val gsonVersion         = "2.10.1"
val h2Version           = "1.4.200"
val hikariVersion       = "5.0.1"
val jansiVersion        = "2.4.0"
val javamelodyVersion   = "1.91.0"
val jettyVersion        = "9.4.49.v20220914" // 9.4.35.v20201120
val jsonVersion         = "4.0.6"
val jtaVersion          = "1.1"
val jtdsVersion         = "1.3.1"
val junitVersion        = "4.13.2"
val logbackVersion      = "1.4.5"
val oracleVersion       = "19.3.0.0"
val postgresqlVersion   = "42.5.2"
val rxjavaVersion       = "3.1.6"
val scalatestVersion    = "3.2.15"
val scalatraVersion     = "2.8.4"
val scilubeVersion      = "3.0.1"
val servletVersion      = "4.0.1"
val slf4jVersion        = "2.0.6"
val sqlserverVersion    = "9.4.0.jre11"
val testcontainersScalaVersion = "0.40.12"
val uuidgenVersion      = "0.1.4"
val vcr4jVersion        = "4.4.1.jre11"
val zeromqVersion       = "0.5.3"

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val buildSettings = Seq(
  organization := "org.mbari.vars",
  scalaVersion in ThisBuild := "2.13.8",
  organizationName := "Monterey Bay Aquarium Research Institute",
  startYear := Some(2017),
  licenses += ("Apache-2.0", new URL(
    "https://www.apache.org/licenses/LICENSE-2.0.txt"
  ))
)


lazy val dependencySettings = Seq(
  resolvers ++= Seq(
    Resolver.githubPackages("mbari-org", "maven")
  )
)

lazy val optionSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-encoding",
    "UTF-8",         // yes, this is 2 args. Specify character encoding used by source files.
    "-explaintypes", // Explain type errors in more detail.
    "-feature",      // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Xlint",
    "-Yrangepos",              // required by SemanticDB compiler plugin
    "-Ywarn-dead-code",        // Warn when dead code is identified.
    "-Ywarn-extra-implicit",   // Warn when more than one implicit parameter section is defined.
    "-Ywarn-numeric-widen",    // Warn when numerics are widened.
    "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports",   // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals",    // Warn if a local definition is unused.
    "-Ywarn-unused:params",    // Warn if a value parameter is unused.
    "-Ywarn-unused:patvars",   // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates",  // Warn if a private member is unused.
    "-Ywarn-value-discard"     // Warn when non-Unit expression results are unused.
  ),
  javacOptions ++= Seq("-target", "17", "-source", "17"),
  updateOptions := updateOptions.value.withCachedResolution(true)
)

lazy val appSettings = buildSettings ++
  dependencySettings ++
  optionSettings ++ Seq(
  fork := true
)

lazy val apps = Map("jetty-main" -> "JettyMain") // for sbt-pack

lazy val annosaurus = (project in file("."))
  .enablePlugins(
    AutomateHeaderPlugin, 
    GitBranchPrompt, 
    GitVersioning,
    JettyPlugin, 
    PackPlugin
  )
  // .enablePlugins(EclipseLinkStaticWeaver)
  // .settings(staticWeaverLogLevel := 0)
  .configs(IntegrationTest)
  .settings(appSettings)
  .settings(
    Defaults.itSettings, // for integration tests
    // Set version based on git tag. I use "0.0.0" format (no leading "v", which is the default)
    // Use `show gitCurrentTags` in sbt to update/see the tags
    git.gitTagToVersionNumber := { tag: String =>
      if(tag matches "[0-9]+\\..*") Some(tag)
      else None
    },
    git.useGitDescribe := true,
    libraryDependencies ++= Seq(
      "ch.qos.logback"                                 % "logback-classic"                   % logbackVersion,
      "ch.qos.logback"                                 % "logback-core"                      % logbackVersion,
      "com.auth0"                                      % "java-jwt"                          % auth0Version,
      "com.dimafeng" %% "testcontainers-scala-scalatest" % testcontainersScalaVersion % "it",
      "com.dimafeng" %% "testcontainers-scala-postgresql" % testcontainersScalaVersion % "it",
      "com.dimafeng" %% "testcontainers-scala-mssqlserver" % testcontainersScalaVersion % "it",
      "com.fatboyindustrial.gson-javatime-serialisers" % "gson-javatime-serialisers"         % gsonJavatimeVersion,
      "com.google.code.gson"                           % "gson"                              % gsonVersion,
      "com.h2database"                                 % "h2"                                % h2Version % "test",
      "com.microsoft.sqlserver"                        % "mssql-jdbc"                        % sqlserverVersion,
      "com.oracle.ojdbc"                               % "ojdbc8"                            % oracleVersion,
      "com.typesafe"                                   % "config"                            % configVersion,
      "com.zaxxer"                                     % "HikariCP"                          % hikariVersion,
      "commons-codec"                                  % "commons-codec"                     % codecVersion,
      "io.circe"                                       %% "circe-core"                       % circeVersion,
      "io.circe"                                       %% "circe-generic"                    % circeVersion,
      "io.circe"                                       %% "circe-parser"                     % circeVersion,
      "io.reactivex.rxjava3"                           % "rxjava"                            % rxjavaVersion,
      "javax.servlet"                                  % "javax.servlet-api"                 % servletVersion,
      "javax.transaction"                              % "jta"                               % jtaVersion,
      "junit"                                          % "junit"                             % junitVersion % "it,test",
      "net.bull.javamelody"                            % "javamelody-core"                   % javamelodyVersion,
      "org.apache.derby"                               % "derby"                             % derbyVersion, //          % "test",
      "org.apache.derby"                               % "derbyclient"                       % derbyVersion, //          % "test",
      "org.apache.derby"                               % "derbynet"                          % derbyVersion, //          % "test",
      "org.apache.derby"                               % "derbyshared"                       % derbyVersion,
      "org.apache.derby"                               % "derbytools"                        % derbyVersion,
      "org.eclipse.jetty"                              % "jetty-server"                      % jettyVersion % "compile,it,test",
      "org.eclipse.jetty"                              % "jetty-servlets"                    % jettyVersion % "compile,it,test",
      "org.eclipse.jetty"                              % "jetty-webapp"                      % jettyVersion % "compile,it,test",
      "org.eclipse.persistence"                        % "org.eclipse.persistence.extension" % eclipselinkVersion,
      "org.eclipse.persistence"                        % "org.eclipse.persistence.jpa"       % eclipselinkVersion,
      "org.fusesource.jansi"                           % "jansi"                             % jansiVersion % "runtime",
      ("org.json4s"                                     %% "json4s-jackson"                   % jsonVersion).cross(CrossVersion.for3Use2_13),
      "org.mbari.uuid"                                 % "uuid-gen"                          % uuidgenVersion,
      "org.mbari.vcr4j"                                % "vcr4j-core"                        % vcr4jVersion,
      "org.postgresql"                                 % "postgresql"                        % postgresqlVersion,
      ("org.scalatest"                                  %% "scalatest"                        % scalatestVersion).cross(CrossVersion.for3Use2_13) % "it,test",
      ("org.scalatra"                                   %% "scalatra-json"                    % scalatraVersion).cross(CrossVersion.for3Use2_13),
      ("org.scalatra"                                   %% "scalatra-scalate"                 % scalatraVersion).cross(CrossVersion.for3Use2_13),
      ("org.scalatra"                                   %% "scalatra-scalatest"               % scalatraVersion).cross(CrossVersion.for3Use2_13),
      ("org.scalatra"                                   %% "scalatra"                         % scalatraVersion).cross(CrossVersion.for3Use2_13),
      "org.slf4j"                                      % "log4j-over-slf4j"                  % slf4jVersion,
      "org.slf4j"                                      % "jul-to-slf4j"                  % slf4jVersion,
      "org.mbari.scilube"                              %% "scilube"                          % scilubeVersion,
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
