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
import org.mbari.annosaurus.AssertUtils
import org.mbari.annosaurus.domain.ImagedMoment
import org.mbari.annosaurus.domain.Annotation
import scala.jdk.CollectionConverters.*
import org.mbari.annosaurus.etc.jdk.Logging.given
import org.mbari.annosaurus.domain.ConcurrentRequest
import junit.framework.Test
import java.time.Duration
import org.mbari.annosaurus.domain.MultiRequest

trait AnnotationControllerITSuite extends BaseDAOSuite {
    given JPADAOFactory    = daoFactory
    given ExecutionContext = ExecutionContext.global
    lazy val controller    = AnnotationController(daoFactory)
    private val log        = System.getLogger(getClass.getName)

    override def beforeAll(): Unit = daoFactory.beforeAll()
    override def afterAll(): Unit  = daoFactory.afterAll()

    test("findByUUID") {
        val im1 = TestUtils.create(1, 2, 3, 2, true).head
        log.atInfo.log("im1: " + im1)
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
        assertEquals(obtained, anno)
    }

    test("bulkCreate") {}

    test("update") {}

    test("bulkUpdate") {}

    test("bulkUpdateRecordedTimestampOnly") {}

    test("delete") {}

}
