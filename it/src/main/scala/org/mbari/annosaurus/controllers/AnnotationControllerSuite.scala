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
import org.mbari.annosaurus.domain.{Annotation, ConcurrentRequest, Localization, MultiRequest}
import io.reactivex.rxjava3.subjects.PublishSubject
import org.mbari.annosaurus.etc.sdk.Futures.*
import org.mbari.annosaurus.etc.zeromq.{AnnotationCreatedMessage, LocalizationCreatedMessage}
import org.mbari.annosaurus.repository.jpa.{BaseDAOSuite, ImagedMomentDAOImpl, JPADAOFactory}
import org.mbari.annosaurus.repository.jpa.TransactionNotifier
import org.mbari.annosaurus.repository.jpa.entity.ObservationEntity
import org.mbari.vcr4j.time.Timecode

import java.time.{Duration, Instant}
import java.util.UUID
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import scala.concurrent.ExecutionContext
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.Using

trait AnnotationControllerSuite extends BaseDAOSuite:
    given JPADAOFactory    = daoFactory
    given ExecutionContext = ExecutionContext.global
    lazy val controller    = AnnotationController(daoFactory)
    private val log        = System.getLogger(getClass.getName)

    override def beforeAll(): Unit = daoFactory.beforeAll()
    override def afterAll(): Unit  = daoFactory.afterAll()

    override val munitTimeout: scala.concurrent.duration.FiniteDuration =
        scala.concurrent.duration.Duration(30, "s")

    test("findByVideoReferenceUuid (JPA) batch-fetches localizations instead of one SELECT per observation") {
        val nObs  = 40
        val im    = TestUtils.create(1, nObs, 1, 0, nLocalizations = 2).head
        val stats = daoFactory.entityManagerFactory.unwrap(classOf[org.hibernate.SessionFactory]).getStatistics
        stats.setStatisticsEnabled(true)
        stats.clear()
        val annotations =
            try exec(controller.findByVideoReferenceUuid(im.getVideoReferenceUuid))
            finally stats.setStatisticsEnabled(false)
        val statements = stats.getPrepareStatementCount
        assertEquals(annotations.size, nObs)
        assert(annotations.forall(_.localizations.size == 2))
        assert(annotations.forall(_.associations.size == 1))
        // without batch fetching this is ~1 + nObs (localizations) + nObs (associations) + ...
        assert(statements < nObs / 2, s"$statements statements for $nObs observations")
    }

    test("findByUUID") {
        val im1 = TestUtils.create(1, 2, 3, 2, true).head
        val obs = im1.getObservations.asScala.head
        val opt = exec(controller.findByUUID(obs.getUuid))
        opt match
            case None       => fail("findByUUID returned None")
            case Some(anno) =>
                // NOTE: this anno is only 1 observations. THe source imagedMoment has two.
                // this is ok and expected. An annotation maps to a single observation!!
                val im2 = Annotation.toEntities(Seq(anno)).head
                AssertUtils.assertSameImagedMoment(im1, im2, false)
    }

    test("countByVideoReferenceUUID") {
        val xs = TestUtils.create(5, 2)
        val e  = xs.flatMap(_.getObservations.asScala)
        val n  = exec(controller.countByVideoReferenceUuid(xs.head.getVideoReferenceUuid))
        assertEquals(n, e.size)
    }

    test("findByVideoReferenceUUID") {

        val xs = TestUtils.create(2, 2, includeData = true)
        val e  = xs.flatMap(_.getObservations.asScala)
        val n  = exec(controller.findByVideoReferenceUuid(xs.head.getVideoReferenceUuid))
        assertEquals(n.size, e.size)
        assert(n.head.ancillaryData.isEmpty)

        val m = exec(
            controller.findByVideoReferenceUuid(
                xs.head.getVideoReferenceUuid,
                Some(1),
                Some(1),
                true
            )
        )
        assertEquals(m.size, 1)
        assert(m.head.ancillaryData.isDefined)
    }

    test("streamByVideoReferenceUUID") {}

    test("streamByVideoReferenceUUIDAndTimestamps") {}

    test("streamByConcurrentRequest") {}

    test("countByConcurrentRequest") {
        val dt = Duration.ofSeconds(1)

        val xs  = TestUtils.create(2, 2) ++ TestUtils.create(2, 2)
        val obs = xs.flatMap(_.getObservations.asScala)
        val ts  = xs.map(_.getRecordedTimestamp())
        val t0  = ts.min.minus(dt)
        val t1  = ts.max.plus(dt)

        val vru = xs.map(_.getVideoReferenceUuid()).distinct
        assertEquals(vru.size, 2)

        val cr0 = new ConcurrentRequest(t0, t1, Seq(vru.head))
        val n   = exec(controller.countByConcurrentRequest(cr0)).intValue()
        assertEquals(n, 4)

        val cr1 = new ConcurrentRequest(t0, t1, vru)
        val m   = exec(controller.countByConcurrentRequest(cr1)).intValue()
        assertEquals(m, 8)

        val cr3 = new ConcurrentRequest(t1, t1.plus(dt), vru)
        val p   = exec(controller.countByConcurrentRequest(cr3)).intValue()
        assertEquals(p, 0)

        // val cr1 = new ConcurrentRequest(t0, t1, Seq(a.head.getVideoReferenceUuid(), b.head.getVideoReferenceUuid()))
        // val m = exec(controller.countByConcurrentRequest(cr1)).intValue()
        // assertEquals(m, ea.size + eb.size)
    }

    test("streamByMultiRequest") {}

    test("countByMultiRequest") {
        val xs  = TestUtils.create(2, 2) ++ TestUtils.create(2, 2)
        val obs = xs.flatMap(_.getObservations.asScala)
        val vru = xs.map(_.getVideoReferenceUuid()).distinct

        val mr = new MultiRequest(vru)
        val n  = exec(controller.countByMultiRequest(mr)).intValue()
        assertEquals(n, obs.size)

        val mr2 = new MultiRequest(Seq(vru.head))
        val m   = exec(controller.countByMultiRequest(mr2)).intValue()
        assertEquals(m, obs.size / 2)
    }

    test("findByAssociationUuid") {
        val xs       = TestUtils.create(1, 1, 1, 1).head
        val obs      = xs.getObservations.asScala.head
        val assoc    = obs.getAssociations.asScala.head
        val obtained = exec(controller.findByAssociationUuid(assoc.getUuid))
        obtained match
            case None       => fail("findByAssociationUuid returned None")
            case Some(anno) =>
                val im2 = Annotation.toEntities(Seq(anno)).head
                AssertUtils.assertSameImagedMoment(xs, im2, false)
    }

    test("findByImageReferenceUUID") {
        val xs       = TestUtils.create(1, 1, 0, 1).head
        val obs      = xs.getObservations.asScala.head
        val imr      = xs.getImageReferences().asScala.head
        val obtained = exec(controller.findByImageReferenceUUID(imr.getUuid)).head
        val expected = Annotation.from(obs, true).roundObservationTimestampToMillis()
        assertEquals(obtained, expected)
    }

    test("create") {
        val im       = TestUtils.build(1, 1).head
        val anno     = Annotation.from(im.getObservations.asScala.head, true)
        val obtained = exec(
            controller.create(
                anno.videoReferenceUuid.get,
                anno.concept.get,
                anno.observer.get,
                anno.observationTimestamp.get,
                anno.validTimecode,
                anno.elapsedTime,
                anno.recordedTimestamp,
                anno.duration,
                anno.group,
                anno.activity
            )
        )

        // Our source anno doesn't have UUIDS set, we remove those to compare the rest of the values
        assert(obtained.observationUuid.isDefined)
        assert(obtained.imagedMomentUuid.isDefined)
        val corrected =
            obtained.copy(observationUuid = None, imagedMomentUuid = None, lastUpdated = None)
        assertEquals(corrected, anno)
    }

    test("create by recordedTimestamp") {
        val recordedDate = Instant.now()
        val concept      = "Nanomia bijuga"
        val observer     = "brian"
        val a            = exec(
            controller
                .create(UUID.randomUUID(), concept, observer, recordedDate = Some(recordedDate))
        )
        assert(a.observationUuid.isDefined)
        assertEquals(a.recordedTimestamp, Some(recordedDate))
        assertEquals(a.concept, Some(concept))
        assertEquals(a.observer, Some(observer))
    }

    test("create(Annotation)") {
        val im        = TestUtils.build(1, 1).head
        val expected  = Annotation.from(im.getObservations.asScala.head, true)
        val obtained  = exec(controller.create(expected)).head
        assert(obtained.observationUuid.isDefined)
        assert(obtained.imagedMomentUuid.isDefined)
        assert(obtained.lastUpdated.isDefined)
        val corrected =
            obtained.copy(observationUuid = None, imagedMomentUuid = None, lastUpdated = None)
        assertEquals(corrected, expected)
    }

    test("bulkCreate a single annotation") {
        // test minimal
        val xs0       = TestUtils.build(1, 1)
        val annos0    = Annotation.fromImagedMoment(xs0.head, true)
        assertEquals(annos0.size, 1)
        val n         = exec(controller.bulkCreate(annos0))
        assertEquals(n.size, xs0.size)
        val obtained  = n.head
        assert(obtained.imagedMomentUuid.isDefined)
        assert(obtained.observationUuid.isDefined)
        val corrected =
            obtained.copy(imagedMomentUuid = None, observationUuid = None, lastUpdated = None)
//        log.atWarn.log(n.head.stringify)
        assertEquals(corrected, annos0.head)
    }

    // https://github.com/mbari-org/annosaurus/issues/42
    test("bulkCreate annotations at same index") {
        val a = TestUtils.create(1, 1).head
        for i <- 0 until 5
        do
            val b         = TestUtils.build(1, 1).head
            b.setTimecode(a.getTimecode)
            b.setRecordedTimestamp(a.getRecordedTimestamp)
            b.setElapsedTime(a.getElapsedTime)
            val annos     = Annotation.fromImagedMoment(b, true)
            val n         = exec(controller.bulkCreate(annos))
            assertEquals(n.size, 1)
            val obtained  = n.head
            assert(obtained.imagedMomentUuid.isDefined)
            assert(obtained.observationUuid.isDefined)
            val corrected =
                obtained.copy(imagedMomentUuid = None, observationUuid = None, lastUpdated = None)
            assertEquals(corrected, annos.head)

    }

    test("bulkCreate a 200 annotations") {
        import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
        val in          = this.getClass.getResourceAsStream("/json/annotation_full_dive.json")
        val readAttempt = Using(Source.fromInputStream(in, "UTF-8")) { source =>
            val json = source.getLines().mkString("\n")
            json
                .reify[Seq[Annotation]]
                .getOrElse(throw new RuntimeException("Failed to parse json"))
                .take(200)
        }
        val annos       = readAttempt.fold(e => throw new RuntimeException("Failed to read json", e), identity)
        assert(annos.nonEmpty)

        val n = controller.bulkCreate(annos).join(Duration.ofSeconds(10))
        assertEquals(n.size, annos.size)
        daoFactory.cleanup() // IMPORTANT or collides with other tests

    }

    test("bulkCreate a 200 annotations with imageReferences") {
        import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
        val in          = this.getClass.getResourceAsStream("/json/annotation_full_dive.json")
        val readAttempt = Using(Source.fromInputStream(in, "UTF-8")) { source =>
            val json = source.getLines().mkString("\n")
            json
                .reify[Seq[Annotation]]
                .getOrElse(throw new RuntimeException("Failed to parse json"))
                .take(200)
        }
        val annos       = readAttempt.fold(e => throw new RuntimeException("Failed to read json", e), identity)
        assert(annos.nonEmpty)
        val n           = controller.bulkCreate(annos).join(Duration.ofSeconds(10))
        assertEquals(n.size, annos.size)
        daoFactory.cleanup() // IMPORTANT or collides with other tests

    }

    test("update") {
        val im0  = TestUtils.create(1, 1).head
        val obs0 = im0.getObservations.asScala.head
        val im1  = TestUtils.build(1, 1).head
        im1.setTimecode(im0.getTimecode)
        val obs1 = im1.getObservations.asScala.head

        val opt = exec(
            controller.update(
                obs0.getUuid,
                Option(im1.getVideoReferenceUuid),
                Option(obs1.getConcept),
                Option(obs1.getObserver),
                obs1.getObservationTimestamp,
                Option(im1.getTimecode),
                Option(im1.getElapsedTime),
                Option(im1.getRecordedTimestamp),
                Option(obs1.getDuration),
                Option(obs1.getGroup),
                Option(obs1.getActivity)
            )
        )

        opt match
            case None       => fail("update returned None")
            case Some(anno) =>
                val im2 = Annotation.toEntities(Seq(anno)).head

                // We need to set the UUIDs to compare the rest of the values
                im1.setUuid(anno.imagedMomentUuid.get)
                obs1.setUuid(anno.observationUuid.get)
                val a1 = Annotation.from(obs1, true)
                val a0 = Annotation.from(obs0, true)
//                log.atDebug.log("ORIGINAL:   " + a0.stringify)
//                log.atDebug.log("NEW VALUES: " + a1.stringify)
//                log.atDebug.log("UPDATED:    " + anno.stringify)
                AssertUtils.assertSameImagedMoment(im1, im2, false)

    }

    test("bulkUpdate") {
        val xs                 =
            TestUtils.create(2, 2) // tested with upt to 30, 10. 100 by 10 takes more than 30sec
        val videoReferenceUuid = xs.head.getVideoReferenceUuid
        val elapsedTime        = Duration.ofSeconds(1234)
        val annos              = xs
            .flatMap(x => Annotation.fromImagedMoment(x, false))
            .map(x =>
                x.copy(
                    elapsedTimeMillis = Some(elapsedTime.toMillis),
                    timecode = Some("01:02:03:04"),
                    recordedTimestamp = Some(Instant.parse("2020-01-01T01:02:03Z")),
                    videoReferenceUuid = Some(videoReferenceUuid),
                    concept = Some("bulkUpdateTest")
                )
            )
        val n                  = exec(controller.bulkUpdate(annos), Duration.ofSeconds(60))
        assertEquals(n.size, annos.size)
        n.foreach { anno =>
//            log.atWarn.log(anno.stringify)
            assert(anno.concept.isDefined)
            assert(anno.elapsedTime.isDefined)
            assert(anno.videoReferenceUuid.isDefined)
            assertEquals(anno.concept.get, "bulkUpdateTest")
            assertEquals(anno.videoReferenceUuid.get, videoReferenceUuid)
            assertEquals(anno.elapsedTime.get, elapsedTime)
        }

    }

    test("bulkUpdateRecordedTimestampOnly") {
        val xs     = TestUtils.build(4, 1).zipWithIndex
        val annos0 =
            for (im, i) <- xs
            yield
                im.setTimecode(new Timecode(s"00:00:0${i}:00"))
                Annotation.from(im.getObservations.iterator().next(), false)

        val annos1 = exec(controller.bulkCreate(annos0))

        val annos2 = annos1.map { anno =>
            anno.copy(recordedTimestamp = Some(Instant.ofEpochMilli(math.random().longValue())))
        }

        val annos3 = exec(controller.bulkUpdateRecordedTimestampOnly(annos2))

        assertEquals(annos3.size, annos2.size)

        val obtained = annos3
            .toList
            .sortBy(_.observationUuid)
        val expected = annos2
            .toList
            .map(_.roundObservationTimestampToMillis())
            .sortBy(_.observationUuid)
        obtained.zip(expected).foreach { (a, b) =>
            assertEquals(a, b)
        }

    }

    test("delete") {
        val im   = TestUtils.create(1, 2).head
        val obs0 = im.getObservations.asScala.head
        val obs1 = im.getObservations.asScala.last
        val ok   = exec(controller.delete(obs0.getUuid))
        assert(ok)
        val opt2 = exec(controller.findByUUID(obs0.getUuid))
        assert(opt2.isEmpty)
        val opt3 = exec(controller.findByUUID(obs1.getUuid))
        assert(opt3.isDefined)
    }

    test("delete single observation") {
        // The parent ImagedMoment should be deleted as well if it has no other observations
        val im   = TestUtils.create(1, 1).head
        val obs  = im.getObservations.asScala.head
        val ok   = exec(controller.delete(obs.getUuid))
        assert(ok)
        val opt2 = exec(controller.findByUUID(obs.getUuid))
        assert(opt2.isEmpty)

        given imDao: ImagedMomentDAOImpl = daoFactory.newImagedMomentDAO()
        val imOpt                        = run(() => imDao.findByUUID(im.getUuid))
        assert(imOpt.isEmpty)
        imDao.close()
    }

    test("rxCreate") {
        val queue      = new LinkedBlockingQueue[TransactionNotifier.Message[?]]()
        val disposable = TransactionNotifier
            .getRxSubject()
            .ofType(classOf[TransactionNotifier.Message[?]])
            .subscribe(queue.offer(_))

        try
            val im      = TestUtils.build(1, 1).head
            val anno    = Annotation.from(im.getObservations.asScala.head, true)
            val created = exec(
                controller.create(
                    anno.videoReferenceUuid.get,
                    anno.concept.get,
                    anno.observer.get,
                    anno.observationTimestamp.get
                )
            )
            val uuid = created.observationUuid.get

            // Drain the queue looking for a CREATE event for our specific observation UUID
            val deadline = System.currentTimeMillis() + 3000L
            var found    = Option.empty[TransactionNotifier.Message[?]]
            while found.isEmpty && System.currentTimeMillis() < deadline do
                Option(queue.poll(200, TimeUnit.MILLISECONDS))
                    .filter(m => m.action() == TransactionNotifier.Action.CREATE
                        && m.clazz() == classOf[ObservationEntity]
                        && m.uuid() == uuid)
                    .foreach(m => found = Some(m))

            assert(found.isDefined, s"No CREATE event received for observation $uuid")
        finally
            disposable.dispose()
    }

    test("rxUpdate") {
        val im         = TestUtils.create(1, 1).head
        val obs        = im.getObservations.asScala.head
        val newConcept = "rxUpdateConcept"

        val queue      = new LinkedBlockingQueue[TransactionNotifier.Message[?]]()
        val disposable = TransactionNotifier
            .getRxSubject()
            .ofType(classOf[TransactionNotifier.Message[?]])
            .subscribe(queue.offer(_))

        try
            exec(controller.update(obs.getUuid, concept = Some(newConcept)))
            val uuid = obs.getUuid

            val deadline = System.currentTimeMillis() + 3000L
            var found    = Option.empty[TransactionNotifier.Message[?]]
            while found.isEmpty && System.currentTimeMillis() < deadline do
                Option(queue.poll(200, TimeUnit.MILLISECONDS))
                    .filter(m => m.action() == TransactionNotifier.Action.UPDATE
                        && m.clazz() == classOf[ObservationEntity]
                        && m.uuid() == uuid)
                    .foreach(m => found = Some(m))

            assert(found.isDefined, s"No UPDATE event received for observation $uuid")
        finally
            disposable.dispose()
    }

    test("rxDelete") {
        val im         = TestUtils.create(1, 1).head
        val obs        = im.getObservations.asScala.head
        val uuid       = obs.getUuid

        val queue      = new LinkedBlockingQueue[TransactionNotifier.Message[?]]()
        val disposable = TransactionNotifier
            .getRxSubject()
            .ofType(classOf[TransactionNotifier.Message[?]])
            .subscribe(queue.offer(_))

        try
            exec(controller.delete(uuid))

            val deadline = System.currentTimeMillis() + 3000L
            var found    = Option.empty[TransactionNotifier.Message[?]]
            while found.isEmpty && System.currentTimeMillis() < deadline do
                Option(queue.poll(200, TimeUnit.MILLISECONDS))
                    .filter(m => m.action() == TransactionNotifier.Action.REMOVE
                        && m.clazz() == classOf[ObservationEntity]
                        && m.uuid() == uuid)
                    .foreach(m => found = Some(m))

            assert(found.isDefined, s"No REMOVE event received for observation $uuid")
        finally
            disposable.dispose()
    }

    // --- localizations carried on annotation create ---

    private lazy val localizationController = LocalizationController(daoFactory)

    private def newAnnotation(localizations: Seq[Localization] = Nil): Annotation =
        Annotation(
            concept = Some("Nanomia bijuga"),
            observer = Some("brian"),
            observationTimestamp = Some(Instant.now()),
            recordedTimestamp = Some(Instant.now()),
            videoReferenceUuid = Some(UUID.randomUUID()),
            localizations = localizations
        )

    private val bbox    =
        Localization("bounding_box", x = Some(0.1), y = Some(0.2), width = Some(0.3), height = Some(0.4))
    private val point   = Localization("point", x = Some(0.5), y = Some(0.6))
    private val polygon = Localization("polygon", vertices = Some(Seq(0.1, 0.1, 0.5, 0.1, 0.5, 0.5)))

    // Today only bulkCreate (2 or more annotations) publishes AnnotationCreatedMessage; a single create(...) does not.
    test("bulkCreate publishes AnnotationCreatedMessages whose content carries the localizations") {
        val bus      = PublishSubject.create[Any]().toSerialized
        val received = scala.collection.mutable.ListBuffer.empty[Any]
        bus.subscribe(x => received.synchronized { received += x })
        val c        = AnnotationController(daoFactory, bus)
        val vr       = UUID.randomUUID()
        val xs       = Seq(
            newAnnotation(Seq(bbox, point)).copy(videoReferenceUuid = Some(vr)),
            newAnnotation(Seq(bbox, point)).copy(videoReferenceUuid = Some(vr))
        )
        val obtained = exec(c.bulkCreate(xs))
        assertEquals(obtained.size, 2)
        // publication runs in a Future callback, so it may land just after the result is returned
        val deadline = System.currentTimeMillis() + 5000
        while received.synchronized(received.size) < 2 && System.currentTimeMillis() < deadline do Thread.sleep(25)
        val msgs     = received.synchronized(received.toList).collect { case m: AnnotationCreatedMessage => m }
        assertEquals(msgs.size, 2)
        msgs.foreach(m =>
            assertEquals(m.content.localizations.map(_.localizationType).toSet, Set("bounding_box", "point"))
            assertEquals(m.content.localizations.size, 2)
        )
        assert(!received.exists(_.isInstanceOf[LocalizationCreatedMessage]))
    }


    test("create(Annotation) with localizations persists them in the same request") {
        val obtained = exec(controller.create(newAnnotation(Seq(bbox, point))))
        assertEquals(obtained.size, 1)
        val a        = obtained.head
        assertEquals(a.localizations.size, 2)
        assert(a.localizations.forall(_.uuid.isDefined))
        assertEquals(a.localizations.map(_.localizationType).toSet, Set("bounding_box", "point"))
        val stored   = exec(localizationController.findByObservationUuid(a.observationUuid.get))
        assertEquals(stored.map(_.uuid).toSet, a.localizations.map(_.uuid).toSet)
        assertEquals(stored.map(_.observationUuid).toSet, Set(a.observationUuid))
    }

    test("create(Annotation) ignores client supplied localization uuids") {
        val clientUuid = UUID.randomUUID()
        val a          = exec(controller.create(newAnnotation(Seq(bbox.copy(uuid = Some(clientUuid)))))).head
        assertEquals(a.localizations.size, 1)
        assert(a.localizations.head.uuid.isDefined)
        assertNotEquals(a.localizations.head.uuid, Some(clientUuid))
    }

    test("bulkCreate persists a localization carried by one of the annotations") {
        val annos    = Seq(newAnnotation(), newAnnotation(Seq(polygon)), newAnnotation())
        val obtained = exec(controller.bulkCreate(annos))
        assertEquals(obtained.size, 3)
        val withLoc  = obtained.filter(_.localizations.nonEmpty)
        assertEquals(withLoc.size, 1)
        val l        = withLoc.head.localizations.head
        assertEquals(l.localizationType, "polygon")
        assert(l.uuid.isDefined)
        val stored   = exec(localizationController.findByObservationUuid(withLoc.head.observationUuid.get))
        assertEquals(stored.map(_.uuid), Seq(l.uuid))
        assertEquals(stored.head.vertices, polygon.vertices)
    }

    test("create(Annotation) with one invalid localization fails and creates nothing") {
        val bad  = bbox.copy(x = Some(0.9), width = Some(0.5)) // x + width > 1
        val anno = newAnnotation(Seq(point, bad))
        val ex   = intercept[LocalizationValidationException] {
            exec(controller.create(anno))
        }
        assert(ex.errors.exists(_.startsWith("annotation 0, localization 1:")), ex.errors.toString)
        assert(exec(controller.findByVideoReferenceUuid(anno.videoReferenceUuid.get)).isEmpty)
    }

    test("bulkCreate with one invalid localization fails and creates nothing") {
        val bad   = bbox.copy(width = Some(-1.0))
        val vrUuid = UUID.randomUUID()
        val annos = Seq(newAnnotation(), newAnnotation(Seq(point)), newAnnotation(Seq(bad)))
            .map(_.copy(videoReferenceUuid = Some(vrUuid)))
        val ex    = intercept[LocalizationValidationException] {
            exec(controller.bulkCreate(annos))
        }
        assert(ex.errors.exists(_.startsWith("annotation 2, localization 0:")), ex.errors.toString)
        assert(exec(controller.findByVideoReferenceUuid(vrUuid)).isEmpty)
    }

    test("create(Annotation) stores the normalized localization") {
        val dupPolygon = Localization("polygon", vertices = Some(Seq(0.1, 0.1, 0.5, 0.1, 0.5, 0.1, 0.5, 0.5)))
        val line       = Localization("line", vertices = Some(Seq(0.1, 0.2, 0.4, 0.6)))
        val a          = exec(controller.create(newAnnotation(Seq(dupPolygon, line)))).head
        val stored     = exec(localizationController.findByObservationUuid(a.observationUuid.get))
        val p          = stored.find(_.localizationType == "polygon").get
        assertEquals(p.vertices, Some(Seq(0.1, 0.1, 0.5, 0.1, 0.5, 0.5)))
        val l          = stored.find(_.localizationType == "line").get
        assertEqualsDouble(l.x.get, 0.1, 1e-9)
        assertEqualsDouble(l.y.get, 0.2, 1e-9)
        assertEqualsDouble(l.width.get, 0.3, 1e-9)
        assertEqualsDouble(l.height.get, 0.4, 1e-9)
    }
