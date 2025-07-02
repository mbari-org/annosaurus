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

package org.mbari.annosaurus.controllers

import org.mbari.annosaurus.DatabaseConfig
import org.mbari.annosaurus.domain.{Count, QueryRequest}
import org.mbari.annosaurus.repository.query.{JDBC, Query, QueryResults, QueryService}

import java.nio.file.Path

class QueryController(databaseConfig: DatabaseConfig, viewName: String):

    private lazy val queryService = new QueryService(databaseConfig, viewName)

    def count(queryRequest: QueryRequest): Either[Throwable, Count] =
        for
            query <- Query.validate(queryRequest, checkSelect = false)
            count <- queryService.count(query)
        yield Count(count)

    def query(queryRequest: QueryRequest): Either[Throwable, QueryResults] =
        for
            query   <- Query.validate(queryRequest, checkWhere = queryRequest.strict.getOrElse(false))
            results <- queryService.query(query)
        yield results

    def queryAndSaveToTsvFile(queryRequest: QueryRequest, path: Path): Either[Throwable, Path] =
        for
            query   <- Query.validate(queryRequest, checkWhere = queryRequest.strict.getOrElse(false))
            tsvFile <- queryService.queryAndSaveToTsvFile(query, path)
        yield path

    def listColumns(): Either[Throwable, Seq[JDBC.Metadata]] =
        queryService.jdbc.listColumnsMetadata(viewName)
