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

package org.mbari.vars.annotation

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import scala.util.Try
import scala.util.control.NonFatal

case class HttpConfig(
    port: Int,
    stopTimeout: Int,
    connectorIdleTimeout: Int,
    webapp: String,
    contextPath: String
)

case class BasicJwtConfig(issuer: String, clientSecret: String, signingSecret: String)

case class ZeroMQConfig(port: Int, enable: Boolean, topic: String)

class AppConfig(config: Config) {

  private[this] val log = LoggerFactory.getLogger(getClass)

  lazy val httpConfig: HttpConfig = {
    val port                 = config.getInt("http.port")
    val stopTimeout          = config.getInt("http.stop.timeout")
    val connectorIdleTimeout = config.getInt("http.connector.idle.timeout")
    val webapp               = config.getString("http.webapp")
    val contextPath          = config.getString("http.context.path")
    HttpConfig(port, stopTimeout, connectorIdleTimeout, webapp, contextPath)
  }

  lazy val authenticationService: String =
    Try(config.getString("authentication.service"))
      .getOrElse("org.mbari.vars.annotation.auth.NoopAuthService")

  lazy val basicJwtConfig: Option[BasicJwtConfig] =
    try {
      val issuer        = config.getString("basicjwt.issuer")
      val clientSecret  = config.getString("basicjwt.client.secret")
      val signingSecret = config.getString("basicjwt.signing.secret")
      Some(BasicJwtConfig(issuer, clientSecret, signingSecret))
    }
    catch {
      case NonFatal(e) => None
    }

  lazy val zeroMQConfig: Option[ZeroMQConfig] =
    try {
      val port   = config.getInt("messaging.zeromq.port")
      val enable = config.getBoolean("messaging.zeromq.enable")
      val topic  = config.getString("messaging.zeromq.topic")
      Some(ZeroMQConfig(port, enable, topic))
    }
    catch {
      case NonFatal(e) =>
        log.warn("Failed to load ZeroMQ configuration", e)
        None
    }

}
