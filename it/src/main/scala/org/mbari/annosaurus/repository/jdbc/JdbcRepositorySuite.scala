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

import org.mbari.annosaurus.repository.jpa.BaseDAOSuite
import org.mbari.annosaurus.controllers.TestUtils
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import junit.framework.Test
import scala.jdk.CollectionConverters.*
import org.mbari.annosaurus.domain.Annotation
import java.time.Duration

trait JdbcRepositorySuite extends BaseDAOSuite {

    given JPADAOFactory = daoFactory

    lazy val repository = new JdbcRepository(daoFactory.entityManagerFactory)

    test("countAll") {
        val xs = TestUtils.create(8, 1)
        val n = repository.countAll()
        assertEquals(n, 8L)
    }

    test("countByQueryConstraint") {

    }
    test("countImagesByVideoReferenceUuid") {
        // Create 16 using 2 different videoReferenceUuids
        TestUtils.create(8, 1)
        val xs = TestUtils.create(8, 1, 0, 1)
        val x = xs.head
        val n = repository.countImagesByVideoReferenceUuid(x.getVideoReferenceUuid())
        assertEquals(n, 8L)
    }

    test("deleteByVideoReferenceUUID") {
        // Create 16 using 2 different videoReferenceUuids
        TestUtils.create(8, 1)
        val xs = TestUtils.create(8, 1, 1, 1, true)
        val x = xs.head
        val n = repository.deleteByVideoReferenceUuid(x.getVideoReferenceUuid())
        assertEquals(n.imagedMomentCount, 8)
        assertEquals(n.observationCount, 8)
        assertEquals(n.ancillaryDataCount, 8)
        assertEquals(n.imageReferenceCount, 8)
        assertEquals(n.associationCount, 8)

    }

    test("findAll") {
        val xs = TestUtils.create(8, 1)
        val xs2 = repository.findAll()
        assert(xs2.size >= 8)
    }

    test("findByConcept") {
        val xs = TestUtils.create(1, 1)
        val x = xs.head
        val obs = x.getObservations().asScala.head
        val xs2 = repository.findByConcept(obs.getConcept())
        assertEquals(xs2.size, 1)
    }

    test("findByConceptWithImages") {
        val xs = TestUtils.create(3, 1, 0, 1, true) ++ TestUtils.create(3, 1)
        val obs = xs.head.getObservations().asScala.head
        val ys = repository.findByConceptWithImages(obs.getConcept())
        assertEquals(ys.size, 1)
    }

    test("findByConcurrentRequest") {}

    test("findByLinkNameAndLinkValue") {
        val xs = TestUtils.create(2, 1, 1)
        val obs = xs.head.getObservations().asScala.head
        val anno = obs.getAssociations().asScala.head
        val xs2 = repository.findByLinkNameAndLinkValue(anno.getLinkName(), anno.getLinkValue())
        assertEquals(xs2.size, 1)
    }

    test("findByMultiRequest") {}
    test("findByQueryConstraint") {}

    test("findByToConceptWithImages") {
        val xs = TestUtils.create(4, 1, 1)
        val obs = xs.head.getObservations().asScala.head
        val anno = obs.getAssociations().asScala.head
        val xs2 = repository.findByToConceptWithImages(anno.getToConcept())
        assertEquals(xs2.size, 1)
    }

    test("findByVideoReferenceUuid") {
        val xs = TestUtils.create(8, 1)
        val x = xs.head
        val xs2 = repository.findByVideoReferenceUuid(x.getVideoReferenceUuid())
        assertEquals(xs2.size, 8)
    }

    test("findByVideoReferenceUuidAndTimestamps") {
        val xs = TestUtils.create(8, 1)
        val x = xs.head
        val ts = xs.map(im => im.getRecordedTimestamp()).sorted
        val ts0 = ts.head.minus(Duration.ofSeconds(1))
        val ts1 = ts(2).plus(Duration.ofSeconds(1))
        val xs2 = repository.findByVideoReferenceUuidAndTimestamps(x.getVideoReferenceUuid(),
            ts0, ts1)
        println("-------- " + xs2)
        assertEquals(xs2.size, 3)
    }

    test("findGeographicRangeByQueryConstraint") {}

    test("findImagedMomentUuidsByConceptWithImages") {
        val xs = TestUtils.create(4, 1, 0, 1)
        val obs = xs.head.getObservations().asScala.head
        val xs2 = repository.findImagedMomentUuidsByConceptWithImages(obs.getConcept())
        assertEquals(xs2.size, 1)
    }

    test("findImagedMomentUuidsByToConceptWithImages") {}
    test("findImagesByVideoReferenceUuid") {}
  
}
