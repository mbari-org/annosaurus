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

package org.mbari.annosaurus.repository.jdbc

import org.mbari.annosaurus.controllers.TestUtils
import org.mbari.annosaurus.domain.QueryConstraints
import org.mbari.annosaurus.repository.jpa.{BaseDAOSuite, JPADAOFactory}

import java.time.Instant
import scala.jdk.CollectionConverters.*

trait AnalysisRepositorySuite extends BaseDAOSuite:

    given JPADAOFactory = daoFactory

    lazy val repository = new AnalysisRepository(daoFactory.entityManagerFactory)

    test("depthHistogram") {
        val xs        = TestUtils.create(5, 5, includeData = true)
        val expected  = xs.flatMap(_.getObservations.asScala).size
        val qcr       = QueryConstraints(videoReferenceUuids = Seq(xs.head.getVideoReferenceUuid))
        val histogram = repository.depthHistogram(qcr)
        assertEquals(histogram.count, expected)

    }

    test("timeHistogram") {
        val xs        = TestUtils.create(5, 5, includeData = true)
        val minTime   = xs.map(_.getRecordedTimestamp).min
        val maxTime   = xs.map(_.getRecordedTimestamp).max
        val expected  = xs.flatMap(_.getObservations.asScala).size
        val qcr       = QueryConstraints(
            videoReferenceUuids = Seq(xs.head.getVideoReferenceUuid),
            minTimestamp = Some(minTime.minusSeconds(24 * 60 * 60)),
            maxTimestamp = Some(maxTime.plusSeconds(24 * 60 * 60))
        )
        val histogram = repository.timeHistogram(qcr, 1)
        assertEquals(histogram.count, expected)
    }
