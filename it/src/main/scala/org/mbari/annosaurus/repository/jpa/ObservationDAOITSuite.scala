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

package org.mbari.annosaurus.repository.jpa

import org.mbari.annosaurus.controllers.TestUtils
import org.mbari.annosaurus.AssertUtils
import java.time.Duration
import org.checkerframework.checker.units.qual.C
import org.mbari.annosaurus.domain.ConcurrentRequest
import org.mbari.annosaurus.domain.MultiRequest
import scala.jdk.CollectionConverters.*


trait ObservationDAOITSuite extends BaseDAOSuite {
    given JPADAOFactory = daoFactory

    test("create") {
        val im = TestUtils.create(1).head
        val obs = TestUtils.randomObservation()
        given dao: ObservationDAOImpl = daoFactory.newObservationDAO()
        val imDao = daoFactory.newImagedMomentDAO(dao)
        run(() => {
            val im0 = imDao.update(im)
            im0.addObservation(obs)
        })
        run(() => dao.findByUUID(obs.getUuid())) match
            case None => fail("should have found the entity")
            case Some(value) => AssertUtils.assertSameObservation(value, obs)
        dao.close()
    }

    test("update") {
        val im = TestUtils.create(1, 1).head
        val obs = im.getObservations().iterator().next()
        given dao: ObservationDAOImpl = daoFactory.newObservationDAO()
        obs.setConcept("new concept")
        run(() => dao.update(obs))
        run(() => dao.findByUUID(obs.getUuid())) match
            case None => fail("should have found the entity")
            case Some(value) => AssertUtils.assertSameObservation(value, obs)
        dao.close()
    }

    test("delete") {
        val im = TestUtils.create(1, 1).head
        val obs = im.getObservations().iterator().next()
        given dao: ObservationDAOImpl = daoFactory.newObservationDAO()
        run(() => {
            val obs0 = dao.update(obs)
            obs0.getImagedMoment().removeObservation(obs0)
            // dao.delete(obs0) // DOn't call delete, just remove from parent
        })
        run(() => dao.findByUUID(obs.getUuid())) match
            case None => // OK
            case Some(value) => fail("should not have found the entity")
        dao.close()
    }

    test("deleteByUUID") {
        val im = TestUtils.create(1, 1).head
        val obs = im.getObservations().iterator().next()
        given dao: ObservationDAOImpl = daoFactory.newObservationDAO()
        run(() => {
            dao.deleteByUUID(obs.getUuid())
        })
        run(() => dao.findByUUID(obs.getUuid())) match
            case None => // OK
            case Some(value) => fail("should not have found the entity")
        dao.close()
    }

    test("findByUUID") {
        val im = TestUtils.create(1, 1).head
        val obs = im.getObservations().iterator().next()
        given dao: ObservationDAOImpl = daoFactory.newObservationDAO()
        run(() => dao.findByUUID(obs.getUuid())) match
            case None => fail("should have found the entity")
            case Some(value) => AssertUtils.assertSameObservation(value, obs)
        dao.close()
    }

    test("findByVideoReferenceUUID") {
        val im = TestUtils.create(1, 1).head
        val obs = im.getObservations().iterator().next()
        given dao: ObservationDAOImpl = daoFactory.newObservationDAO()
        val xs = run(() => dao.findByVideoReferenceUUID(im.getVideoReferenceUuid()))
        assert(xs.size >= 1)
        val opt = xs.filter(_.getUuid() == obs.getUuid()).headOption
        assert(opt.isDefined)
        AssertUtils.assertSameObservation(opt.get, obs)
        dao.close()
    }

    test("countByVideoReferenceUUIDAndTimestamps".flaky) {
        val xs = TestUtils.create(10, 1)
        given dao: ObservationDAOImpl = daoFactory.newObservationDAO()
        val ts = xs.map(_.getRecordedTimestamp())
        val t0 = ts.min.minus(Duration.ofSeconds(1))
        val t1 = ts.max.plus(Duration.ofSeconds(1))
        val count = run(() => dao.countByVideoReferenceUUIDAndTimestamps(
            xs.head.getVideoReferenceUuid(),
            t0,
            t1
        ))
        assertEquals(count, xs.size)
        dao.close()
    }

    test("countByConcurrentRequest") {
        val xs = TestUtils.create(5, 1) ++ TestUtils.create(5, 1)
        given dao: ObservationDAOImpl = daoFactory.newObservationDAO()
        val ts = xs.map(_.getRecordedTimestamp())
        val t0 = ts.min.minus(Duration.ofSeconds(1))
        val t1 = ts.max.plus(Duration.ofSeconds(1))
        val cr = new ConcurrentRequest(t0, t1, xs.map(_.getVideoReferenceUuid()).distinct)
        val count = run(() => dao.countByConcurrentRequest(cr))
        assertEquals(count, xs.size.longValue())
        dao.close()
    }

    test("countByMultiRequest") {
        val xs = TestUtils.create(5, 1) ++ TestUtils.create(5, 1)
        given dao: ObservationDAOImpl = daoFactory.newObservationDAO()
        val mr = new MultiRequest(xs.map(_.getVideoReferenceUuid()).distinct)
        val count = run(() => dao.countByMultiRequest(mr))
        assertEquals(count, xs.size.longValue())
        dao.close()
    }

    test("findAllConcepts") {
        val xs = TestUtils.create(5, 1)
        val existingConcepts = xs.flatMap(_.getObservations().asScala.map(_.getConcept()))
        given dao: ObservationDAOImpl = daoFactory.newObservationDAO()
        val concepts = run(() => dao.findAllConcepts())
        assert(concepts.size >= xs.size)
        for (c <- existingConcepts) assert(concepts.contains(c))
        dao.close()
    }

    test("findAllGroups") {
        val xs = TestUtils.create(5, 1)
        val existingGroups = xs.flatMap(_.getObservations().asScala.map(_.getGroup())).filter(_ != null)
        given dao: ObservationDAOImpl = daoFactory.newObservationDAO()
        val groups = run(() => dao.findAllGroups())
        assert(groups.size >= xs.size)
        for (g <- existingGroups) assert(groups.contains(g))
        dao.close()
    }

    test("findAllActivities") {
        val xs = TestUtils.create(5, 1)
        val existingActivities = xs.flatMap(_.getObservations().asScala.map(_.getActivity())).filter(_ != null)
        given dao: ObservationDAOImpl = daoFactory.newObservationDAO()
        val activities = run(() => dao.findAllActivities())
        assert(activities.size >= xs.size)
        for (a <- existingActivities) assert(activities.contains(a))
        dao.close()
    }

    test("findAll") {
        val xs = TestUtils.create(1, 1)
        val obs = xs.head.getObservations().iterator().next()
        given dao: ObservationDAOImpl = daoFactory.newObservationDAO()
        val observations = run(() => dao.findAll())
        assert(observations.size >= xs.size)
        observations.filter(x => x.getUuid() == obs.getUuid()) match
            case Nil => fail("should have found the entity")
            case _ => // OK
        dao.close()
    }

    test("countByConcept") {
        val xs = TestUtils.create(5, 1)
        val existingConcepts = xs.flatMap(_.getObservations().asScala.map(_.getConcept()))
        given dao: ObservationDAOImpl = daoFactory.newObservationDAO()
        for (c <- existingConcepts) {
            val count = run(() => dao.countByConcept(c))
            assert(count == 1)
        }
        dao.close()
    }

    test("countByConceptWithImages") {
        val xs = TestUtils.create(3, 1)
        val existingConcepts = xs.flatMap(_.getObservations().asScala.map(_.getConcept()))
        given dao: ObservationDAOImpl = daoFactory.newObservationDAO()
        for (c <- existingConcepts) {
            val count = run(() => dao.countByConceptWithImages(c))
            assert(count == 0)
        }

        val ys = TestUtils.create(3, 1, 0, 1)
        val existingConcepts2 = ys.flatMap(_.getObservations().asScala.map(_.getConcept()))
        for (c <- existingConcepts2) {
            val count = run(() => dao.countByConceptWithImages(c))
            assert(count == 1)
        }
        dao.close()
    }

    test("countByVideoReferenceUUID") {
        val xs = TestUtils.create(4, 1)
        val videoReferenceUuid = xs.head.getVideoReferenceUuid()
        given dao: ObservationDAOImpl = daoFactory.newObservationDAO()
        val count = run(() => dao.countByVideoReferenceUUID(videoReferenceUuid))
        assertEquals(count, xs.size)
        dao.close()
    }

    test("countAllByVideoReferenceUuids") {
        val xs = TestUtils.create(2, 1) ++ TestUtils.create(3, 1)
        given dao: ObservationDAOImpl = daoFactory.newObservationDAO()
        val counts = run(() => dao.countAllByVideoReferenceUuids())
        
        for 
            (uuid, n) <- xs.groupBy(_.getVideoReferenceUuid())
        do
            assert(counts.contains(uuid))
            assertEquals(counts(uuid), n.size)
        dao.close()
    }

    test("updateConcept") {
        val xs = TestUtils.create(1, 1).head
        val oldConcept = xs.getObservations().iterator().next().getConcept()
        val newConcept = "foobarbim"
        given dao: ObservationDAOImpl = daoFactory.newObservationDAO()
        val count = run(() => dao.updateConcept(oldConcept, newConcept))
        assertEquals(count, 1)
        dao.close()
    }

    test("changeImageMoment") {
        val a = TestUtils.create(1, 1).head
        val b = TestUtils.create(1, 0).head
        val obs = a.getObservations().iterator().next()
        given dao: ObservationDAOImpl = daoFactory.newObservationDAO()
        val count = run(() => dao.changeImageMoment(b.getUuid(), obs.getUuid()))
        assertEquals(count, 1)
        run(() => dao.findByUUID(obs.getUuid())) match
            case None => fail("should have found the entity")
            case Some(obs2) =>
                assertEquals(obs2.getImagedMoment().getUuid(), b.getUuid())
        
        val imDao = daoFactory.newImagedMomentDAO(dao)
        run(() => imDao.findByUUID(a.getUuid())) match
            case None => fail("should have found the entity")
            case Some(im) =>
                assertEquals(im.getObservations().size(), 0)

        dao.close()

    }

}
