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

        val vertx =
            Vertx.vertx(new VertxOptions().setWorkerPoolSize(AppConfig.NumberOfVertxWorkers))

        // enable deflate and gzip compression
        val httpServerOptions = new HttpServerOptions()
        httpServerOptions.setCompressionSupported(true)

        val server = vertx.createHttpServer(httpServerOptions)
        val router = Router.router(vertx)

        // Log all requests at DEBUG, and log the time taken for each request at INFO. This 
        // gives us visibility into all requests and their performance without overwhelming 
        // the logs with INFO-level messages.
        val debugLogger = log.atDebug // Avoid object allocation
        val infoLogger  = log.atInfo  // Avoid object allocation
        router.route()
           .handler(ctx => {
                val start  = System.currentTimeMillis()
                val method = ctx.request().method()
                val path   = ctx.request().uri()
                val remoteAddress = ctx.request().remoteAddress()
                debugLogger.log(s"→ $method $path from $remoteAddress")
                ctx.addEndHandler(_ =>
                    val dt = System.currentTimeMillis() - start
                    infoLogger.log(s"← $method $path ${dt}ms from $remoteAddress")
                )
                ctx.next()
            })

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
        interpreter.blockingRoute(Endpoints.metricsEndpoint).apply(router)

        // Add our documentation endpoints
        Endpoints
            .docEndpoints
            .foreach(endpoint =>
                interpreter
                    .blockingRoute(endpoint)
                    .apply(router)
            )

        router
            .getRoutes()
            .forEach(r => log.atInfo.log(f"Adding route: ${r.methods()}%8s ${r.getPath}%s"))

        router
            .errorHandler(
                500,
                ctx =>
                    Option(ctx.failure()) match
                        case Some(_: IllegalArgumentException) =>
                            // Client sent a malformed URL (e.g. invalid percent-encoding like %uf).
                            // Log at WARN — this is a client error, not a server fault.
                            // Avoid calling ctx.request().path() here as path normalization is
                            // what threw in the first place; use uri() for the raw, unnormalized value.
                            log.atWarn.withCause(ctx.failure()).log(s"Bad request (malformed URL): ${ctx.request().uri()}")
                            if !ctx.response().ended() then ctx.response().setStatusCode(400).end()
                        case Some(t) =>
                            log.atError.withCause(t).log(s"Unhandled exception in route: ${ctx.request().uri()}")
                            if !ctx.response().ended() then ctx.response().setStatusCode(500).end()
                        case None    =>
                            log.atError.log(s"Error 500 in route: ${ctx.request().uri()}")
                            if !ctx.response().ended() then ctx.response().setStatusCode(500).end()
            )


        val program = server.requestHandler(router).listen(port).asScala

        Await.result(program, Duration.Inf)
