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

import org.mbari.annosaurus.domain.{
    DepthHistogramSC,
    ErrorMsg,
    QueryConstraints,
    QueryConstraintsResponseSC,
    QueryConstraintsSC,
    TimeHistogramSC
}
import org.mbari.annosaurus.endpoints.CustomTapirJsonCirce.*
import org.mbari.annosaurus.etc.circe.CirceCodecs.given
import org.mbari.annosaurus.repository.jdbc.AnalysisRepository
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.{ExecutionContext, Future}

// Endpoint[SECURITY_INPUT, INPUT, ERROR_OUTPUT, OUTPUT, -R]
class AnalysisEndpoints(repository: AnalysisRepository)(implicit val executor: ExecutionContext) extends Endpoints:

    private val base = "histogram"
    private val tag  = "Analysis"

    val depthHistogram: Endpoint[Unit, (Option[Int], QueryConstraints), ErrorMsg, QueryConstraintsResponseSC[
        DepthHistogramSC
    ], Any] = openEndpoint
        .post
        .in(base / "depth")
        .in(query[Option[Int]]("size").description("Bin size in meters"))
        .in(jsonBody[QueryConstraints].description("Query constraints"))
        .out(
            jsonBody[QueryConstraintsResponseSC[DepthHistogramSC]]
                .description("Histogram of depths")
        )
        .description("Generate a histogram of depths base on the query constraints")
        .name("depthHistogram")
        .tag(tag)

    val depthHistogramImpl: ServerEndpoint[Any, Future] =
        depthHistogram.serverLogic { case (binSizeMeters, constraints) =>
            val f = Future(
                repository.depthHistogram(constraints, binSizeMeters.getOrElse(50))
            ).map(dh => QueryConstraintsResponseSC(constraints.toSnakeCase, dh.toSnakeCase))
            handleErrors(f)
        }

    val timeHistogram: Endpoint[Unit, (Option[Int], QueryConstraints), ErrorMsg, QueryConstraintsResponseSC[
        TimeHistogramSC
    ], Any] = openEndpoint
        .post
        .in(base / "time")
        .in(query[Option[Int]]("size").description("Bin size in days").default(Some(50)))
        .in(jsonBody[QueryConstraints].description("Query constraints"))
        .out(jsonBody[QueryConstraintsResponseSC[TimeHistogramSC]].description("Histogram of time"))
        .description("Generate a histogram of times base on the query constraints")
        .name("timeHistogram")
        .tag(tag)

    val timeHistogramImpl: ServerEndpoint[Any, Future] =
        timeHistogram.serverLogic { case (binSizeDays, constraints) =>
            val f = Future(
                repository.timeHistogram(constraints, binSizeDays.getOrElse(50))
            ).map(dh => QueryConstraintsResponseSC(constraints.toSnakeCase, dh.toSnakeCase))
            handleErrors(f)
        }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] =
        List(depthHistogram, timeHistogram)

    override def allImpl: List[ServerEndpoint[Any, Future]] =
        List(depthHistogramImpl, timeHistogramImpl)
