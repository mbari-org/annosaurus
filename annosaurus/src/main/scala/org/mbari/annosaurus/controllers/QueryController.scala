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

class QueryController(databaseConfig: DatabaseConfig, viewName: String) {

    private lazy val queryService = new QueryService(databaseConfig, viewName)

    def count(queryRequest: QueryRequest): Either[Throwable, Count] =
        val query = Query.from(queryRequest)
        queryService.count(query).map(Count(_))

    def query(queryRequest: QueryRequest): Either[Throwable, QueryResults] =
        val query = Query.from(queryRequest)
        queryService.query(query)

    def listColumns(): Either[Throwable, Seq[JDBC.Metadata]] =
        queryService.jdbc.listColumnsMetadata(viewName)

}
