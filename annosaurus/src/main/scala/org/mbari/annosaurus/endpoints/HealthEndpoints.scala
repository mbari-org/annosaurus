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

import org.mbari.annosaurus.domain.{ErrorMsg, HealthStatus}
import org.mbari.annosaurus.etc.circe.CirceCodecs.given
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.{ExecutionContext, Future}

class HealthEndpoints(using ec: ExecutionContext) extends Endpoints:

    val healthEndpoint: Endpoint[Unit, Unit, ErrorMsg, HealthStatus, Any] =
        openEndpoint
            .get
            .in("health")
            .out(jsonBody[HealthStatus])
            .name("health")
            .description("Health check")
            .tag("Health")

    val healthEndpointImpl: ServerEndpoint[Any, Future] =
        healthEndpoint.serverLogic(_ => handleErrors(Future(HealthStatus.default)))

    override def all: List[Endpoint[?, ?, ?, ?, ?]] =
        List(healthEndpoint)

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(healthEndpointImpl)
