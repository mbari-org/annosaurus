import Dependencies.*

onChangedBuildSource := ReloadOnSourceChanges

// ThisBuild / javacOptions ++= Seq("-target", "21", "-source", "21")
javacOptions ++= Seq("--release", "21")
licenses         := Seq("Apache-2.0" -> uri("http://www.apache.org/licenses/LICENSE-2.0"))
organization     := "org.mbari"
organizationName := "Monterey Bay Aquarium Research Institute"
scalaVersion     := "3.10.0"
// scalaVersion     := "3.3.1" // Fails. See https://github.com/lampepfl/dotty/issues/17069#issuecomment-1763053572
scalacOptions ++= Seq(
    "-deprecation",  // Emit warning and location for usages of deprecated APIs.
    "-encoding",
    "UTF-8",         // yes, this is 2 args. Specify character encoding used by source files.
    "-explaintypes", // Explain type errors in more detail.
    "-feature",      // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Wunused:imports"
)

semanticdbEnabled := true
semanticdbVersion := scalafixSemanticdb.revision

startYear        := Some(2017)
//updateOptions    := updateOptions.value.withCachedResolution(true)
versionScheme    := Some("semver-spec")

Test / fork              := true
Test / parallelExecution := false
Test / testOptions ++= Seq(
    Tests.Argument(TestFrameworks.MUnit, "-b"),
    Tests.Argument(TestFrameworks.MUnit, "--log=debug")
)
Test / javaOptions ++= Seq(
    "-Duser.timezone=UTC"
)

lazy val annosaurus = (project in file("annosaurus"))
    .enablePlugins(
        AutomateHeaderPlugin,
        GitBranchPrompt,
        GitVersioning,
        JavaAppPackaging
    )
    // .settings(staticWeaverLogLevel := 0)
    .settings(
        // sbt 2 moves output to target/out/jvm/scala-<version>/<project>/. Keep `sbt stage`
        // writing to annosaurus/target/universal/stage, which the Dockerfile and docs expect.
        Universal / UniversalPlugin.autoImport.stagingDirectory := baseDirectory.value / "target" / "universal" / "stage",
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
        // Set version based on git tag. I use "0.0.0" format (no leading "v", which is the default)
        // Use `show gitCurrentTags` in sbt to update/see the tags
        git.gitTagToVersionNumber := { (gitTag: String) =>
            if (gitTag.matches("[0-9]+\\..*")) Some(gitTag)
            else None
        },
        git.useGitDescribe        := true,
        libraryDependencies ++= Seq(
            auth0,
            circeCore,
            circeGeneric,
            circeParser,
            commonsCodec,
            flywayCore,
            flywayPostgres,
            flywaySqlserver,
            hibernateCore,
            hibernateEnvers,
            hibernateHikari,
            hikariCp,
            jansi             % Runtime,
            javaxServlet,
            javaxTransaction,
            jnats,
            junit             % Test,
            logbackClassic,
            mssqlserver,
            munit             % Test,
            oracle,
            postgresql,
            rxJava3,
            scommons,
            slf4j,
            slf4jJul,
            slf4jLog4j,
            slf4jSystem,
            tapirCirce,
            tapirPrometheus,
            tapirServerStub       % Test,
            testcontainersCore    % Test,
            tapirSttpCirce,
            tapirSwagger,
            tapirVertex,
            typesafeConfig,
            // uuidgen,
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
            junit,
            munit,
            slf4j,
            tapirServerStub,
            testcontainersCore
        )
    )

lazy val itPostgres = (project in file("it-postgres"))
  .dependsOn(integrationTests)
  .enablePlugins(
    AutomateHeaderPlugin
  )
  .settings(
    libraryDependencies ++= Seq(
      testcontainersPostgres
    )
  )

lazy val itSqlserver = (project in file("it-sqlserver"))
  .dependsOn(integrationTests)
  .enablePlugins(
    AutomateHeaderPlugin
  )
  .settings(
    libraryDependencies ++= Seq(
      testcontainersSqlserver
    )
  )
