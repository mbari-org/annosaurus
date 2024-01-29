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
import org.mbari.annosaurus.domain.QueryConstraints
import org.mbari.annosaurus.domain.MultiRequest
import org.mbari.annosaurus.domain.ConcurrentRequest
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}

trait JdbcRepositorySuite extends BaseDAOSuite {

    given JPADAOFactory = daoFactory

    lazy val repository = new JdbcRepository(daoFactory.entityManagerFactory)

    test("countAll") {
        val xs = TestUtils.create(8, 1)
        val n  = repository.countAll()
        assertEquals(n, 8L)
    }

    test("countByQueryConstraint") {
        val xs = TestUtils.create(8, 1)
        val qc = new QueryConstraints(videoReferenceUuids = Seq(xs.head.getVideoReferenceUuid()))
        val n  = repository.countByQueryConstraint(qc)
        assertEquals(n, 8)
    }
    test("countImagesByVideoReferenceUuid") {
        // Create 16 using 2 different videoReferenceUuids
        TestUtils.create(8, 1)
        val xs = TestUtils.create(8, 1, 0, 1)
        val x  = xs.head
        val n  = repository.countImagesByVideoReferenceUuid(x.getVideoReferenceUuid())
        assertEquals(n, 8L)
    }

    test("deleteByVideoReferenceUUID") {
        // Create 16 using 2 different videoReferenceUuids
        TestUtils.create(8, 1)
        val xs = TestUtils.create(8, 1, 1, 1, true)
        val x  = xs.head
        val n  = repository.deleteByVideoReferenceUuid(x.getVideoReferenceUuid())
        assertEquals(n.imagedMomentCount, 8)
        assertEquals(n.observationCount, 8)
        assertEquals(n.ancillaryDataCount, 8)
        assertEquals(n.imageReferenceCount, 8)
        assertEquals(n.associationCount, 8)

    }

    test("findAll") {
        val xs  = TestUtils.create(8, 1)
        val xs2 = repository.findAll()
        assert(xs2.size >= 8)
    }

    test("findByConcept") {
        val xs       = TestUtils.create(1, 1, 1, 1, true)
        val x        = xs.head
        val obs      = x.getObservations().asScala.head
        val xs2      = repository.findByConcept(obs.getConcept(), includeAncillaryData = true)
        assertEquals(xs2.size, 1)
        val expected = Annotation
            .from(obs, true)
            .removeForeignKeys()
        val obtained = xs2.head.removeForeignKeys()
//        println("OBTAINED: " + obtained.stringify)
//        println("EXPECTED: " + expected.stringify)
        assertEquals(expected, obtained)
    }

    test("findByConceptWithImages") {
        val xs  = TestUtils.create(3, 1, 0, 1, true) ++ TestUtils.create(3, 1)
        val obs = xs.head.getObservations().asScala.head
        val ys  = repository.findByConceptWithImages(obs.getConcept())
        assertEquals(ys.size, 1)
    }

    test("findByConcurrentRequest") {
        val xs    = TestUtils.create(8, 1) ++ TestUtils.create(8, 1)
        val uuids = xs.map(im => im.getVideoReferenceUuid()).distinct
        val ts    = xs.map(im => im.getRecordedTimestamp()).sortBy(_.toEpochMilli())
        val ts0   = ts.head.minus(Duration.ofSeconds(1))
        val ts1   = ts.last.plus(Duration.ofSeconds(1))
        val cr    = ConcurrentRequest(ts0, ts1, uuids)
        val ys    = repository.findByConcurrentRequest(cr)
        assertEquals(ys.size, 16)
    }

    test("findByLinkNameAndLinkValue") {
        val xs   = TestUtils.create(2, 1, 1)
        val obs  = xs.head.getObservations().asScala.head
        val anno = obs.getAssociations().asScala.head
        val xs2  = repository.findByLinkNameAndLinkValue(anno.getLinkName(), anno.getLinkValue())
        assertEquals(xs2.size, 1)
    }

    test("findByMultiRequest") {
        val xs = TestUtils.create(8, 1, 1, 1, true)
        val mr = MultiRequest(Seq(xs.head.getVideoReferenceUuid()))
        val ys = repository.findByMultiRequest(mr, includeAncillaryData = true)
        assertEquals(ys.size, 8)
        for (y <- ys) {
            assert(y.ancillaryData.isDefined)
            assertEquals(y.imageReferences.size, 1)
            assertEquals(y.associations.size, 1)
        }
    }

    test("findByQueryConstraint") {

        val seed = TestUtils
            .build(50, 1, 1, 1, true)
            .map(im => {
                val os = im.getObservations().asScala
                os.foreach(o => {
                    o.setGroup("group-foo")
                    o.setObserver("observer-foo")
                    o.setActivity("activity-foo")
                })
                im
            })
        val xs   = TestUtils.create(seed)
        val ys   = TestUtils.create(50, 1, 1, 1, true)
        val x    = xs.head
        val o    = x.getObservations().asScala.head
        val a    = o.getAssociations().asScala.head
        val d    = x.getAncillaryDatum()

        val vru1 = xs.head.getVideoReferenceUuid()
        val vru2 = ys.head.getVideoReferenceUuid()
        val qc1  = new QueryConstraints(videoReferenceUuids = Seq(vru1, vru2), data = Some(true))
        val o1   = repository.findByQueryConstraint(qc1)
        assertEquals(o1.size, xs.size + ys.size)
        for a <- o1
        do
//            println(a.stringify)
            assert(a.ancillaryData.isDefined)
            assertEquals(a.imageReferences.size, 1)
            assertEquals(a.associations.size, 1)

        val qc2 = new QueryConstraints(videoReferenceUuids = Seq(vru1))
        val o2  = repository.findByQueryConstraint(qc2)
        assertEquals(o2.size, xs.size)

        val qc3 = qc2.copy(concepts = Seq(o.getConcept()))
        val o3  = repository.findByQueryConstraint(qc3)
        assertEquals(o3.size, 1)

        val qc4 = qc2.copy(observers = Seq(o.getObserver()))
        val o4  = repository.findByQueryConstraint(qc4)
        assertEquals(o4.size, xs.size)

        val qc5 = qc3.copy(observers = Seq(o.getObserver()))
        val o5  = repository.findByQueryConstraint(qc5)
        assertEquals(o5.size, 1)

        val qc6 = qc5.copy(groups = Seq(o.getGroup()))
        val o6  = repository.findByQueryConstraint(qc6)
        assertEquals(o6.size, 1)

        val qc7 = qc6.copy(activities = Seq(o.getActivity()))
        val o7  = repository.findByQueryConstraint(qc7)
        assertEquals(o7.size, 1)

        val qc8 = qc7.copy(
            minDepth = Some(d.getDepthMeters() - 1),
            maxDepth = Some(d.getDepthMeters() + 1)
        )
        val o8  = repository.findByQueryConstraint(qc8)
        assertEquals(o8.size, 1)

        val qc9 = qc8.copy(minLat = Some(d.getLatitude() - 1), maxLat = Some(d.getLatitude() + 1))
        val o9  = repository.findByQueryConstraint(qc9)
        assertEquals(o9.size, 1)

        val qc10 =
            qc9.copy(minLon = Some(d.getLongitude() - 1), maxLon = Some(d.getLongitude() + 1))
        val o10  = repository.findByQueryConstraint(qc10)
        assertEquals(o10.size, 1)

        val qc11 = qc10.copy(
            minTimestamp = Some(x.getRecordedTimestamp().minusSeconds(1)),
            maxTimestamp = Some(x.getRecordedTimestamp().plusSeconds(1))
        )
        val o11  = repository.findByQueryConstraint(qc11)
        assertEquals(o11.size, 1)

        val qc12 = qc11.copy(linkName = Some(a.getLinkName()), linkValue = Some(a.getLinkValue()))
        val o12  = repository.findByQueryConstraint(qc12)
        assertEquals(o12.size, 1)

    }

    test("findByToConceptWithImages") {
        val xs   = TestUtils.create(4, 1, 1)
        val obs  = xs.head.getObservations().asScala.head
        val anno = obs.getAssociations().asScala.head
        val xs2  = repository.findByToConceptWithImages(anno.getToConcept())
        assertEquals(xs2.size, 1)
    }

    test("findByVideoReferenceUuid") {
        val xs  = TestUtils.create(8, 1, 1, 1, true)
        val x   = xs.head
        val xs2 = repository.findByVideoReferenceUuid(
            x.getVideoReferenceUuid(),
            includeAncillaryData = true
        )
        assertEquals(xs2.size, xs.size)
        for (x <- xs2) {
            assert(x.ancillaryData.isDefined)
            assertEquals(x.imageReferences.size, 1)
            assertEquals(x.associations.size, 1)
        }
    }

    test("findByVideoReferenceUuidAndTimestamps") {
        val xs  = TestUtils.create(8, 1)
        val x   = xs.head
        val ts  = xs.map(im => im.getRecordedTimestamp()).sortBy(_.toEpochMilli())
        val ts0 = ts.head.minus(Duration.ofSeconds(1))
        val ts1 = ts(2).plus(Duration.ofSeconds(1))

        val expected = xs.filter(im => {
            val t = im.getRecordedTimestamp()
            im.getVideoReferenceUuid() == x.getVideoReferenceUuid() &&
            t.isAfter(ts0) && t.isBefore(ts1)
        })

        val obtained =
            repository.findByVideoReferenceUuidAndTimestamps(x.getVideoReferenceUuid(), ts0, ts1)
//        println("-------- " + obtained)
        assertEquals(obtained.size, expected.size)
    }

    test("findGeographicRangeByQueryConstraint") {
        val xs  = TestUtils.create(40, 1, 0, 0, true)
        val qc  = new QueryConstraints(videoReferenceUuids = Seq(xs.head.getVideoReferenceUuid()))
        val opt = repository.findGeographicRangeByQueryConstraint(qc)
        assert(opt.isDefined)
        val g   = opt.get

        val lats   = xs.map(im => im.getAncillaryDatum().getLatitude())
        val lons   = xs.map(im => im.getAncillaryDatum().getLongitude())
        val depths = xs.map(im => im.getAncillaryDatum().getDepthMeters())
        assertEqualsDouble(g.minLatitude, lats.min, 0.0001)
        assertEqualsDouble(g.maxLatitude, lats.max, 0.0001)
        assertEqualsDouble(g.minLongitude, lons.min, 0.0001)
        assertEqualsDouble(g.maxLongitude, lons.max, 0.0001)
        assertEqualsDouble(g.minDepthMeters, depths.min.doubleValue(), 0.0001)
        assertEqualsDouble(g.maxDepthMeters, depths.max.doubleValue(), 0.0001)
    }

    test("findImagedMomentUuidsByConceptWithImages") {
        val xs  = TestUtils.create(4, 1, 0, 1)
        val obs = xs.head.getObservations().asScala.head
        val xs2 = repository.findImagedMomentUuidsByConceptWithImages(obs.getConcept())
        assertEquals(xs2.size, 1)
    }

    test("findImagedMomentUuidsByToConceptWithImages") {
        val xs  = TestUtils.create(4, 1, 1, 1)
        val x   = xs.head
        val a   = x.getObservations().asScala.head.getAssociations().asScala.head
        val xs2 = repository.findImagedMomentUuidsByToConceptWithImages(a.getToConcept())
        assertEquals(xs2.size, 1)
    }

    test("findImagesByVideoReferenceUuid") {
        val xs  = TestUtils.create(8, 1)
        val x   = xs.head
        val xs2 = repository.findImagesByVideoReferenceUuid(x.getVideoReferenceUuid())
        assertEquals(xs2.size, 0)

        val ys  = TestUtils.create(8, 1, 1, 2)
        val y   = ys.head
        val ys2 = repository.findImagesByVideoReferenceUuid(y.getVideoReferenceUuid())
        assertEquals(ys2.size, 16)
    }

}
