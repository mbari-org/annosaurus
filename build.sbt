import Dependencies._

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / javacOptions ++= Seq("-target", "21", "-source", "21")
ThisBuild / licenses         := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / organization     := "org.mbari.vars"
ThisBuild / organizationName := "Monterey Bay Aquarium Research Institute"
ThisBuild / resolvers ++= Seq(Resolver.githubPackages("mbari-org", "maven"))
ThisBuild / scalaVersion     := "2.13.12"
ThisBuild / scalacOptions ++= Seq(
  "-deprecation",  // Emit warning and location for usages of deprecated APIs.
  "-encoding",
  "UTF-8",         // yes, this is 2 args. Specify character encoding used by source files.
  "-explaintypes", // Explain type errors in more detail.
  "-feature",      // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked"
)
ThisBuild / startYear        := Some(2017)
ThisBuild / updateOptions := updateOptions.value.withCachedResolution(true)
ThisBuild / versionScheme    := Some("semver-spec")

ThisBuild / Test / fork := true
ThisBuild / Test / parallelExecution     := false
ThisBuild / Test / testOptions += Tests.Argument(TestFrameworks.MUnit, "-b")


lazy val annosaurus = (project in file("annosaurus"))
  .enablePlugins(
    AutomateHeaderPlugin,
    GitBranchPrompt,
    GitVersioning,
    EclipseLinkStaticWeaver,
    JavaAppPackaging
  )
  // .settings(staticWeaverLogLevel := 0)
  .settings(
    // Set version based on git tag. I use "0.0.0" format (no leading "v", which is the default)
    // Use `show gitCurrentTags` in sbt to update/see the tags
    // https://stackoverflow.com/questions/22772812/using-sbt-native-packager-how-can-i-simply-prepend-a-directory-to-my-bash-scrip
    bashScriptExtraDefines ++= Seq(
      """addJava "-Dconfig.file=${app_home}/../conf/application.conf"""",
      """addJava "-Dlogback.configurationFile=${app_home}/../conf/logback.xml""""
    ),
    batScriptExtraDefines ++= Seq(
      """call :add_java "-Dconfig.file=%APP_HOME%\conf\application.conf"""",
      """call :add_java "-Dlogback.configurationFile=%APP_HOME%\conf\logback.xml""""
    ),
    git.gitTagToVersionNumber := { tag: String =>
      if (tag matches "[0-9]+\\..*") Some(tag)
      else None
    },
    git.useGitDescribe        := true,
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
      h2                % Test,
      hikariCp,
      jansi             % Runtime,
      javaxServlet,
      javaxTransaction,
      jettyServer,
      jettyServlets,
      jettyWebapp,
      jmelody,
      json4sJackson,
      junit             % Test,
      logbackClassic,
      mssqlserver,
      munit             % Test,
      oracle,
      postgresql,
      rxJava3,
      scalatest         % Test,
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


// Aliases
addCommandAlias("cleanall", ";clean;clean-files")

lazy val integrationTests = (project in file("it"))
  .dependsOn(annosaurus)
  .enablePlugins(
    AutomateHeaderPlugin
  )
  .settings(
    libraryDependencies ++= Seq(
      junit % Test,
      slf4j
    )
  )

lazy val itOracle = (project in file("it-oracle"))
  .dependsOn(annosaurusIt)
  .enablePlugins(
    AutomateHeaderPlugin
  )
  .settings(
    libraryDependencies ++= Seq(
      junit                   % Test,
      munit                   % Test,
      scalatest               % Test,
      testcontainersMunit     % Test,
      testcontainersScalatest % Test
    )
  )

lazy val itPostgres = (project in file("it-postgres"))
  .dependsOn(annosaurusIt)
  .enablePlugins(
    AutomateHeaderPlugin
  )
  .settings(
    libraryDependencies ++= Seq(
      junit                    % Test,
      munit                    % Test,
      scalatest                % Test,
      testcontainersMunit      % Test,
      testcontainersPostgresql % Test,
      testcontainersScalatest  % Test
    )
  )

lazy val itSqlserver = (project in file("it-sqlserver"))
  .dependsOn(annosaurusIt)
  .enablePlugins(
    AutomateHeaderPlugin
  )
  .settings(
    libraryDependencies ++= Seq(
      junit                   % Test,
      munit                   % Test,
      scalatest               % Test,
      testcontainersMunit     % Test,
      testcontainersScalatest % Test,
      testcontainersSqlserver % Test
    )
  )

