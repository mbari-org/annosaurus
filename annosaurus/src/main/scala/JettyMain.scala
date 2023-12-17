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

import org.slf4j.LoggerFactory
import org.eclipse.jetty.servlet.DefaultServlet
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

class JettyMain {}
import javax.servlet.DispatcherType
// import net.bull.javamelody.{MonitoringFilter, Parameter, ReportServlet, SessionListener}
import org.eclipse.jetty.server._
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.webapp.WebAppContext
// import org.eclipse.jetty.ee10.servlet.FilterHolder  // Jetty 12
// import org.eclipse.jetty.ee10.webapp.WebAppContext  // Jetty 12
import org.mbari.annosaurus.Constants
import org.mbari.annosaurus.etc.zeromq.ZeroMQPublisher
import org.scalatra.servlet.ScalatraListener
import org.slf4j.bridge.SLF4JBridgeHandler
import java.util.{List => JList}

object JettyMain {

    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()

    // hold on to messaging objects so they don't get GC'd
    private[this] val zmq = ZeroMQPublisher.autowire(Constants.AppConfig.zeroMQConfig)

    def main(args: Array[String]): Unit = {
        System.setProperty("user.timezone", "UTC")
        val s = """                                                
      |   __ _ _ __  _ __   ___  ___  __ _ _   _ _ __ _   _ ___ 
      |  / _` | '_ \| '_ \ / _ \/ __|/ _` | | | | '__| | | / __|
      | | (_| | | | | | | | (_) \__ \ (_| | |_| | |  | |_| \__ \
      |  \__,_|_| |_|_| |_|\___/|___/\__,_|\__,_|_|   \__,_|___/""".stripMargin
        println(s)

        val conf = Constants.AppConfig.httpConfig

        LoggerFactory
            .getLogger(getClass)
            .atInfo
            .log("Starting Jetty server on port {}", conf.port)
        ZeroMQPublisher.log(zmq)

        val server: Server         = new Server(conf.port)
        server.setStopAtShutdown(true)
        val context: WebAppContext = new WebAppContext()
        context.setContextPath(conf.contextPath)
        context.setResourceBase("src/main/webapp")
        context.addEventListener(new ScalatraListener)
        context.addServlet(classOf[DefaultServlet], "/")
        server.setHandler(context)
        server.start()
        server.join()

        // server.setStopTimeout(conf.stopTimeout.toLong)
        // server.setStopAtShutdown(true)

        // val httpConfig = new HttpConfiguration()
        // httpConfig.setSendDateHeader(true)
        // httpConfig.setSendServerVersion(false)

        // val connector = new NetworkTrafficServerConnector(server, new HttpConnectionFactory(httpConfig))
        // connector.setPort(conf.port)
        // connector.setIdleTimeout(conf.connectorIdleTimeout.toLong)
        // server.addConnector(connector)

        // val webApp = new WebAppContext
        // webApp.setContextPath(conf.contextPath)
        // webApp.setResourceBase("src/main/webapp")
        // // webApp.setResourceBase(conf.webapp)
        // // webApp.setEventListeners(Array(new ScalatraListener))
        // webApp.setEventListeners(java.util.List.of(new ScalatraListener))

        // // Add JavaMelody for monitoring
        // // webApp.addServlet(classOf[ReportServlet], "/monitoring")
        // // webApp.addEventListener(new SessionListener)
        // // val monitoringFilter = new FilterHolder(new MonitoringFilter())
        // // monitoringFilter.setInitParameter(Parameter.APPLICATION_NAME.getCode, conf.webapp)
        // // monitoringFilter.setInitParameter("authorized-users", "adminz:Cranchiidae")

        // // webApp.addFilter(
        // //   monitoringFilter,
        // //   "/*",
        // //   java.util.EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC)
        // // )

        // server.setHandler(webApp)

        // server.start()
    }
}
