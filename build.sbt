import Dependencies._

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val buildSettings = Seq(
  organization := "org.mbari.vars",
  scalaVersion in ThisBuild := "3.3.1",
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
    // JettyPlugin, 
    // PackPlugin
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
      logbackClassic,
      auth0,
      testcontainersScalatest,
      testcontainersPostgresql,
      testcontainersSqlserver,
      fatboyGson,
      gson,
      h2 % Test,
      mssqlserver,
      oracle,
      typesafeConfig,
      hikariCp,
      commonsCodec,
      circeCore,
      circeGeneric,
      circeParser,
      rxJava3,
      javaxServlet,
      javaxTransaction,
      junit % "it,test",
      jmelody,
      derbyClient,
      derby,
      derbyNet,
      derbyShared,
      derbyTools,
      jettyServer,
      jettyServlets,
      jettyWebapp,
      eclipselinkExtension,
      eclipselinkJpa,
      jansi % "runtime",
      json4sJackson,
      uuidgen,
      vcr4jCore,
      postgresql,
      scalatest % "it,test",
      scalatraJson,
      scalatraScalatest % "it,test",
      scalatra,
      slf4jJul,
      slf4jLog4j,
      slf4j,
      scilube,
      zeromq
    ).map(
      _.excludeAll(
        ExclusionRule("org.slf4j", "slf4j-jdk14"),
        ExclusionRule("org.slf4j", "slf4j-log4j12"),
        ExclusionRule("javax.servlet", "servlet-api")
      )
    )
//    mainClass in assembly := Some("JettyMain")
  )
  // .settings( // config sbt-pack
  //   packMain := apps,
  //   packExtraClasspath := apps
  //     .keys
  //     .map(k => k -> Seq("${PROG_HOME}/conf"))
  //     .toMap,
  //   packJvmOpts := apps
  //     .keys
  //     .map(k => k -> Seq("-Duser.timezone=UTC", "-Xmx4g"))
  //     .toMap,
  //   packDuplicateJarStrategy := "latest",
  //   packJarNameConvention := "original"
  // )

// Aliases
addCommandAlias("cleanall", ";clean;clean-files")
