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

import org.mbari.annosaurus.domain.CachedAncillaryDatum
import org.mbari.annosaurus.repository.jpa.{BaseDAOSuite, JPADAOFactory}

import java.sql.Timestamp

trait CachedAncillaryDatumControllerITSuite extends BaseDAOSuite {

    given JPADAOFactory = daoFactory

    val controller = new CachedAncillaryDatumController(daoFactory)

    test("create using params") {
        val im  = TestUtils.create(1, 1).head
        val ad  = TestUtils.randomData()
        ad.setAltitude(null)
        val opt = exec(
            controller.create(
                im.getUuid,
                ad.getLatitude,
                ad.getLongitude,
                ad.getDepthMeters,
                Option(ad.getAltitude),
                Option(ad.getCrs),
                Option(ad.getSalinity),
                Option(ad.getTemperatureCelsius),
                Option(ad.getOxygenMlL),
                Option(ad.getPressureDbar),
                Option(ad.getLightTransmission),
                Option(ad.getX),
                Option(ad.getY),
                Option(ad.getZ),
                Option(ad.getPosePositionUnits),
                Option(ad.getPhi),
                Option(ad.getTheta),
                Option(ad.getPsi)
            )
        )

        opt match
            case None           => fail("Failed to create CachedAncillaryDatum")
            case Some(obtained) =>
                assert(obtained.uuid.isDefined)
                assert(obtained.imagedMomentUuid.isDefined)
                assertEquals(obtained.imagedMomentUuid.orNull, im.getUuid)

                // update the source with a few fields that were null
                val expected = CachedAncillaryDatum
                    .from(ad)
                    .copy(
                        lastUpdated = obtained.lastUpdated,
                        uuid = obtained.uuid,
                        recordedTimestamp = obtained.recordedTimestamp,
                        imagedMomentUuid = obtained.imagedMomentUuid
                    )

                assertEquals(expected, obtained)

    }

    test("create using uuid + DTO") {
        val im  = TestUtils.create(1, 1).head
        val ad  = TestUtils.randomData()
        ad.setAltitude(null)
        val opt = exec(
            controller.create(
                im.getUuid,
                CachedAncillaryDatum.from(ad)
            )
        )

        opt match
            case None           => fail("Failed to create CachedAncillaryDatum")
            case Some(obtained) =>
                assert(obtained.uuid.isDefined)
                assert(obtained.imagedMomentUuid.isDefined)
                assertEquals(obtained.imagedMomentUuid.orNull, im.getUuid)

                // update the source with a few fields that were null
                val expected = CachedAncillaryDatum
                    .from(ad)
                    .copy(
                        lastUpdated = obtained.lastUpdated,
                        uuid = obtained.uuid,
                        recordedTimestamp = obtained.recordedTimestamp,
                        imagedMomentUuid = obtained.imagedMomentUuid
                    )

                assertEquals(expected, obtained)
    }

    test("create using extended DTO") {
        val im       = TestUtils.create(1, 1).head
        val ad       = TestUtils.randomData()
        val original = CachedAncillaryDatum.from(ad).copy(imagedMomentUuid = Option(im.getUuid))
        val opt      = exec(controller.create(original))
        opt match
            case None           => fail("Failed to create CachedAncillaryDatum")
            case Some(obtained) =>
                val expected = original.copy(
                    lastUpdated = obtained.lastUpdated,
                    uuid = obtained.uuid,
                    recordedTimestamp = obtained.recordedTimestamp,
                    imagedMomentUuid = obtained.imagedMomentUuid
                )
                assertEquals(obtained, expected)
    }

    test("update using params") {
        val im       = TestUtils.create(1, includeData = true).head
        val obtained = exec(
            controller.update(
                im.getAncillaryDatum.getUuid,
                Some(12.3456),
                Some(123.4567),
                Some(-1000.0),
            )
        )
        obtained match
            case None           => fail("Failed to update CachedAncillaryDatum")
            case Some(obtained) =>
                assert(obtained.uuid.isDefined)
                assert(obtained.imagedMomentUuid.isDefined)
                assertEquals(obtained.imagedMomentUuid.orNull, im.getUuid)
                assertEquals(obtained.latitude.orNull, 12.3456)
                assertEquals(obtained.longitude.orNull, 123.4567)
                assertEquals(obtained.depthMeters.orNull, -1000.0)
    }

    test("findByVideoReferenceUUID") {
        val im = TestUtils.create(1, includeData = true).head
        val obtained = exec(controller.findByVideoReferenceUUID(im.getVideoReferenceUuid)).head
        val expected = CachedAncillaryDatum
            .from(im.getAncillaryDatum)
            .copy(
                lastUpdated = obtained.lastUpdated,
                uuid = obtained.uuid,
                recordedTimestamp = obtained.recordedTimestamp,
                imagedMomentUuid = obtained.imagedMomentUuid
            )
        assertEquals(obtained, expected)

    }

    test("findByObservationUUID") {
        val im = TestUtils.create(1, 1, includeData = true).head
        val obs = im.getObservations.iterator().next()
        val obtained = exec(controller.findByObservationUUID(obs.getUuid)).head
        val expected = CachedAncillaryDatum
            .from(im.getAncillaryDatum)
            .copy(
                lastUpdated = obtained.lastUpdated,
                uuid = obtained.uuid,
                recordedTimestamp = obtained.recordedTimestamp,
                imagedMomentUuid = obtained.imagedMomentUuid
            )
        assertEquals(obtained, expected)
    }

    test("findByImagedMomentUUID") {
        val im = TestUtils.create(1, includeData = true).head
        val obtained = exec(controller.findByImagedMomentUUID(im.getUuid)).head
        val expected = CachedAncillaryDatum
            .from(im.getAncillaryDatum)
            .copy(
                lastUpdated = obtained.lastUpdated,
                uuid = obtained.uuid,
                recordedTimestamp = obtained.recordedTimestamp,
                imagedMomentUuid = obtained.imagedMomentUuid
            )
        assertEquals(obtained, expected)
    }

    test("bulkCreateOrUpdate") {}

    test("merge") {}

    test("deleteByVideoReferenceUuid") {
        val xs = TestUtils.create(4, 1, 1, 1, includeData = true)
        val ok = exec(controller.deleteByVideoReferenceUuid(xs.head.getVideoReferenceUuid))
        val obtained = exec(controller.findByVideoReferenceUUID(xs.head.getVideoReferenceUuid))
        assertEquals(obtained.size, 0)
    }

    // TOOD: verify that null fields do not come back as zero

}
