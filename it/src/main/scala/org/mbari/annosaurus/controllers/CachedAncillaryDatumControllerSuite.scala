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
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.etc.jdk.Numbers.*
import java.sql.Timestamp
import java.time.{Duration, Instant}
import scala.util.Random

trait CachedAncillaryDatumControllerSuite extends BaseDAOSuite {

    given JPADAOFactory = daoFactory

    val controller = new CachedAncillaryDatumController(daoFactory)

    test("create using params") {
        val im = TestUtils.create(1, 1).head
        val ad = TestUtils.randomData()
        ad.setAltitude(null) // FIXME: null values are being returnd as zero
        ad.setZ(null)
        val opt = exec(
            controller.create(
                im.getUuid,
                ad.getLatitude,
                ad.getLongitude,
                ad.getDepthMeters,      // IMPORTANT!!
                ad.getAltitude.asFloat, // Don't do Option(ad.getAltitude) because it will be Some(0.0) for nulls
                Option(ad.getCrs),      // The extension methods asFloat, asDouble handle null corretly
                ad.getSalinity.asFloat,
                ad.getTemperatureCelsius.asFloat,
                ad.getOxygenMlL.asFloat,
                ad.getPressureDbar.asFloat,
                ad.getLightTransmission.asFloat,
                ad.getX.asDouble,
                ad.getY.asDouble,
                ad.getZ.asDouble,
                Option(ad.getPosePositionUnits),
                ad.getPhi.asDouble,
                ad.getTheta.asDouble,
                ad.getPsi.asDouble
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
                Some(-1000.0f)
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
                assertEquals(obtained.depthMeters.orNull, -1000.0f)
    }

    test("findByVideoReferenceUUID") {
        val im       = TestUtils.create(1, includeData = true).head
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
        val im       = TestUtils.create(1, 1, includeData = true).head
        val obs      = im.getObservations.iterator().next()
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
        val im       = TestUtils.create(1, includeData = true).head
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

    test("bulkCreateOrUpdate (create)") {
        // test create
        val s0 = TestUtils
            .create(4, 1, 1, 1, false)
            .map(x => {
                val ad = TestUtils.randomData()
                ad.setDepthMeters(1000)
                x.setAncillaryDatum(ad)
                CachedAncillaryDatum.from(ad, true)
            })
        val a0 = exec(controller.bulkCreateOrUpdate(s0))
        for s <- s0
        do
            assert(s.imagedMomentUuid.isDefined)
            val opt      = exec(controller.findByImagedMomentUUID(s.imagedMomentUuid.get))
            assert(opt.isDefined)
            val obtained = opt.get
            val expected = s.copy(
                uuid = obtained.uuid,
                lastUpdated = obtained.lastUpdated,
                recordedTimestamp = obtained.recordedTimestamp,
                imagedMomentUuid = obtained.imagedMomentUuid
            )
            assertEquals(obtained, expected)

    }

    test("bulkCreateOrUpdate (update)") {
        // test update
        val s0 = TestUtils
            .create(4, 1, 1, 1, true)
            .map(x => {
                val ad = x.getAncillaryDatum
                ad.setDepthMeters(1000)
                CachedAncillaryDatum.from(ad, true)
            })
        val a0 = exec(controller.bulkCreateOrUpdate(s0))
        for s <- s0
        do
            assert(s.imagedMomentUuid.isDefined)
            val opt      = exec(controller.findByImagedMomentUUID(s.imagedMomentUuid.get))
            assert(opt.isDefined)
            val obtained = opt.get
            val expected = s.copy(
                uuid = obtained.uuid,
                lastUpdated = obtained.lastUpdated,
                recordedTimestamp = obtained.recordedTimestamp,
                imagedMomentUuid = obtained.imagedMomentUuid
            )
            assertEquals(obtained, expected)
    }

    test("bulkCreateOrUpdate (mixed create/update") {
        val s0 = TestUtils
            .create(4, 1, 1, 1, true)
            .map(x => {
                val ad = x.getAncillaryDatum
                ad.setDepthMeters(1000)
                CachedAncillaryDatum.from(ad, true)
            }) ++ TestUtils
            .create(4, 1, 1, 1, false)
            .map(x => {
                val ad = TestUtils.randomData()
                ad.setDepthMeters(1000)
                x.setAncillaryDatum(ad)
                CachedAncillaryDatum.from(ad, true)
            })

        val a0 = exec(controller.bulkCreateOrUpdate(s0))
        for s <- s0
        do
            assert(s.imagedMomentUuid.isDefined)
            val opt      = exec(controller.findByImagedMomentUUID(s.imagedMomentUuid.get))
            assert(opt.isDefined)
            val obtained = opt.get
            val expected = s.copy(
                uuid = obtained.uuid,
                lastUpdated = obtained.lastUpdated,
                recordedTimestamp = obtained.recordedTimestamp,
                imagedMomentUuid = obtained.imagedMomentUuid
            )
            assertEquals(obtained, expected)
    }

    test("merge") {
        val xs             = TestUtils.create(10)
        val minEpochMillis = xs.map(_.getRecordedTimestamp.toEpochMilli).min
        val s0             = xs
            .zipWithIndex
            .map((im, idx) => {
                val ts = Instant
                    .ofEpochMilli(im.getRecordedTimestamp.toEpochMilli + Random.nextInt(14000))
                CachedAncillaryDatum
                    .from(TestUtils.randomData())
                    .copy(recordedTimestamp = Some(ts), depthMeters = Some(1000))
            })
        exec(controller.merge(s0, xs.head.getVideoReferenceUuid, Duration.ofSeconds(15)))
        for x <- xs
        do
            val opt = exec(controller.findByImagedMomentUUID(x.getUuid))
            opt match
                case None           => fail("Failed to find CachedAncillaryDatum")
                case Some(obtained) =>
                    assert(obtained.uuid.isDefined)
                    assert(obtained.imagedMomentUuid.isDefined)
                    assertEquals(obtained.imagedMomentUuid.orNull, x.getUuid)
                    assertEquals(obtained.depthMeters.orNull, 1000.0f)
                    assertEquals(obtained.recordedTimestamp.orNull, x.getRecordedTimestamp)
    }

    test("deleteByVideoReferenceUuid") {
        val xs       = TestUtils.create(4, 1, 1, 1, includeData = true)
        val ok       = exec(controller.deleteByVideoReferenceUuid(xs.head.getVideoReferenceUuid))
        val obtained = exec(controller.findByVideoReferenceUUID(xs.head.getVideoReferenceUuid))
        assertEquals(obtained.size, 0)
    }

    // TOOD: verify that null fields do not come back as zero
    test("inserted null fields should not be zero") {
        val im  = TestUtils.create(1, includeData = false).head
        val ad  = CachedAncillaryDatum(
            latitude = Some(12.345),
            longitude = Some(123.456),
            depthMeters = Some(1000)
        )
        val opt = exec(controller.create(im.getUuid, ad))
        opt match
            case None           => fail("Failed to create CachedAncillaryDatum")
            case Some(obtained) =>
                assert(obtained.uuid.isDefined)
                assert(obtained.imagedMomentUuid.isDefined)
                assertEquals(obtained.imagedMomentUuid.orNull, im.getUuid)
                assertEquals(obtained.latitude.orNull, 12.345)
                assertEquals(obtained.longitude.orNull, 123.456)
                assertEquals(obtained.depthMeters.orNull, 1000.0f)
                assert(obtained.altitude.isEmpty)
//                println(obtained.stringify)
                assert(obtained.crs.isEmpty)
                assert(obtained.salinity.isEmpty)
                assert(obtained.temperatureCelsius.isEmpty)
    }

}
