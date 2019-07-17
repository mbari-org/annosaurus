package org.mbari.vars.annotation

import com.typesafe.config.{Config, ConfigFactory}

case class HttpConfig(port: Int,
                      stopTimeout: Int,
                      connectorIdleTimeout: Int,
                      webapp: String,
                      contextPath: String)

class AppConfig(config: Config) {

  lazy val httpConfig: HttpConfig = {
    val port = config.getInt("http.port")
    val stopTimeout = config.getInt("http.stop.timeout")
    val connectorIdleTimeout = config.getInt("http.connector.idle.timeout")
    val webapp = config.getString("http.webapp")
    val contextPath = config.getString("http.context.path")
    HttpConfig(port, stopTimeout, connectorIdleTimeout, webapp, contextPath)
  }




}
