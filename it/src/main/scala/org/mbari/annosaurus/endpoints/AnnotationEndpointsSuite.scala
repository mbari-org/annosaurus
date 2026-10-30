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

import org.mbari.annosaurus.controllers.{AnnotationController, TestUtils}
import org.mbari.annosaurus.domain.{
    Annotation,
    AnnotationCreate,
    AnnotationSC,
    Localization,
    ConcurrentRequest,
    ConcurrentRequestCountSC,
    MultiRequest,
    MultiRequestCountSC
}
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.etc.sdk.Futures.*
import org.mbari.annosaurus.etc.sdk.Reflect
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import sttp.client3.*
import sttp.model.StatusCode

import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.Duration as ScalaDuration
import scala.jdk.CollectionConverters.*

trait AnnotationEndpointsSuite extends EndpointsSuite:

    // If the stress test fails we wait for 10 seconds and then fail the test
    override val munitTimeout = ScalaDuration(10, TimeUnit.SECONDS)

    private val log              = System.getLogger(getClass.getName)
    given JPADAOFactory          = daoFactory
    given jwtService: JwtService = new JwtService("mbari", "foo", "bar")
    private lazy val controller  = new AnnotationController(daoFactory)
    private lazy val endpoints   = new AnnotationEndpoints(controller)

    test("findAnnotationByUuid") {
        val im  = TestUtils.create(1, 1).head
        val obs = im.getObservations.iterator.next()
        runGet(
            endpoints.findAnnotationByUuidImpl,
            s"http://test.com/v1/annotations/${obs.getUuid}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[AnnotationSC](response.body).toCamelCase
                val expected = Annotation
                    .from(obs)
                    .copy(lastUpdated = obtained.lastUpdated)
                    .roundObservationTimestampToMillis()
                assertEquals(obtained, expected)
        )
    }

    private def noKeys(l: Localization): Localization = l.copy(observationUuid = None, lastUpdated = None)

    private def expectedLocalizations(obs: org.mbari.annosaurus.repository.jpa.entity.ObservationEntity) =
        obs.getLocalizations.asScala.map(x => noKeys(Localization.from(x))).toSeq.sortBy(_.localizationType)

    test("findAnnotationByUuid returns localizations (box and polygon)") {
        val im  = TestUtils.create(1, 1, 2, 0, nLocalizations = 4).head
        val obs = im.getObservations.iterator.next()
        runGet(
            endpoints.findAnnotationByUuidImpl,
            s"http://test.com/v1/annotations/${obs.getUuid}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[AnnotationSC](response.body).toCamelCase
                assertEquals(obtained.localizations.map(noKeys).sortBy(_.localizationType), expectedLocalizations(obs))
                assertEquals(obtained.localizations.size, 4)
                assertEquals(obtained.associations.size, 2)
        )
    }

    test("findAnnotationsByImageReferenceUuid returns localizations") {
        val im  = TestUtils.create(1, 1, 1, 1, nLocalizations = 4).head
        val obs = im.getObservations.iterator.next()
        val ir  = im.getImageReferences.iterator().next()
        runGet(
            endpoints.findAnnotationByImageReferenceUuidImpl,
            s"http://test.com/v1/annotations/imagereference/${ir.getUuid}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val ys = checkResponse[Seq[AnnotationSC]](response.body).map(_.toCamelCase)
                assertEquals(ys.length, 1)
                assertEquals(ys.head.localizations.map(noKeys).sortBy(_.localizationType), expectedLocalizations(obs))
                assertEquals(ys.head.associations.size, 1)
        )
    }

    test("findAnnotationsByVideoReferenceUuid returns localizations; associations-only gives []") {
        val im1 = TestUtils.create(1, 1, 0, 0, nLocalizations = 4).head
        val obs = im1.getObservations.iterator.next()
        runGet(
            endpoints.findAnnotationsByVideoReferenceUuidImpl,
            s"http://test.com/v1/annotations/videoreference/${im1.getVideoReferenceUuid}",
            response =>
                val ys = checkResponse[Seq[AnnotationSC]](response.body).map(_.toCamelCase)
                assertEquals(ys.length, 1)
                assertEquals(ys.head.localizations.map(noKeys).sortBy(_.localizationType), expectedLocalizations(obs))
        )
        val im2 = TestUtils.create(1, 1, 2, 0).head
        runGet(
            endpoints.findAnnotationsByVideoReferenceUuidImpl,
            s"http://test.com/v1/annotations/videoreference/${im2.getVideoReferenceUuid}",
            response =>
                val ys = checkResponse[Seq[AnnotationSC]](response.body).map(_.toCamelCase)
                assertEquals(ys.head.localizations, Nil)
                assertEquals(ys.head.associations.size, 2)
        )
    }

    test("findAnnotationByUuid (404)") {
        runGet(
            endpoints.findAnnotationByUuidImpl,
            s"http://test.com/v1/annotations/00000000-0000-0000-0000-000000000000",
            response => assertEquals(response.code, StatusCode.NotFound)
        )
    }

    test("findAnnotationsByAssociationUuid") {
        val im  = TestUtils.create(1, 1, 1, 1).head
        val obs = im.getObservations.iterator.next()
        val assoc = obs.getAssociations.iterator().next()
        runGet(
            endpoints.findAnnotationByAssociationUuidImpl,
            s"http://test.com/v1/annotations/association/${assoc.getUuid}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[AnnotationSC](response.body).toCamelCase
                val expected = Annotation
                    .from(obs)
                    .copy(lastUpdated = obtained.lastUpdated)
                    .roundObservationTimestampToMillis()
                assertEquals(obtained, expected)
        )
    }

    test("findAnnotationsByImageReferenceUuid") {
        val im  = TestUtils.create(1, 1, 0, 1).head
        val obs = im.getObservations.iterator.next()
        val ir  = im.getImageReferences.iterator().next()
        runGet(
            endpoints.findAnnotationByImageReferenceUuidImpl,
            s"http://test.com/v1/annotations/imagereference/${ir.getUuid}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val ys       = checkResponse[Seq[AnnotationSC]](response.body).map(_.toCamelCase)
                assertEquals(ys.length, 1)
                val obtained = ys.head
                val expected = Annotation
                    .from(obs)
                    .copy(lastUpdated = obtained.lastUpdated)
                    .roundObservationTimestampToMillis()
                assertEquals(obtained, expected)
        )
    }

//    test("findAnnotationsByImageReferenceUuid (404)") {
//        runGet(
//            endpoints.findAnnotationByImageReferenceUuidImpl,
//            s"http://test.com/v1/annotations/imagereference/00000000-0000-0000-0000-000000000000",
//            response => {
//                assertEquals(response.code, StatusCode.NotFound)
//            }
//        )
//    }

    test("findAnnotationsByVideoReferenceUuid") {
        val im  = TestUtils.create(1, 1, 0, 1).head
        val obs = im.getObservations.iterator.next()
        runGet(
            endpoints.findAnnotationsByVideoReferenceUuidImpl,
            s"http://test.com/v1/annotations/videoreference/${im.getVideoReferenceUuid}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val ys       = checkResponse[Seq[AnnotationSC]](response.body).map(_.toCamelCase)
                assertEquals(ys.length, 1)
                val obtained = ys.head
                val expected = Annotation
                    .from(obs)
                    .copy(lastUpdated = obtained.lastUpdated)
                    .roundObservationTimestampToMillis()
                assertEquals(obtained, expected)
        )
    }

//    test("findAnnotationsByVideoReferenceUuid (404)") {
//        runGet(
//            endpoints.findAnnotationsByVideoReferenceUuidImpl,
//            s"http://test.com/v1/annotations/videoreference/00000000-0000-0000-0000-000000000000",
//            response => {
//                assertEquals(response.code, StatusCode.NotFound)
//            }
//        )
//    }

    test("createAnnotation (json)") {
        val im          = TestUtils.build(1, 1).head
        val obs         = im.getObservations.iterator.next()
        val anno        = Annotation.from(obs)
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.createAnnotationImpl)
        val response    = basicRequest
            .post(uri"http://test.com/v1/annotations")
            .body(anno.toSnakeCase.stringify)
            .auth
            .bearer(jwt)
            .contentType("application/json")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[AnnotationSC](response.body).toCamelCase
        val expected    = anno.copy(
            imagedMomentUuid = obtained.imagedMomentUuid,
            observationUuid = obtained.observationUuid,
            lastUpdated = obtained.lastUpdated
        )
        assertEquals(obtained, expected)
    }

    test("createAnnotation (form)") {
        val im          = TestUtils.build(1, 1).head
        val obs         = im.getObservations.iterator.next()
        val anno        = Annotation.from(obs)
        val annoCreate  = AnnotationCreate.fromAnnotation(anno)
        val formData    = Reflect.toFormBody(annoCreate.toSnakeCase)
//        println(formData)
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.createAnnotationImpl)
        val response    = basicRequest
            .post(uri"http://test.com/v1/annotations")
            .body(formData)
            .auth
            .bearer(jwt)
            .contentType("application/x-www-form-urlencoded")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[AnnotationSC](response.body).toCamelCase
        val expected    = anno.copy(
            imagedMomentUuid = obtained.imagedMomentUuid,
            observationUuid = obtained.observationUuid,
            lastUpdated = obtained.lastUpdated
        )
        assertEquals(obtained, expected)
    }

    test("bulkCreateAnnotations (simple)") {
        val xs          = TestUtils.build(2, 5)
        val annos       = xs
            .flatMap(Annotation.fromImagedMoment(_))
            .map(_.toSnakeCase)
            .sortBy(_.concept)
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStop = newBackendStub(endpoints.bulkCreateAnnotationsImpl)
        val response    = basicRequest
            .post(uri"http://test.com/v1/annotations/bulk")
            .body(annos.stringify)
            .auth
            .bearer(jwt)
            .contentType("application/json")
            .send(backendStop)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[Seq[AnnotationSC]](response.body).map(_.toCamelCase)
        for o <- obtained
        do
            assert(o.observationUuid.isDefined)
            assert(o.imagedMomentUuid.isDefined)

        val normalized =
            obtained
                .map(_.copy(observationUuid = None, imagedMomentUuid = None, lastUpdated = None))
                .sortBy(_.concept)

        for (o, e) <- normalized.zip(annos)
        do assertEquals(o, e.toCamelCase)
    }

    test("bulkCreateAnnotations (with associations)") {
        val xs          = TestUtils.assignOrderedTimestamps(TestUtils.build(200, 1, 2), Some(100))
        val expected    = xs
            .flatMap(Annotation.fromImagedMoment(_))
            .map(_.toSnakeCase)
            .sortBy(_.timecode.getOrElse(""))
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStop = newBackendStub(endpoints.bulkCreateAnnotationsImpl)
        val response    = basicRequest
            .post(uri"http://test.com/v1/annotations/bulk")
            .body(expected.stringify)
            .auth
            .bearer(jwt)
            .contentType("application/json")
            .send(backendStop)
            .join
        assertEquals(response.code, StatusCode.Ok)

        // Everything below is snake_case
        val obtained = checkResponse[Seq[AnnotationSC]](response.body).sortBy(_.timecode.getOrElse(""))
        for o <- obtained
        do
            assert(o.observation_uuid.isDefined)
            assert(o.imaged_moment_uuid.isDefined)

        for (o, e) <- obtained.zip(expected)
        do
            // normalize the response and remove assoations
            val e0 = e.copy(associations = Nil)
            val o0 = o.copy(
                observation_uuid = None,
                imaged_moment_uuid = None,
                associations = Nil,
                last_updated = None
            )
            assertEquals(o0, e0)

            // compare associations
            val ea0 = e.associations.sortBy(_.to_concept)
            val oa0 = o
                .associations
                .sortBy(_.to_concept)
                .map(_.copy(uuid = None, last_updated_time = None))
            assertEquals(oa0, ea0)

    }

    test("bulkCreateAnnotations (with images)") {
        val xs          = TestUtils.build(2, 5, 0, 2)
        val expected    = xs
            .flatMap(Annotation.fromImagedMoment(_))
            .map(_.toSnakeCase)
            .sortBy(_.concept)
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStop = newBackendStub(endpoints.bulkCreateAnnotationsImpl)
        val response    = basicRequest
            .post(uri"http://test.com/v1/annotations/bulk")
            .body(expected.stringify)
            .auth
            .bearer(jwt)
            .contentType("application/json")
            .send(backendStop)
            .join
        assertEquals(response.code, StatusCode.Ok)

        // Everything below is snake_case
        val obtained = checkResponse[Seq[AnnotationSC]](response.body).sortBy(_.concept)
        for o <- obtained
        do
            assert(o.observation_uuid.isDefined)
            assert(o.imaged_moment_uuid.isDefined)

        for (o, e) <- obtained.zip(expected)
        do
            // normalize the response and remove assoations
            val e0 =
                e.copy(associations = Nil, image_references = Nil).toCamelCase.removeForeignKeys()
            val o0 = o
                .copy(
                    observation_uuid = None,
                    imaged_moment_uuid = None,
                    associations = Nil,
                    image_references = Nil,
                    last_updated = None
                )
                .toCamelCase
//            println("EXPECTED: " + e0.stringify)
//            println("OBTAINED: " + o0.stringify)
            assertEquals(o0, e0)

            // compare images
            val ea0 = e.image_references.sortBy(_.url.toExternalForm)
            val oa0 = o
                .image_references
                .sortBy(_.url.toExternalForm)
                .map(_.copy(uuid = None, last_updated_time = None))
            assertEquals(oa0, ea0)
    }

    test("createAnnotation (json) with localizations returns them with uuids") {
        val vrUuid      = UUID.randomUUID()
        val json        =
            s"""{"video_reference_uuid":"$vrUuid","concept":"Nanomia","observer":"brian",
               |"recorded_timestamp":"2026-10-28T00:00:00Z",
               |"localizations":[{"localization_type":"bounding_box","x":0.1,"y":0.2,"width":0.3,"height":0.4},
               |{"localization_type":"point","x":0.5,"y":0.6}]}""".stripMargin
        val jwt         = jwtService.authorize("foo").orNull
        val backendStub = newBackendStub(endpoints.createAnnotationImpl)
        val response    = basicRequest
            .post(uri"http://test.com/v1/annotations")
            .body(json)
            .auth
            .bearer(jwt)
            .contentType("application/json")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[AnnotationSC](response.body)
        assertEquals(obtained.localizations.map(_.localization_type).toSet, Set("bounding_box", "point"))
        assert(obtained.localizations.forall(_.uuid.isDefined))
    }

    test("createAnnotation (form) with localizations as a JSON field") {
        val vrUuid      = UUID.randomUUID()
        val locs        = java.net.URLEncoder.encode(
            """[{"localization_type":"point","x":0.5,"y":0.6}]""",
            java.nio.charset.StandardCharsets.UTF_8
        )
        val formData    =
            s"video_reference_uuid=$vrUuid&concept=Nanomia&observer=brian&recorded_timestamp=2026-10-28T00:00:00Z&localizations=$locs"
        val jwt         = jwtService.authorize("foo").orNull
        val backendStub = newBackendStub(endpoints.createAnnotationImpl)
        val response    = basicRequest
            .post(uri"http://test.com/v1/annotations")
            .body(formData)
            .auth
            .bearer(jwt)
            .contentType("application/x-www-form-urlencoded")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[AnnotationSC](response.body)
        assertEquals(obtained.localizations.map(_.localization_type), Seq("point"))
        assert(obtained.localizations.head.uuid.isDefined)
    }

    test("createAnnotation (json) with an invalid localization returns 400 and creates nothing") {
        val vrUuid      = UUID.randomUUID()
        val json        =
            s"""{"video_reference_uuid":"$vrUuid","concept":"Nanomia","observer":"brian",
               |"recorded_timestamp":"2026-10-28T00:00:00Z",
               |"localizations":[{"localization_type":"point","x":0.5,"y":0.6},
               |{"localization_type":"bounding_box","x":0.9,"y":0.2,"width":0.5,"height":0.4}]}""".stripMargin
        val jwt         = jwtService.authorize("foo").orNull
        val backendStub = newBackendStub(endpoints.createAnnotationImpl)
        val response    = basicRequest
            .post(uri"http://test.com/v1/annotations")
            .body(json)
            .auth
            .bearer(jwt)
            .contentType("application/json")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.BadRequest)
        assert(response.body.left.exists(_.contains("annotation 0, localization 1")), response.body.toString)
        assert(controller.findByVideoReferenceUuid(vrUuid).join.isEmpty)
    }

    test("bulkCreateAnnotations with an invalid localization returns 400 and creates nothing") {
        val vrUuid      = UUID.randomUUID()
        val json        =
            s"""[{"video_reference_uuid":"$vrUuid","concept":"A","observer":"brian","recorded_timestamp":"2026-10-28T00:00:00Z"},
               |{"video_reference_uuid":"$vrUuid","concept":"B","observer":"brian","recorded_timestamp":"2026-10-28T00:00:01Z",
               |"localizations":[{"localization_type":"polygon","vertices":[0.1,0.1,0.5,0.1,0.5,0.5]}]},
               |{"video_reference_uuid":"$vrUuid","concept":"C","observer":"brian","recorded_timestamp":"2026-10-28T00:00:02Z",
               |"localizations":[{"localization_type":"point","x":1.5,"y":0.6}]}]""".stripMargin
        val jwt         = jwtService.authorize("foo").orNull
        val backendStub = newBackendStub(endpoints.bulkCreateAnnotationsImpl)
        val response    = basicRequest
            .post(uri"http://test.com/v1/annotations/bulk")
            .body(json)
            .auth
            .bearer(jwt)
            .contentType("application/json")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.BadRequest)
        assert(response.body.left.exists(_.contains("annotation 2, localization 0")), response.body.toString)
        assert(controller.findByVideoReferenceUuid(vrUuid).join.isEmpty)
    }

    test("bulkCreateAnnotations with an over-length localization field returns 400 and creates nothing (DAOs)") {
        val vrUuid      = UUID.randomUUID()
        val url         = s"http://test.com/${UUID.randomUUID()}.png"
        val longGen     = "g" * 129
        val json        =
            s"""[{"video_reference_uuid":"$vrUuid","concept":"A","observer":"brian","recorded_timestamp":"2026-10-28T00:00:00Z",
               |"image_references":[{"url":"$url","format":"image/png","width_pixels":100,"height_pixels":100}]},
               |{"video_reference_uuid":"$vrUuid","concept":"B","observer":"brian","recorded_timestamp":"2026-10-28T00:00:01Z",
               |"localizations":[{"localization_type":"point","x":0.5,"y":0.5,"generator":"$longGen"}]}]""".stripMargin
        val jwt         = jwtService.authorize("foo").orNull
        val backendStub = newBackendStub(endpoints.bulkCreateAnnotationsImpl)
        val response    = basicRequest
            .post(uri"http://test.com/v1/annotations/bulk")
            .body(json)
            .auth
            .bearer(jwt)
            .contentType("application/json")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.BadRequest)
        assert(response.body.left.exists(_.contains("annotation 1, localization 0")), response.body.toString)
        assert(response.body.left.exists(_.contains("generator")), response.body.toString)
        val imDao  = daoFactory.newImagedMomentDAO()
        val obsDao = daoFactory.newObservationDAO(imDao)
        val irDao  = daoFactory.newImageReferenceDAO(imDao)
        try
            assert(imDao.findByVideoReferenceUUID(vrUuid).isEmpty, "no imaged moment")
            assert(obsDao.findByVideoReferenceUuid(vrUuid).isEmpty, "no observation")
            assert(irDao.findByURL(java.net.URI.create(url).toURL).isEmpty, "no image reference")
        finally imDao.close()
    }

    test("bulkCreateAnnotations with localizations persists them") {
        val vrUuid      = UUID.randomUUID()
        val json        =
            s"""[{"video_reference_uuid":"$vrUuid","concept":"A","observer":"brian","recorded_timestamp":"2026-10-28T00:00:00Z"},
               |{"video_reference_uuid":"$vrUuid","concept":"B","observer":"brian","recorded_timestamp":"2026-10-28T00:00:01Z",
               |"localizations":[{"localization_type":"polygon","vertices":[0.1,0.1,0.5,0.1,0.5,0.5]}]}]""".stripMargin
        val jwt         = jwtService.authorize("foo").orNull
        val backendStub = newBackendStub(endpoints.bulkCreateAnnotationsImpl)
        val response    = basicRequest
            .post(uri"http://test.com/v1/annotations/bulk")
            .body(json)
            .auth
            .bearer(jwt)
            .contentType("application/json")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[Seq[AnnotationSC]](response.body)
        val b           = obtained.find(_.concept.contains("B")).get
        assertEquals(b.localizations.map(_.localization_type), Seq("polygon"))
        assert(b.localizations.head.uuid.isDefined)
        assertEquals(obtained.find(_.concept.contains("A")).get.localizations, Nil)
    }

    test("countByConcurrentRequest (CamelCase)") {
        val xs                  = (0 until 5).flatMap(_ => TestUtils.create(2, 2))
        val expected            = xs.flatMap(_.getObservations.asScala).length.toLong
        val ts                  = xs.map(_.getRecordedTimestamp)
        val minTs               = ts.min
        val maxTs               = ts.max.plus(Duration.ofSeconds(1))
        val videoReferenceUuids = xs.map(_.getVideoReferenceUuid).distinct
        val cr                  = ConcurrentRequest(minTs, maxTs, videoReferenceUuids)
        runPost(
            endpoints.countByConcurrentRequestImpl,
            "http://test.com/v1/annotations/concurrent/count",
            cr.stringify,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val crc      = checkResponse[ConcurrentRequestCountSC](response.body)
                val obtained = crc.count
                assertEquals(obtained, expected)
        )
    }

    test("countByConcurrentRequest (snake_case)") {
        val xs                  = (0 until 5).flatMap(_ => TestUtils.create(2, 2))
        val expected            = xs.flatMap(_.getObservations.asScala).length.toLong
        val ts                  = xs.map(_.getRecordedTimestamp)
        val minTs               = ts.min
        val maxTs               = ts.max.plus(Duration.ofSeconds(1))
        val videoReferenceUuids = xs.map(_.getVideoReferenceUuid).distinct
        val cr                  = ConcurrentRequest(minTs, maxTs, videoReferenceUuids).toSnakeCase
        runPost(
            endpoints.countByConcurrentRequestImpl,
            "http://test.com/v1/annotations/concurrent/count",
            cr.stringify,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val crc      = checkResponse[ConcurrentRequestCountSC](response.body)
                val obtained = crc.count
                assertEquals(obtained, expected)
        )
    }

    test("countByMultiRequest (CamelCase)") {
        val xs                  = (0 until 5).flatMap(_ => TestUtils.create(2, 2))
        val expected            = xs.flatMap(_.getObservations.asScala).length.toLong
        val videoReferenceUuids = xs.map(_.getVideoReferenceUuid).distinct
        val mr                  = MultiRequest(videoReferenceUuids)
        runPost(
            endpoints.countByMultiRequestImpl,
            "http://test.com/v1/annotations/multi/count",
            mr.stringify,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val mrc      = checkResponse[MultiRequestCountSC](response.body)
                val obtained = mrc.count
                assertEquals(obtained, expected)
        )
    }

    test("countByMultiRequest (snake_case)") {
        val xs                  = (0 until 5).flatMap(_ => TestUtils.create(2, 2))
        val expected            = xs.flatMap(_.getObservations.asScala).length.toLong
        val videoReferenceUuids = xs.map(_.getVideoReferenceUuid).distinct
        val mr                  = MultiRequest(videoReferenceUuids).toSnakeCase
        runPost(
            endpoints.countByMultiRequestImpl,
            "http://test.com/v1/annotations/multi/count",
            mr.stringify,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val mrc      = checkResponse[MultiRequestCountSC](response.body)
                val obtained = mrc.count
                assertEquals(obtained, expected)
        )
    }

//    test("updateAnnotation (CamelCase json)") {
//        val im = TestUtils.create(1, 1).head
//        val expected = Annotation.from(im.getObservations.iterator.next())
//            .copy(activity = Some("foo"), concept = Some("bar"))
//        val au = AnnotationCreate.fromAnnotation(expected)
//        val jwt = jwtService.authorize("foo").orNull
//        assert(jwt != null)
//        val backendStub = newBackendStub(endpoints.updateAnnotationImpl)
//        val response = basicRequest
//            .put(uri"http://test.com/v1/annotations/${expected.observationUuid}")
//            .body(au.stringify)
//            .auth.bearer(jwt)
//            .contentType("application/json")
//            .send(backendStub)
//            .join
//        assertEquals(response.code, StatusCode.Ok)
//        val obtained = checkResponse[AnnotationSC](response.body).toCamelCase
//        assertEquals(obtained, expected)
//    }

    test("updateAnnotation (snake_case json)") {
        val im          = TestUtils.create(1, 1).head
        val expected    = Annotation
            .from(im.getObservations.iterator.next())
            .copy(activity = Some("foo"), concept = Some("bar"), lastUpdated = None)
            .roundObservationTimestampToMillis()
        val au          = AnnotationCreate.fromAnnotation(expected).toSnakeCase
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.updateAnnotationImpl)
        val response    = basicRequest
            .put(uri"http://test.com/v1/annotations/${expected.observationUuid}")
            .body(au.stringify)
            .auth
            .bearer(jwt)
            .contentType("application/json")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    =
            checkResponse[AnnotationSC](response.body).toCamelCase.copy(lastUpdated = None)
        assertEquals(obtained, expected)
    }

//    test("updateAnnotation (CamelCase form)") {
//        val im = TestUtils.create(1, 1).head
//        val expected = Annotation.from(im.getObservations.iterator.next())
//            .copy(activity = Some("foo"), concept = Some("bar"))
//        val au = AnnotationCreate.fromAnnotation(expected)
//        val jwt = jwtService.authorize("foo").orNull
//        assert(jwt != null)
//        val backendStub = newBackendStub(endpoints.updateAnnotationImpl)
//        val body = Reflect.toFormBody(au)
//        val response = basicRequest
//            .put(uri"http://test.com/v1/annotations/${expected.observationUuid}")
//            .body(body)
//            .auth.bearer(jwt)
//            .contentType("application/x-www-form-urlencoded")
//            .send(backendStub)
//            .join
//        assertEquals(response.code, StatusCode.Ok)
//        val obtained = checkResponse[AnnotationSC](response.body).toCamelCase
//        assertEquals(obtained, expected)
//    }

    test("updateAnnotation (snake_case form)") {
        val im          = TestUtils.create(1, 1).head
        val expected    = Annotation
            .from(im.getObservations.iterator.next())
            .copy(activity = Some("foo"), concept = Some("bar"), lastUpdated = None)
            .roundObservationTimestampToMillis()
        val au          = AnnotationCreate.fromAnnotation(expected).toSnakeCase
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.updateAnnotationImpl)
        val body        = Reflect.toFormBody(au)
        val response    = basicRequest
            .put(uri"http://test.com/v1/annotations/${expected.observationUuid}")
            .body(body)
            .auth
            .bearer(jwt)
            .contentType("application/x-www-form-urlencoded")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    =
            checkResponse[AnnotationSC](response.body).toCamelCase.copy(lastUpdated = None)
        assertEquals(obtained, expected)
    }

    test("bulkUpdateAnnotations") {
        val xs          = TestUtils.create(2, 2)
        val expected    = xs
            .flatMap(Annotation.fromImagedMoment(_))
            .map(_.toSnakeCase)
            .map(_.copy(activity = Some("foofoo"), concept = Some("barbar"), last_updated = None))
            .sortBy(_.concept)
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStop = newBackendStub(endpoints.bulkUpdateAnnotationsImpl)
        val response    = basicRequest
            .put(uri"http://test.com/v1/annotations/bulk")
            .body(expected.stringify)
            .auth
            .bearer(jwt)
            .contentType("application/json")
            .send(backendStop)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[Seq[AnnotationSC]](response.body)
        val corrected   = obtained.map(_.copy(last_updated = None)).sortBy(_.concept)
        assertEquals(corrected, expected)
    }

    test("create (stress test)") {
        val count       = new AtomicInteger(0)
        val jwt         = jwtService.authorize("foo").orNull
        val uuid        = UUID.randomUUID()
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.createAnnotationImpl)
        val n           = 1000
        var threads     = (0 until n)
            .map(i => TestUtils.build(1, 1).head)
            .map(im =>
                val obs = im.getObservations.iterator.next()
                val a   = Annotation.from(obs)
                a.copy(videoReferenceUuid = Some(uuid), timecode = None)
            )
            .map(anno =>
                val r: Runnable = () =>
                    val response = basicRequest
                        .post(uri"http://test.com/v1/annotations")
                        .body(anno.toSnakeCase.stringify)
                        .auth
                        .bearer(jwt)
                        .contentType("application/json")
                        .send(backendStub)
                        .join
                    assertEquals(response.code, StatusCode.Ok)
                    val obtained = checkResponse[AnnotationSC](response.body).toCamelCase
                    val expected = anno.copy(
                        imagedMomentUuid = obtained.imagedMomentUuid,
                        observationUuid = obtained.observationUuid,
                        lastUpdated = obtained.lastUpdated
                    )
                    assertEquals(obtained, expected)
                    count.incrementAndGet()
                r
            )
            .foreach(runnable => Thread.startVirtualThread(runnable))
        while count.get() < n do Thread.sleep(100)

    }
