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
                val expected = Annotation.from(obs)
                    .copy(lastUpdated = obtained.lastUpdated)
                    .roundObservationTimestampToMillis()
                assertEquals(obtained, expected)
        )
    }

    test("findAnnotationByUuid (404)") {
        runGet(
            endpoints.findAnnotationByUuidImpl,
            s"http://test.com/v1/annotations/00000000-0000-0000-0000-000000000000",
            response => assertEquals(response.code, StatusCode.NotFound)
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
        val xs          = TestUtils.build(2, 5, 2)
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
