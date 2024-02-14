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

package org.mbari.annosaurus.repository.jpa

import jakarta.persistence.EntityManager
import java.util.UUID
import org.mbari.vcr4j.time.Timecode
import java.time.Duration
import java.time.Instant
import junit.framework.Test
import org.mbari.annosaurus.controllers.TestUtils
import junit.framework.Assert
import org.mbari.annosaurus.AssertUtils
import scala.jdk.CollectionConverters.*
import org.mbari.annosaurus.domain.WindowRequest

trait ImagedMomentDAOSuite extends BaseDAOSuite {

    given JPADAOFactory = daoFactory

    test("create w/ manual transaction") {
        val em  = daoFactory.entityManagerFactory.createEntityManager()
        val dao = new ImagedMomentDAOImpl(em)
        val im  = dao.newPersistentObject(
            UUID.randomUUID(),
            Some(Timecode("01:02:03:04")),
            Some(Duration.ofSeconds(10)),
            Some(Instant.now)
        )
        val t   = em.getTransaction()
        t.begin()
        dao.create(im)
        t.commit()
        dao.close()
        assert(im.getUuid() != null)

    }

    test("create") {
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val im                         = dao.newPersistentObject(
            UUID.randomUUID(),
            Some(Timecode("01:02:03:04")),
            Some(Duration.ofSeconds(10)),
            Some(Instant.now)
        )
        run(() => dao.create(im))

        dao.close()
        assert(im.getUuid() != null)

    }

    test("Create 2") {
        val im                         = TestUtils.build(1, 2, 2, 2, true).head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        run(() => dao.create(im))
        dao.close()
        assert(im.getUuid() != null)
    }

    test("update") {
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val im0                        = TestUtils.create(1).head
        val d                          = Duration.ofSeconds(1000)
        im0.setElapsedTime(d)
        run(() => dao.update(im0))
        val opt                        = run(() => dao.findByUUID(im0.getUuid()))
        assert(opt.isDefined)
        assertEquals(d, opt.get.getElapsedTime())
    }

    test("delete") {
        val im0                        = TestUtils.create(1).head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val opt0                       = run(() => dao.findByUUID(im0.getUuid()))
        assert(opt0.isDefined)
        run(() => {
            dao.findByUUID(im0.getUuid()) match {
                case Some(x) => dao.delete(x)
                case None    => fail("Could not find imaged moment")
            }
        })
        val opt1                       = run(() => dao.findByUUID(im0.getUuid()))
        assert(opt1.isEmpty)
    }

    test("deleteByUUID") {
        val im0                        = TestUtils.create(1).head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val opt0                       = run(() => dao.findByUUID(im0.getUuid()))
        assert(opt0.isDefined)
        run(() => dao.deleteByUUID(im0.getUuid()))
        val opt1                       = run(() => dao.findByUUID(im0.getUuid()))
        assert(opt1.isEmpty)
    }

    test("findByUUID") {
        val im0                        = TestUtils.create(1).head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val opt0                       = run(() => dao.findByUUID(im0.getUuid()))
        assert(opt0.isDefined)
        AssertUtils.assertSameImagedMoment(opt0.get, im0)
    }

    test("findAll") {
        val im0                        = TestUtils.create(1).head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val xs                         = run(() => dao.findAll())
        assert(xs.size >= 1)
    }

    test("findBetweenUpdatedDates") {
        val im0                        = TestUtils.create(1).head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val dt                         = Duration.ofSeconds(1)
        val t0                         = im0.getLastUpdatedTime().toInstant().minus(dt)
        val t1                         = im0.getLastUpdatedTime().toInstant().plus(dt)
        val xs                         = run(() => dao.findBetweenUpdatedDates(t0, t1))
        assert(xs.size >= 1)
    }

    test("streamBetweenUpdatedDates") {}

    test("streamByVideoReferenceUUIDAndTimestamps") {}
    test("streamVideoReferenceUuidsBetweenUpdateDates") {}

    test("countAll") {
        val im0                        = TestUtils.create(1).head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val n                          = run(() => dao.countAll())
        assert(n >= 1)
    }
    test("countWithImages") {
        val im0                        = TestUtils.create(1, 0, 0, 1).head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val n                          = run(() => dao.countWithImages())
        assert(n >= 1)
    }

    test("findWithImages") {
        val im0                        = TestUtils.create(1, 0, 0, 1).head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val xs                         = run(() => dao.findWithImages())
        assert(xs.size >= 1)
        xs.filter(_.getUuid() == im0.getUuid())
            .foreach(x => AssertUtils.assertSameImagedMoment(x, im0))
    }

    test("countByLinkName") {
        val im0                        = TestUtils.create(1, 1, 1, 1).head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val linkName                   =
            im0.getObservations().asScala.head.getAssociations().asScala.head.getLinkName()
        val n                          = run(() => dao.countByLinkName(linkName))
        assert(n >= 1)
    }

    test("findByLinkName") {
        val im0                        = TestUtils.create(1, 1, 1, 1).head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val linkName                   =
            im0.getObservations().asScala.head.getAssociations().asScala.head.getLinkName()
        val xs                         = run(() => dao.findByLinkName(linkName))
        assert(xs.size == 1)
        AssertUtils.assertSameImagedMoment(xs.head, im0)

    }
    test("countByConcept") {
        val im0                        = TestUtils.create(1, 1, 1, 1).head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val concept                    = im0.getObservations().asScala.head.getConcept()
        val n                          = run(() => dao.countByConcept(concept))
        assert(n == 1)
    }
    test("findByConcept") {
        val im0                        = TestUtils.create(1, 1, 1, 1).head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val concept                    = im0.getObservations().asScala.head.getConcept()
        val xs                         = run(() => dao.findByConcept(concept))
        assert(xs.size == 1)
        AssertUtils.assertSameImagedMoment(xs.head, im0)
    }
    test("streamByConcept") {}
    test("countByConceptWithImages") {
        val im0                        = TestUtils.create(1, 1, 1, 1).head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val concept                    = im0.getObservations().asScala.head.getConcept()
        val n                          = run(() => dao.countByConceptWithImages(concept))
        assert(n == 1)
    }

    test("countModifiedBeforeDate") {
        val xs                         = TestUtils.create(10, 1)
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val t                          = xs.head.getLastUpdatedTime().toInstant().plus(Duration.ofSeconds(1))
        val n                          = run(() => dao.countModifiedBeforeDate(xs.head.getVideoReferenceUuid(), t))
        assert(n >= 1)
    }

    test("findByConceptWithImages") {
        val im0                        = TestUtils.create(1, 1, 1, 1).head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val concept                    = im0.getObservations().asScala.head.getConcept()
        val xs                         = run(() => dao.findByConceptWithImages(concept))
        assert(xs.size == 1)
        AssertUtils.assertSameImagedMoment(xs.head, im0)
    }

    test("countBetweenUpdatedDates") {
        val im0                        = TestUtils.create(1, 1, 1, 1).head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val dt                         = Duration.ofSeconds(1)
        val t0                         = im0.getLastUpdatedTime().toInstant().minus(dt)
        val t1                         = im0.getLastUpdatedTime().toInstant().plus(dt)
        val n                          = run(() => dao.countBetweenUpdatedDates(t0, t1))
        assert(n >= 1)
    }

    test("countAllByVideoReferenceUuids") {
        val xs                         = TestUtils.create(1)
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val m                          = run(() => dao.countAllByVideoReferenceUuids())
        assert(m.size >= 1)
    }

    test("findAllVideoReferenceUUIDs") {
        val xs                         = TestUtils.create(1)
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val m                          = run(() => dao.findAllVideoReferenceUUIDs())
        assert(m.size >= 1)
        assert(m.find(_ == xs.head.getVideoReferenceUuid()).isDefined)
    }

    test("findByVideoReferenceUUID") {
        val xs                         = TestUtils.create(1)
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val m                          = run(() => dao.findByVideoReferenceUUID(xs.head.getVideoReferenceUuid()))
        assert(m.size >= 1)
        AssertUtils.assertSameImagedMoment(m.head, xs.head)
    }

    test("streamByVideoReferenceUUID") {}
    test("countByVideoReferenceUUID") {
        val xs                         = TestUtils.create(10)
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val m                          = run(() => dao.countByVideoReferenceUUID(xs.head.getVideoReferenceUuid()))
        assertEquals(m, 10)
    }

    test("countByVideoReferenceUUIDWithImages") {
        val xs                         = TestUtils.create(10, 0, 0, 1)
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val m                          = run(() => dao.countByVideoReferenceUUIDWithImages(xs.head.getVideoReferenceUuid()))
        assertEquals(m, 10)
    }

    test("findWithImageReferences") {
        val xs                         = TestUtils.create(1, 0, 0, 3)
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val m                          = run(() => dao.findWithImageReferences(xs.head.getVideoReferenceUuid()))
        assert(m.size >= 1)
        AssertUtils.assertSameImagedMoment(m.head, xs.head)
    }

    test("findByImageReferenceUUID") {
        val xs                         = TestUtils.create(1, 0, 0, 1)
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val imageRefUuid               = xs.head.getImageReferences().asScala.head.getUuid()
        val opt                        = run(() => dao.findByImageReferenceUUID(imageRefUuid))
        assert(opt.isDefined)
        AssertUtils.assertSameImagedMoment(opt.get, xs.head)
    }

    test("findByVideoReferenceUUIDAndTimecode") {
        val xs                         = TestUtils.create(1)
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val im                         = xs.head
        im.setTimecode(Timecode("01:02:03:04"))
        run(() => dao.update(im))
        val t                          = im.getTimecode()
        val opt                        = run(() => dao.findByVideoReferenceUUIDAndTimecode(im.getVideoReferenceUuid(), t))
        assert(opt.isDefined)
        AssertUtils.assertSameImagedMoment(opt.get, im)
    }

    test("findByVideoReferenceUUIDAndRecordedDate") {
        val xs                         = TestUtils.create(1)
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val t                          = xs.head.getRecordedTimestamp()
        val opt                        = run(() =>
            dao.findByVideoReferenceUUIDAndRecordedDate(xs.head.getVideoReferenceUuid(), t)
        )
        assert(opt.isDefined)
        AssertUtils.assertSameImagedMoment(opt.get, xs.head)
    }

    test("findByVideoReferenceUUIDAndElapsedTime") {
        val xs                         = TestUtils.create(1)
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val t                          = xs.head.getElapsedTime()
        val opt                        = run(() =>
            dao.findByVideoReferenceUUIDAndElapsedTime(xs.head.getVideoReferenceUuid(), t)
        )
        assert(opt.isDefined)
        AssertUtils.assertSameImagedMoment(opt.get, xs.head)
    }

    test("findByWindowRequest") {
        val xs                         = TestUtils.create(1)
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val t0                         = xs.head.getRecordedTimestamp().minus(Duration.ofSeconds(1))
        val t1                         = xs.head.getRecordedTimestamp().plus(Duration.ofSeconds(1))
        val r                          = WindowRequest(Seq(xs.head.getVideoReferenceUuid()), xs.head.getUuid(), 2000L)
        val ys                         = run(() => dao.findByWindowRequest(r))
        assert(ys.size == 1)
        AssertUtils.assertSameImagedMoment(ys.head, xs.head)
    }
    test("findByVideoReferenceUUIDAndIndex") {
        val im0 = TestUtils.create(1).head

        // Make sure it has a timecode
        im0.setTimecode(Timecode("01:02:03:04"))
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        run(() => dao.update(im0))

        val timecode = im0.getTimecode()
        val im1      = run(() =>
            dao.findByVideoReferenceUUIDAndIndex(im0.getVideoReferenceUuid(), Some(timecode))
        )
        assert(im1.isDefined)
        AssertUtils.assertSameImagedMoment(im1.get, im0)

        val elapsedTime = im0.getElapsedTime()
        val im2         = run(() =>
            dao.findByVideoReferenceUUIDAndIndex(
                im0.getVideoReferenceUuid(),
                None,
                Some(elapsedTime)
            )
        )
        assert(im2.isDefined)
        AssertUtils.assertSameImagedMoment(im2.get, im0)

        val recordedTimestamp = im0.getRecordedTimestamp()
        val im3               = run(() =>
            dao.findByVideoReferenceUUIDAndIndex(
                im0.getVideoReferenceUuid(),
                None,
                None,
                Some(recordedTimestamp)
            )
        )
        assert(im3.isDefined)
        AssertUtils.assertSameImagedMoment(im3.get, im0)

    }

    test("findByObservationUUID") {
        val im0                        = TestUtils.create(1, 1, 1, 1).head
        val o                          = im0.getObservations().asScala.head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val im1                        = run(() => dao.findByObservationUUID(o.getUuid()))
        assert(im1.isDefined)
        AssertUtils.assertSameImagedMoment(im1.get, im0)
    }

    test("updateRecordedTimestampByObservationUuid") {
        val im0                        = TestUtils.create(1, 1, 1, 1).head
        val o                          = im0.getObservations().asScala.head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val t                          = Instant.now()
        val b                          = run(() => dao.updateRecordedTimestampByObservationUuid(o.getUuid(), t))
        assert(b)
        val im1                        = run(() => dao.findByObservationUUID(o.getUuid()))
        assert(im1.isDefined)
        assertEquals(t, im1.get.getRecordedTimestamp())
    }

    test("deleteByVideoReferenceUUUID") {
        val xs                         = TestUtils.create(2, 1, 1, 1)
        val videoReferenceUuid         = xs.head.getVideoReferenceUuid()
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val b                          = run(() => dao.deleteByVideoReferenceUUUID(videoReferenceUuid))
//        println(b)
        assert(b == 2)
        val im1                        = run(() => dao.findByVideoReferenceUUID(videoReferenceUuid))
        assert(im1.isEmpty)
    }

    test("deleteIfEmpty") {

        // Should not delete because it has an observation
        val xs                         = TestUtils.create(1, 1, 1, 1)
        val im0                        = xs.head
        val o                          = im0.getObservations().asScala.head
        val a                          = o.getAssociations().asScala.head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        run(() => dao.deleteIfEmpty(im0))
        val im1                        = run(() => dao.findByUUID(im0.getUuid()))
        assert(im1.isDefined)

        // Should delete because it has no observations
        val im3 = TestUtils.create(1).head
        run(() => dao.deleteIfEmpty(im3))
        val im4 = run(() => dao.findByUUID(im3.getUuid()))
        assert(im4.isEmpty)

    }

    test("deleteIfEmptyByUUID") {

        // Should not delete because it has an observation
        val xs                         = TestUtils.create(1, 1, 1, 1)
        val im0                        = xs.head
        val o                          = im0.getObservations().asScala.head
        val a                          = o.getAssociations().asScala.head
        given dao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        run(() => dao.deleteIfEmptyByUUID(im0.getUuid()))
        val im1                        = run(() => dao.findByUUID(im0.getUuid()))
        assert(im1.isDefined)

        // Should delete because it has no observations
        val im3 = TestUtils.create(1).head
        run(() => dao.deleteIfEmptyByUUID(im3.getUuid()))
        val im4 = run(() => dao.findByUUID(im3.getUuid()))
        assert(im4.isEmpty)
    }

}
