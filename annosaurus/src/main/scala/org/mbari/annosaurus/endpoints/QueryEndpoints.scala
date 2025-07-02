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

import org.mbari.annosaurus.controllers.QueryController
import org.mbari.annosaurus.domain.{Count, ErrorMsg, QueryRequest}
import org.mbari.annosaurus.endpoints.CustomTapirJsonCirce.*
import org.mbari.annosaurus.etc.circe.CirceCodecs.given
import org.mbari.annosaurus.repository.query.{JDBC, QueryResults}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ServerEndpoint.Full

import java.io.File
import java.nio.file.Files
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

class QueryEndpoints(queryController: QueryController)(using executionContext: ExecutionContext) extends Endpoints:

    private val base = "query"
    private val tag  = "Query"
    private val scheduler = Executors.newScheduledThreadPool(1)

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

    val downloadTsv: Endpoint[Unit, QueryRequest, ErrorMsg, File, Any] =
        openEndpoint
            .post
            .in(base / "download")
            .in(jsonBody[QueryRequest])
            .out(fileBody)
            .out(header("Content-Disposition", "attachment; filename=results.tsv"))
            .out(header("Content-Type", "text/tab-separated-values"))
            .name("downloadTsv")
            .description("Run a query and download the results as a tab-separate value file")
            .tag(tag)

    val downloadTsvImpl: Full[Unit, Unit, QueryRequest, ErrorMsg, File, Any, Future] =
        downloadTsv.serverLogic(request =>
            val path = Files.createTempFile("query", ".tsv")
            val task: Runnable = () => Files.delete(path)
            scheduler.schedule(task, 2, java.util.concurrent.TimeUnit.MINUTES)
            val future = handleEitherAsync(
                queryController.queryAndSaveToTsvFile(request, path).map(_ => path.toFile())
            )
            future
        )

    // val runQueryStreaming =
    //     openEndpoint
    //         .post
    //         .in(base / "run")
    //         .in(jsonBody[QueryRequest])
    //         .out(streamTextBody(Fs2Streams[Future]), (CodecFormat.TextPlain(), Some(StandardCharsets.UTF_8)))
    //         .name("runQueryStreaming")
    //         .description("Run a query and stream the results")
    //         .tag(tag)

    // val runQueryStreamingImpl: Full[Unit, Unit, QueryRequest, ErrorMsg, Future[Seq[String]], Any, Future] =
    //     runQueryStreaming.serverLogic(request =>
    //         handleEitherAsync(
    //             queryController.query(request).map(QueryResults.toTsvStream)
    //         )
    //     )

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

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        listColumns,
        runQuery,
        downloadTsv,
        count
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        listColumnsImpl,
        runQueryImpl,
        downloadTsvImpl,
        countImpl
    )
