import sbt._
object Dependencies {

    lazy val auth0 = "com.auth0" % "java-jwt" % "4.5.0"

    val circeVersion      = "0.14.13"
    lazy val circeCore    = "io.circe" %% "circe-core"    % circeVersion
    lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
    lazy val circeParser  = "io.circe" %% "circe-parser"  % circeVersion

    lazy val commonsCodec = "commons-codec" % "commons-codec" % "1.18.0"


    val hibernateVersion     = "6.6.15.Final"  //"6.4.8.Final" //"6.5.1.Final" - envers is throwing: The column name REV is not valid.
    lazy val hibernateCore   = "org.hibernate.orm" % "hibernate-core"     % hibernateVersion
    lazy val hibernateEnvers = "org.hibernate.orm" % "hibernate-envers"   % hibernateVersion
    lazy val hibernateHikari = "org.hibernate.orm" % "hibernate-hikaricp" % hibernateVersion

    lazy val hikariCp         = "com.zaxxer"           % "HikariCP"          % "6.3.0"
    lazy val jansi            = "org.fusesource.jansi" % "jansi"             % "2.4.1"
    lazy val javaxServlet     = "javax.servlet"        % "javax.servlet-api" % "4.0.1"
    lazy val javaxTransaction = "javax.transaction"    % "jta"               % "1.1"

    lazy val junit         = "junit"               % "junit"           % "4.13.2"
    lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.5.16"

    lazy val mssqlserver = "com.microsoft.sqlserver" % "mssql-jdbc" % "12.10.0.jre11"
    lazy val munit       = "org.scalameta"          %% "munit"      % "1.1.1"
    lazy val oracle      = "com.oracle.ojdbc"        % "ojdbc8"     % "19.3.0.0"
    lazy val postgresql  = "org.postgresql"          % "postgresql" % "42.7.5"
    lazy val rxJava3     = "io.reactivex.rxjava3"    % "rxjava"     % "3.1.10"

    // lazy val scilube = "org.mbari.scilube" %% "scilube" % "3.0.1"
    lazy val scommons = "org.mbari.commons" %% "scommons" % "0.0.7"

    val slf4jVersion    = "2.0.17"
    lazy val slf4j      = "org.slf4j" % "slf4j-api"        % slf4jVersion
    lazy val slf4jJul   = "org.slf4j" % "jul-to-slf4j"     % slf4jVersion
    lazy val slf4jLog4j = "org.slf4j" % "log4j-over-slf4j" % slf4jVersion
    lazy val slf4jSystem = "org.slf4j" % "slf4j-jdk-platform-logging" % slf4jVersion

    private val tapirVersion = "1.11.29"
    lazy val tapirCirce      = "com.softwaremill.sttp.tapir"   %% "tapir-json-circe"         % tapirVersion
    lazy val tapirHelidon    = "com.softwaremill.sttp.tapir"   %% "tapir-nima-server"        % tapirVersion
    lazy val tapirPrometheus = "com.softwaremill.sttp.tapir"   %% "tapir-prometheus-metrics" % tapirVersion
    lazy val tapirServerStub = "com.softwaremill.sttp.tapir"   %% "tapir-sttp-stub-server"   % tapirVersion
    lazy val tapirSwagger    = "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui-bundle"  % tapirVersion
    lazy val tapirVertex     = "com.softwaremill.sttp.tapir"   %% "tapir-vertx-server"       % tapirVersion

    lazy val tapirSttpCirce  = "com.softwaremill.sttp.client3" %% "circe"                    % "3.11.0"

    // val testcontainersScalaVersion = "0.41.0"
    // lazy val testcontainersPostgresql =
    //   "com.dimafeng" %% "testcontainers-scala-postgresql" % testcontainersScalaVersion
    // lazy val testcontainersScalatest =
    //   "com.dimafeng" %% "testcontainers-scala-scalatest" % testcontainersScalaVersion
    // lazy val testcontainersMunit =
    //   "com.dimafeng" %% "testcontainers-scala-munit" % testcontainersScalaVersion
    // lazy val testcontainersSqlserver =
    //   "com.dimafeng" %% "testcontainers-scala-mssqlserver" % testcontainersScalaVersion

    val testcontainersVersion        = "1.21.0"
    lazy val testcontainersCore      = "org.testcontainers" % "testcontainers" % testcontainersVersion
    lazy val testcontainersSqlserver = "org.testcontainers" % "mssqlserver"    % testcontainersVersion
    lazy val testcontainersOracle    = "org.testcontainers" % "oracle-xe"      % testcontainersVersion
    lazy val testcontainersPostgres  = "org.testcontainers" % "postgresql"     % testcontainersVersion

    lazy val typesafeConfig = "com.typesafe"    % "config"     % "1.4.3"
    lazy val vcr4jCore      = "org.mbari.vcr4j" % "vcr4j-core" % "5.3.1"
    lazy val zeromq         = "org.zeromq"      % "jeromq"     % "0.6.0"

}
