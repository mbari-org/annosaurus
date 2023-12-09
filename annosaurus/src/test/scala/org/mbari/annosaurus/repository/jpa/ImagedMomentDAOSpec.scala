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

import org.mbari.annosaurus.model.simple.WindowRequest
import org.mbari.annosaurus.repository.ImagedMomentDAO
import org.mbari.annosaurus.repository.jpa.entity.ImagedMomentEntity
import org.mbari.vcr4j.time.Timecode
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{Duration, Instant}
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration => SDuration}

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-06-27T15:12:00
  */
class ImagedMomentDAOSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory = TestDAOFactory.Instance

  private[this] val timeout            = SDuration(2, TimeUnit.SECONDS)
  private[this] val dao                = daoFactory.newImagedMomentDAO()
  private[this] val videoReferenceUUID = UUID.randomUUID()
  private[this] val now                = Instant.now()
  private[this] val imagedMoment0 =
    ImagedMomentEntity(Some(videoReferenceUUID), Some(now), elapsedTime = Some(Duration.ofMinutes(1)))
  private[this] val imagedMoment1 = ImagedMomentEntity(
    Some(videoReferenceUUID),
    Some(now.plusSeconds(60)),
    elapsedTime = Some(Duration.ofMinutes(5))
  )

  private type IMDAO = ImagedMomentDAO[ImagedMomentEntity]
  def run[R](fn: IMDAO => R): R = Await.result(dao.runTransaction(fn), timeout)

  "ImagedMomentDAOImpl" should "create" in {
    run(_.create(imagedMoment0))
    imagedMoment0.uuid should not be null

    // --- Add a second
    run(_.create(imagedMoment1))
  }

  it should "update" in {
    val timecode = new Timecode(2345, 29.97)
    run(d => {
      val im = d
        .findByVideoReferenceUUID(videoReferenceUUID)
        .find(_.uuid == imagedMoment0.uuid)
      im shouldBe defined
      im.get.timecode = timecode
    })

    val imagedMoment = run(_.findByVideoReferenceUUID(videoReferenceUUID))
      .find(_.uuid == imagedMoment0.uuid)
    imagedMoment should not be empty
    imagedMoment.get.timecode should not be null
    imagedMoment.get.timecode.toString should be(timecode.toString)
  }

  it should "findByUUID" in {
    val im = run(_.findByUUID(imagedMoment0.uuid))
    im shouldBe defined
  }

  it should "findByVideoReferenceUUIDAndElapsedTime" in {
    val im = run(
      _.findByVideoReferenceUUIDAndElapsedTime(
        imagedMoment0.videoReferenceUUID,
        imagedMoment0.elapsedTime
      )
    )
    im shouldBe defined
  }

  it should "findByVideoReferenceUUIDAndRecordedDate" in {
    val im = run(
      _.findByVideoReferenceUUIDAndRecordedDate(
        imagedMoment0.videoReferenceUUID,
        imagedMoment0.recordedDate
      )
    )
    im shouldBe defined
  }

  it should "findByVideoReferenceUUIDAndIndex" in {
    val im0 = run(
      _.findByVideoReferenceUUIDAndIndex(
        videoReferenceUUID,
        elapsedTime = Option(imagedMoment0.elapsedTime)
      )
    )
    im0 shouldBe defined
    val im1 = run(
      _.findByVideoReferenceUUIDAndIndex(
        videoReferenceUUID,
        recordedDate = Option(imagedMoment0.recordedDate)
      )
    )
    im1 shouldBe defined
  }

  it should "findByWindowRequest" in {
    val windowRequest =
      WindowRequest(Seq(videoReferenceUUID), imagedMoment0.uuid, Duration.ofSeconds(61))
    val im0 = run(_.findByWindowRequest(windowRequest))
    im0.size should be >= 2
  }

  it should "findAll" in {
    run(_.create(imagedMoment1))
    val all = run(_.findAll())
//    println(all)
    all.size should be >= 2
  }

  it should "deleteByUUID" in {
    run(_.delete(imagedMoment0))
    val im = run(_.findByUUID(imagedMoment0.uuid))
    im shouldBe empty
  }

  it should "delete" in {
    val im = run(_.findAll()).filter(_.uuid == imagedMoment1.uuid)
    run(d => im.foreach(d.delete))
    val imCheck = run(_.findAll()).filter(_.uuid == imagedMoment1.uuid)
    imCheck shouldBe empty
  }

  override protected def afterAll(): Unit = {
    daoFactory.cleanup()
  }

}
