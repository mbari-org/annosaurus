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

import CustomTapirJsonCirce.*
import org.mbari.annosaurus.controllers.QueryController
import org.mbari.annosaurus.domain.{Count, ErrorMsg, QueryRequest}
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.etc.tapir.TapirCodecs.given
import org.mbari.annosaurus.repository.query.{JDBC, QueryResults}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ServerEndpoint.Full

import scala.util.Success
import scala.concurrent.{ExecutionContext, Future}

import org.mbari.annosaurus.etc.tapir.TapirCodecs

class QueryEndpoints(queryController: QueryController)(using executionContext: ExecutionContext)
    extends Endpoints {

    private val base = "query"
    private val tag  = "Query"

    val listColumns: Endpoint[Unit, Unit, ErrorMsg, Seq[JDBC.Metadata], Any] = openEndpoint
        .get
        .in(base / "columns")
        .out(jsonBody[Seq[JDBC.Metadata]])
        .name("listQueryColumns")
        .description("List columns in the query view")
        .tag(tag)

    val listColumnsImpl: Full[Unit, Unit, Unit, ErrorMsg, Seq[JDBC.Metadata], Any, Future] =
        listColumns.serverLogic(_ => handleEitherAsync(queryController.listColumns()))

    val runQuery: Endpoint[Unit, QueryRequest, ErrorMsg, String, Any] =
        openEndpoint
            .post
            .in(base / "run")
            .in(jsonBody[QueryRequest])
            .out(stringBody)
            .name("runQuery")
            .description("Run a query")
            .tag(tag)

    val runQueryImpl: Full[Unit, Unit, QueryRequest, ErrorMsg, String, Any, Future] =
        runQuery.serverLogic(request =>
            handleEitherAsync(
                queryController.query(request).map(QueryResults.toTsv)
            )
        )

    val count: Endpoint[Unit, QueryRequest, ErrorMsg, Count, Any] = openEndpoint
        .post
        .in(base / "count")
        .in(jsonBody[QueryRequest])
        .out(jsonBody[Count])
        .name("countQuery")
        .description("Count the number of rows in a query")
        .tag(tag)

    val countImpl: Full[Unit, Unit, QueryRequest, ErrorMsg, Count, Any, Future] =
        count.serverLogic(request => handleEitherAsync(queryController.count(request)))

    override def all: List[Endpoint[_, _, _, _, _]] = List(
        listColumns,
        runQuery,
        count
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        listColumnsImpl,
        runQueryImpl,
        countImpl
    )
}
