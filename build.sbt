import Dependencies._

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val buildSettings = Seq(
  organization := "org.mbari.vars",
  scalaVersion in ThisBuild := "2.13.12",
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

lazy val annosaurus = (project in file("annosaurus"))
  .enablePlugins(
    AutomateHeaderPlugin, 
    GitBranchPrompt, 
    GitVersioning,
    EclipseLinkStaticWeaver
    // JettyPlugin, 
    // PackPlugin
  )
  // .enablePlugins(EclipseLinkStaticWeaver)
  // .settings(staticWeaverLogLevel := 0)
  .settings(appSettings)
  .settings(
    // Set version based on git tag. I use "0.0.0" format (no leading "v", which is the default)
    // Use `show gitCurrentTags` in sbt to update/see the tags
    git.gitTagToVersionNumber := { tag: String =>
      if(tag matches "[0-9]+\\..*") Some(tag)
      else None
    },
    git.useGitDescribe := true,
    libraryDependencies ++= Seq(
      auth0,
      circeCore,
      circeGeneric,
      circeParser,
      commonsCodec,
      derby,
      derbyClient,
      derbyNet,
      derbyShared,
      derbyTools,
      eclipselinkExtension,
      eclipselinkJpa,
      fatboyGson,
      gson,
      h2 % Test,
      hikariCp,
      jansi % Runtime,
      javaxServlet,
      javaxTransaction,
      jettyServer,
      jettyServlets,
      jettyWebapp,
      jmelody,
      json4sJackson,
      junit % Test,
      logbackClassic,
      mssqlserver,
      oracle,
      postgresql,
      rxJava3,
      scalatest % Test,
      scalatra,
      scalatraJson,
      scalatraScalatest % Test,
      scilube,
      slf4j,
      slf4jJul,
      slf4jLog4j,
      typesafeConfig,
      uuidgen,
      vcr4jCore,
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

lazy val annosaurusIt = (project in file("annosaurus-it"))
  .dependsOn(annosaurus)
  .enablePlugins(
    AutomateHeaderPlugin
  )
  .settings(appSettings)
  .settings(
    libraryDependencies ++= Seq(
      junit % Test,
      slf4j
    )
  )

lazy val annosaurusItOracle = (project in file("annosaurus-it-oracle"))
  .dependsOn(annosaurusIt)
  .enablePlugins(
    AutomateHeaderPlugin
  )
  .settings(appSettings)
  .settings(
    libraryDependencies ++= Seq(
      junit % Test,
      scalatest % Test,
      testcontainersScalatest % Test
    )
  )

lazy val annosaurusItPostgres = (project in file("annosaurus-it-postgres"))
  .dependsOn(annosaurusIt)
  .enablePlugins(
    AutomateHeaderPlugin
  )
  .settings(appSettings)
  .settings(
    libraryDependencies ++= Seq(
      junit % Test,
      scalatest % Test,
      testcontainersPostgresql % Test,
      testcontainersScalatest % Test
    )
  )

lazy val annosaurusItSqlserver = (project in file("annosaurus-it-sqlserver"))
  .dependsOn(annosaurusIt)
  .enablePlugins(
    AutomateHeaderPlugin
  )
  .settings(appSettings)
  .settings(
    libraryDependencies ++= Seq(
      junit % Test,
      scalatest % Test,
      testcontainersScalatest % Test,
      testcontainersSqlserver % Test
    )
  )
