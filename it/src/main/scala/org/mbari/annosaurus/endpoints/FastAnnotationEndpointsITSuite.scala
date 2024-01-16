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

import org.mbari.annosaurus.controllers.TestUtils
import org.mbari.annosaurus.domain.{AnnotationSC, QueryConstraints, QueryConstraintsResponseSC}
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.repository.jdbc.JdbcRepository
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import sttp.model.StatusCode
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import sttp.client3.*
import org.mbari.annosaurus.etc.sdk.Futures.*

import scala.jdk.CollectionConverters.*

trait FastAnnotationEndpointsITSuite extends EndpointsSuite {

    private val log = System.getLogger(getClass.getName)
    given JPADAOFactory = daoFactory
    given jwtService: JwtService = new JwtService("mbari", "foo", "bar")
    private lazy val repository = new JdbcRepository(daoFactory.entityManagerFactory)
    private lazy val endpoints = new FastAnnotationEndpoints(repository)

    test("findAllAnnotations".flaky) {
        val xs = TestUtils.create(2, 2, 1, 1, true)
        runGet(
            endpoints.findAllAnnotationsImpl,
            "http://test.com/v1/fast?data=true",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val annotations = checkResponse[Seq[AnnotationSC]](response.body)
                assert(annotations.size >= 4)
//                println(response.body)
// TODO this is not returning the ancillary data
            }
        )
    }

    test("findAnnotationsByQueryConstraints") {
        val xs = TestUtils.create(2, 2, 1, 1, true) ++ TestUtils.create(2, 2, 1, 1, false)
        val obs = xs.flatMap(_.getObservations.asScala)
        val videoReferenceUuids = xs.map(_.getVideoReferenceUuid).distinct
        val qc = QueryConstraints(limit = Some(10), offset = Some(0), videoReferenceUuids = videoReferenceUuids)
        val jwt = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.findAnnotationsByQueryConstraintsImpl)
        val response = basicRequest
          .get(uri"http://test.com/v1/fast?data=true")
          .header("Authorization", s"Bearer $jwt")
          .header("Content-Type", "application/x-www-form-urlencoded")
          .body(qc.toSnakeCase.stringify)
          .send(backendStub)
          .join
        assertEquals(response.code, StatusCode.Ok)
        val qcr = checkResponse[QueryConstraintsResponseSC[Seq[AnnotationSC]]](response.body)
        assertEquals(qcr.content.size, obs.size)
        println(qcr.stringify)
    }


}
