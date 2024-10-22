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

import org.mbari.annosaurus.controllers.{ImagedMomentController, ObservationController, TestUtils}
import org.mbari.annosaurus.domain.{
    ConceptCount,
    Count,
    CountForVideoReferenceSC,
    Observation,
    ObservationSC,
    ObservationUpdateSC,
    ObservationsUpdate,
    RenameConcept,
    RenameCountSC
}
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.etc.sdk.Futures.join
import org.mbari.annosaurus.etc.sdk.Reflect
import org.mbari.annosaurus.repository.jdbc.JdbcRepository
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import sttp.client3.*
import sttp.model.StatusCode

import java.time.Duration
import java.util.UUID
import scala.jdk.CollectionConverters.*
import scala.util.Random

trait ObservationEndpointsSuite extends EndpointsSuite:

    private val log = System.getLogger(getClass.getName)

    given JPADAOFactory = daoFactory

    given jwtService: JwtService    = new JwtService("mbari", "foo", "bar")
    private lazy val controller     = ObservationController(daoFactory)
    private lazy val jdbcRepository = new JdbcRepository(daoFactory.entityManagerFactory)
    private lazy val endpoints      = new ObservationEndpoints(controller, jdbcRepository)

    test("findObservationByUuid") {
        val im          = TestUtils.create(1, 1).head
        val observation = im.getObservations.iterator().next()
        val uuid        = observation.getUuid
        runGet(
            endpoints.findObservationByUuidImpl,
            s"http://test.com/v1/observations/$uuid",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val expected = Observation.from(observation, true)
                val obtained = checkResponse[ObservationSC](response.body).toCamelCase
                assertEquals(obtained, expected)
        )
    }

    test("findObservationsByVideoReferenceUuid") {
        val xs                 = TestUtils.create(2, 2)
        val videoReferenceUuid = xs.head.getVideoReferenceUuid
        runGet(
            endpoints.findObservationsByVideoReferenceUuidImpl,
            s"http://test.com/v1/observations/videoreference/$videoReferenceUuid",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val expected = xs
                    .flatMap(_.getObservations.asScala)
                    .map(Observation.from(_, true))
                    .sortBy(_.uuid)
                    .toSet
                val obtained = checkResponse[Seq[ObservationSC]](response.body)
                    .map(_.toCamelCase)
                    .sortBy(_.uuid)
                    .toSet
                assertEquals(obtained, expected)
        )
    }

    test("findActivities") {
        val xs                 = TestUtils.create(4, 4)
        val videoReferenceUuid = xs.head.getVideoReferenceUuid
        val expected           = xs
            .flatMap(_.getObservations.asScala.map(_.getActivity))
            .distinct
            .filter(_ != null)
            .sorted
        runGet(
            endpoints.findActivitiesImpl,
            s"http://test.com/v1/observations/activities",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[String]](response.body).sorted
                for a <- expected
                do assert(obtained.contains(a))
        )
    }

    test("findObservationByAssociationUuid") {
        val im              = TestUtils.create(1, 1, 1).head
        val associationUuid =
            im.getObservations.iterator().next().getAssociations.iterator().next().getUuid
        runGet(
            endpoints.findObservationByAssociationUuidImpl,
            s"http://test.com/v1/observations/association/$associationUuid",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val expected = Observation.from(im.getObservations.iterator().next(), true)
                val obtained = checkResponse[ObservationSC](response.body).toCamelCase
                assertEquals(obtained, expected)
        )
    }

    test("findAllConcepts") {
        val xs       = TestUtils.create(1, 6).head
        val expected = xs
            .getObservations
            .asScala
            .map(_.getConcept)
            .filter(_ != null)
            .toSeq
            .sorted
        runGet(
            endpoints.findAllConceptsImpl,
            s"http://test.com/v1/observations/concepts",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[String]](response.body).sorted
                for c <- expected
                do assert(obtained.contains(c))
        )
    }

    test("findConceptsByVideoReferenceUuid") {
        val xs                 = TestUtils.create(1, 6).head
        val videoReferenceUuid = xs.getVideoReferenceUuid
        val expected           = xs
            .getObservations
            .asScala
            .map(_.getConcept)
            .filter(_ != null)
            .toSeq
            .sorted
        runGet(
            endpoints.findConceptsByVideoReferenceUuidImpl,
            s"http://test.com/v1/observations/concepts/$videoReferenceUuid",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[String]](response.body).sorted
                assertEquals(obtained.size, expected.size)
                for c <- expected
                do assert(obtained.contains(c))
        )
    }

    test("countObservationsByConcept") {
        val xs       = TestUtils.build(6, 1)
        val expected = xs.flatMap(_.getObservations.asScala).size
        val concept  = s"concept-${xs.head.getVideoReferenceUuid}"
        for
            x   <- xs
            obs <- x.getObservations.asScala
        do obs.setConcept(concept)
//        val imagedMoments = xs.map(x => ImagedMoment.from(x, true))
        val controller = ImagedMomentController(daoFactory)
        val ys         = controller.create(xs).join
        runGet(
            endpoints.countObservationsByConceptImpl,
            s"http://test.com/v1/observations/concept/count/$concept",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[ConceptCount](response.body)
                assertEquals(obtained.concept, concept)
                assertEquals(obtained.count, expected.longValue)
        )
    }

    test("countImagesByConcept") {
        val seed     = TestUtils.build(1, 6, 0, 1).head
        seed.getObservations.asScala.foreach(_.setConcept(s"concept-${seed.getVideoReferenceUuid}"))
        val c        = ImagedMomentController(daoFactory)
        val im       = c.create(Seq(seed)).join.head
        val concept  = im.observations.head.concept
        val expected = im.observations.size
        runGet(
            endpoints.countImagesByConceptImpl,
            s"http://test.com/v1/observations/concept/images/count/$concept",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[ConceptCount](response.body)
                assertEquals(obtained.concept, concept)
                assertEquals(obtained.count, expected.longValue)
        )
    }

    test("findGroups") {

        val xs                 = TestUtils.create(4, 4)
        val videoReferenceUuid = xs.head.getVideoReferenceUuid
        val expected           = xs
            .flatMap(_.getObservations.asScala.map(_.getGroup))
            .distinct
            .filter(_ != null)
            .sorted
        runGet(
            endpoints.findGroupsImpl,
            "http://test.com/v1/observations/groups",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[String]](response.body).sorted
                for a <- expected
                do assert(obtained.contains(a))
        )
    }

    test("countByVideoReferenceUuid") {
        val im                 = TestUtils.create(1, 2).head
        val videoReferenceUuid = im.getVideoReferenceUuid
        val expected           = im.getObservations.size

        // Test with no start/end
        runGet(
            endpoints.countByVideoReferenceUuidImpl,
            s"http://test.com/v1/observations/videoreference/count/$videoReferenceUuid",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[CountForVideoReferenceSC](response.body)
                assertEquals(obtained.count, expected)
        )

        // Test with start/end
        val t0 = im.getRecordedTimestamp.minus(Duration.ofSeconds(1))
        val t1 = im.getRecordedTimestamp.plus(Duration.ofSeconds(1))
        runGet(
            endpoints.countByVideoReferenceUuidImpl,
            s"http://test.com/v1/observations/videoreference/count/$videoReferenceUuid?start=$t0&end=$t1",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[CountForVideoReferenceSC](response.body)
                assertEquals(obtained.count, expected)
        )

        // Test with start/end that should return no results
        val t2 = im.getRecordedTimestamp.plus(Duration.ofSeconds(2))
        runGet(
            endpoints.countByVideoReferenceUuidImpl,
            s"http://test.com/v1/observations/videoreference/count/$videoReferenceUuid?start=$t1&end=$t2",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[CountForVideoReferenceSC](response.body)
                assertEquals(obtained.count, 0)
        )

        // Test with bogus videoreference uuid but valid start/end that should return no results
        runGet(
            endpoints.countByVideoReferenceUuidImpl,
            s"http://test.com/v1/observations/videoreference/count/${UUID.randomUUID()}?start=$t0&end=$t1",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[CountForVideoReferenceSC](response.body)
                assertEquals(obtained.count, 0)
        )
    }

    test("countAllGroupByVideoReferenceUuid") {
        val im                 = TestUtils.create(1, 3).head
        val videoReferenceUuid = im.getVideoReferenceUuid
        runGet(
            endpoints.countAllGroupByVideoReferenceUuidImpl,
            s"http://test.com/v1/observations/counts",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val results = checkResponse[Seq[CountForVideoReferenceSC]](response.body)
                results.find(_.video_reference_uuid == videoReferenceUuid) match
                    case Some(result) => assertEquals(result.count, im.getObservations.size)
                    case None         =>
                        fail(
                            s"Expected to find a result with videoreference uuid $videoReferenceUuid"
                        )
        )
    }

    test("renameConcept (json)") {
        val seed          = TestUtils.build(2, 1, 0, 1)
        val expectedCount = seed.flatMap(_.getObservations.asScala).size
        val concept       = s"concept-${seed.head.getVideoReferenceUuid}"
        for
            im  <- seed
            obs <- im.getObservations.asScala
        do obs.setConcept(concept)
        val c  = ImagedMomentController(daoFactory)
        val xs = c.create(seed).join

        val jwt        = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val newConcept = s"new-concept-${seed.head.getVideoReferenceUuid}"
        val dto        = RenameConcept(newConcept, concept)
        val body       = dto.stringify
        val backend    = newBackendStub(endpoints.renameConceptImpl)
        val response   = basicRequest
            .put(uri"http://test.com/v1/observations/concept/rename?old=$concept&new=$newConcept")
            .auth
            .bearer(jwt)
            .body(body)
            .contentType("application/json")
            .send(backend)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained   = checkResponse[RenameCountSC](response.body)
        assertEquals(obtained.old_concept, concept)
        assertEquals(obtained.new_concept, newConcept)
        assertEquals(obtained.count, expectedCount.longValue)

        // Check that the concept was renamed
        val d  = ObservationController(daoFactory)
        val v1 = d.findAllConcepts.join.toSet
        assert(v1.contains(newConcept))
        assert(!v1.contains(concept))
    }

    test("renameConcept (form)") {
        val seed          = TestUtils.build(2, 1, 0, 1)
        val expectedCount = seed.flatMap(_.getObservations.asScala).size
        val concept       = s"concept-${seed.head.getVideoReferenceUuid}"
        for
            im  <- seed
            obs <- im.getObservations.asScala
        do obs.setConcept(concept)
        val c  = ImagedMomentController(daoFactory)
        val xs = c.create(seed).join

        val jwt        = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val newConcept = s"new-concept-${seed.head.getVideoReferenceUuid}"
        val dto        = RenameConcept(newConcept, concept)
        val body       = Reflect.toFormBody(dto)
        val backend    = newBackendStub(endpoints.renameConceptImpl)
        val response   = basicRequest
            .put(uri"http://test.com/v1/observations/concept/rename?old=$concept&new=$newConcept")
            .auth
            .bearer(jwt)
            .body(body)
            .contentType("application/x-www-form-urlencoded")
            .send(backend)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained   = checkResponse[RenameCountSC](response.body)
        assertEquals(obtained.old_concept, concept)
        assertEquals(obtained.new_concept, newConcept)
        assertEquals(obtained.count, expectedCount.longValue)

        // Check that the concept was renamed
        val d  = ObservationController(daoFactory)
        val v1 = d.findAllConcepts.join.toSet
        assert(v1.contains(newConcept))
        assert(!v1.contains(concept))
    }

    test("updateOneObservation (json)") {
        val obs       = TestUtils.create(1, 1).head.getObservations.iterator().next()
        val update    = ObservationUpdateSC(
            concept = Some("new-concept"),
            observer = Some("new-observer"),
            activity = Some("new-activity"),
            group = Some("new-group"),
            duration_millis = Some(10L)
        )
        val jwt       = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backend   = newBackendStub(endpoints.updateOneObservationImpl)
        val response0 = basicRequest
            .put(uri"http://test.com/v1/observations/${obs.getUuid}")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/json")
            .body(update.stringify)
            .send(backend)
            .join
        assertEquals(response0.code, StatusCode.Ok)
        val obtained  = checkResponse[ObservationSC](response0.body)
        assertEquals(obtained.concept, update.concept.get)
        assertEquals(obtained.observer, update.observer)
        assertEquals(obtained.activity, update.activity)
        assertEquals(obtained.group, update.group)
        assertEquals(obtained.duration_millis, update.duration_millis)
        assertEquals(obtained.uuid.get, obs.getUuid)

        // TODO test moving to new imagedmoment
    }

    test("updateOneObservation (form)") {
        val obs       = TestUtils.create(1, 1).head.getObservations.iterator().next()
        val n         = Random.nextInt(100000)
        val d         = Random.nextLong()
        val update    = ObservationUpdateSC(
            concept = Some("new-concept" + n),
            observer = Some("new-observer" + n),
            activity = Some("new-activity" + n),
            group = Some("new-group" + n),
            duration_millis = Some(d)
        )
        val formData  = Reflect.toFormBody(update)
        val jwt       = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backend   = newBackendStub(endpoints.updateOneObservationImpl)
        val response0 = basicRequest
            .put(uri"http://test.com/v1/observations/${obs.getUuid}")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(formData)
            .send(backend)
            .join
        assertEquals(response0.code, StatusCode.Ok)
        val obtained  = checkResponse[ObservationSC](response0.body)
        assertEquals(obtained.concept, update.concept.get)
        assertEquals(obtained.observer, update.observer)
        assertEquals(obtained.activity, update.activity)
        assertEquals(obtained.group, update.group)
        assertEquals(obtained.duration_millis, update.duration_millis)
        assertEquals(obtained.uuid.get, obs.getUuid)
    }

    test("deleteDuration") {
        val xs       = TestUtils.build(1, 1).head
        val obs      = xs.getObservations.iterator().next()
        obs.setDuration(Duration.ofSeconds(100L))
        val c        = ImagedMomentController(daoFactory)
        val im       = c.create(Seq(xs)).join.head
        assert(im.observations.head.duration.isDefined)
        val jwt      = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backend  = newBackendStub(endpoints.deleteDurationImpl)
        val response = basicRequest
            .put(uri"http://test.com/v1/observations/delete/duration/${obs.getUuid}")
            .header("Authorization", s"Bearer $jwt")
            .send(backend)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained = checkResponse[ObservationSC](response.body)
        assertEquals(obtained.duration_millis, None)
    }

    test("deleteOneObservation") {
        val im       = TestUtils.create(1, 1).head
        val obs      = im.getObservations.iterator().next()
        val jwt      = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backend  = newBackendStub(endpoints.deleteOneObservationImpl)
        val response = basicRequest
            .delete(uri"http://test.com/v1/observations/${obs.getUuid}")
            .header("Authorization", s"Bearer $jwt")
            .send(backend)
            .join
        assertEquals(response.code, StatusCode.NoContent)

        val c        = ObservationController(daoFactory)
        val obtained = c.findByUUID(obs.getUuid).join
        assertEquals(obtained, None)
    }

    test("deleteManyObservations") {
        val im               = TestUtils.create(4, 2)
        val observationUuids = im.flatMap(_.getObservations.asScala.map(_.getUuid)).toSeq
        val jwt              = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backend          = newBackendStub(endpoints.deleteManyObservationsImpl)
        val response         = basicRequest
            .post(uri"http://test.com/v1/observations/delete")
            .auth
            .bearer(jwt)
            .contentType("application/json")
            .body(observationUuids.stringify)
            .send(backend)
            .join
        assertEquals(response.code, StatusCode.NoContent)

        for
            i <- im
            o <- i.getObservations.asScala
        do
            val obtained = controller.findByUUID(o.getUuid).join
            assertEquals(obtained, None)
    }

    test("updateManyObservations") {
        val im               = TestUtils.create(20, 2)
        val observationUuids = im.flatMap(_.getObservations.asScala.map(_.getUuid)).toSeq
        val update           = ObservationsUpdate(
            observationUuids,
            concept = Some("new-concept"),
            observer = Some("new-observer"),
            activity = Some("new-activity"),
            group = Some("new-group")
        )
        val jwt              = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backend          = newBackendStub(endpoints.updateManyObservationsImpl)
        val response         = basicRequest
            .put(uri"http://test.com/v1/observations/bulk")
            .auth
            .bearer(jwt)
            .contentType("application/json")
            .body(update.stringify)
            .send(backend)
            .join
        val obtained         = checkResponse[Count](response.body)
        assertEquals(obtained.count, observationUuids.size.toLong)

        for uuid <- observationUuids
        do
            val opt      = controller.findByUUID(uuid).join
            assert(opt.isDefined)
            val obtained = opt.get
            assertEquals(obtained.concept, update.concept.get)
            assertEquals(obtained.observer, update.observer)
            assertEquals(obtained.activity, update.activity)
            assertEquals(obtained.group, update.group)

    }
