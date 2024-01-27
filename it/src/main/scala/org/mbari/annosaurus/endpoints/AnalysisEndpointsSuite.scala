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
import org.mbari.annosaurus.domain.{
    DepthHistogramSC,
    QueryConstraints,
    QueryConstraintsResponseSC,
    TimeHistogramSC
}
import sttp.tapir.*
import sttp.client3.*
import sttp.model.StatusCode
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.etc.sdk.Futures.join
import org.mbari.annosaurus.repository.jdbc.AnalysisRepository
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}

import scala.jdk.CollectionConverters.*

trait AnalysisEndpointsSuite extends EndpointsSuite {

    given JPADAOFactory         = daoFactory
    private lazy val repository = new AnalysisRepository(daoFactory.entityManagerFactory)
    private lazy val endpoints  = new AnalysisEndpoints(repository)

    test("depthHistogram") {
        val xs                  = TestUtils.create(10, 10, includeData = true)
        val expected            = xs.flatMap(_.getObservations.asScala).size
        val videoReferenceUuids = xs.map(_.getVideoReferenceUuid).distinct
        val qcr                 = QueryConstraints(videoReferenceUuids = videoReferenceUuids)
//        println(qcr.toSnakeCase.stringify)
        runPost(
            endpoints.depthHistogramImpl,
            s"http://test.com/v1/histogram/depth",
            qcr.toSnakeCase.stringify,
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val qcResponse =
                    checkResponse[QueryConstraintsResponseSC[DepthHistogramSC]](response.body)
                val obtained   = qcResponse.content.count
                assertEquals(obtained, expected)
            }
        )
    }

    test("depthHistogram (100m)") {
        val xs                  = TestUtils.create(10, 10, includeData = true)
        val expected            = xs.flatMap(_.getObservations.asScala).size
        val videoReferenceUuids = xs.map(_.getVideoReferenceUuid).distinct
        val qcr                 = QueryConstraints(videoReferenceUuids = videoReferenceUuids)
//        println(qcr.toSnakeCase.stringify)
        runPost(
            endpoints.depthHistogramImpl,
            s"http://test.com/v1/histogram/depth?size=100",
            qcr.toSnakeCase.stringify,
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val qcResponse =
                    checkResponse[QueryConstraintsResponseSC[DepthHistogramSC]](response.body)
                val obtained   = qcResponse.content.count
                assertEquals(obtained, expected)
            }
        )
    }

    // TODO both the depth and time histogram logic needs to be reworked. They can give incorrect results
    test("timeHistogram".flaky) {
        val xs                  = TestUtils.create(10, 10, includeData = true)
        val expected            =
            xs.filter(_.getRecordedTimestamp != null).flatMap(_.getObservations.asScala).size
        val videoReferenceUuids = xs.map(_.getVideoReferenceUuid).distinct
        val qcr                 = QueryConstraints(videoReferenceUuids = videoReferenceUuids)
//        println(qcr.toSnakeCase.stringify)
        runPost(
            endpoints.timeHistogramImpl,
            s"http://test.com/v1/histogram/time",
            qcr.toSnakeCase.stringify,
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val qcResponse =
                    checkResponse[QueryConstraintsResponseSC[TimeHistogramSC]](response.body)
                val obtained   = qcResponse.content.count
                assertEquals(obtained, expected)
            }
        )
    }

    test("timeHistogram (100 days)") {
        val xs                  = TestUtils.create(10, 10, includeData = true)
        val expected            = xs.flatMap(_.getObservations.asScala).size
        val videoReferenceUuids = xs.map(_.getVideoReferenceUuid).distinct
        val qcr                 = QueryConstraints(videoReferenceUuids = videoReferenceUuids)
//        println(qcr.toSnakeCase.stringify)
        runPost(
            endpoints.timeHistogramImpl,
            s"http://test.com/v1/histogram/time?size=100",
            qcr.toSnakeCase.stringify,
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val qcResponse =
                    checkResponse[QueryConstraintsResponseSC[TimeHistogramSC]](response.body)
                val obtained   = qcResponse.content.count
                assertEquals(obtained, expected)
            }
        )
    }

}
