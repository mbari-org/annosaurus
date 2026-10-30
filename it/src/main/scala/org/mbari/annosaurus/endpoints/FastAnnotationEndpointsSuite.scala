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

import org.mbari.annosaurus.controllers.{LocalizationController, TestUtils}
import org.mbari.annosaurus.domain.{
    Annotation,
    AnnotationSC,
    Localization,
    LocalizationSC,
    CachedAncillaryDatum,
    ConcurrentRequest,
    Count,
    DeleteCountSC,
    GeographicRangeSC,
    ImageSC,
    MultiRequest,
    QueryConstraints,
    QueryConstraintsResponseSC
}
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.etc.sdk.Futures.*
import org.mbari.annosaurus.repository.jdbc.JdbcRepository
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import sttp.client3.*
import sttp.model.StatusCode

import java.util.UUID
import scala.jdk.CollectionConverters.*

trait FastAnnotationEndpointsSuite extends EndpointsSuite:

    private val log              = System.getLogger(getClass.getName)
    given JPADAOFactory          = daoFactory
    given jwtService: JwtService = new JwtService("mbari", "foo", "bar")
    private lazy val repository  = new JdbcRepository(daoFactory.entityManagerFactory)
    private lazy val endpoints   = new FastAnnotationEndpoints(repository)

    test("findAllAnnotations".flaky) {
        val xs = TestUtils.create(2, 2, 1, 1, true)
        runGet(
            endpoints.findAllAnnotationsImpl,
            "http://test.com/v1/fast?data=true",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val annotations = checkResponse[Seq[AnnotationSC]](response.body)
                assert(annotations.size >= 4)
                for a <- annotations
                do
                    assertEquals(a.image_references.size, 1)
                    assertEquals(a.associations.size, 1)
                    assert(a.ancillary_data.isDefined)
//                println(response.body)
// TODO this is not returning the ancillary data
        )
    }

    test("findAnnotationsByQueryConstraints (snake_case)") {
        val xs                  = TestUtils.create(2, 2, 1, 1, true) ++ TestUtils.create(2, 2, 1, 1, true)
        val obs                 = xs.flatMap(_.getObservations.asScala)
        val videoReferenceUuids = xs.map(_.getVideoReferenceUuid).distinct
        val qc                  = QueryConstraints(
            limit = Some(10),
            offset = Some(0),
            videoReferenceUuids = videoReferenceUuids,
            data = Some(true)
        )
        val jwt                 = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub         = newBackendStub(endpoints.findAnnotationsByQueryConstraintsImpl)
        val response            = basicRequest
            .post(uri"http://test.com/v1/fast")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/json")
            .body(qc.toSnakeCase.stringify)
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val qcr                 = checkResponse[QueryConstraintsResponseSC[Seq[AnnotationSC]]](response.body)
        assertEquals(qcr.content.size, obs.size)
        for a <- qcr.content
        do
            assertEquals(a.image_references.size, 1)
            assertEquals(a.associations.size, 1)
            assert(a.ancillary_data.isDefined)
    }

    test("findAnnotationsByQueryConstraints (camelCase)") {
        val xs                  = TestUtils.create(2, 2, 1, 1, true) ++ TestUtils.create(2, 2, 1, 1, true)
        val obs                 = xs.flatMap(_.getObservations.asScala)
        val videoReferenceUuids = xs.map(_.getVideoReferenceUuid).distinct
        val qc                  = QueryConstraints(
            limit = Some(10),
            offset = Some(0),
            videoReferenceUuids = videoReferenceUuids,
            data = Some(true)
        )
        val jwt                 = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub         = newBackendStub(endpoints.findAnnotationsByQueryConstraintsImpl)
        val response            = basicRequest
            .post(uri"http://test.com/v1/fast")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/json")
            .body(qc.stringify)
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val qcr                 = checkResponse[QueryConstraintsResponseSC[Seq[AnnotationSC]]](response.body)
        assertEquals(qcr.content.size, obs.size)
        for a <- qcr.content
        do
            assertEquals(a.image_references.size, 1)
            assertEquals(a.associations.size, 1)
            assert(a.ancillary_data.isDefined)
    }

    test("findGeoRangeByQueryConstraints (snake_case)") {
        val xs                  = TestUtils.create(10, 1, includeData = true)
        val videoReferenceUuids = xs.map(_.getVideoReferenceUuid).distinct
        val qc                  = QueryConstraints(
            limit = Some(10),
            offset = Some(0),
            videoReferenceUuids = videoReferenceUuids,
            data = Some(true)
        )
        val jwt                 = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub         = newBackendStub(endpoints.findGeoRangeByQueryConstraintsImpl)
        val response            = basicRequest
            .post(uri"http://test.com/v1/fast/georange")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/json")
            .body(qc.toSnakeCase.stringify)
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val qcr                 = checkResponse[QueryConstraintsResponseSC[GeographicRangeSC]](response.body)
        val gr                  = qcr.content.toCamelCase

        val data     = xs.map(_.getAncillaryDatum).map(CachedAncillaryDatum.from(_, true))
        val minLat   = data.map(_.latitude).min.getOrElse(-1)
        val maxLat   = data.map(_.latitude).max.getOrElse(-1)
        val minLon   = data.map(_.longitude).min.getOrElse(-1)
        val maxLon   = data.map(_.longitude).max.getOrElse(-1)
        val minDepth = data.map(_.depthMeters).min.getOrElse(-1f)
        val maxDepth = data.map(_.depthMeters).max.getOrElse(-1f)

        assertEquals(gr.minLatitude, minLat)
        assertEquals(gr.maxLatitude, maxLat)
        assertEquals(gr.minLongitude, minLon)
        assertEquals(gr.maxLongitude, maxLon)
        assertEqualsFloat(gr.minDepthMeters.floatValue(), minDepth, 0.00001)
        assertEqualsFloat(gr.maxDepthMeters.floatValue(), maxDepth, 0.0001)

    }

    test("findGeoRangeByQueryConstraints (camelCase)") {
        val xs                  = TestUtils.create(10, 1, includeData = true)
        val videoReferenceUuids = xs.map(_.getVideoReferenceUuid).distinct
        val qc                  = QueryConstraints(
            limit = Some(10),
            offset = Some(0),
            videoReferenceUuids = videoReferenceUuids,
            data = Some(true)
        )
        val jwt                 = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub         = newBackendStub(endpoints.findGeoRangeByQueryConstraintsImpl)
        val response            = basicRequest
            .post(uri"http://test.com/v1/fast/georange")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/json")
            .body(qc.stringify)
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val qcr                 = checkResponse[QueryConstraintsResponseSC[GeographicRangeSC]](response.body)
        val gr                  = qcr.content.toCamelCase

        val data     = xs.map(_.getAncillaryDatum).map(CachedAncillaryDatum.from(_, true))
        val minLat   = data.map(_.latitude).min.getOrElse(-1)
        val maxLat   = data.map(_.latitude).max.getOrElse(-1)
        val minLon   = data.map(_.longitude).min.getOrElse(-1)
        val maxLon   = data.map(_.longitude).max.getOrElse(-1)
        val minDepth = data.map(_.depthMeters).min.getOrElse(-1f)
        val maxDepth = data.map(_.depthMeters).max.getOrElse(-1f)

        assertEquals(gr.minLatitude, minLat)
        assertEquals(gr.maxLatitude, maxLat)
        assertEquals(gr.minLongitude, minLon)
        assertEquals(gr.maxLongitude, maxLon)
        assertEqualsFloat(gr.minDepthMeters.floatValue(), minDepth, 0.00001)
        assertEqualsFloat(gr.maxDepthMeters.floatValue(), maxDepth, 0.0001)

    }

    test("countAnnotationsByQueryConstraints (snake_case)") {
        val xs                  = TestUtils.create(2, 2, 1, 1, true) ++ TestUtils.create(2, 2, 1, 1, true)
        val expected            = xs.flatMap(_.getObservations.asScala).size
        val videoReferenceUuids = xs.map(_.getVideoReferenceUuid).distinct
        val qc                  = QueryConstraints(
            limit = Some(10),
            offset = Some(0),
            videoReferenceUuids = videoReferenceUuids,
            data = Some(true)
        )
        val jwt                 = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub         = newBackendStub(endpoints.countAnnotationsByQueryConstraintsImpl)
        val response            = basicRequest
            .post(uri"http://test.com/v1/fast/count")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/json")
            .body(qc.toSnakeCase.stringify)
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val qcr                 = checkResponse[QueryConstraintsResponseSC[Count]](response.body)
        val n                   = qcr.content.count
        assertEquals(n, expected.longValue)
    }

    test("countAnnotationsByQueryConstraints (camelCase)") {
        val xs                  = TestUtils.create(2, 2, 1, 1, true) ++ TestUtils.create(2, 2, 1, 1, true)
        val expected            = xs.flatMap(_.getObservations.asScala).size
        val videoReferenceUuids = xs.map(_.getVideoReferenceUuid).distinct
        val qc                  = QueryConstraints(
            limit = Some(10),
            offset = Some(0),
            videoReferenceUuids = videoReferenceUuids,
            data = Some(true)
        )
        val jwt                 = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub         = newBackendStub(endpoints.countAnnotationsByQueryConstraintsImpl)
        val response            = basicRequest
            .post(uri"http://test.com/v1/fast/count")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/json")
            .body(qc.stringify)
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val qcr                 = checkResponse[QueryConstraintsResponseSC[Count]](response.body)
        val n                   = qcr.content.count
        assertEquals(n, expected.longValue)
    }

    test("countAllAnnotations") {
        val xs = TestUtils.create(2, 1)
        runGet(
            endpoints.countAllAnnotationsImpl,
            "http://test.com/v1/fast/count",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val count = checkResponse[Count](response.body)
                assert(count.count >= xs.size)
        )
    }

    private lazy val localizationController = new LocalizationController(daoFactory)

    /**
     * What the read paths return: no observation join key, ordered by type so comparisons are deterministic. The
     * stale-write token is compared separately (against a JPA read of the stored row) by [[assertLocalizations]].
     */
    private def expectedLocalizations(im: org.mbari.annosaurus.repository.jpa.entity.ImagedMomentEntity) =
        im.getObservations.asScala.map(o =>
            o.getUuid -> o.getLocalizations.asScala
                .map(Localization.from(_).copy(observationUuid = None, lastUpdated = None))
                .toSeq
                .sortBy(_.localizationType)
        ).toMap

    /** The JDBC read path keeps the stale-write token, equal to what the JPA path returns for the same row. */
    private def assertTokenMatchesJpa(l: LocalizationSC): Unit =
        assert(l.last_updated_time.isDefined, s"fast path dropped last_updated_time: $l")
        assertEquals(l.observation_uuid, None)
        val jpa = localizationController.findByUUID(l.uuid.get).join.getOrElse(fail(s"no localization ${l.uuid}"))
        assertEquals(l.last_updated_time, jpa.lastUpdated)

    private def assertLocalizations(
        annotations: Seq[AnnotationSC],
        expected: Map[UUID, Seq[Localization]]
    ): Unit =
        assertEquals(annotations.size, expected.size)
        for a <- annotations
        do
            a.localizations.foreach(assertTokenMatchesJpa)
            val obtained = a.localizations.map(_.toCamelCase.copy(lastUpdated = None)).sortBy(_.localizationType)
            assertEquals(obtained, expected(a.observation_uuid.get))

    test("fast videoreference: the localization token can be used for a PUT; a stale one gets 409") {
        val im              = TestUtils.create(1, 1, 0, 0, nLocalizations = 1).head
        val obsUuid         = im.getObservations.iterator.next().getUuid
        var token: Option[java.time.Instant] = None
        var tpe: String     = ""
        runGet(
            endpoints.findAnnotationsByVideoReferenceUuidImpl,
            s"http://test.com/v1/fast/videoreference/${im.getVideoReferenceUuid}",
            response =>
                val l = checkResponse[Seq[AnnotationSC]](response.body).head.localizations.head
                token = l.last_updated_time
                tpe = l.localization_type
        )
        assert(token.isDefined)
        assertEquals(tpe, "bounding_box")
        val locEndpoints    = new LocalizationEndpoints(localizationController)
        val jwt             = Some(jwtService.authorize("foo").getOrElse(fail("no jwt")))
        def body(t: java.time.Instant) =
            s"""{"localization_type":"bounding_box","x":0.2,"y":0.2,"width":0.1,"height":0.1,"last_updated_time":"$t"}"""
        runPut(
            locEndpoints.upsertLocalizationImpl,
            s"http://test.com/v1/localizations/$obsUuid/bounding_box",
            body(token.get),
            response => assertEquals(response.code, StatusCode.Ok, response.body.toString),
            jwt
        )
        runPut(
            locEndpoints.upsertLocalizationImpl,
            s"http://test.com/v1/localizations/$obsUuid/bounding_box",
            body(token.get), // now stale: the previous PUT advanced the version
            response => assertEquals(response.code, StatusCode.Conflict, response.body.toString),
            jwt
        )
    }

    test("fast videoreference returns localizations (box and polygon) without disturbing associations") {
        val im       = TestUtils.create(1, 1, 2, 1, nLocalizations = 4).head
        val expected = expectedLocalizations(im)
        assertEquals(expected.values.head.size, 4)
        runGet(
            endpoints.findAnnotationsByVideoReferenceUuidImpl,
            s"http://test.com/v1/fast/videoreference/${im.getVideoReferenceUuid}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val annotations = checkResponse[Seq[AnnotationSC]](response.body)
                assertLocalizations(annotations, expected)
                assert(annotations.head.localizations.exists(_.vertices.isDefined))
                assertEquals(annotations.head.associations.size, 2)
                assertEquals(annotations.head.image_references.size, 1)
        )
    }

    test("fast videoreference: associations only gives localizations == []") {
        val im = TestUtils.create(1, 1, 2, 0).head
        runGet(
            endpoints.findAnnotationsByVideoReferenceUuidImpl,
            s"http://test.com/v1/fast/videoreference/${im.getVideoReferenceUuid}",
            response =>
                val annotations = checkResponse[Seq[AnnotationSC]](response.body)
                assertEquals(annotations.size, 1)
                assertEquals(annotations.head.localizations, Nil)
                assertEquals(annotations.head.associations.size, 2)
        )
    }

    test("fast videoreference: localizations are not cross-contaminated between observations") {
        val im       = TestUtils.create(1, 3, 2, 0, nLocalizations = 2).head
        val expected = expectedLocalizations(im)
        runGet(
            endpoints.findAnnotationsByVideoReferenceUuidImpl,
            s"http://test.com/v1/fast/videoreference/${im.getVideoReferenceUuid}",
            response =>
                val annotations = checkResponse[Seq[AnnotationSC]](response.body)
                assertLocalizations(annotations, expected)
                assert(annotations.forall(_.associations.size == 2))
        )
    }

    test("fast concept returns localizations") {
        val im       = TestUtils.create(1, 1, 1, 0, nLocalizations = 2).head
        val obs      = im.getObservations.iterator.next()
        val expected = expectedLocalizations(im)
        runGet(
            endpoints.findAnnotationsByConceptImpl,
            s"http://test.com/v1/fast/concept/${obs.getConcept}",
            response =>
                val annotations = checkResponse[Seq[AnnotationSC]](response.body)
                assertLocalizations(annotations, expected)
        )
    }

    test("fast concurrent request returns localizations") {
        val im       = TestUtils.create(1, 2, 1, 0, nLocalizations = 2).head
        val expected = expectedLocalizations(im)
        val cr       = ConcurrentRequest(
            im.getRecordedTimestamp.minusSeconds(1),
            im.getRecordedTimestamp.plusSeconds(1),
            Seq(im.getVideoReferenceUuid))
        runPost(
            endpoints.findAnnotationsByConcurrentRequestImpl,
            "http://test.com/v1/fast/concurrent",
            cr.toSnakeCase.stringify,
            response =>
                val annotations = checkResponse[Seq[AnnotationSC]](response.body)
                assertLocalizations(annotations, expected)
        )
    }

    test("fast multi request returns localizations") {
        val im       = TestUtils.create(1, 2, 1, 0, nLocalizations = 2).head
        val expected = expectedLocalizations(im)
        runPost(
            endpoints.findAnnotationsByMultiRequestImpl,
            "http://test.com/v1/fast/multi",
            MultiRequest(Seq(im.getVideoReferenceUuid)).toSnakeCase.stringify,
            response =>
                val annotations = checkResponse[Seq[AnnotationSC]](response.body)
                assertLocalizations(annotations, expected)
        )
    }

    test("fast query constraints returns localizations") {
        val im       = TestUtils.create(1, 2, 1, 0, nLocalizations = 2).head
        val expected = expectedLocalizations(im)
        val qc       = QueryConstraints(videoReferenceUuids = Seq(im.getVideoReferenceUuid))
        runPost(
            endpoints.findAnnotationsByQueryConstraintsImpl,
            "http://test.com/v1/fast",
            qc.toSnakeCase.stringify,
            response =>
                val qcr = checkResponse[QueryConstraintsResponseSC[Seq[AnnotationSC]]](response.body)
                assertLocalizations(qcr.content, expected)
        )
    }

    test("findAnnotationsByVideoReferenceUuid") {
        val xs                 = TestUtils.create(2, 2, 1, 1, true)
        val videoReferenceUuid = xs.head.getVideoReferenceUuid
        runGet(
            endpoints.findAnnotationsByVideoReferenceUuidImpl,
            s"http://test.com/v1/fast/videoreference/$videoReferenceUuid?data=true",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val annotations = checkResponse[Seq[AnnotationSC]](response.body)
                assertEquals(annotations.size, 4)
                for a <- annotations
                do
                    assertEquals(a.image_references.size, 1)
                    assertEquals(a.associations.size, 1)
                    assert(a.ancillary_data.isDefined)
        )
    }

    test("findImagesByVideoReferenceUuid") {
        val xs                 = TestUtils.create(2, 2, 1, 1)
        val videoReferenceUuid = xs.head.getVideoReferenceUuid
        runGet(
            endpoints.findImagesByVideoReferenceUuidImpl,
            s"http://test.com/v1/fast/images/videoreference/$videoReferenceUuid",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val images = checkResponse[Seq[ImageSC]](response.body)
                assertEquals(images.size, xs.size)
        )
    }

    test("countImagesByVideoReferenceUuid") {
        val xs                 = TestUtils.create(2, 2, 1, 1)
        val videoReferenceUuid = xs.head.getVideoReferenceUuid
        runGet(
            endpoints.countImagesByVideoReferenceUuidImpl,
            s"http://test.com/v1/fast/images/count/videoreference/$videoReferenceUuid",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val count = checkResponse[Count](response.body)
                assertEquals(count.count, xs.size.longValue)
        )
    }

    test("findAnnotationsByConcept") {
        val xs      = TestUtils.create(2, 2, 1, 1, true)
        val concept = xs.head.getObservations.iterator().next().getConcept
        runGet(
            endpoints.findAnnotationsByConceptImpl,
            s"http://test.com/v1/fast/concept/$concept?data=true",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val annotations = checkResponse[Seq[AnnotationSC]](response.body)
                assertEquals(annotations.size, 1)
                for a <- annotations
                do
//                    println(a.stringify)
                    assertEquals(a.image_references.size, 1)
                    assertEquals(a.associations.size, 1)
                    assert(a.ancillary_data.isDefined)
        )
    }

    test("findAnnotationsWithImagesByConcept") {
        val xs      = TestUtils.create(2, 2, 1, 1, true)
        val concept = xs.head.getObservations.iterator().next().getConcept
        runGet(
            endpoints.findAnnotationsWithImagesByConceptImpl,
            s"http://test.com/v1/fast/concept/images/$concept?data=true",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val annotations = checkResponse[Seq[AnnotationSC]](response.body)
                assertEquals(annotations.size, 1)
                for a <- annotations
                do
                    assertEquals(a.image_references.size, 1)
                    assertEquals(a.associations.size, 1)
                    assert(a.ancillary_data.isDefined)
        )
    }

    test("findImagedMomentUuidsByConcept") {
        val xs         = TestUtils.create(2, 2, 1, 1, true)
        val annotation = xs.head.getObservations.iterator().next()
        val concept    = annotation.getConcept
        runGet(
            endpoints.findImagedMomentUuidsByConceptImpl,
            s"http://test.com/v1/fast/imagedmoments/concept/images/$concept",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val imagedMomentUuids = checkResponse[Seq[String]](response.body)
                assertEquals(imagedMomentUuids.size, 1)
        )
    }

    test("findImagedMomentUuidsByToConcept") {
        val xs        = TestUtils.create(2, 2, 1, 1, true)
        val im        = xs.head
        val ass       = im.getObservations.iterator().next().getAssociations.iterator().next()
        val toConcept = ass.getToConcept
        runGet(
            endpoints.findImagedMomentUuidsByToConceptImpl,
            s"http://test.com/v1/fast/imagedmoments/toconcept/images/$toConcept",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val imagedMomentUuids = checkResponse[Seq[UUID]](response.body)
                assertEquals(imagedMomentUuids.size, 1)
                assertEquals(imagedMomentUuids.head, im.getUuid)
        )
    }

    test("findAnnotationsByLinkNameAndLinkValue") {
        val im  = TestUtils.create(1, 1, 1, 1).head
        assert(im.getUuid != null)
        val obs = im.getObservations.iterator().next()
        assert(obs.getUuid != null)
        assert(obs.getObservationTimestamp() != null)
        val ass = obs.getAssociations.iterator().next()
        runGet(
            endpoints.findAnnotationsByLinkNameAndLinkValueImpl,
            s"http://test.com/v1/fast/details/${ass.getLinkName}/${ass.getLinkValue}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val annos    = checkResponse[Seq[AnnotationSC]](response.body)
                // println(annos.stringify)
                assertEquals(annos.size, 1)
                val obtained = annos.head.toCamelCase
                val expected = TestUtils.stripLastUpdated(
                    Annotation.from(obs).removeForeignKeys().roundObservationTimestampToMillis()
                )
//                println("EXPECTED: " + expected.stringify)
//                println("OBTAINED: " + obtained.stringify)
                assertEquals(obtained, expected)
        )

    }

    test("deleteAnnotationsByVideoReferenceUuid") {
        val xs                          = TestUtils.create(2, 1, 1)
        val jwt                         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub                 = newBackendStub(endpoints.deleteAnnotationsByVideoReferenceUuidImpl)
        val response                    = basicRequest
            .delete(uri"http://test.com/v1/fast/videoreference/${xs.head.getVideoReferenceUuid}")
            .header("Authorization", s"Bearer $jwt")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
//        println(response.body)
        val count                       = checkResponse[DeleteCountSC](response.body).toCamelCase
        val expectedImageMomentCount    = xs.size
        val expectedObservationCount    = xs.flatMap(_.getObservations.asScala).size
        val expectedAncillaryDataCount  = xs.flatMap(im => Option(im.getAncillaryDatum)).size
        val expectedImageReferenceCount = xs.flatMap(_.getImageReferences.asScala).size
        val expectedAssociationCount    = xs.flatMap(_.getObservations.asScala).flatMap(_.getAssociations.asScala).size
        assertEquals(count.observationCount, expectedObservationCount)
        assertEquals(count.imageReferenceCount, expectedImageReferenceCount)
        assertEquals(count.ancillaryDataCount, expectedAncillaryDataCount)
        assertEquals(count.imagedMomentCount, expectedImageMomentCount)
        assertEquals(count.associationCount, expectedAssociationCount)
        assertEquals(count.videoReferenceUuid, xs.head.getVideoReferenceUuid)

    }

    test("deleteAnnotationsByVideoReferenceUuid (with localizations)") {
        val xs          = TestUtils.create(2, 1, nLocalizations = 2)
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.deleteAnnotationsByVideoReferenceUuidImpl)
        val response    = basicRequest
            .delete(uri"http://test.com/v1/fast/videoreference/${xs.head.getVideoReferenceUuid}")
            .header("Authorization", s"Bearer $jwt")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        assert(response.body.toString.contains("\"localization_count\":4"), response.body.toString)
        val count       = checkResponse[DeleteCountSC](response.body).toCamelCase
        assertEquals(count.errorMessage, None)
        assertEquals(count.localizationCount, 4)
        assertEquals(count.observationCount, 2)
    }

    test("deleteAnnotationsByVideoReferenceUuid (large)") {
        val xs                          = TestUtils.create(100, 5, 3, 2, true)
        val jwt                         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub                 = newBackendStub(endpoints.deleteAnnotationsByVideoReferenceUuidImpl)
        val response                    = basicRequest
            .delete(uri"http://test.com/v1/fast/videoreference/${xs.head.getVideoReferenceUuid}")
            .header("Authorization", s"Bearer $jwt")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val count                       = checkResponse[DeleteCountSC](response.body).toCamelCase
        val expectedImageMomentCount    = xs.size
        val expectedObservationCount    = xs.flatMap(_.getObservations.asScala).size
        val expectedAncillaryDataCount  = xs.flatMap(im => Option(im.getAncillaryDatum)).size
        val expectedImageReferenceCount = xs.flatMap(_.getImageReferences.asScala).size
        val expectedAssociationCount    = xs.flatMap(_.getObservations.asScala).flatMap(_.getAssociations.asScala).size
        assertEquals(count.observationCount, expectedObservationCount)
        assertEquals(count.imageReferenceCount, expectedImageReferenceCount)
        assertEquals(count.ancillaryDataCount, expectedAncillaryDataCount)
        assertEquals(count.imagedMomentCount, expectedImageMomentCount)
        assertEquals(count.associationCount, expectedAssociationCount)
        assertEquals(count.videoReferenceUuid, xs.head.getVideoReferenceUuid)

    }

    test("findAnnotationsByConcurrentRequest") {
        val xs                  = TestUtils.create(2, 2) ++ TestUtils.create(2, 2)
        val videoReferenceUuids = xs.map(_.getVideoReferenceUuid).distinct
        val ts                  = xs.map(_.getRecordedTimestamp).distinct
        val t0                  = ts.min
        val t1                  = ts.max
        val cr                  = ConcurrentRequest(t0, t1, videoReferenceUuids)
        val body                = cr.toSnakeCase.stringify
        runPost(
            endpoints.findAnnotationsByConcurrentRequestImpl,
            "http://test.com/v1/fast/concurrent",
            body,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val annotations = checkResponse[Seq[AnnotationSC]](response.body)
                val expected    = xs.flatMap(_.getObservations.asScala).size
                val obtained    = annotations.size
                assertEquals(obtained, expected)
        )
    }

    test("findAnnotationsByMultiRequest") {
        val xs = TestUtils.create(2, 2) ++ TestUtils.create(2, 2)

        val videoReferenceUuids = xs.map(_.getVideoReferenceUuid).distinct
        val mr                  = MultiRequest(videoReferenceUuids)
        val body                = mr.toSnakeCase.stringify
        runPost(
            endpoints.findAnnotationsByMultiRequestImpl,
            "http://test.com/v1/fast/multi",
            body,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val annotations = checkResponse[Seq[AnnotationSC]](response.body)
                val expected    = xs.flatMap(_.getObservations.asScala).size
                val obtained    = annotations.size
                assertEquals(obtained, expected)
        )

    }
