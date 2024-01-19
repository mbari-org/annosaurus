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

import org.mbari.annosaurus.repository.jpa.{BaseDAOSuite, ImagedMomentDAOImpl, JPADAOFactory}

import scala.concurrent.ExecutionContext
import org.mbari.annosaurus.AssertUtils
import org.mbari.annosaurus.domain.Annotation

import scala.jdk.CollectionConverters.*
import org.mbari.annosaurus.etc.jdk.Logging.given
import org.mbari.annosaurus.domain.ConcurrentRequest

import java.time.{Duration, Instant}
import org.mbari.annosaurus.domain.MultiRequest
import org.mbari.annosaurus.etc.circe.CirceCodecs.{given, *}
import org.mbari.vcr4j.time.Timecode

import java.util.UUID
import scala.io.Source
import scala.util.{Failure, Success, Using}

trait AnnotationControllerSuite extends BaseDAOSuite {
    given JPADAOFactory    = daoFactory
    given ExecutionContext = ExecutionContext.global
    lazy val controller    = AnnotationController(daoFactory)
    private val log        = System.getLogger(getClass.getName)

    override def beforeAll(): Unit = daoFactory.beforeAll()
    override def afterAll(): Unit  = daoFactory.afterAll()

    test("findByUUID") {
        val im1 = TestUtils.create(1, 2, 3, 2, true).head
        val obs = im1.getObservations.asScala.head
        val opt = exec(controller.findByUUID(obs.getUuid))
        opt match
            case None       => fail("findByUUID returned None")
            case Some(anno) =>
                // NOTE: this anno is only 1 observations. THe source imagedMoment has two.
                // this is ok and expected. An annotation maps to a single observation!!
                val im2 = Annotation.toEntities(Seq(anno)).head
                AssertUtils.assertSameImagedMoment(im1, im2, false)
    }

    test("countByVideoReferenceUUID") {
        val xs = TestUtils.create(5, 2)
        val e  = xs.flatMap(_.getObservations.asScala)
        val n  = exec(controller.countByVideoReferenceUuid(xs.head.getVideoReferenceUuid))
        assertEquals(n, e.size)
    }

    test("findByVideoReferenceUUID") {

        val xs = TestUtils.create(2, 2, includeData = true)
        val e  = xs.flatMap(_.getObservations.asScala)
        val n  = exec(controller.findByVideoReferenceUuid(xs.head.getVideoReferenceUuid))
        assertEquals(n.size, e.size)
        assert(n.head.ancillaryData.isEmpty)

        val m = exec(
            controller.findByVideoReferenceUuid(
                xs.head.getVideoReferenceUuid,
                Some(1),
                Some(1),
                true
            )
        )
        assertEquals(m.size, 1)
        assert(m.head.ancillaryData.isDefined)
    }

    test("streamByVideoReferenceUUID") {}

    test("streamByVideoReferenceUUIDAndTimestamps") {}

    test("streamByConcurrentRequest") {}

    test("countByConcurrentRequest") {
        val dt = Duration.ofSeconds(1)

        val xs  = TestUtils.create(2, 2) ++ TestUtils.create(2, 2)
        val obs = xs.flatMap(_.getObservations.asScala)
        val ts  = xs.map(_.getRecordedTimestamp())
        val t0  = ts.min.minus(dt)
        val t1  = ts.max.plus(dt)

        val vru = xs.map(_.getVideoReferenceUuid()).distinct
        assertEquals(vru.size, 2)

        val cr0 = new ConcurrentRequest(t0, t1, Seq(vru.head))
        val n   = exec(controller.countByConcurrentRequest(cr0)).intValue()
        assertEquals(n, 4)

        val cr1 = new ConcurrentRequest(t0, t1, vru)
        val m   = exec(controller.countByConcurrentRequest(cr1)).intValue()
        assertEquals(m, 8)

        val cr3 = new ConcurrentRequest(t1, t1.plus(dt), vru)
        val p   = exec(controller.countByConcurrentRequest(cr3)).intValue()
        assertEquals(p, 0)

        // val cr1 = new ConcurrentRequest(t0, t1, Seq(a.head.getVideoReferenceUuid(), b.head.getVideoReferenceUuid()))
        // val m = exec(controller.countByConcurrentRequest(cr1)).intValue()
        // assertEquals(m, ea.size + eb.size)
    }

    test("streamByMultiRequest") {}

    test("countByMultiRequest") {
        val xs  = TestUtils.create(2, 2) ++ TestUtils.create(2, 2)
        val obs = xs.flatMap(_.getObservations.asScala)
        val vru = xs.map(_.getVideoReferenceUuid()).distinct

        val mr = new MultiRequest(vru)
        val n  = exec(controller.countByMultiRequest(mr)).intValue()
        assertEquals(n, obs.size)

        val mr2 = new MultiRequest(Seq(vru.head))
        val m   = exec(controller.countByMultiRequest(mr2)).intValue()
        assertEquals(m, obs.size / 2)
    }

    test("findByImageReferenceUUID") {
        val xs       = TestUtils.create(1, 1, 0, 1).head
        val obs      = xs.getObservations.asScala.head
        val imr      = xs.getImageReferences().asScala.head
        val obtained = exec(controller.findByImageReferenceUUID(imr.getUuid)).head
        val expected = Annotation.from(obs, true)
        assertEquals(obtained, expected)
    }

    test("create") {
        val im       = TestUtils.build(1, 1).head
        val anno     = Annotation.from(im.getObservations.asScala.head, true)
        val obtained = exec(
            controller.create(
                anno.videoReferenceUuid.get,
                anno.concept.get,
                anno.observer.get,
                anno.observationTimestamp.get,
                anno.validTimecode,
                anno.elapsedTime,
                anno.recordedTimestamp,
                anno.duration,
                anno.group,
                anno.activity
            )
        )

        // Our source anno doesn't have UUIDS set, we remove those to compare the rest of the values
        assert(obtained.observationUuid.isDefined)
        assert(obtained.imagedMomentUuid.isDefined)
        val corrected = obtained.copy(observationUuid = None, imagedMomentUuid = None)
        assertEquals(corrected, anno)
    }

    test("create by recordedTimestamp") {
        val recordedDate  = Instant.now()
        val concept = "Nanomia bijuga"
        val observer = "brian"
        val a = exec(
            controller
                .create(UUID.randomUUID(), concept, observer, recordedDate = Some(recordedDate))
        )
        assert(a.observationUuid.isDefined)
        assertEquals(a.recordedTimestamp, Some(recordedDate))
        assertEquals(a.concept, Some(concept))
        assertEquals(a.observer, Some(observer))
    }


    test("bulkCreate a single annotation") {
        // test minimal
        val xs0       = TestUtils.build(1, 1)
        val annos0    = Annotation.fromImagedMoment(xs0.head, true)
        assertEquals(annos0.size, 1)
        val n         = exec(controller.bulkCreate(annos0))
        assertEquals(n.size, xs0.size)
        val obtained  = n.head
        assert(obtained.imagedMomentUuid.isDefined)
        assert(obtained.observationUuid.isDefined)
        val corrected = obtained.copy(imagedMomentUuid = None, observationUuid = None)
//        log.atWarn.log(n.head.stringify)
        assertEquals(corrected, annos0.head)
    }

    test("bulkCreate a 200 annotations") {
        import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
        val url = this.getClass.getResource("/json/annotation_full_dive.json").toURI
        Using(Source.fromFile(url)) { source =>
            val json = source.getLines().mkString("\n")
            val annos = json.reify[Array[Annotation]]
                .getOrElse(throw new RuntimeException("Failed to parse json"))
                .take(200)
            assert(annos.nonEmpty)
            val n = exec(controller.bulkCreate(annos), Duration.ofSeconds(120))
            assertEquals(n.size, annos.size)

        } match {
            case Failure(e) => fail(e.toString)
            case Success(_) =>
            // trust but verify
        }
    }

    test("bulkCreate a 200 annotations with imageReferences") {
        import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
        val url = this.getClass.getResource("/json/annotation_full_dive.json").toURI
        Using(Source.fromFile(url)) { source =>
            val json = source.getLines().mkString("\n")
            val annos = json.reify[Array[Annotation]]
                .getOrElse(throw new RuntimeException("Failed to parse json"))
                .filter(_.imageReferences.nonEmpty)
                .take(200)
            assert(annos.nonEmpty)
            val n = exec(controller.bulkCreate(annos), Duration.ofSeconds(120))
            assertEquals(n.size, annos.size)

        } match {
            case Failure(e) => fail(e.toString)
            case Success(_) =>
            // trust but verify
        }
    }

    test("update") {
        val im0 = TestUtils.create(1, 1).head
        val obs0 = im0.getObservations.asScala.head
        val im1 = TestUtils.build(1, 1).head
        im1.setTimecode(im0.getTimecode)
        val obs1 = im1.getObservations.asScala.head

        val opt = exec(controller.update(
            obs0.getUuid,
            Option(im1.getVideoReferenceUuid),
            Option(obs1.getConcept),
            Option(obs1.getObserver),
            obs1.getObservationTimestamp,
            Option(im1.getTimecode),
            Option(im1.getElapsedTime),
            Option(im1.getRecordedTimestamp),
            Option(obs1.getDuration),
            Option(obs1.getGroup),
            Option(obs1.getActivity)
        ))

        opt match
            case None       => fail("update returned None")
            case Some(anno) =>

                val im2 = Annotation.toEntities(Seq(anno)).head

                // We need to set the UUIDs to compare the rest of the values
                im1.setUuid(anno.imagedMomentUuid.get)
                obs1.setUuid(anno.observationUuid.get)
                val a1 = Annotation.from(obs1, true)
                val a0 = Annotation.from(obs0, true)
                log.atDebug.log("ORIGINAL:   " + a0.stringify)
                log.atDebug.log("NEW VALUES: " + a1.stringify)
                log.atDebug.log("UPDATED:    " + anno.stringify)
                AssertUtils.assertSameImagedMoment(im1, im2, false)

    }

    test("bulkUpdate") {
        val xs = TestUtils.create(2, 2) // tested with upt to 30, 10. 100 by 10 takes more than 30sec
        val videoReferenceUuid = xs.head.getVideoReferenceUuid
        val elapsedTime = Duration.ofSeconds(1234)
        val annos = xs.flatMap(x => Annotation.fromImagedMoment(x, false))
            .map(x => x.copy(elapsedTimeMillis = Some(elapsedTime.toMillis),
                timecode = Some("01:02:03:04"),
                recordedTimestamp = Some(Instant.parse("2020-01-01T01:02:03Z")),
                videoReferenceUuid = Some(videoReferenceUuid),
                concept = Some("bulkUpdateTest")))
        val n = exec(controller.bulkUpdate(annos), Duration.ofSeconds(60))
        assertEquals(n.size, annos.size)
        n.foreach { anno =>
            log.atWarn.log(anno.stringify)
            assert(anno.concept.isDefined)
            assert(anno.elapsedTime.isDefined)
            assert(anno.videoReferenceUuid.isDefined)
            assertEquals(anno.concept.get, "bulkUpdateTest")
            assertEquals(anno.videoReferenceUuid.get, videoReferenceUuid)
            assertEquals(anno.elapsedTime.get, elapsedTime)
        }


    }

    test("bulkUpdateRecordedTimestampOnly") {
        val xs = TestUtils.build(4, 1).zipWithIndex
        val annos0 = for
            (im, i) <- xs
        yield
            im.setTimecode(new Timecode(s"00:00:0${i}:00"))
            Annotation.from(im.getObservations.iterator().next(), false)

        val annos1 = exec(controller.bulkCreate(annos0))

        val annos2 = annos1.map { anno =>
            anno.copy(recordedTimestamp = Some(Instant.ofEpochMilli(math.random().longValue())))
        }

        val annos3 = exec(controller.bulkUpdateRecordedTimestampOnly(annos2))

        assertEquals(annos3.size, annos2.size)

        val obtained = annos3.toList
            .sortBy(_.observationUuid)
        val expected = annos2.toList
            .sortBy(_.observationUuid)
        obtained.zip(expected).foreach { (a, b) =>
            assertEquals(a, b)
        }

    }

    test("delete") {
        val im = TestUtils.create(1, 2).head
        val obs0 = im.getObservations.asScala.head
        val obs1 = im.getObservations.asScala.last
        val ok = exec(controller.delete(obs0.getUuid))
        assert(ok)
        val opt2 = exec(controller.findByUUID(obs0.getUuid))
        assert(opt2.isEmpty)
        val opt3 = exec(controller.findByUUID(obs1.getUuid))
        assert(opt3.isDefined)
    }

    test("delete single observation") {
        // The parent ImagedMoment should be deleted as well if it has no other observations
        val im = TestUtils.create(1, 1).head
        val obs = im.getObservations.asScala.head
        val ok = exec(controller.delete(obs.getUuid))
        assert(ok)
        val opt2 = exec(controller.findByUUID(obs.getUuid))
        assert(opt2.isEmpty)

        given imDao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val imOpt = run(() => imDao.findByUUID(im.getUuid))
        assert(imOpt.isEmpty)
        imDao.close()
    }

}
