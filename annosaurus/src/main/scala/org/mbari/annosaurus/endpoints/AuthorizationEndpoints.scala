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

package org.mbari.annosaurus.endpoints

import org.mbari.annosaurus.domain.{Authorization, ErrorMsg, Unauthorized}
import org.mbari.annosaurus.etc.circe.CirceCodecs.given
import org.mbari.annosaurus.etc.jdk.Logging.given
import org.mbari.annosaurus.etc.jwt.JwtService
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.Endpoint
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import org.mbari.annosaurus.domain.{BadRequest, ErrorMsg, NotFound, ServerError, Unauthorized}

class AuthorizationEndpoints()(using ec: ExecutionContext, jwtService: JwtService)
    extends Endpoints:
    
    private val base = "auth"

    private val log = System.getLogger(getClass().getName())

    val authEndpoint: Endpoint[String, Unit, ErrorMsg, Authorization, Any] =
        endpoint
            .post
            .in(base)
            .securityIn(header[String]("APIKEY"))
            .out(jsonBody[Authorization])
            .errorOut(
                oneOf[ErrorMsg](
                    oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[BadRequest])),
                    oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[NotFound])),
                    oneOfVariant(
                        statusCode(StatusCode.InternalServerError).and(jsonBody[ServerError])
                    ),
                    oneOfVariant(statusCode(StatusCode.Unauthorized).and(jsonBody[Unauthorized]))
                )
            )
            .name("authenticate")
            .description("Exchange an API key for a JWT")
            .tag("auth")

    val authEndpointImpl: ServerEndpoint[Any, Future] =
        authEndpoint
            .serverSecurityLogic(apiKey =>
                jwtService.authorize(apiKey) match
                    case None      => Future(Left(Unauthorized("Invalid API key")))
                    case Some(jwt) => Future(Right(Authorization.bearer(jwt)))
            )
            .serverLogic(bearerAuth => Unit => Future(Right(bearerAuth)))

    override val all: List[Endpoint[?, ?, ?, ?, ?]]         = List(authEndpoint)
    override val allImpl: List[ServerEndpoint[Any, Future]] =
        List(authEndpointImpl)
