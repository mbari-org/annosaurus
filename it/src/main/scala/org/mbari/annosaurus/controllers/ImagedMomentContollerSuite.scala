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

import org.mbari.annosaurus.repository.jpa.BaseDAOSuite
import org.mbari.annosaurus.repository.jpa.JPADAOFactory

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.*
import org.mbari.annosaurus.etc.jdk.Logging.given
import org.mbari.annosaurus.AssertUtils

import java.time.Duration
import org.mbari.annosaurus.domain.WindowRequest

import java.time.Instant
import java.util.UUID
import org.mbari.annosaurus.repository.jpa.ImagedMomentDAOImpl
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.repository.jpa.entity.ImagedMomentEntity

import java.net.URI
import scala.util.Random

trait ImagedMomentContollerSuite extends BaseDAOSuite {

    given JPADAOFactory    = daoFactory
    given ExecutionContext = ExecutionContext.global
    lazy val controller    = ImagedMomentController(daoFactory)
    private val log        = System.getLogger(getClass.getName)

    test("findAll") {
        val xs       = TestUtils.create(2)
        val obtained = exec(controller.findAll())
        assert(obtained.size >= 2)
    }

    test("countAll") {
        val xs = TestUtils.create(2)
        val n  = exec(controller.countAll())
        assert(n >= 2)
    }

    test("countByVideoReferenceUUIDWithImages") {
        val im0 = TestUtils.create(1).head
        val n   = exec(controller.countByVideoReferenceUUIDWithImages(im0.getVideoReferenceUuid))
        assertEquals(n, 0)

        val im1 = TestUtils.create(1, 0, 0, 1).head
        val n2  = exec(controller.countByVideoReferenceUUIDWithImages(im1.getVideoReferenceUuid))
        assertEquals(n2, 1)

    }

    test("findWithImages") {
        val im1 = TestUtils.create(1, 0, 0, 1).head
        val im2 = exec(controller.findWithImages())
        im2.find(im => im.videoReferenceUuid == im1.getVideoReferenceUuid()) match
            case None        => fail("Could not find imagedMoment with images")
            case Some(value) =>
                AssertUtils.assertSameImagedMoment(value.toEntity, im1)
    }

    test("countWithImages") {
        val im1 = TestUtils.create(1, 0, 0, 1).head
        val n   = exec(controller.countWithImages())
        assert(n >= 1)
    }

    test("findByLinkName") {
        val im1      = TestUtils.create(1, 1, 1, 1).head
        val linkName = im1
            .getObservations()
            .iterator()
            .next()
            .getAssociations()
            .iterator()
            .next()
            .getLinkName()
        val im2      = exec(controller.findByLinkName(linkName))
        assertEquals(im2.size, 1)
        AssertUtils.assertSameImagedMoment(im1, im2.head.toEntity)
    }

    test("countByLinkName") {
        val im1      = TestUtils.create(1, 1, 1, 1).head
        val linkName = im1
            .getObservations()
            .iterator()
            .next()
            .getAssociations()
            .iterator()
            .next()
            .getLinkName()
        val n        = exec(controller.countByLinkName(linkName))
        assertEquals(n, 1)
    }

    test("findAllVideoReferenceUUIDs") {
        val im1 = TestUtils.create(1).head
        val xs  = exec(controller.findAllVideoReferenceUUIDs()).toSeq
        assert(xs.contains(im1.getVideoReferenceUuid()))
    }

    test("findByVideoReferenceUUID") {
        val im1 = TestUtils.create(1).head
        val im2 = exec(controller.findByVideoReferenceUUID(im1.getVideoReferenceUuid()))
        assertEquals(im2.size, 1)
        AssertUtils.assertSameImagedMoment(im1, im2.head.toEntity)
    }

    test("findByImageReferenceUUID") {
        val im1 = TestUtils.create(1, 1, 1, 1).head
        val ir1 = im1.getImageReferences().iterator().next()
        val im2 = exec(controller.findByImageReferenceUUID(ir1.getUuid()))
        assertEquals(im2.size, 1)
        AssertUtils.assertSameImagedMoment(im1, im2.head.toEntity)
    }

    test("findByObservationUUID") {
        val im1  = TestUtils.create(1, 1).head
        val obs1 = im1.getObservations().iterator().next()
        val im2  = exec(controller.findByObservationUUID(obs1.getUuid()))
        assertEquals(im2.size, 1)
        AssertUtils.assertSameImagedMoment(im1, im2.head.toEntity)
    }

    test("findWithImageReferences") {
        val im1 = TestUtils.create(1).head
        val im2 = exec(controller.findWithImageReferences(im1.getVideoReferenceUuid()))
        im2.find(im => im.videoReferenceUuid == im1.getVideoReferenceUuid()) match
            case None        => // ok
            case Some(value) =>
                log.atError.log("Found imagedMoment with images: " + value.stringify)
                fail("Found imagedMoment without images")

        val im3 = TestUtils.create(1, 0, 0, 1).head
        val im4 = exec(controller.findWithImageReferences(im3.getVideoReferenceUuid()))
        im4.find(im => im.videoReferenceUuid == im3.getVideoReferenceUuid()) match
            case None        => fail("Could not find imagedMoment with images")
            case Some(value) =>
                AssertUtils.assertSameImagedMoment(value.toEntity, im3)

    }

    test("findBetweenUpdatedDates") {
        val im1 = TestUtils.create(1).head
        val t   = im1.getLastUpdatedTime().toInstant()
        val dt  = Duration.ofSeconds(1)
        val im2 = exec(controller.findBetweenUpdatedDates(t.minus(dt), t.plus(dt)))
        im2.find(im => im.videoReferenceUuid == im1.getVideoReferenceUuid()) match
            case None        => fail("Could not find imagedMoment updatd between dates")
            case Some(value) =>
                AssertUtils.assertSameImagedMoment(value.toEntity, im1)

    }

    test("countBetweenUpdatedDates") {
        val im1 = TestUtils.create(1).head
        val t   = im1.getLastUpdatedTime().toInstant()
        val dt  = Duration.ofSeconds(1)
        val n   = exec(controller.countBetweenUpdatedDates(t.minus(dt), t.plus(dt)))
        assert(n >= 1)
    }

    test("countAllGroupByVideoReferenceUUID") {
        val xs = TestUtils.create(4)
        val ys = TestUtils.create(3)
        val n  = exec(controller.countAllGroupByVideoReferenceUUID())
        assertEquals(n(xs.head.getVideoReferenceUuid()), xs.size)
        assertEquals(n(ys.head.getVideoReferenceUuid()), ys.size)
    }

    test("countByVideoReferenceUuid") {
        val xs = TestUtils.create(4)
        val n  = exec(controller.countByVideoReferenceUuid(xs.head.getVideoReferenceUuid()))
        assertEquals(n, xs.size)
    }

    test("findByConcept") {
        val im      = TestUtils.create(1, 1).head
        val obs     = im.getObservations().iterator().next()
        val concept = obs.getConcept()
        val im2     = exec(controller.findByConcept(concept))
        assertEquals(im2.size, 1)
        AssertUtils.assertSameImagedMoment(im, im2.head.toEntity)
    }

    test("countByConcept") {
        val im      = TestUtils.create(1, 1).head
        val obs     = im.getObservations().iterator().next()
        val concept = obs.getConcept()
        val n       = exec(controller.countByConcept(concept))
        assertEquals(n, 1)
    }

    test("findByConceptWithImages") {
        val im0 = TestUtils.create(1, 1).head
        val c   = im0.getObservations().iterator().next().getConcept()
        val im1 = exec(controller.findByConceptWithImages(c))
        assertEquals(im1.size, 0)

        val im      = TestUtils.create(1, 1, 1, 1).head
        val obs     = im.getObservations().iterator().next()
        val concept = obs.getConcept()
        val im2     = exec(controller.findByConceptWithImages(concept))
        assertEquals(im2.size, 1)
        AssertUtils.assertSameImagedMoment(im, im2.head.toEntity)
    }

    test("countModifiedBeforeDate") {
        val im = TestUtils.create(3).head
        val t  = im.getLastUpdatedTime().toInstant().plus(Duration.ofSeconds(1))
        val n  = exec(controller.countModifiedBeforeDate(im.getVideoReferenceUuid(), t))
        assertEquals(n, 3)
    }

    test("deleteByVideoReferenceUUID") {
        val xs = TestUtils.create(2)
        val n  = exec(controller.deleteByVideoReferenceUUID(xs.head.getVideoReferenceUuid()))
        assertEquals(n, 2)

        val im2 = exec(controller.findByVideoReferenceUUID(xs.head.getVideoReferenceUuid()))
        assertEquals(im2.size, 0)
    }

    test("findByWindowRequest") {
        val im  = TestUtils.create(1).head
        val t   = im.getRecordedTimestamp()
        val wr  = new WindowRequest(Seq(im.getVideoReferenceUuid()), im.getUuid(), 1000)
        val im2 = exec(controller.findByWindowRequest(wr))
        assertEquals(im2.size, 1)
        AssertUtils.assertSameImagedMoment(im, im2.head.toEntity)
    }

    test("create from params") {
        val im0 = TestUtils.build(1).head
        val im1 = exec(
            controller.create(
                im0.getVideoReferenceUuid(),
                Option(im0.getTimecode()),
                Option(im0.getRecordedTimestamp()),
                Option(im0.getElapsedTime())
            )
        )
        assert(im1.uuid.isDefined)
        im0.setUuid(im1.uuid.get)
        AssertUtils.assertSameImagedMoment(im0, im1.toEntity)
    }

    test("create from entity") {
        val im0 = TestUtils.build(1).head
        val im1 = exec(controller.create(Seq(im0))).head
        assert(im1.uuid.isDefined)
        im0.setUuid(im1.uuid.get)
        AssertUtils.assertSameImagedMoment(im0, im1.toEntity)
    }

    test("create from entity graph") {
        val im0 = TestUtils.build(1, 1, 1, 1, true).head
        val im1 = exec(controller.create(Seq(im0))).head
        log.atWarn.log("im1: " + im1.stringify)
        assert(im1.uuid.isDefined)
        im0.setUuid(im1.uuid.get)
        im0.getObservations.iterator().next().setUuid(im1.observations.head.uuid.get)

        AssertUtils.assertSameImagedMoment(im0, im1.toEntity)
    }

    test(
        "create one imagedmoment if multiple imagedmoments are created with same recordedTimestamp"
    ) {
        val im0 = TestUtils.build(1).head
        im0.setTimecode(null)
        im0.setElapsedTime(null)
        val im1 = TestUtils.build(1).head
        im1.setVideoReferenceUuid(im0.getVideoReferenceUuid)
        im1.setRecordedTimestamp(im0.getRecordedTimestamp)
        im1.setTimecode(null)
        im1.setElapsedTime(null)
        val im2 = exec(controller.create(Seq(im0)))
        val im3 = exec(controller.create(Seq(im1)))
        im2.foreach(im => assert(im.uuid.isDefined))
        im3.foreach(im => assert(im.uuid.isDefined))
        assertEquals(im2.head.uuid, im3.head.uuid)
    }

    test("create should fail when inserting the same URL more than once") {
        val videoReferenceUuid = UUID.randomUUID()
        val url                = URI.create("http://www.mbari.org/foo/image.png").toURL
        intercept[Exception] {
            for (i <- 0 until 2) {
                val source         = new ImagedMomentEntity(
                    videoReferenceUuid,
                    Instant.now().plus(Duration.ofSeconds(Random.nextInt())),
                    null,
                    null
                )
                val imageReference = TestUtils.randomImageReference()
                imageReference.setUrl(url)
                source.addImageReference(imageReference)
                exec(controller.create(Seq(source)))
            }
        }
    }

    test("create multiple") {
        val xs = TestUtils.build(2, 1)
        val ys = exec(controller.create(xs))
        assertEquals(ys.size, 2)
        val x  = xs.head
        val y  = ys.head
        x.setUuid(y.uuid.get)
        AssertUtils.assertSameImagedMoment(x, y.toEntity)
    }

    test("update") {
        val im0 = TestUtils.create(1).head
        val im1 = exec(
            controller.update(
                im0.getUuid(),
                recordedDate = Option(im0.getRecordedTimestamp().plus(Duration.ofSeconds(1)))
            )
        )
        assertEquals(
            im1.recordedTimestamp,
            Some(im0.getRecordedTimestamp().plus(Duration.ofSeconds(1)))
        )
        assertEquals(
            Option(im0.getTimecode()).map(_.toString()),
            im1.validTimecode.map(_.toString())
        )
        assertEquals(im0.getElapsedTime(), im1.elapsedTime.orNull)
    }

    test("updateRecordedTimestampByObservationUuid") {
        val im0 = TestUtils.create(1, 1).head
        val obs = im0.getObservations().iterator().next()
        val ok  =
            exec(controller.updateRecordedTimestampByObservationUuid(obs.getUuid(), Instant.EPOCH))
        assert(ok)
        val im1 = exec(controller.findByObservationUUID(obs.getUuid())).head
        assertEquals(im1.recordedTimestamp, Some(Instant.EPOCH))
    }

    test("updateRecordedTimestamps") {
        val im0 = TestUtils.create(1, 1).head
        val im1 = exec(
            controller.updateRecordedTimestamps(im0.getVideoReferenceUuid(), Instant.EPOCH)
        ).head
        assertEquals(im1.recordedTimestamp, Some(Instant.EPOCH.plus(im0.getElapsedTime())))
    }

    test("findOrCreateImagedMoment from params") {

        // create
        val vru                        = UUID.randomUUID()
        val ts                         = Instant.now()
        given dao: ImagedMomentDAOImpl = controller.daoFactory.newImagedMomentDAO()
        val im1                        = run(() =>
            ImagedMomentController.findOrCreateImagedMoment(
                dao,
                vru,
                elapsedTime = Option(Duration.ofSeconds(1)),
                recordedDate = Option(ts)
            )
        )
        assertEquals(im1.getVideoReferenceUuid(), vru)
        assertEquals(im1.getRecordedTimestamp(), ts)
        assertEquals(im1.getElapsedTime(), Duration.ofSeconds(1))

        // find
        val im2 = run(() =>
            ImagedMomentController.findOrCreateImagedMoment(
                dao,
                vru,
                elapsedTime = Option(Duration.ofSeconds(1)),
                recordedDate = Option(ts)
            )
        )
        AssertUtils.assertSameImagedMoment(im2, im1)

    }

    test("findOrCreateImagedMoment from entity") {
        // create
        val vru                        = UUID.randomUUID()
        val im0                        = TestUtils.randomImagedMoment(videoReferenceUuid = vru)
        given dao: ImagedMomentDAOImpl = controller.daoFactory.newImagedMomentDAO()
        val im1                        = run(() => ImagedMomentController.findOrCreateImagedMoment(dao, im0))
        im0.setUuid(im1.getUuid())
        AssertUtils.assertSameImagedMoment(im1, im0)

        // find
        val im2 = run(() => ImagedMomentController.findOrCreateImagedMoment(dao, im0))
        AssertUtils.assertSameImagedMoment(im2, im1)
    }

}
