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

import io.reactivex.rxjava3.subjects.{PublishSubject, Subject}
import org.mbari.annosaurus.domain.Localization
import org.mbari.annosaurus.etc.zeromq.{AnnotationCreatedMessage, AssociationCreatedMessage, LocalizationCreatedMessage}
import org.mbari.annosaurus.repository.NotFoundInDatastoreException
import org.mbari.annosaurus.repository.jpa.{BaseDAOSuite, JPADAOFactory}

import java.util.UUID
import scala.jdk.CollectionConverters.*

trait LocalizationControllerSuite extends BaseDAOSuite:

    implicit val df: JPADAOFactory = daoFactory

    override def beforeAll(): Unit = daoFactory.beforeAll()
    override def afterAll(): Unit  = daoFactory.afterAll()

    lazy val controller = new LocalizationController(daoFactory)

    private def newObservationUuid(): UUID =
        TestUtils.create(1, 1).head.getObservations.asScala.head.getUuid

    private def box(x: Double = 0.1, generator: Option[String] = None): Localization =
        Localization("bounding_box", x = Some(x), y = Some(0.1), width = Some(0.2), height = Some(0.2), generator = generator)

    test("upsert creates a localization with a uuid and lastUpdated") {
        val obsUuid = newObservationUuid()
        val a       = exec(controller.upsert(obsUuid, "bounding_box", box()))
        assert(a.uuid.isDefined)
        assert(a.lastUpdated.isDefined)
        assertEquals(a.observationUuid, Some(obsUuid))
        assertEquals(exec(controller.findByObservationUuid(obsUuid)).map(_.uuid), Seq(a.uuid))
    }

    test("upsert on the same key is idempotent and keeps the uuid") {
        val obsUuid = newObservationUuid()
        val a       = exec(controller.upsert(obsUuid, "bounding_box", box(0.1)))
        val b       = exec(controller.upsert(obsUuid, "bounding_box", box(0.3)))
        assertEquals(b.uuid, a.uuid)
        val xs      = exec(controller.findByObservationUuid(obsUuid))
        assertEquals(xs.size, 1)
        assertEquals(xs.head.x, Some(0.3))
    }

    test("upsert accepts the lastUpdated returned by the previous response") {
        val obsUuid = newObservationUuid()
        val a       = exec(controller.upsert(obsUuid, "bounding_box", box(0.1)))
        val b       = exec(controller.upsert(obsUuid, "bounding_box", box(0.2).copy(lastUpdated = a.lastUpdated)))
        val c       = exec(controller.upsert(obsUuid, "bounding_box", box(0.3).copy(lastUpdated = b.lastUpdated)))
        assertEquals(c.x, Some(0.3))
        // A value read back from the database round trips as well
        val d       = exec(controller.findByObservationUuid(obsUuid)).head
        val e       = exec(controller.upsert(obsUuid, "bounding_box", box(0.4).copy(lastUpdated = d.lastUpdated)))
        assertEquals(e.x, Some(0.4))
    }

    test("upsert with a stale lastUpdated fails and writes nothing") {
        val obsUuid = newObservationUuid()
        val a       = exec(controller.upsert(obsUuid, "bounding_box", box(0.1)))
        val stale   = a.lastUpdated.map(_.minusSeconds(60))
        val ex      = intercept[StaleLocalizationException] {
            exec(controller.upsert(obsUuid, "bounding_box", box(0.5).copy(lastUpdated = stale)))
        }
        assertEquals(ex.current.x, Some(0.1))
        val xs      = exec(controller.findByObservationUuid(obsUuid))
        assertEquals(xs.size, 1)
        assertEquals(xs.head.x, Some(0.1))
        assertEquals(xs.head.lastUpdated.map(_.toEpochMilli), a.lastUpdated.map(_.toEpochMilli))
    }

    test("upsert without lastUpdated overwrites") {
        val obsUuid = newObservationUuid()
        exec(controller.upsert(obsUuid, "bounding_box", box(0.1, Some("a"))))
        exec(controller.upsert(obsUuid, "bounding_box", box(0.5, Some("b"))))
        val xs      = exec(controller.findByObservationUuid(obsUuid))
        assertEquals(xs.size, 1)
        assertEquals(xs.head.x, Some(0.5))
        assertEquals(xs.head.generator, Some("b"))
    }

    test("upsert with an invalid bbox fails and writes nothing") {
        val obsUuid = newObservationUuid()
        intercept[LocalizationValidationException] {
            exec(controller.upsert(obsUuid, "bounding_box", box().copy(width = Some(-1.0))))
        }
        assert(exec(controller.findByObservationUuid(obsUuid)).isEmpty)
    }

    test("upsert for an unknown observation fails") {
        intercept[NotFoundInDatastoreException] {
            exec(controller.upsert(UUID.randomUUID(), "bounding_box", box()))
        }
    }

    test("delete returns true then false") {
        val obsUuid = newObservationUuid()
        exec(controller.upsert(obsUuid, "bounding_box", box()))
        assert(exec(controller.delete(obsUuid, "bounding_box")))
        assert(!exec(controller.delete(obsUuid, "bounding_box")))
        assert(exec(controller.findByObservationUuid(obsUuid)).isEmpty)
    }

    test("findByVideoReferenceUuid") {
        val im = TestUtils.create(1, 1, nLocalizations = 2).head
        val xs = exec(controller.findByVideoReferenceUuid(im.getVideoReferenceUuid, None, None))
        assertEquals(xs.size, 2)
    }

    test("bulkUpsert of valid items writes all of them") {
        val obs = (0 until 3).map(_ => newObservationUuid())
        val xs  = exec(controller.bulkUpsert(obs.map(o => box().copy(observationUuid = Some(o)))))
        assertEquals(xs.size, 3)
        assert(xs.forall(_.uuid.isDefined))
        obs.foreach(o => assertEquals(exec(controller.findByObservationUuid(o)).size, 1))
    }

    test("bulkUpsert with an invalid item writes nothing and reports its index") {
        val obs   = (0 until 3).map(_ => newObservationUuid())
        val items = obs.zipWithIndex.map((o, i) =>
            val b = box().copy(observationUuid = Some(o))
            if i == 2 then b.copy(width = Some(-1.0)) else b
        )
        val ex    = intercept[BulkLocalizationException] { exec(controller.bulkUpsert(items)) }
        assertEquals(ex.failures.map(_._1), Seq(2))
        obs.foreach(o => assert(exec(controller.findByObservationUuid(o)).isEmpty))
    }

    test("bulkUpsert with a stale item rolls back the whole batch") {
        val obs   = (0 until 2).map(_ => newObservationUuid())
        val a     = exec(controller.upsert(obs(1), "bounding_box", box(0.1)))
        val items = Seq(
            box(0.5).copy(observationUuid = Some(obs(0))),
            box(0.6).copy(observationUuid = Some(obs(1)), lastUpdated = a.lastUpdated.map(_.minusSeconds(60)))
        )
        val ex    = intercept[BulkLocalizationException] { exec(controller.bulkUpsert(items)) }
        assertEquals(ex.failures.map(_._1), Seq(1))
        assert(exec(controller.findByObservationUuid(obs(0))).isEmpty)
        val kept  = exec(controller.findByObservationUuid(obs(1))).head
        assertEquals(kept.x, Some(0.1))
        assertEquals(kept.lastUpdated, a.lastUpdated)
    }

    test("bulkUpsert with an unknown observation reports every failing index and writes nothing") {
        val obs     = (0 until 2).map(_ => newObservationUuid())
        val unknown = UUID.randomUUID()
        val items   = Seq(
            box().copy(observationUuid = Some(obs(0))),
            box().copy(observationUuid = Some(unknown)),
            box().copy(observationUuid = Some(obs(1)), width = Some(-1.0)),
            box().copy(observationUuid = Some(UUID.randomUUID()))
        )
        val ex      = intercept[BulkLocalizationException] { exec(controller.bulkUpsert(items)) }
        assertEquals(ex.failures.map(_._1), Seq(1, 2, 3))
        assert(ex.failures.head._2.contains(s"observation $unknown not found"))
        obs.foreach(o => assert(exec(controller.findByObservationUuid(o)).isEmpty))
    }

    test("bulkUpsert rejects duplicate keys in one batch and writes nothing") {
        val obs   = (0 until 2).map(_ => newObservationUuid())
        val items = Seq(
            box(0.1).copy(observationUuid = Some(obs(0))),
            box(0.2).copy(observationUuid = Some(obs(1))),
            box(0.3).copy(observationUuid = Some(obs(0)))
        )
        val ex    = intercept[BulkLocalizationException] { exec(controller.bulkUpsert(items)) }
        assertEquals(ex.failures, Seq((2, Seq("duplicate key in batch"))))
        obs.foreach(o => assert(exec(controller.findByObservationUuid(o)).isEmpty))
    }

    /** A controller on a private bus, plus everything published to that bus. */
    private def isolatedController(): (LocalizationController, scala.collection.mutable.ListBuffer[Any]) =
        val bus      = PublishSubject.create[Any]().toSerialized
        val received = scala.collection.mutable.ListBuffer.empty[Any]
        bus.subscribe(x => received.synchronized { received += x })
        (new LocalizationController(daoFactory, bus), received)

    test("upsert publishes exactly one localization message and no association message") {
        val (c, received) = isolatedController()
        val obsUuid       = newObservationUuid()
        val a             = exec(c.upsert(obsUuid, "bounding_box", box()))
        assertEquals(received.toList, List(LocalizationCreatedMessage(a)))
        assert(!received.exists(_.isInstanceOf[AssociationCreatedMessage]))
        assert(!received.exists(_.isInstanceOf[AnnotationCreatedMessage]))
    }

    test("bulkUpsert of 3 items publishes 3 localization messages") {
        val (c, received) = isolatedController()
        val obs           = (0 until 3).map(_ => newObservationUuid())
        val xs            = exec(c.bulkUpsert(obs.map(o => box().copy(observationUuid = Some(o)))))
        assertEquals(received.toList, xs.map(LocalizationCreatedMessage(_)).toList)
    }

    test("a failed bulkUpsert publishes nothing") {
        val (c, received) = isolatedController()
        val obs           = (0 until 3).map(_ => newObservationUuid())
        val items         = obs.zipWithIndex.map((o, i) =>
            val b = box().copy(observationUuid = Some(o))
            if i == 2 then b.copy(width = Some(-1.0)) else b
        )
        intercept[BulkLocalizationException] { exec(c.bulkUpsert(items)) }
        assert(received.isEmpty)
    }

    test("a bulkUpsert failing at database time (unknown observation last) publishes nothing and writes nothing") {
        val (c, received) = isolatedController()
        val obs           = (0 until 2).map(_ => newObservationUuid())
        val items         = obs.map(o => box().copy(observationUuid = Some(o))) :+
            box().copy(observationUuid = Some(UUID.randomUUID()))
        val ex            = intercept[BulkLocalizationException] { exec(c.bulkUpsert(items)) }
        assertEquals(ex.failures.map(_._1), Seq(2))
        assert(received.isEmpty, received.toString)
        obs.foreach(o => assert(exec(c.findByObservationUuid(o)).isEmpty))
    }

    test("a bulkUpsert failing at database time (stale last item) publishes nothing and writes nothing") {
        val obs      = (0 until 3).map(_ => newObservationUuid())
        val existing = exec(controller.upsert(obs(2), "bounding_box", box(0.1)))
        // Subscribe only after the setup upsert so its message is not counted
        val (c, received) = isolatedController()
        val items         = Seq(
            box(0.5).copy(observationUuid = Some(obs(0))),
            box(0.6).copy(observationUuid = Some(obs(1))),
            box(0.7).copy(observationUuid = Some(obs(2)), lastUpdated = existing.lastUpdated.map(_.minusSeconds(60)))
        )
        val ex            = intercept[BulkLocalizationException] { exec(c.bulkUpsert(items)) }
        assertEquals(ex.failures.map(_._1), Seq(2))
        assert(received.isEmpty, received.toString)
        obs.take(2).foreach(o => assert(exec(c.findByObservationUuid(o)).isEmpty))
        assertEquals(exec(c.findByObservationUuid(obs(2))).head.x, Some(0.1))
    }

    test("concurrent first upserts for one key only succeed or conflict, and leave one row") {
        val obsUuid = newObservationUuid()
        val futures = (0 until 12).map(i =>
            controller
                .upsert(obsUuid, "bounding_box", box(0.01 * (i + 1)))
                .transform(t => scala.util.Success(t))
        )
        val results = exec(scala.concurrent.Future.sequence(futures))
        results.foreach {
            case scala.util.Success(_)                          => ()
            case scala.util.Failure(_: StaleLocalizationException) => ()
            case scala.util.Failure(e)                          => fail(s"Unexpected failure: $e", e)
        }
        assert(results.exists(_.isSuccess))
        assertEquals(exec(controller.findByObservationUuid(obsUuid)).size, 1)
    }
