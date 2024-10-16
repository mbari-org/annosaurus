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
import org.mbari.annosaurus.domain.{ErrorMsg, QueryRequest}
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
            .out(header("Content-Type", "text/tab-separated-values"))
            .name("runQuery")
            .description("Run a query")
            .tag(tag)

    val runQueryImpl =
        runQuery.serverLogic(request =>
            handleEitherAsync(
                queryController.query(request).map(QueryResults.toTsv).map(_.mkString("\n"))
            )
        )

    override def all: List[Endpoint[_, _, _, _, _]] = List(
        listColumns,
        runQuery
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        listColumnsImpl,
        runQueryImpl
    )
}
