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

import org.mbari.annosaurus.domain.Index
import org.mbari.annosaurus.repository.jpa.{BaseDAOSuite, JPADAOFactory}

import java.time.{Duration, Instant}

trait IndexControllerSuite extends BaseDAOSuite {

    given JPADAOFactory         = daoFactory
    private lazy val controller = new IndexController(daoFactory)

    test("findByVideoReferenceUUID") {
        val im       = TestUtils.create().head
        val xs       = exec(controller.findByVideoReferenceUUID(im.getVideoReferenceUuid))
        assert(xs.size == 1)
        val obtained = xs.head
        assertEquals(obtained.timecode.orNull, Option(im.getTimecode).map(_.toString).orNull)
        assertEquals(obtained.elapsedTime.orNull, im.getElapsedTime)
        assertEquals(obtained.recordedTimestamp.orNull, im.getRecordedTimestamp)
    }

    test("updateRecordedTimestamp") {
        val xs       = TestUtils.create(5)
        val newStart = Instant.now()
        val updated  =
            exec(controller.updateRecordedTimestamps(xs.head.getVideoReferenceUuid, newStart))
        for x <- updated
        do
            val elapsedTime       = x.elapsedTime.getOrElse(Duration.ZERO)
            val recordedTimestamp = x.recordedTimestamp.getOrElse(Instant.MIN)
            val expected          = newStart.plus(elapsedTime)
            assertEquals(recordedTimestamp, expected)
    }

    test("bulkUpdateRecordedTimestamps") {
        val xs       = TestUtils.create(5)
        val newStart = Instant.parse("1968-09-22T02:00:00Z")
        val ys       = xs.map(x =>
            Index(
                x.getVideoReferenceUuid,
                Option(x.getTimecode).map(_.toString),
                Option(x.getElapsedTime).map(_.toMillis),
                Option(newStart.plus(x.getElapsedTime))
            )
        )
        val updated  = exec(controller.bulkUpdateRecordedTimestamps(ys))
        for x <- updated
        do
            val elapsedTime       = x.elapsedTime.getOrElse(Duration.ZERO)
            val recordedTimestamp = x.recordedTimestamp.getOrElse(Instant.MIN)
            val expected          = newStart.plus(elapsedTime)
            assertEquals(recordedTimestamp, expected)
    }

    test("delete") {
        // Nothing to do. This is NOT implemented for index. Use ImagedMomentController instead
    }

    test("findAll") {
        // Nothing to do. This is NOT implemented for index. Use ImagedMomentController instead
    }

    test("findByUUID") {
        val im       = TestUtils.create().head
        val opt      = exec(controller.findByUUID(im.getUuid))
        assert(opt.isDefined)
        val obtained = opt.get
        assertEquals(obtained.timecode.orNull, Option(im.getTimecode).map(_.toString).orNull)
        assertEquals(obtained.elapsedTime.orNull, im.getElapsedTime)
        assertEquals(obtained.recordedTimestamp.orNull, im.getRecordedTimestamp)

    }

}
