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

import io.circe.*
import io.circe.parser.*
import org.mbari.annosaurus.etc.sdk.Futures.*
import org.mbari.annosaurus.repository.jpa.BaseDAOSuite
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.stub.TapirStubInterpreter

import scala.concurrent.Future
import sttp.tapir.server.vertx.VertxFutureServerOptions
import sttp.tapir.server.interceptor.exception.ExceptionHandler
import sttp.tapir.server.interceptor.CustomiseInterceptors
import sttp.model.StatusCode
import sttp.client3.SttpBackend
import sttp.tapir.server.model.ValuedEndpointOutput

trait EndpointsSuite extends BaseDAOSuite:

    def runDelete(
        ep: ServerEndpoint[Any, Future],
        uri: String,
        assertions: Response[Either[String, String]] => Unit,
        jwt: Option[String] = None
    ): Unit =
        val backendStub = newBackendStub(ep)
        val u           = uri"$uri"
        val request     = jwt match
            case None => basicRequest.delete(u)
            case Some(bearer) =>
                basicRequest
                    .delete(u)
                    .auth
                    .bearer(bearer)
        val response    = request.send(backendStub).join
        assertions(response)

    def runPut(
        ep: ServerEndpoint[Any, Future],
        uri: String,
        body: String,
        assertions: Response[Either[String, String]] => Unit,
        jwt: Option[String] = None
    ): Unit =
        val backendStub = newBackendStub(ep)
        val u           = uri"$uri"
        val request     = jwt match
            case None => basicRequest.put(u).body(body)
            case Some(bearer) =>
                basicRequest
                    .put(u)
                    .body(body)
                    .auth
                    .bearer(bearer)
        val response    = request.send(backendStub).join
        assertions(response)

    def runPost(
        ep: ServerEndpoint[Any, Future],
        uri: String,
        body: String,
        assertions: Response[Either[String, String]] => Unit,
        jwt: Option[String] = None
    ): Unit =
        val backendStub = newBackendStub(ep)
        val u           = uri"$uri"
        val request     = jwt match
            case None => basicRequest.post(u).body(body)
            case Some(bearer) =>
                basicRequest
                    .post(u)
                    .body(body)
                    .auth
                    .bearer(bearer)

        val response    = request.send(backendStub).join
        assertions(response)

    def runGet(
        ep: ServerEndpoint[Any, Future],
        uri: String,
        assertions: Response[Either[String, String]] => Unit
    ): Unit =
        val backendStub = newBackendStub(ep)
        val u           = uri"$uri"
        val request     = basicRequest.get(u)
        val response    = request.send(backendStub).join
        assertions(response)

    def checkResponse[T: Decoder](responseBody: Either[String, String]): T =
        responseBody match
            case Left(e)     => fail(e)
            case Right(json) =>
                decode[T](json) match
                    case Left(error)  => fail(error.getLocalizedMessage)
                    case Right(value) => value

    /** Creates a stubbed backend for testing endpoints. Adds exception logging to the stub.
      * @param serverEndpoint
      * @return
      */
    def newBackendStub(serverEndpoint: ServerEndpoint[Any, Future]): SttpBackend[Future, Any] =
        // --- START: This block adds exception logging to the stub
        val exceptionHandler = ExceptionHandler.pure[Future](ctx =>
            Some(
                ValuedEndpointOutput(
                    sttp.tapir.stringBody.and(sttp.tapir.statusCode),
                    (s"failed due to ${ctx.e.getMessage}", StatusCode.InternalServerError)
                )
            )
        )

        val customOptions: CustomiseInterceptors[Future, VertxFutureServerOptions] =
            import scala.concurrent.ExecutionContext.Implicits.global
            VertxFutureServerOptions
                .customiseInterceptors
                .exceptionHandler(exceptionHandler)
        // --- END: This block adds exception logging to the stub
        TapirStubInterpreter(customOptions, SttpBackendStub.asynchronousFuture)
            .whenServerEndpointRunLogic(serverEndpoint)
            .backend()
