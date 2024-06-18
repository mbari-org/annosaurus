import sbt._
object Dependencies {

    lazy val auth0 = "com.auth0" % "java-jwt" % "4.4.0"

    val circeVersion      = "0.14.8"
    lazy val circeCore    = "io.circe" %% "circe-core"    % circeVersion
    lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
    lazy val circeParser  = "io.circe" %% "circe-parser"  % circeVersion

    lazy val commonsCodec = "commons-codec" % "commons-codec" % "1.17.0"

    val derbyVersion     = "10.17.1.0"
    lazy val derby       = "org.apache.derby" % "derby"       % derbyVersion //          % "test"
    lazy val derbyClient = "org.apache.derby" % "derbyclient" % derbyVersion //          % "test"
    lazy val derbyNet    = "org.apache.derby" % "derbynet"    % derbyVersion //          % "test"
    lazy val derbyShared = "org.apache.derby" % "derbyshared" % derbyVersion
    lazy val derbyTools  = "org.apache.derby" % "derbytools"  % derbyVersion

    // val eclipselinkVersion        = "4.0.2"
    // lazy val eclipselinkExtension = "org.eclipse.persistence" % "org.eclipse.persistence.extension" % eclipselinkVersion
    // lazy val eclipselinkJpa       = "org.eclipse.persistence" % "org.eclipse.persistence.jpa" % eclipselinkVersion

    lazy val fatboyGson       = "com.fatboyindustrial.gson-javatime-serialisers" % "gson-javatime-serialisers" % "1.1.2"
    lazy val gson             = "com.google.code.gson" % "gson"              % "2.10.1"
    lazy val h2               = "com.h2database"       % "h2"                % "2.2.224"

    val hibernateVersion     = "6.5.2.Final"  //"6.4.8.Final" //"6.5.1.Final" - envers is throwing: The column name REV is not valid. 
    lazy val hibernateCore   = "org.hibernate.orm" % "hibernate-core"     % hibernateVersion
    lazy val hibernateEnvers = "org.hibernate.orm" % "hibernate-envers"   % hibernateVersion
    lazy val hibernateHikari = "org.hibernate.orm" % "hibernate-hikaricp" % hibernateVersion

    lazy val hikariCp         = "com.zaxxer"           % "HikariCP"          % "5.1.0"
    lazy val jansi            = "org.fusesource.jansi" % "jansi"             % "2.4.1"
    lazy val javaxServlet     = "javax.servlet"        % "javax.servlet-api" % "4.0.1"
    lazy val javaxTransaction = "javax.transaction"    % "jta"               % "1.1"

    // val jettyVersion       = "12.0.2"
    // lazy val jettyServer   = "org.eclipse.jetty" % "jetty-server" % jettyVersion
    // lazy val jettyServlets = "org.eclipse.jetty.ee10" % "jetty-ee10-servlets" % jettyVersion
    // lazy val jettyWebapp   = "org.eclipse.jetty.ee10" % "jetty-ee10-webapp" % jettyVersion

    val jettyVersion       = "11.0.19"
    lazy val jettyServer   = "org.eclipse.jetty" % "jetty-server"   % jettyVersion
    lazy val jettyServlets = "org.eclipse.jetty" % "jetty-servlets" % jettyVersion
    lazy val jettyWebapp   = "org.eclipse.jetty" % "jetty-webapp"   % jettyVersion

    lazy val jmelody       = "net.bull.javamelody" % "javamelody-core" % "2.0.1"
    lazy val json4sJackson = "org.json4s"         %% "json4s-jackson"  % "4.0.7"
    lazy val junit         = "junit"               % "junit"           % "4.13.2"

    val logbackVersion      = "1.5.6"
    lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackVersion
    lazy val logbackCore    = "ch.qos.logback" % "logback-core"    % logbackVersion

    lazy val mssqlserver = "com.microsoft.sqlserver" % "mssql-jdbc" % "12.6.2.jre11"
    lazy val munit       = "org.scalameta"          %% "munit"      % "1.0.0"
    lazy val oracle      = "com.oracle.ojdbc"        % "ojdbc8"     % "19.3.0.0"
    lazy val postgresql  = "org.postgresql"          % "postgresql" % "42.7.3"
    lazy val rxJava3     = "io.reactivex.rxjava3"    % "rxjava"     % "3.1.8"
    lazy val scalatest   = "org.scalatest"          %% "scalatest"  % "3.2.18"

    val scalatraVersion        = "3.1.0"
    lazy val scalatra          = "org.scalatra" %% "scalatra-jakarta"           % scalatraVersion
    lazy val scalatraJson      = "org.scalatra" %% "scalatra-json-jakarta"      % scalatraVersion
    lazy val scalatraScalatest = "org.scalatra" %% "scalatra-scalatest-jakarta" % scalatraVersion

    lazy val scilube = "org.mbari.scilube" %% "scilube" % "3.0.1"

    val slf4jVersion    = "2.0.13"
    lazy val slf4j      = "org.slf4j" % "slf4j-api"        % slf4jVersion
    lazy val slf4jJul   = "org.slf4j" % "jul-to-slf4j"     % slf4jVersion
    lazy val slf4jLog4j = "org.slf4j" % "log4j-over-slf4j" % slf4jVersion
    lazy val slf4jSystem = "org.slf4j" % "slf4j-jdk-platform-logging" % slf4jVersion

    private val tapirVersion = "1.10.9"
    lazy val tapirCirce      = "com.softwaremill.sttp.tapir"   %% "tapir-json-circe"         % tapirVersion
    lazy val tapirHelidon    = "com.softwaremill.sttp.tapir"   %% "tapir-nima-server"        % tapirVersion
    lazy val tapirPrometheus = "com.softwaremill.sttp.tapir"   %% "tapir-prometheus-metrics" % tapirVersion
    lazy val tapirServerStub = "com.softwaremill.sttp.tapir"   %% "tapir-sttp-stub-server"   % tapirVersion
    lazy val tapirSwagger    = "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui-bundle"  % tapirVersion
    lazy val tapirVertex     = "com.softwaremill.sttp.tapir"   %% "tapir-vertx-server"       % tapirVersion

    lazy val tapirSttpCirce  = "com.softwaremill.sttp.client3" %% "circe"                    % "3.9.7"

    // val testcontainersScalaVersion = "0.41.0"
    // lazy val testcontainersPostgresql =
    //   "com.dimafeng" %% "testcontainers-scala-postgresql" % testcontainersScalaVersion
    // lazy val testcontainersScalatest =
    //   "com.dimafeng" %% "testcontainers-scala-scalatest" % testcontainersScalaVersion
    // lazy val testcontainersMunit =
    //   "com.dimafeng" %% "testcontainers-scala-munit" % testcontainersScalaVersion
    // lazy val testcontainersSqlserver =
    //   "com.dimafeng" %% "testcontainers-scala-mssqlserver" % testcontainersScalaVersion

    val testcontainersVersion        = "1.19.8"
    lazy val testcontainersCore      = "org.testcontainers" % "testcontainers" % testcontainersVersion
    lazy val testcontainersSqlserver = "org.testcontainers" % "mssqlserver"    % testcontainersVersion
    lazy val testcontainersOracle    = "org.testcontainers" % "oracle-xe"      % testcontainersVersion
    lazy val testcontainersPostgres  = "org.testcontainers" % "postgresql"     % testcontainersVersion

    lazy val typesafeConfig = "com.typesafe"    % "config"     % "1.4.3"
    lazy val uuidgen        = "org.mbari.uuid"  % "uuid-gen"   % "0.1.4"
    lazy val vcr4jCore      = "org.mbari.vcr4j" % "vcr4j-core" % "5.2.0"
    lazy val zeromq         = "org.zeromq"      % "jeromq"     % "0.6.0"

}
