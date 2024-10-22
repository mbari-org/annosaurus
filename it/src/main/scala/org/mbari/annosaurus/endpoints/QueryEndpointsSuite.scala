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

import org.mbari.annosaurus.controllers.{QueryController, TestUtils}
import org.mbari.annosaurus.domain.{ConstraintRequest, Count, QueryRequest}
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import org.mbari.annosaurus.repository.query.JDBC
import org.mbari.annosaurus.etc.jdk.Loggers.{*, given}
import sttp.model.StatusCode
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}

import scala.jdk.CollectionConverters.*

trait QueryEndpointsSuite extends EndpointsSuite {

    private val log = System.getLogger(getClass.getName)

    given JPADAOFactory = daoFactory

    private lazy val controller = new QueryController(daoFactory.databaseConfig, daoFactory.annotationView)
    private lazy val endpoints = new QueryEndpoints(controller)

    test("listColumns") {
        runGet(endpoints.listColumnsImpl,
            s"http://test.com/v1/query/columns",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[JDBC.Metadata]](response.body)
//                log.atDebug.log(s"Columns: $obtained")
                assert(obtained.nonEmpty)
        )
    }

    test("runQuery") {
        val xs = TestUtils.create(2, 2)
        val expected = ("concept" +: xs.flatMap(_.getObservations.asScala.map(_.getConcept))).distinct.sorted.mkString("\n")
        val queryRequest = QueryRequest(select = Some(Seq("concept")), distinct = Some(true))
        runPost(endpoints.runQueryImpl,
            s"http://test.com/v1/query/run",
            queryRequest.stringify,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                response.body match
                    case Left(e) => fail(e)
                    case Right(body) =>
                        val obtained = body.split("\n").sorted.mkString("\n")
//                        log.atDebug.log(s"Query results: $obtained")
                        assertEquals(obtained, expected)
        )
    }

    test("count") {
        val xs = TestUtils.create(2, 2)
        val expected = 4L
        val queryRequest = QueryRequest(distinct = Some(true), where = Some(Seq(ConstraintRequest("concept", isnull = Some(false)))))
        runPost(endpoints.countImpl,
            s"http://test.com/v1/query/count",
            queryRequest.stringify,
            response =>
//                println(response)
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Count](response.body)
                assertEquals(obtained.count, expected)
        )
    }

}
