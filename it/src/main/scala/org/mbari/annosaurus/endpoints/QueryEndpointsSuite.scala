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
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.etc.jdk.Loggers.given
import org.mbari.annosaurus.etc.sdk.Futures.*
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import org.mbari.annosaurus.repository.query.JDBC
import sttp.client3.*
import sttp.model.StatusCode

import java.time.{Duration, Instant}
import scala.jdk.CollectionConverters.*

trait QueryEndpointsSuite extends EndpointsSuite:

    private val log = System.getLogger(getClass.getName)

    given JPADAOFactory = daoFactory

    private lazy val controller = new QueryController(daoFactory.databaseConfig, daoFactory.annotationView)
    private lazy val endpoints  = new QueryEndpoints(controller)

    test("listColumns") {
        runGet(
            endpoints.listColumnsImpl,
            s"http://test.com/v1/query/columns",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[JDBC.Metadata]](response.body)
//                log.atDebug.log(s"Columns: $obtained")
                assert(obtained.nonEmpty)
        )
    }

    test("runQuery") {
        val xs           = TestUtils.create(2, 2)
        val expected     =
            ("concept" +: xs.flatMap(_.getObservations.asScala.map(_.getConcept))).distinct.sorted.mkString("\n")
        val queryRequest = QueryRequest(select = Some(Seq("concept")), distinct = Some(true))
        runPost(
            endpoints.runQueryImpl,
            s"http://test.com/v1/query/run",
            queryRequest.stringify,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                response.body match
                    case Left(e)     => fail(e)
                    case Right(body) =>
                        val obtained = body.split("\n").sorted.mkString("\n")
//                        log.atDebug.log(s"Query results: $obtained")
                        assertEquals(obtained, expected)
        )
    }

    test("runQuery - notlike constraint") {
        val xs           = TestUtils.create(2, 2)
        val conceptToExclude = xs.head.getObservations.asScala.head.getConcept
        val xsFiltered   = xs.filterNot(_.getObservations.asScala.exists(_.getConcept == conceptToExclude))
        val expected     =
            ("concept" +: xsFiltered.flatMap(_.getObservations.asScala.map(_.getConcept))).distinct.sorted.mkString("\n")
        val queryRequest = QueryRequest(select = Some(Seq("concept")), distinct = Some(true), where = Some(Seq(
            ConstraintRequest(
                column = "concept",
                notlike = Some(conceptToExclude)
            )
        )))
        runPost(
            endpoints.runQueryImpl,
            s"http://test.com/v1/query/run",
            queryRequest.stringify,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                response.body match
                    case Left(e)     => fail(e)
                    case Right(body) =>
                        val obtained = body.split("\n").sorted.mkString("\n")
//                        log.atDebug.log(s"Query results: $obtained")
                        assertEquals(obtained, expected)
        )
    }

    test("runQueryAndCache") {
        val xs           = TestUtils.create(2, 2)
        val expected     =
            ("concept" +: xs.flatMap(_.getObservations.asScala.map(_.getConcept))).distinct.sorted.mkString("\n")
        val queryRequest = QueryRequest(select = Some(Seq("concept")), distinct = Some(true))
        val stub         = newBackendStub(endpoints.downloadTsvImpl)
        val u            = uri"http://test.com/v1/query/download"
        val request      = basicRequest.post(u).body(queryRequest.stringify)
        val response     = request.send(stub).join
        assertEquals(response.code, StatusCode.Ok)
        log.atInfo.log(s"Query results: ${response.body}")

        // runPost(
        //     endpoints.runQueryAndCacheImpl,
        //     s"http://test.com/v1/query/run2",
        //     queryRequest.stringify,
        //     response =>
        //         assertEquals(response.code, StatusCode.Ok)
        //         response.body match
        //             case Left(e)     => fail(e)
        //             case Right(body) =>
        //                 // val obtained = body.split("\n").sorted.mkString("\n")
        //                log.atDebug.log(s"Query results: $body")
        //                 // assertEquals(obtained, expected)
        // )
    }

    test("count") {
        val xs           = TestUtils.create(2, 2)
        val expected     = 4L
        val queryRequest =
            QueryRequest(distinct = Some(true), where = Some(Seq(ConstraintRequest("concept", isnull = Some(false)))))
        runPost(
            endpoints.countImpl,
            s"http://test.com/v1/query/count",
            queryRequest.stringify,
            response =>
//                println(response)
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Count](response.body)
                assertEquals(obtained.count, expected)
        )
    }

    test("count with 'between' constraint") {
        val xs                = TestUtils.create(2, 2)
        val expected          = 4L
        val minTime           = Instant.now().minus(Duration.ofDays(40))
        val maxTime           = Instant.now().plus(Duration.ofDays(40))
        val constraintAttempt =
            s"""{"column":"index_recorded_timestamp","between":["${minTime}","$maxTime"]}""".reify[ConstraintRequest]
        assert(constraintAttempt.isRight)
        val constraint        =
            constraintAttempt.getOrElse(ConstraintRequest("index_recorded_timestamp", isnull = Some(false)))
        val queryRequest      =
            QueryRequest(
                distinct = Some(true),
                where = Some(Seq(constraint))
            )
        runPost(
            endpoints.countImpl,
            s"http://test.com/v1/query/count",
            queryRequest.stringify,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Count](response.body)
                assertEquals(obtained.count, expected)
        )
    }
