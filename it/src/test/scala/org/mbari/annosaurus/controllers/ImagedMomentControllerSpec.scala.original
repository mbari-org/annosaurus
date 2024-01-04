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

import org.mbari.annosaurus.domain.*
import org.mbari.annosaurus.repository.jpa.{DerbyTestDAOFactory, TestDAOFactory}
import org.mbari.annosaurus.repository.jpa.entity.ImagedMomentEntity
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.slf4j.LoggerFactory

import java.net.URL
import java.time.{Duration, Instant}
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration as SDuration
import scala.concurrent.{Await, Future}
import scala.util.Random
import scala.jdk.CollectionConverters.*

class ImagedMomentControllerSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory    = DerbyTestDAOFactory
  private[this] val entityFactory = new TestEntityFactory(daoFactory)
  private[this] val controller = new ImagedMomentController(
    daoFactory
  )
  private[this] val recordedDate       = Instant.now()
  private[this] val log                = LoggerFactory.getLogger(getClass)
  private[this] val timeout            = SDuration(200, TimeUnit.SECONDS)
  private[this] val videoReferenceUuid = UUID.randomUUID()

  def exec[R](fn: () => Future[R]): R = Await.result(fn.apply(), timeout)

  "ImagedMomentController" should "create by recorded timestamp" in {
    val a = exec(() => controller.create(videoReferenceUuid, recordedDate = Some(recordedDate)))
    a.getRecordedTimestamp() should be(recordedDate)
    a.getTimecode() should be(null)
    a.getElapsedTime() should be(null)
    a.getVideoReferenceUuid() should be(videoReferenceUuid)
  }

  it should "find by videoReferenceUuid and recordedDate" in {
    val a = ImagedMomentController.findOrCreateImagedMoment(
      controller.newDAO(),
      videoReferenceUuid,
      recordedDate = Some(recordedDate)
    )
    a should not be (null)
    checkUuids(a)
  }

  it should "create one imagedmoment if multiple creates use the same recordedDate" in {
    val now = Instant.parse("2007-01-02T00:12:34.3456Z")
    val a   = exec(() => controller.create(videoReferenceUuid, recordedDate = Some(now)))
    val b   = exec(() => controller.create(videoReferenceUuid, recordedDate = Some(now)))
    a.getUuid() should be(b.getUuid())
  }

  it should "create one imagedmoment if multiple creates use the same recordedDate parsed from a string" in {

    val s = "2019-09-22T01:23:45.6789Z"

    def create(): Future[ImagedMomentEntity] = {
      val i = Instant.parse(s)
      val c = new ImagedMomentController(daoFactory)
      c.create(videoReferenceUuid, recordedDate = Some(i))
    }

    val a = exec(create)
    val b = exec(create)
    a.getUuid() should be(b.getUuid())
  }

  it should "fail if trying to insert the same URL more than once" in {
    val videoReferenceUuid = UUID.randomUUID()
    val url                = new URL("http://www.mbari.org/foo/image.png")
    val dao                = daoFactory.newImagedMomentDAO()
    assertThrows[Exception] {
      for (i <- 0 until 2) {
        val source = entityFactory.createImagedMoment(
          1,
          videoReferenceUuid = videoReferenceUuid,
          concept = "URL Test" + i,
          recordedTimestamp = Instant.now().plus(Duration.ofSeconds(Random.nextInt()))
        )
        source.getImageReferences().stream.forEach(_.setUrl(url))
        exec(() => dao.runTransaction(d => controller.create(d, source)))
      }
    }
    dao.close()
  }

  it should "create a single merged imagedmoment from ones with the same index" in {
    val now                = Instant.parse("2011-01-02T00:12:34.3456Z")
    val videoReferenceUuid = UUID.randomUUID()
    val dao                = daoFactory.newImagedMomentDAO()
    for (i <- 0 to 2) {
      val source = entityFactory.createImagedMoment(
        1,
        videoReferenceUuid = videoReferenceUuid,
        concept = "Yo" + i,
        recordedTimestamp = now
      )
//      println(source)
      exec(() => dao.runTransaction(d => controller.create(d, source)))
    }
    dao.close()

    val ims = exec(() => controller.findByVideoReferenceUUID(videoReferenceUuid))
    ims.size should be(1)

  }

  it should "create a single imagedmoment with multiple observations" in {
    val imagedMoment    = entityFactory.createImagedMoment(1, concept = "Create test")
    val imDao           = daoFactory.newImagedMomentDAO()
    val newImagedMoment = controller.create(imDao, imagedMoment)
    //print(newImagedMoment)
    checkUuids(newImagedMoment)
    newImagedMoment.getObservations.size should be(1)
    newImagedMoment.getObservations().asScala.head.getUuid should not be null
    newImagedMoment.getObservations().asScala.head.getAssociations.size should be(1)
  }

  it should "create multiple imagedmoments" in {
    val now           = Instant.now()
    val imagedMoment0 = entityFactory.createImagedMoment(1, concept = "A", recordedTimestamp = now)
    val imagedMoment1 = entityFactory.createImagedMoment(1, concept = "B", recordedTimestamp = now)
    val imDao         = daoFactory.newImagedMomentDAO()
    val future = imDao.runTransaction(d => {
      controller.create(d, imagedMoment0) :: controller.create(d, imagedMoment1) :: Nil
    })
    val ims = exec(() => future)
    imDao.close()
    ims.size should be(2)
    ims.foreach(checkUuids)
//    print(ims)
  }

  it should "create multiple imagedMoments with the same videoreference" in {
    val videoReferenceUuid = UUID.randomUUID()

  }

  it should "delete by videoReferenceUuid" in {
    val videoReferenceUuid = UUID.randomUUID()
    val now                = Instant.now()
    val n                  = 10
    val ims = (1 to n).map(i =>
      entityFactory.createImagedMoment(
        1,
        videoReferenceUuid = videoReferenceUuid,
        concept = s"delete $i",
        recordedTimestamp = now.plus(Duration.ofSeconds(i))
      )
    )
    ims.size should be(n)

    val newImagedMoments = exec(() => controller.create(ims))
    newImagedMoments.size should be(n)
    newImagedMoments.foreach(checkUuids)
    val sanityCount = exec(() => controller.findByVideoReferenceUUID(videoReferenceUuid))
    sanityCount.size should be(n)

    val deleteCount = exec(() => controller.deleteByVideoReferenceUUID(videoReferenceUuid))
    deleteCount should be(newImagedMoments.size)

    val remainingCount = exec(() => controller.findByVideoReferenceUUID(videoReferenceUuid))
    remainingCount.isEmpty should be(true)

  }

  private def checkUuids(imagedMoment: ImagedMomentEntity): Unit = {
    imagedMoment.getUuid() should not be null
    for (obs <- imagedMoment.getObservations().asScala) {
      obs.getUuid() should not be null
      for (ass <- obs.getAssociations().asScala) {
        ass.getUuid() should not be null
      }
    }
    for (ir <- imagedMoment.getImageReferences().asScala) {
      ir.getUuid() should not be null
    }
    Option(imagedMoment.getAncillaryDatum()).foreach(ad => ad.getUuid() should not be null)
  }

  it should "create one imagedmoment if multiple creates use the same elasped_time" in {
    val elapsedTime = Duration.ofMillis(123456)
    val a           = exec(() => controller.create(videoReferenceUuid, elapsedTime = Some(elapsedTime)))
    val b           = exec(() => controller.create(videoReferenceUuid, elapsedTime = Some(elapsedTime)))
    a.getUuid() should be(b.getUuid())
  }

  it should "stream/find video_reference_uuids modified between dates" in {
    val end                 = Instant.now()
    val start               = end.minus(Duration.ofMinutes(2))
    val (closeable, stream) = controller.streamVideoReferenceUuidsBetweenUpdatedDates(start, end)
    val uuids = stream
      .iterator()
      .asScala
      .toSeq

    uuids.size should not be 0
  }

  it should "count imagedmoments with images" in {
    val videoReferenceUuid = UUID.randomUUID();
    val imagedMoments = List(entityFactory.createImagedMoment(4, videoReferenceUuid, "foo"),  entityFactory.createImagedMoment(2, videoReferenceUuid, "bar"))
    val dao = daoFactory.newImagedMomentDAO()
    exec(() => dao.runTransaction(d => imagedMoments.foreach(i => d.create(i))))
    dao.close()
    val n = exec(() => controller.countByVideoReferenceUUIDWithImages(videoReferenceUuid))
    imagedMoments(0).getImageReferences.size should be (4)
    imagedMoments(1).getImageReferences.size should be (2)
    n should be (2)
  }


  override protected def afterAll(): Unit = {
    daoFactory.cleanup()
  }

}
