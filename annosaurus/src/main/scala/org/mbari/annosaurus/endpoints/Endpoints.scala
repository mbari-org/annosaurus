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

import java.net.URI

import org.mbari.annosaurus.domain.{BadRequest, ErrorMsg, NotFound, ServerError, Unauthorized}
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}

import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.etc.jdk.Logging.given

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import sttp.model.StatusCode
import sttp.model.headers.WWWAuthenticateChallenge
import sttp.tapir.*
import sttp.tapir.Endpoint
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import java.time.Instant
import org.mbari.annosaurus.domain.{DepthHistogram, Image, QueryConstraints, QueryConstraintsResponse}
import org.mbari.annosaurus.domain.ImagedMoment
import org.mbari.annosaurus.domain.Observation
import org.mbari.annosaurus.domain.Association

import org.mbari.annosaurus.domain.ImageReference
import java.net.URL
import org.mbari.annosaurus.domain.CachedAncillaryDatum

case class Paging(offset: Option[Int] = Some(0), limit: Option[Int] = Some(100))

trait Endpoints:
    val log = System.getLogger(getClass.getName)

    given Schema[URI]                                      = Schema.string
    given Schema[URL]                                      = Schema.string
    given Schema[Option[URI]]                              = Schema.string
    given Schema[QueryConstraints]                         = Schema.derived[QueryConstraints]
    given Schema[DepthHistogram]                           = Schema.derived[DepthHistogram]
    given Schema[QueryConstraintsResponse[DepthHistogram]] =
        Schema.derived[QueryConstraintsResponse[DepthHistogram]]
    given Schema[Association]                              = Schema.derived[Association]
    given Schema[Observation]                              = Schema.derived[Observation]
    given Schema[ImageReference]                           = Schema.derived[ImageReference]
    given Schema[CachedAncillaryDatum]                     = Schema.derived[CachedAncillaryDatum]
    given Schema[ImagedMoment]                             = Schema.derived[ImagedMoment]
    // given Schema[VideoReference]  = Schema.derived[VideoReference]
    // given Schema[Video]           = Schema.derived[Video]
    // given Schema[Media]           = Schema.derived[Media]
    // given Schema[MoveVideoParams] = Schema.derived[MoveVideoParams]

    def all: List[Endpoint[?, ?, ?, ?, ?]]
    def allImpl: List[ServerEndpoint[Any, Future]]

    def handleErrors[T](f: Future[T])(using ec: ExecutionContext): Future[Either[ErrorMsg, T]] =
        f.transform:
            case Success(value)     =>
                // log.atError.log(value.toString())
                Success(Right(value))
            case Failure(exception) =>
                log.atError.withCause(exception).log("Error")
                Success(Left(ServerError(exception.getMessage)))

    def handleOption[T](f: Future[Option[T]])(using
        ec: ExecutionContext
    ): Future[Either[ErrorMsg, T]] =
        f.transform:
            case Success(Some(value)) => Success(Right(value))
            case Success(None)        => Success(Left(NotFound("Not found")))
            case Failure(exception)   =>
                log.atError.withCause(exception).log("Error")
                Success(Left(ServerError(exception.getMessage)))

    val secureEndpoint: Endpoint[Option[String], Unit, ErrorMsg, Unit, Any] = endpoint
        .securityIn(auth.bearer[Option[String]](WWWAuthenticateChallenge.bearer))
        .errorOut(
            oneOf[ErrorMsg](
                oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[BadRequest])),
                oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[NotFound])),
                oneOfVariant(statusCode(StatusCode.InternalServerError).and(jsonBody[ServerError])),
                oneOfVariant(statusCode(StatusCode.Unauthorized).and(jsonBody[Unauthorized]))
            )
        )

    val paging: EndpointInput[Paging] =
        query[Option[Int]]("offset")
            .and(query[Option[Int]]("limit"))
            .mapTo[Paging]

    val openEndpoint: Endpoint[Unit, Unit, ErrorMsg, Unit, Any] = endpoint.errorOut(
        oneOf[ErrorMsg](
            oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[BadRequest])),
            oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[NotFound])),
            oneOfVariant(statusCode(StatusCode.InternalServerError).and(jsonBody[ServerError]))
        )
    )

    def verify(
        jwtOpt: Option[String]
    )(using jwtService: JwtService, ec: ExecutionContext): Future[Either[Unauthorized, Unit]] =
        jwtOpt match
            case None      => Future(Left(Unauthorized("Missing token")))
            case Some(jwt) =>
                Future(
                    if jwtService.verify(jwt) then Right(())
                    else Left(Unauthorized("Invalid token"))
                )
