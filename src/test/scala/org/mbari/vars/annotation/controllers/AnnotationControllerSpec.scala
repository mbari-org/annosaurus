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

package org.mbari.vars.annotation.controllers

import java.nio.charset.StandardCharsets
import java.time.{Duration, Instant}
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.Constants
import org.mbari.vars.annotation.dao.jpa.{AnnotationImpl, AssociationImpl, SpecDAOFactory, TestDAOFactory}
import org.mbari.vars.annotation.model.Annotation
import org.mbari.vars.annotation.model.simple.{ConcurrentRequest, MultiRequest}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration => SDuration}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.Random

/**
 * @author Brian Schlining
 * @since 2017-01-23T15:12:00
 */
class AnnotationControllerSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory = TestDAOFactory.Instance
  private[this] val entityFactory = new TestEntityFactory(daoFactory)
  private[this] val controller = new AnnotationController(daoFactory.asInstanceOf[BasicDAOFactory])
  private[this] val timeout = SDuration(200, TimeUnit.SECONDS)
  private[this] val recordedDate = Instant.now()
//  private[this] val log = LoggerFactory.getLogger(getClass)

  def exec[R](fn: () => Future[R]): R = Await.result(fn.apply(), timeout)

  "AnnotationController" should "create by recorded timestamp" in {
    val a = exec(() => controller.create(
      UUID.randomUUID(),
      "Nanomia bijuga",
      "brian",
      recordedDate = Some(recordedDate)))
    a.concept should be("Nanomia bijuga")
    a.observer should be("brian")
    a.recordedTimestamp should be(recordedDate)
  }

  it should "create using existing recorded timestamp" in {
    val a = exec(() => controller.create(
      UUID.randomUUID(),
      "Grimpoteuthis",
      "brian",
      recordedDate = Some(recordedDate)))
    a.concept should be("Grimpoteuthis")
    a.observer should be("brian")
    a.recordedTimestamp should be(recordedDate)
  }

  it should "find by videoReferenceUuid" in {
    val a = exec(() => controller.create(
      UUID.randomUUID(),
      concept = "Slime mold",
      observer = "brian",
      recordedDate = Some(recordedDate)))

    val b = exec(() => controller.findByVideoReferenceUUID(a.videoReferenceUuid))
    b should not be empty
    b.size should be(1)
    b.head.concept should be("Slime mold")
  }

  it should "stream by videoReferenceUuid" in {
    val videoReferenceUuid = UUID.randomUUID()
    val xs = for (i <- 0 until 3) yield {
      exec(() => controller.create(
         videoReferenceUuid,
        "Grimpoteuthis " + i,
        "brian",
        recordedDate = Some(recordedDate.plusSeconds(i * 3600))))
    }

    val (closeable, stream) = controller.streamByVideoReferenceUUID(videoReferenceUuid)
    val annos = stream.iterator().asScala.toList
    annos.size should be(xs.size)
  }

  it should "stream by videoReferenceUuid and timestamps" in {
    val videoReferenceUuid = UUID.randomUUID()
    val xs = for (i <- 0 until 5) yield {
      exec(() => controller.create(
        videoReferenceUuid,
        "Goblin " + i,
        "brian",
        recordedDate = Some(recordedDate.plusSeconds(i * 3600))))
    }

    xs.size should be (5)

    val (closeable, stream) = controller.streamByVideoReferenceUUIDAndTimestamps(videoReferenceUuid,
        recordedDate,
      recordedDate.plusSeconds(4000))
    val annos = stream.iterator().asScala.toList
    annos.size should be(2)
  }

  it should "stream by concurrent request" in {
    val start = Instant.now()
    val uuids = 0 until 5 map(_ => UUID.randomUUID())
    val xs = for {
      uuid <- uuids
      i <- 0 until 5
    } yield {
      exec(() => controller.create(uuid,
        "Sharktopod",
        "brian",
        recordedDate = Some(Instant.now())))
    }

    val end = Instant.now()
    xs.size should be(25)
    val concurrentRequest = ConcurrentRequest(start, end, uuids)

    // Verify count call is working
    val a = exec(() => controller.countByConcurrentRequest(concurrentRequest))
    a.intValue() should be (xs.size)

    // Verify stream works
    val (closeable, stream) = controller.streamByConcurrentRequest(concurrentRequest, None, None)
    val annos = stream.iterator().asScala.toList
    annos.size should be (xs.size)
    closeable.close()

    // Verify limit and offset work
    val (closeable2, stream2) = controller.streamByConcurrentRequest(concurrentRequest, Some(10), Some(2))
    val annos2 = stream2.iterator().asScala.toList
    annos2.size should be (10)
    closeable2.close()

  }

  it should "stream by multi request" in {
    val uuids = 0 until 5 map(_ => UUID.randomUUID())
    val xs = for {
      uuid <- uuids
      i <- 0 until 5
    } yield {
      exec(() => controller.create(uuid,
        "Octosaurus",
        "brian",
        recordedDate = Some(Instant.now())))
    }

    xs.size should be(25)
    val multiRequest = MultiRequest(uuids)

    // Verify count call is working
    val a = exec(() => controller.countByMultiRequest(multiRequest))
    a.intValue() should be (xs.size)

    // Verify stream works
    val (closeable, stream) = controller.streamByMultiRequest(multiRequest, None, None)
    val annos = stream.iterator().asScala.toList
    annos.size should be (xs.size)
    closeable.close()

    // Verify limit and offset work
    val (closeable2, stream2) = controller.streamByMultiRequest(multiRequest, Some(10), Some(2))
    val annos2 = stream2.iterator().asScala.toList
    annos2.size should be (10)
    closeable2.close()

  }

  it should "create and update" in {
    val a = exec(() => controller.create(
      UUID.randomUUID(),
      "Grimpoteuthis",
      "brian",
      recordedDate = Some(recordedDate)))
    a.concept should be("Grimpoteuthis")
    a.observer should be("brian")
    a.recordedTimestamp should be(recordedDate)

    val b = exec(() => controller.update(a.observationUuid, Some(a.videoReferenceUuid),
      concept = Some("Nanomia bijuga")))
    b should not be (empty)
    b.get.concept should be("Nanomia bijuga")
  }

  it should "create and update with different elapsed times" in {
    val et0 = Duration.ofSeconds(30)
    val et1 = Duration.ofSeconds(60)
    val a = exec(() => controller.create(
      UUID.randomUUID(),
      "Grimpoteuthis",
      "brian",
      elapsedTime = Some(et0)))
    a.concept should be("Grimpoteuthis")
    a.observer should be("brian")
    a.elapsedTime should be(et0)

    val b = exec(() => controller.update(
      a.observationUuid,
      Some(a.videoReferenceUuid),
      concept = Some("Nanomia bijuga"),
      elapsedTime = Some(et1)))
    b should not be (empty)
    b.get.concept should be("Nanomia bijuga")
    b.get.elapsedTime should be(et1)

    val c = exec(() => controller.update(
      a.observationUuid,
      elapsedTime = Some(et0)))
    c should not be (empty)
    c.get.elapsedTime should be(et0)
  }

  //  it should "report insert benchmark" in {
  //    val start = System.nanoTime()
  //    val n = 10000
  //    for (i <- 0 until n) {
  //      exec(() => controller.create(
  //        UUID.randomUUID(),
  //        "Nanomia bijuga",
  //        "brian",
  //        recordedDate = Some(recordedDate)))
  //    }
  //    val end = System.nanoTime();
  //    val nanos = end - start
  //    val duration = Duration.ofNanos(nanos)
  //    log.info(s"Inserted $n records in $duration")
  //  }

  it should "bulk insert simple annotations" in {
    val videoReferenceUuid = UUID.randomUUID()
    val now = Instant.now()
    val annos = (0 until 4).map(i =>
      AnnotationImpl(videoReferenceUuid, "concept" + i, "brian", recordedDate = Some(now.plus(Duration.ofSeconds(Random.nextInt(1000))))))
    val newAnnos = exec(() => controller.bulkCreate(annos))
    newAnnos.foreach(checkUuids)
    newAnnos.size should be (4)
  }

  it should "bulk insert complex annotations" in {
    val videoReferenceUuid = UUID.randomUUID()
    val now = Instant.now()
    val imagedMoments = (1 until 3).map(i => entityFactory.createImagedMoment(i,
      videoReferenceUuid,
      "complex bulk insert " + i,
      now.plus(Duration.ofSeconds(Random.nextInt(10000)))
    ))
    val annos = imagedMoments.flatMap(i => AnnotationImpl(i))
    val newAnnos = exec(() => controller.bulkCreate(annos))
    newAnnos.size should be (3)
    newAnnos.foreach(checkUuids)
  }

  it should "bulk insert a full dive of annotations" in {

    daoFactory.cleanup()
    val df = daoFactory.asInstanceOf[BasicDAOFactory]
    val imagedMomentController = new ImagedMomentController(df)
    val associationController = new AssociationController(df)
    val observationController = new ObservationController(df)
    val imageReferenceController = new ImageReferenceController(df)

    // --- Read data
    val url = getClass.getResource("/json/annotation_full_dive.json").toURI
    val source = Source.fromFile(url, "UTF-8")
    val json = source.getLines()
      .mkString("\n")
    source.close()

    // --- Insert all annotations
    val annos = Constants.GSON.fromJson(json, classOf[Array[AnnotationImpl]])
    annos should not be null
    annos.isEmpty should be (false)
    val newAnnos = exec(() => controller.bulkCreate(annos))
    newAnnos.size should be (annos.size)

    // --- Trust insert, but verify
    val videoReferenceUuid = newAnnos.head.videoReferenceUuid
    val n = exec(() => imagedMomentController.countByVideoReferenceUuid(videoReferenceUuid))
    n should be > (0)
    val obsCount = exec(() => observationController.countByVideoReferenceUUID(videoReferenceUuid))
    obsCount should be (newAnnos.size)

    val allAssociations = annos.flatMap(_.associations)
    val insertedAssociations = exec(() => associationController.findAll())
    insertedAssociations should have size allAssociations.size

    // --- Delete all annotations
    videoReferenceUuid should not be null
    val deleteCount = exec(() => imagedMomentController.deleteByVideoReferenceUUID(videoReferenceUuid))
    deleteCount should equal  (newAnnos.size +- 1)

    val associations = exec(() => associationController.findAll())
    associations should be (empty)

    val imageReferences = exec(() => imageReferenceController.findAll())
    imageReferences should be (empty)

    val observations = exec(() => observationController.findAll())
    observations should be (empty)

    val imagedMoments = exec(() => imagedMomentController.findAll())
    imagedMoments should be (empty)

//    val newAnnos0 = exec(() => controller.bulkCreate(annos))
//    newAnnos0.size should be (annos.size)

  }

  def checkUuids(a: Annotation): Unit = {
    a.imagedMomentUuid should not be null
    a.imageReferences.foreach(_.uuid should not be null)
    a.associations.foreach(_.uuid should not be null)
  }

  protected override def afterAll(): Unit = {
    daoFactory.cleanup()
  }

}
