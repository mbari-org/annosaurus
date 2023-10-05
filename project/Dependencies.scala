import sbt._
object Dependencies {

  lazy val auth0 = "com.auth0" % "java-jwt" % "3.19.4"

  val circeVersion      = "0.14.6"
  lazy val circeCore    = "io.circe" %% "circe-core" % circeVersion
  lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  lazy val circeParser  = "io.circe" %% "circe-parser" % circeVersion

  lazy val commonsCodec = "commons-codec" % "commons-codec" % "1.16.0"

  val derbyVersion     = "10.16.1.1"
  lazy val derby       = "org.apache.derby" % "derby" % derbyVersion //          % "test"
  lazy val derbyClient = "org.apache.derby" % "derbyclient" % derbyVersion //          % "test"
  lazy val derbyNet    = "org.apache.derby" % "derbynet" % derbyVersion //          % "test"
  lazy val derbyShared = "org.apache.derby" % "derbyshared" % derbyVersion
  lazy val derbyTools  = "org.apache.derby" % "derbytools" % derbyVersion

  val eclipselinkVersion = "2.7.13"
  lazy val eclipselinkExtension =
    "org.eclipse.persistence" % "org.eclipse.persistence.extension" % eclipselinkVersion
  lazy val eclipselinkJpa =
    "org.eclipse.persistence" % "org.eclipse.persistence.jpa" % eclipselinkVersion

  lazy val fatboyGson =
    "com.fatboyindustrial.gson-javatime-serialisers" % "gson-javatime-serialisers" % "1.1.2"
  lazy val gson             = "com.google.code.gson" % "gson"              % "2.10.1"
  lazy val h2               = "com.h2database"       % "h2"                % "1.4.200"
  lazy val hikariCp         = "com.zaxxer"           % "HikariCP"          % "5.0.1"
  lazy val jansi            = "org.fusesource.jansi" % "jansi"             % "2.4.0"
  lazy val javaxServlet     = "javax.servlet"        % "javax.servlet-api" % "4.0.1"
  lazy val javaxTransaction = "javax.transaction"    % "jta"               % "1.1"

  val jettyVersion       = "9.4.52.v20230823"
  lazy val jettyServer   = "org.eclipse.jetty" % "jetty-server" % jettyVersion
  lazy val jettyServlets = "org.eclipse.jetty" % "jetty-servlets" % jettyVersion
  lazy val jettyWebapp   = "org.eclipse.jetty" % "jetty-webapp" % jettyVersion

  lazy val jmelody       = "net.bull.javamelody" % "javamelody-core" % "1.94.0"
  lazy val json4sJackson = "org.json4s"          %% "json4s-jackson" % "4.0.6"
  lazy val junit         = "junit"               % "junit"           % "4.13.2"

  val logbackVersion      = "1.4.11"
  lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackVersion
  lazy val logbackCore    = "ch.qos.logback" % "logback-core" % logbackVersion

  lazy val mssqlserver      = "com.microsoft.sqlserver" % "mssql-jdbc" % "9.4.0.jre11"
  lazy val oracle           = "com.oracle.ojdbc"        % "ojdbc8"     % "19.3.0.0"
  lazy val postgresql       = "org.postgresql"          % "postgresql" % "42.6.0"
  lazy val rxJava3          = "io.reactivex.rxjava3"    % "rxjava"     % "3.1.8"
  lazy val scalatest = "org.scalatest"           %% "scalatest" % "3.2.17"

  val scalatraVersion        = "3.0.0-M5-javax"
  lazy val scalatra          = "org.scalatra" %% "scalatra" % scalatraVersion
  lazy val scalatraJson      = "org.scalatra" %% "scalatra-json" % scalatraVersion
  lazy val scalatraScalatest = "org.scalatra" %% "scalatra-scalatest" % scalatraVersion

  lazy val scilube = "org.mbari.scilube" %% "scilube" % "3.0.1"

  val slf4jVersion    = "2.0.9"
  lazy val slf4j      = "org.slf4j" % "slf4j-api" % slf4jVersion
  lazy val slf4jJul   = "org.slf4j" % "jul-to-slf4j" % slf4jVersion
  lazy val slf4jLog4j = "org.slf4j" % "log4j-over-slf4j" % slf4jVersion

  val testcontainersScalaVersion = "0.41.0"
  lazy val testcontainersPostgresql =
    "com.dimafeng" %% "testcontainers-scala-postgresql" % testcontainersScalaVersion
  lazy val testcontainersScalatest =
    "com.dimafeng" %% "testcontainers-scala-scalatest" % testcontainersScalaVersion
  lazy val testcontainersSqlserver =
    "com.dimafeng" %% "testcontainers-scala-mssqlserver" % testcontainersScalaVersion

  lazy val typesafeConfig = "com.typesafe"    % "config"     % "1.4.2"
  lazy val uuidgen        = "org.mbari.uuid"  % "uuid-gen"   % "0.1.4"
  lazy val vcr4jCore      = "org.mbari.vcr4j" % "vcr4j-core" % "5.2.0"
  lazy val zeromq         = "org.zeromq"      % "jeromq"     % "0.5.3"

}
