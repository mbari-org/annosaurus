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

package org.mbari.annosaurus.endpoints

import org.mbari.annosaurus.controllers.{ImagedMomentController, TestUtils}
import org.mbari.annosaurus.domain.{
    Annotation,
    ConceptCount,
    Count,
    CountForVideoReferenceSC,
    ImagedMoment,
    ImagedMomentSC,
    ImagedMomentTimestampUpdateSC,
    MoveImagedMoments,
    WindowRequest
}
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.etc.jdk.Instants
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.etc.sdk.Futures.*
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import sttp.client3.*
import sttp.model.StatusCode

import java.time.{Duration, Instant}
import scala.jdk.CollectionConverters.*

trait ImagedMomentEndpointsSuite extends EndpointsSuite:

    private val log              = System.getLogger(getClass.getName)
    given JPADAOFactory          = daoFactory
    given jwtService: JwtService = new JwtService("mbari", "foo", "bar")
    private lazy val controller  = new ImagedMomentController(daoFactory)
    private lazy val endpoints   = new ImagedMomentEndpoints(controller)

    test("findAllImagedMoments") {
        val xs = TestUtils.create(2)
        runGet(
            endpoints.findAllImagedMomentsImpl,
            s"http://test.com/v1/imagedmoments?limit=10&offset=0",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val imagedMoments = checkResponse[Seq[ImagedMomentSC]](response.body)
                assert(imagedMoments.size >= 2)
        )
    }

    test("countAllImagedMoments") {
        val xs = TestUtils.create(2)
        runGet(
            endpoints.countAllImagedMomentsImpl,
            s"http://test.com/v1/imagedmoments/count/all",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val count = checkResponse[Count](response.body)
                assert(count.count >= 2)
        )
    }

    test("findImagedMomentsWithImages") {
        val im = TestUtils.create(1, nImageReferences = 1).head
        runGet(
            endpoints.findImagedMomentsWithImagesImpl,
            s"http://test.com/v1/imagedmoments/find/images?limit=10&offset=0",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val imagedMoments = checkResponse[Seq[ImagedMomentSC]](response.body)
                assert(imagedMoments.nonEmpty)
                val expected      = ImagedMoment.from(im, true)
                val obtained      = imagedMoments.filter(_.uuid.get == im.getUuid).head.toCamelCase
                assertEquals(obtained, expected)
        )
    }

    test("countImagedMomentsWithImages") {
        val im = TestUtils.create(1, nImageReferences = 1).head
        runGet(
            endpoints.countImagedMomentsWithImagesImpl,
            s"http://test.com/v1/imagedmoments/count/images",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val count = checkResponse[Count](response.body)
                assert(count.count >= 1)
        )
    }

    test("countImagesForVideoReference") {
        val xs = TestUtils.create(4, nImageReferences = 1)
        runGet(
            endpoints.countImagesForVideoReferenceImpl,
            s"http://test.com/v1/imagedmoments/count/images/${xs.head.getVideoReferenceUuid}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val count = checkResponse[Count](response.body)
                assertEquals(count.count, xs.size.longValue)
        )

        val uuid = java.util.UUID.randomUUID()
        runGet(
            endpoints.countImagesForVideoReferenceImpl,
            s"http://test.com/v1/imagedmoments/count/images/$uuid",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val count = checkResponse[Count](response.body)
                assertEquals(count.count, 0L)
        )
    }

    test("findImagedMomentsByLinkName") {
        val im       = TestUtils.create(4, 1, 2).head
        val ass      = im.getObservations.iterator().next().getAssociations.iterator().next()
        val linkName = ass.getLinkName
        runGet(
            endpoints.findImagedMomentsByLinkNameImpl,
            s"http://test.com/v1/imagedmoments/find/linkname/${linkName}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val imagedMoments = checkResponse[Seq[ImagedMomentSC]](response.body)
                assertEquals(imagedMoments.size, 1)
                val expected      = ImagedMoment.from(im, true).roundObservationTimestampsToMillis()
                val obtained      = imagedMoments.head.toCamelCase
                assertEquals(obtained, expected)
        )

        val linkName1 = "foo"
        runGet(
            endpoints.findImagedMomentsByLinkNameImpl,
            s"http://test.com/v1/imagedmoments/find/linkname/$linkName1",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val imagedMoments = checkResponse[Seq[ImagedMomentSC]](response.body)
                assert(imagedMoments.isEmpty)
        )
    }

    test("countImagedMomentsByLinkName") {
        val im       = TestUtils.create(1, 1, 1).head
        val ass      = im.getObservations.iterator().next().getAssociations.iterator().next()
        val linkName = ass.getLinkName
        runGet(
            endpoints.countImagedMomentsByLinkNameImpl,
            s"http://test.com/v1/imagedmoments/count/linkname/$linkName",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val count = checkResponse[Count](response.body)
                assertEquals(count.count, 1.longValue)
        )
    }

    test("findImagedMomentByUUID") {
        val im = TestUtils.create(1).head
        runGet(
            endpoints.findImagedMomentByUUIDImpl,
            s"http://test.com/v1/imagedmoments/${im.getUuid}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[ImagedMomentSC](response.body).toCamelCase
                val expected = ImagedMoment.from(im, true)
                assertEquals(obtained, expected)
        )
    }

    test("findImagedMomentsByConceptName") {
        val im      = TestUtils.create(1, 1).head
        val concept = im.getObservations.iterator().next().getConcept
        runGet(
            endpoints.findImagedMomentsByConceptNameImpl,
            s"http://test.com/v1/imagedmoments/concept/$concept",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val imagedMoments = checkResponse[Seq[ImagedMomentSC]](response.body)
                assertEquals(imagedMoments.size, 1)
                val expected      = ImagedMoment.from(im, true).roundObservationTimestampsToMillis()
                val obtained      = imagedMoments.head.toCamelCase
                assertEquals(obtained, expected)
        )
    }

    test("findImagedMomentsByConceptNameWithImages") {
        val im      = TestUtils.create(1, 1, 1, 2).head
        val concept = im.getObservations.iterator().next().getConcept
        runGet(
            endpoints.findImagedMomentsByConceptNameWithImagesImpl,
            s"http://test.com/v1/imagedmoments/concept/images/$concept",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val imagedMoments = checkResponse[Seq[ImagedMomentSC]](response.body)
                assertEquals(imagedMoments.size, 1)
                val expected      = ImagedMoment.from(im, true).roundObservationTimestampsToMillis()
                val obtained      = imagedMoments.head.toCamelCase
                assertEquals(obtained, expected)
        )

        val im0      = TestUtils.create(1, 1).head
        val concept0 = im0.getObservations.iterator().next().getConcept
        runGet(
            endpoints.findImagedMomentsByConceptNameWithImagesImpl,
            s"http://test.com/v1/imagedmoments/concept/images/$concept0",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val imagedMoments = checkResponse[Seq[ImagedMomentSC]](response.body)
                assert(imagedMoments.isEmpty)
        )
    }

    test("countImagedMomentsByConceptName") {
        val concept = "foo"
        val xs      = TestUtils.build(4, 1)
        for
            x <- xs
            o <- x.getObservations.asScala
        do o.setConcept(concept)
        val c = ImagedMomentController(daoFactory)
        c.create(xs).join
        runGet(
            endpoints.countImagedMomentsByConceptNameImpl,
            s"http://test.com/v1/imagedmoments/concept/count/$concept",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val count = checkResponse[ConceptCount](response.body)
                assertEquals(count.concept, concept)
                assertEquals(count.count, 4L)
        )
    }

    test("countImagedMomentsByConceptNameWithImages") {
        val concept = "foo"
        val xs      = TestUtils.build(4, 1, 1, 2) ++ TestUtils.build(4, 1, 0, 1)
        for
            x <- xs
            o <- x.getObservations.asScala
        do o.setConcept(concept)
        val c = ImagedMomentController(daoFactory)
        c.create(xs).join
        runGet(
            endpoints.countImagedMomentsByConceptNameWithImagesImpl,
            s"http://test.com/v1/imagedmoments/concept/images/count/$concept",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val count = checkResponse[ConceptCount](response.body)
                assertEquals(count.concept, concept)
                assertEquals(count.count, xs.size.longValue)
        )
    }

    test("findImagedMomentsBetweenModifiedDates") {
        val xs          = TestUtils.create(4, 1, 1, 2)
        val im          = xs.head
        val modified    = im.getLastUpdatedTime.toInstant
        val start       = modified.minus(java.time.Duration.ofDays(1))
        val startString = Instants.formatCompactIso8601(start)
        val end         = modified.plus(java.time.Duration.ofDays(1))
        val endString   = Instants.formatCompactIso8601(end)
        runGet(
            endpoints.findImagedMomentsBetweenModifiedDatesImpl,
            s"http://test.com/v1/imagedmoments/modified/$startString/$endString?limit=10&offset=0",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val imagedMoments = checkResponse[Seq[ImagedMomentSC]](response.body)
                assert(imagedMoments.size >= xs.size)
                for
                    x  <- xs
                    im <- imagedMoments.find(_.uuid.get == x.getUuid)
                do
                    val expected = ImagedMoment.from(x, true).roundObservationTimestampsToMillis()
                    val obtained = im.toCamelCase
                    assertEquals(obtained, expected)
        )
    }

    test("countImagedMomentsBetweenModifiedDates".flaky) {
        val xs          = TestUtils.create(4, 1, 1, 2)
        val im          = xs.head
        val modified    = im.getLastUpdatedTime.toInstant
        val start       = modified.minus(java.time.Duration.ofDays(1))
        val startString = Instants.formatCompactIso8601(start)
        val end         = modified.plus(java.time.Duration.ofDays(1))
        val endString   = Instants.formatCompactIso8601(end)
        runGet(
            endpoints.countImagedMomentsBetweenModifiedDatesImpl,
            s"http://test.com/v1/imagedmoments/modified/count/$startString/$endString",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val count = checkResponse[Count](response.body)
                assert(count.count >= xs.size)
        )
    }

    test("countsByVideoReference") {
        val xs = TestUtils.create(2) ++ TestUtils.create(3)
        runGet(
            endpoints.countsPerVideoReferenceImpl,
            s"http://test.com/v1/imagedmoments/counts",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val counts = checkResponse[Seq[CountForVideoReferenceSC]](response.body)
                assert(counts.size >= 2)
                for
                    (videoReferenceUuid, imagedMoments) <- xs.groupBy(_.getVideoReferenceUuid)
                    count                               <- counts.find(_.video_reference_uuid == videoReferenceUuid)
                do assertEquals(count.count, imagedMoments.size)
        )
    }

    test("findAllVideoReferenceUUIDs") {
        val xs = TestUtils.create(2) ++ TestUtils.create(3)
        runGet(
            endpoints.findAllVideoReferenceUUIDsImpl,
            s"http://test.com/v1/imagedmoments/videoreference",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val uuids = checkResponse[Seq[java.util.UUID]](response.body)
                assert(uuids.size >= 2)
                for x <- xs
                do assert(uuids.contains(x.getVideoReferenceUuid))
        )
    }

    test("findByVideoReferenceUuid") {
        val xs                 = TestUtils.create(2, 2, 2, 2)
        val videoReferenceUuid = xs.head.getVideoReferenceUuid
        runGet(
            endpoints.findImagedMomentsByVideoReferenceUuidImpl,
            s"http://test.com/v1/imagedmoments/videoreference/${videoReferenceUuid}?limit=10&offset=0",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val imagedMoments = checkResponse[Seq[ImagedMomentSC]](response.body)
                assertEquals(imagedMoments.size, xs.size)
                for
                    x  <- xs
                    im <- imagedMoments.find(_.uuid.get == x.getUuid)
                do
                    val expected = ImagedMoment.from(x, true).roundObservationTimestampsToMillis()
                    val obtained = im.toCamelCase
                    assertEquals(obtained, expected)
        )
    }

    test("countModifiedBeforeDate") {
        val xs          = TestUtils.create(2, 2, 2, 2)
        val im          = xs.head
        val modified    = im.getLastUpdatedTime.toInstant
        val start       = modified.plus(java.time.Duration.ofSeconds(1))
        val startString = Instants.formatCompactIso8601(start)
        runGet(
            endpoints.countModifiedBeforeDateImpl,
            s"http://test.com/v1/imagedmoments/videoreference/modified/${im.getVideoReferenceUuid}/$startString",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val count = checkResponse[CountForVideoReferenceSC](response.body)
                assertEquals(count.count, xs.size)
        )
    }

    test("findImagedMomentsByWindowRequest") {
        val xs                  = TestUtils.create(2, 2, 2, 2)
        val videoReferenceUuids = xs.map(_.getVideoReferenceUuid).toSet
        val times               = xs.map(_.getRecordedTimestamp).sorted
        val diff                = Duration.between(times.head, times.last)
        val windowRequest       = WindowRequest(videoReferenceUuids.toSeq, xs.head.getUuid, diff.toMillis)
        runPost(
            endpoints.findImagedMomentsByWindowRequestImpl,
            s"http://test.com/v1/imagedmoments/windowrequest",
            windowRequest.toSnakeCase.stringify,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val imagedMoments = checkResponse[Seq[ImagedMomentSC]](response.body)
                assertEquals(imagedMoments.size, xs.size)
                for
                    x  <- xs
                    im <- imagedMoments.find(_.uuid.get == x.getUuid)
                do
                    val expected = ImagedMoment.from(x, true).copy(lastUpdated = None).roundObservationTimestampsToMillis()
                    val obtained = im.toCamelCase.copy(lastUpdated = None)
                    assertEquals(obtained, expected)
        )
    }

    test("deleteByVideoReferenceUUID") {
        val xs                 = TestUtils.create(2, 2)
        val videoReferenceUuid = xs.head.getVideoReferenceUuid
        runDelete(
            endpoints.deleteByVideoReferenceUUIDImpl,
            s"http://test.com/v1/imagedmoments/videoreference/${videoReferenceUuid}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val count = checkResponse[CountForVideoReferenceSC](response.body)
                assertEquals(count.video_reference_uuid, videoReferenceUuid)
                assertEquals(count.count, xs.size)
        )
        val c                  = ImagedMomentController(daoFactory)
        val imagedMoments      = c.findByVideoReferenceUUID(videoReferenceUuid).join
        assert(imagedMoments.isEmpty)
    }

    test("findByImageReferenceUUID") {
        val im                 = TestUtils.create(1, 0, 0, 1).head
        val imageReferenceUuid = im.getImageReferences.iterator().next().getUuid
        runGet(
            endpoints.findByImageReferenceUUIDImpl,
            s"http://test.com/v1/imagedmoments/imagereference/${imageReferenceUuid}?limit=10&offset=0",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val imagedMoment = checkResponse[ImagedMomentSC](response.body)
                val expected     = ImagedMoment.from(im, true)
                val obtained     = imagedMoment.toCamelCase
                assertEquals(obtained, expected)
        )
    }

    test("findByObservationUUID") {
        val im              = TestUtils.create(1, 1).head
        val observationUuid = im.getObservations.iterator().next().getUuid
        runGet(
            endpoints.findByObservationUUIDImpl,
            s"http://test.com/v1/imagedmoments/observation/${observationUuid}?limit=10&offset=0",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val imagedMoment = checkResponse[ImagedMomentSC](response.body)
                val expected     = ImagedMoment.from(im, true).roundObservationTimestampsToMillis()
                val obtained     = imagedMoment.toCamelCase
                assertEquals(obtained, expected)
        )
    }

    test("updateImagedMoment (json)") {
        val im          = TestUtils.create(1).head
        val expected    = ImagedMoment
            .from(im, true)
            .copy(elapsedTimeMillis = Some(1000L))
        val json        = expected.toSnakeCase.stringify
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.updateImagedMomentImpl)
        val response    = basicRequest
            .put(uri"http://test.com/v1/imagedmoments/${im.getUuid}")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/json")
            .body(json)
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[ImagedMomentSC](response.body).toCamelCase
        assertEquals(obtained, expected)
    }

    test("updateImagedMoment (form)") {
        val im          = TestUtils.create(1).head
        val expected    = ImagedMoment
            .from(im, true)
            .copy(elapsedTimeMillis = Some(1000L))
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.updateImagedMomentImpl)
        val response    = basicRequest
            .put(uri"http://test.com/v1/imagedmoments/${im.getUuid}")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("elapsed_time_millis=1000")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[ImagedMomentSC](response.body).toCamelCase
        assertEquals(obtained, expected)
    }

    test("updateRecordedTimestampsForVideoReference") {
        val xs                   = TestUtils.create(4)
        val newRecordedTimestamp = Instant.now().minus(Duration.ofDays(1))
        val ts                   = Instants.CompactTimeFormatterNs.format(newRecordedTimestamp)
//        val ts                   = Instants.formatCompactIso8601(newRecordedTimestamp)
        val jwt                  = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub          = newBackendStub(endpoints.updateRecordedTimestampsForVideoReferenceImpl)
        val response             = basicRequest
            .put(uri"http://test.com/v1/imagedmoments/newtime/${xs.head.getVideoReferenceUuid}/$ts")
            .auth
            .bearer(jwt)
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained             = checkResponse[Seq[ImagedMomentSC]](response.body)
        assertEquals(obtained.size, xs.size)
        for
            x  <- xs
            im <- obtained.find(_.uuid.get == x.getUuid)
        do
            assert(im.recorded_timestamp.isDefined)
            val expected = newRecordedTimestamp.plus(x.getElapsedTime)
            val obtained = im.recorded_timestamp.get
            assertEquals(obtained, expected)
    }

    test("updateRecordedTimestampForObservationUuid") {
        val xs          = TestUtils.create(4, 2)
        val annos       = for
            x <- xs
            o <- x.getObservations.asScala
        yield
        // We just need [{observation_uuid, recorded_timestamp}] all other fields are ignored
        Annotation(
            observationUuid = Some(o.getUuid),
            recordedTimestamp = Some(x.getRecordedTimestamp)
        ).toSnakeCase
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.updateRecordedTimestampForObservationUuidImpl)
        val response    = basicRequest
            .put(uri"http://test.com/v1/imagedmoments/tapetime")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/json")
            .body(annos.stringify)
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[ImagedMomentTimestampUpdateSC](response.body)
        assertEquals(obtained.annotation_count, annos.size)
        assertEquals(obtained.timestamps_updated, annos.size)
    }

    test("deleteImagedMoment") {
        val im           = TestUtils.create(1).head
        val jwt          = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub  = newBackendStub(endpoints.deleteImagedMomentImpl)
        val response     = basicRequest
            .delete(uri"http://test.com/v1/imagedmoments/${im.getUuid}")
            .header("Authorization", s"Bearer $jwt")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.NoContent)
        val c            = ImagedMomentController(daoFactory)
        val imagedMoment = c.findByUUID(im.getUuid).join
        assert(imagedMoment.isEmpty)
    }

    test("bulkMove (camelCase)") {
        val xs          = TestUtils.create(4)
        val newVideoRef = java.util.UUID.randomUUID()
        val moveRequest = MoveImagedMoments(newVideoRef, xs.map(_.getUuid))
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.bulkMoveImpl)
        val response    = basicRequest
            .put(uri"http://test.com/v1/imagedmoments/bulk/move")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/json")
            .body(moveRequest.stringify)
            .send(backendStub)
            .join

        assertEquals(response.code, StatusCode.Ok)
        val count = checkResponse[Count](response.body)
        assertEquals(count.count.intValue, xs.size)

    }

    test("bulkMove (snake_case)") {
        val xs          = TestUtils.create(4)
        val newVideoRef = java.util.UUID.randomUUID()
        val moveRequest = MoveImagedMoments(newVideoRef, xs.map(_.getUuid))
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.bulkMoveImpl)
        val response    = basicRequest
            .put(uri"http://test.com/v1/imagedmoments/bulk/move")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/json")
            .body(moveRequest.toSnakeCase.stringify)
            .send(backendStub)
            .join

        assertEquals(response.code, StatusCode.Ok)
        val count = checkResponse[Count](response.body)
        assertEquals(count.count.intValue, xs.size)

    }
