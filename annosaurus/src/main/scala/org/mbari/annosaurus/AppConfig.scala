/*
 * Copyright 2017 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.annosaurus

import com.typesafe.config.ConfigFactory
import org.mbari.annosaurus.etc.jdk.Logging
import org.mbari.annosaurus.etc.jdk.Logging.{*, given}
import org.mbari.annosaurus.etc.jwt.JwtService

import scala.util.Try
import scala.util.control.NonFatal

object AppConfig:

    private val log = Logging(getClass)

    val Name: String = "annosaurus"

    val Version: String =
        val v = Try(getClass.getPackage.getImplementationVersion).getOrElse("0.0.0")
        if v == null then "0.0.0" else v

    val Description: String = "Annotation Service"

    /** We should have the same # of max db connections as vertx workers */
    val NumberOfVertxWorkers: Int = 20

    private lazy val Config = ConfigFactory.load()

    lazy val DefaultJwtService: JwtService = JwtService(
        issuer = Config.getString("basicjwt.issuer"),
        apiKey = Config.getString("basicjwt.client.secret"),
        signingSecret = Config.getString("basicjwt.signing.secret")
    )

    lazy val DefaultHttpConfig: HttpConfig = HttpConfig(
        port = Config.getInt("http.port"),
        stopTimeout = Config.getInt("http.stop.timeout"),
        connectorIdleTimeout = Config.getInt("http.connector.idle.timeout"),
        contextPath = Config.getString("http.context.path")
    )

    lazy val DefaultZeroMQConfig: Option[ZeroMQConfig] =
        try
            val port   = Config.getInt("messaging.zeromq.port")
            val enable = Config.getBoolean("messaging.zeromq.enable")
            val topic  = Config.getString("messaging.zeromq.topic")
            Some(ZeroMQConfig(port, enable, topic))
        catch
            case NonFatal(e) =>
                log.atWarn.withCause(e).log("Failed to load ZeroMQ configuration")
                None

    lazy val DefaultDatabaseConfig: DatabaseConfig = DatabaseConfig(
        url = Config.getString("database.url"),
        user = Config.getString("database.user"),
        password = Config.getString("database.password"),
        driver = Config.getString("database.driver"),
        queryView = Config.getString("database.query.view")
    )

case class HttpConfig(
    port: Int,
    stopTimeout: Int,
    connectorIdleTimeout: Int,
    contextPath: String
)

case class DatabaseConfig(
    url: String,
    user: String,
    password: String,
    driver: String,
    queryView: String
):
    def newConnection(): java.sql.Connection =
        Class.forName(driver)
        java.sql.DriverManager.getConnection(url, user, password)
