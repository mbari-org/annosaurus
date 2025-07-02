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

import org.mbari.annosaurus.AssertUtils
import org.mbari.annosaurus.repository.jpa.{BaseDAOSuite, JPADAOFactory}

import java.time.Duration
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.*

trait ObservationControllerSuite extends BaseDAOSuite:

    given JPADAOFactory    = daoFactory
    given ExecutionContext = ExecutionContext.global
    lazy val controller    = ObservationController(daoFactory)
    private val log        = System.getLogger(getClass.getName)

    test("findAll") {
        val xs       = TestUtils.create(1, 1)
        val expected = xs.flatMap(im => im.getObservations().asScala).toSet
        val obtained = exec(controller.findAll())
        assertEquals(obtained.size, 1)
        AssertUtils.assertSameObservation(expected.head, obtained.head.toEntity)
    }

    test("findByUUID") {
        val xs       = TestUtils.create(1, 1)
        val expected = xs.flatMap(im => im.getObservations().asScala).toSet
        val obtained = exec(controller.findByUUID(expected.head.getUuid()))
        assertEquals(obtained.size, 1)
        AssertUtils.assertSameObservation(expected.head, obtained.head.toEntity)
    }

    test("create using params") {
        val im   = TestUtils.create(1).head
        val obs0 = TestUtils.randomObservation()
        val obs1 = exec(
            controller.create(
                im.getUuid(),
                obs0.getConcept(),
                obs0.getObserver(),
                obs0.getObservationTimestamp(),
                Option(obs0.getDuration()),
                Option(obs0.getGroup()),
                Option(obs0.getActivity())
            )
        )
        assert(obs1.uuid.isDefined)
        obs0.setUuid(obs1.uuid.orNull)
        AssertUtils.assertSameObservation(obs0, obs1.toEntity)
    }

    test("update using params") {
        val im = TestUtils.create(1, 1).head
        val o0 = im.getObservations().iterator().next()

        val newConcept  = "foobar"
        val newActivity = "baz"
        // explicitly keep old observationTimestamp for testing
        val opt         = exec(
            controller.update(
                o0.getUuid(),
                concept = Option(newConcept),
                activity = Option(newActivity),
                observationDate = o0.getObservationTimestamp()
            )
        )
        assert(opt.isDefined)

        o0.setConcept(newConcept)
        o0.setActivity(newActivity)
        AssertUtils.assertSameObservation(o0, opt.get.toEntity)
    }

    test("findAllConcepts") {
        val xs           = TestUtils.create(1, 4)
        val expected     = xs
            .flatMap(im => im.getObservations().asScala.map(_.getConcept()))
            .toSet
            .filter(_ != null)
        val obtained     = exec(controller.findAllConcepts)
        val intersection = obtained.toSet.intersect(expected)
        assertEquals(intersection, expected)
    }

    test("findAllGroups") {
        val xs           = TestUtils.create(1, 8)
        val expected     =
            xs.flatMap(im => im.getObservations().asScala.map(_.getGroup())).toSet.filter(_ != null)
        val obtained     = exec(controller.findAllGroups)
        val intersection = obtained.toSet.intersect(expected)
        assertEquals(intersection, expected)
    }

    test("findAllActivities") {
        val xs           = TestUtils.create(1, 9)
        val expected     = xs
            .flatMap(im => im.getObservations.asScala.map(_.getActivity()))
            .toSet
            .filter(_ != null)
        val obtained     = exec(controller.findAllActivities)
        val intersection = obtained.toSet.intersect(expected)
        assertEquals(intersection, expected)
    }

    test("findAlLConceptsByVideoReferenceUuid") {
        val xs           = TestUtils.create(1, 10)
        val expected     = xs
            .flatMap(im => im.getObservations().asScala.map(_.getConcept()))
            .toSet
            .filter(_ != null)
        val obtained     =
            exec(controller.findAllConceptsByVideoReferenceUuid(xs.head.getVideoReferenceUuid()))
        val intersection = obtained.toSet.intersect(expected)
        assertEquals(intersection, expected)
    }

    test("findByVideoReferenceUuid") {
        val xs       = TestUtils.create(1, 1)
        val expected = xs.flatMap(im => im.getObservations().asScala).toSet
        val obtained = exec(controller.findByVideoReferenceUuid(xs.head.getVideoReferenceUuid()))
        assertEquals(obtained.size, 1)
        AssertUtils.assertSameObservation(expected.head, obtained.head.toEntity)
    }

    test("findByAssociationUuid") {
        val im       = TestUtils.create(1, 2, 1).head
        val o0       = im.getObservations().iterator().next()
        val a0       = o0.getAssociations().iterator().next()
        val obtained = exec(controller.findByAssociationUuid(a0.getUuid()))
        AssertUtils.assertSameObservation(o0, obtained.get.toEntity)
    }

    test("delete") {
        val im       = TestUtils.create(1, 2).head
        val o0       = im.getObservations().iterator().next()
        val uuid     = o0.getUuid()
        val deleted  = exec(controller.delete(uuid))
        assert(deleted)
        val obtained = exec(controller.findByUUID(uuid))
        assert(obtained.isEmpty)
    }

    test("deleteDuration") {
        val im      = TestUtils.create(1, 1).head
        val o0      = im.getObservations().iterator().next()
        o0.setDuration(Duration.ofSeconds(5))
        // We explcitly keep the observationTimestamp or it will be changed by the update method
        exec(controller.update(o0.getUuid(), observationDate = o0.getObservationTimestamp(), duration = Option(o0.getDuration())))
        val sanity  = exec(controller.findByUUID(o0.getUuid()))
        assertEquals(sanity.get.duration.orNull, o0.getDuration())
        val deleted = exec(controller.deleteDuration(o0.getUuid()))
        assert(deleted.isDefined)
        o0.setDuration(null)
        AssertUtils.assertSameObservation(o0, deleted.get.toEntity)
    }

    test("bulkDelete") {
        val xs       = TestUtils.create(2, 2)
        val obsUuids = xs.flatMap(im => im.getObservations().asScala.map(_.getUuid()))
        val ok       = exec(controller.bulkDelete(obsUuids))
        assert(ok)
        for uuid <- obsUuids
        do
            val obtained = exec(controller.findByUUID(uuid))
            assert(obtained.isEmpty)
    }

    test("countByConcept") {
        val xs      = TestUtils.create(1, 1)
        val concept = xs.head.getObservations().iterator().next().getConcept()
        val count   = exec(controller.countByConcept(concept))
        assertEquals(count, 1)
    }

    test("countByConceptWithImages") {

        // no images
        val xs      = TestUtils.create(1, 1)
        val concept = xs.head.getObservations().iterator().next().getConcept()
        val count   = exec(controller.countByConceptWithImages(concept))
        assertEquals(count, 0)

        // with images
        val xs2      = TestUtils.create(1, 1, 0, 1)
        val concept2 = xs2.head.getObservations().iterator().next().getConcept()
        val count2   = exec(controller.countByConceptWithImages(concept2))
        assertEquals(count2, 1)
    }

    test("countByVideoReferenceUuid") {
        val xs    = TestUtils.create(2, 2)
        val uuid  = xs.head.getVideoReferenceUuid()
        val count = exec(controller.countByVideoReferenceUuid(uuid))
        assertEquals(count, 4)
    }

    test("countByVideoReferenceUuidAndTimestamps") {
        val xs    = TestUtils.create(1, 1, 1, 1)
        val im    = xs.head
        val o0    = im.getObservations().iterator().next()
        val start = im.getRecordedTimestamp().minusSeconds(1)
        val end   = im.getRecordedTimestamp().plusSeconds(1)
        val count = exec(
            controller.countByVideoReferenceUuidAndTimestamps(
                im.getVideoReferenceUuid(),
                start,
                end
            )
        )
        assertEquals(count, 1)

        val start2 = im.getRecordedTimestamp().minusSeconds(10)
        val end2   = im.getRecordedTimestamp().minusSeconds(9)
        val count2 = exec(
            controller.countByVideoReferenceUuidAndTimestamps(
                im.getVideoReferenceUuid(),
                start2,
                end2
            )
        )
        assertEquals(count2, 0)
    }

    test("countAllGroupByVideoReferenceUuid") {
        val xs     = TestUtils.create(2, 2)
        val n      = xs.flatMap(im => im.getObservations().asScala).size
        val counts = exec(controller.countAllGroupByVideoReferenceUuid())
        assertEquals(counts.size, 1)
        val uuids  = xs.map(_.getVideoReferenceUuid())
        for uuid <- uuids
        do
            assert(counts.contains(uuid))
            assertEquals(counts(uuid), n)
    }

    test("udpateConcept") {
        val xs         = TestUtils.create(1, 1)
        val im         = xs.head
        val o0         = im.getObservations().iterator().next()
        val oldConcept = o0.getConcept()
        val newConcept = "foobar"
        val count      = exec(controller.updateConcept(oldConcept, newConcept))
        assertEquals(count, 1)
        val obtained   = exec(controller.findByUUID(o0.getUuid()))

        o0.setConcept(newConcept)
        AssertUtils.assertSameObservation(o0, obtained.get.toEntity)
    }
