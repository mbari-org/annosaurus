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

import io.vertx.core.http.HttpServerOptions
import io.vertx.core.{Vertx, VertxOptions}
import io.vertx.ext.web.Router
import org.mbari.annosaurus.etc.jdk.Loggers
import org.mbari.annosaurus.etc.jdk.Loggers.given
import org.mbari.annosaurus.etc.zeromq.ZeroMQPublisher
import sttp.tapir.server.vertx.VertxFutureServerInterpreter.VertxFutureToScalaFuture
import sttp.tapir.server.vertx.{VertxFutureServerInterpreter, VertxFutureServerOptions}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object Main:

    // hold on to messaging objects so they don't get GC'd
    private val zmq = ZeroMQPublisher.autowire(AppConfig.DefaultZeroMQConfig)
    private val log = Loggers(this.getClass)

    def main(args: Array[String]): Unit =

        System.setProperty("user.timezone", "UTC")
        val s =
            """
              |   __ _ _ __  _ __   ___  ___  __ _ _   _ _ __ _   _ ___
              |  / _` | '_ \| '_ \ / _ \/ __|/ _` | | | | '__| | | / __|
              | | (_| | | | | | | | (_) \__ \ (_| | |_| | |  | |_| \__ \
              |  \__,_|_| |_|_| |_|\___/|___/\__,_|\__,_|_|   \__,_|___/""".stripMargin + s"  v${AppConfig.Version}"

        println(s)

        given ExecutionContext = ExecutionContext.global

        ZeroMQPublisher.log(zmq)

        // Disable server log: https://github.com/softwaremill/tapir/issues/3272
        // https://softwaremill.com/benchmarking-tapir-part-1/
        // https://softwaremill.com/benchmarking-tapir-part-2/

        val serverOptions = VertxFutureServerOptions
            .customiseInterceptors
            .serverLog(None)
            .metricsInterceptor(Endpoints.prometheusMetrics.metricsInterceptor())
            .options


        val port = sys.env.get("HTTP_PORT").flatMap(_.toIntOption).getOrElse(8080)
        log.atInfo.log(s"Starting ${AppConfig.Name} v${AppConfig.Version} on port $port")

        val vertx  =
            Vertx.vertx(new VertxOptions().setWorkerPoolSize(AppConfig.NumberOfVertxWorkers))

        // enable deflate and gzip compression
        val httpServerOptions = new HttpServerOptions()
        httpServerOptions.setCompressionSupported(true)

        val server = vertx.createHttpServer(httpServerOptions)
        val router = Router.router(vertx)

        // NOTE: Don't add a handler. It will intercept all requests (Originally: Log all requests)
//        router.route()
//            .handler(ctx => log.atInfo.log(s"${ctx.request().method()} ${ctx.request().path()}"))

        val interpreter = VertxFutureServerInterpreter(serverOptions)

        // For VertX, we need to separate the non-blocking endpoints from the blocking ones
        Endpoints
            .nonBlockingEndpoints
            .foreach(endpoint =>
                interpreter
                    .route(endpoint)
                    .apply(router) // attaches to vertx router
            )

        Endpoints
            .blockingEndpoints
            .foreach(endpoint =>
                interpreter
                    .blockingRoute(endpoint)
                    .apply(router) // attaches to vertx router
            )

        // Add our metrics endpoints
        interpreter.route(Endpoints.metricsEndpoint).apply(router)

        // Add our documentation endpoints
        Endpoints
            .docEndpoints
            .foreach(endpoint =>
                interpreter
                    .route(endpoint)
                    .apply(router)
            )

        router
            .getRoutes()
            .forEach(r => log.atInfo.log(f"Adding route: ${r.methods()}%8s ${r.getPath}%s"))

        val program = server.requestHandler(router).listen(port).asScala

        Await.result(program, Duration.Inf)
