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

import java.net.URL
import java.time.{Duration, Instant}
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.dao.jpa.{AnnotationImpl, TestDAOFactory}
import org.mbari.vars.annotation.model.ImagedMoment
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration => SDuration}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class ImagedMomentControllerSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory = TestDAOFactory.Instance
  private[this] val controller = new ImagedMomentController(daoFactory.asInstanceOf[BasicDAOFactory])
  private[this] val recordedDate = Instant.now()
  private[this] val log = LoggerFactory.getLogger(getClass)
  private[this] val timeout = SDuration(200, TimeUnit.SECONDS)
  private[this] val videoReferenceUuid = UUID.randomUUID()
  private[this] val random = new Random()

  def exec[R](fn: () => Future[R]): R = Await.result(fn.apply(), timeout)

  "ImagedMomentController" should "create by recorded timestamp" in {
    val a = exec(() => controller.create(videoReferenceUuid, recordedDate = Some(recordedDate)))
    a.recordedDate should be (recordedDate)
    a.timecode should be (null)
    a.elapsedTime should be (null)
    a.videoReferenceUUID should be (videoReferenceUuid)
  }

  it should "find by videoReferenceUuid and recordedDate" in {
    val a = ImagedMomentController.findOrCreateImagedMoment(controller.newDAO(),
      videoReferenceUuid,
      recordedDate = Some(recordedDate))
    a should not be (null)
  }

  it should "create one imagedmoment if multiple creates use the same recordedDate" in {
    val a = exec(() => controller.create(videoReferenceUuid, recordedDate = Some(recordedDate)))
    val b = exec(() => controller.create(videoReferenceUuid, recordedDate = Some(recordedDate)))
    a.uuid should be (b.uuid)
  }

  it should "create one imagedmoment if multiple creates use the same recordedDate parsed from a string" in {

    val s = "2019-09-22T01:23:45.6789Z"

    def create(): Future[ImagedMoment] = {
      val i = Instant.parse(s)
      val c = new ImagedMomentController(daoFactory.asInstanceOf[BasicDAOFactory])
      c.create(videoReferenceUuid, recordedDate = Some(i))
    }

    val a = exec(create)
    val b = exec(create)
    a.uuid should be (b.uuid)
  }

  it should "create a single imagedmoment with multiple observations" in {
    val imagedMoment = createImagedMoment(1, concept = "Create test")
    val imDao = daoFactory.newImagedMomentDAO()
    val newImagedMoment = controller.create(imDao, imagedMoment)
    print(newImagedMoment)
  }

  private def createImagedMoment(n: Int,
                                  videoReferenceUuid: UUID = UUID.randomUUID(),
                                  concept: String = "foo",
                                  recordedTimestamp: Instant = Instant.now()): ImagedMoment = {

    val imDao = daoFactory.newImagedMomentDAO()
    val obsDao = daoFactory.newObservationDAO(imDao)
    val assDao = daoFactory.newAssociationDAO(imDao)
    val dataDao = daoFactory.newCachedAncillaryDatumDAO(imDao)
    val irDao = daoFactory.newImageReferenceDAO(imDao)
    val imagedMoment = imDao.newPersistentObject(videoReferenceUuid, recordedDate = Some(recordedTimestamp))
    for (i <- 0 until n) {
      val observation = obsDao.newPersistentObject(concept, "brian")
      imagedMoment.addObservation(observation)
      for (j <- 0 until n) {
        val association = assDao.newPersistentObject("test" + j, linkValue = Some("value " + i))
        observation.addAssociation(association)
      }

      val imageReference = irDao.newPersistentObject(new URL("http://www.mbari.org/path/name" + i))
      imagedMoment.addImageReference(imageReference)
    }

    val data = dataDao.newPersistentObject(random.nextDouble() * 90,
      random.nextDouble() * 180,
      random.nextDouble() * 2000)

    imagedMoment.ancillaryDatum = data
    imDao.close()
    imagedMoment

  }



  it should "create one imagedmoment if multiple creates use the same elasped_time" in {
    val elapsedTime = Duration.ofMillis(123456)
    val a = exec(() => controller.create(videoReferenceUuid, elapsedTime = Some(elapsedTime)))
    val b = exec(() => controller.create(videoReferenceUuid, elapsedTime = Some(elapsedTime)))
    a.uuid should be (b.uuid)
  }

  it should "stream/find video_reference_uuids modified between dates" in {
    val end = Instant.now()
    val start = end.minus(Duration.ofMinutes(2))
    val (closeable, stream) = controller.streamVideoReferenceUuidsBetweenUpdatedDates(start, end)
    val uuids = stream.iterator()
      .asScala
      .toSeq

    uuids.size should not be 0
  }

  protected override def afterAll(): Unit = {
    daoFactory.cleanup()
  }

}
