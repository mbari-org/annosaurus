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

import org.mbari.annosaurus.domain.WindowRequest
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
import scala.concurrent.duration.Duration as SDuration

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-06-27T15:12:00
  */
class ImagedMomentDAOSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory = DerbyTestDAOFactory

  private[this] val timeout            = SDuration(2, TimeUnit.SECONDS)
  private[this] val dao                = daoFactory.newImagedMomentDAO()
  private[this] val videoReferenceUUID = UUID.randomUUID()
  private[this] val now                = Instant.now()
  private[this] val imagedMoment0 =
    ImagedMomentEntity(videoReferenceUUID, now, null, Duration.ofMinutes(1))
  private[this] val imagedMoment1 = ImagedMomentEntity(
    videoReferenceUUID,
    now.plusSeconds(60),
    null,
    Duration.ofMinutes(5)
  )

  private type IMDAO = ImagedMomentDAO[ImagedMomentEntity]
  def run[R](fn: IMDAO => R): R = Await.result(dao.runTransaction(fn), timeout)

  "ImagedMomentDAOImpl" should "create" in {
    run(_.create(imagedMoment0))
    imagedMoment0.getUuid() should not be null

    // --- Add a second
    run(_.create(imagedMoment1))
  }

  it should "update" in {
    val timecode = new Timecode(2345, 29.97)
    run(d => {
      val im = d
        .findByVideoReferenceUUID(videoReferenceUUID)
        .find(_.getUuid() == imagedMoment0.getUuid)
      im shouldBe defined
      im.get.setTimecode(timecode)
    })

    val imagedMoment = run(_.findByVideoReferenceUUID(videoReferenceUUID))
      .find(_.getUuid() == imagedMoment0.getUuid())
    imagedMoment should not be empty
    imagedMoment.get.getTimecode() should not be null
    imagedMoment.get.getTimecode().toString should be(timecode.toString)
  }

  it should "findByUUID" in {
    val im = run(_.findByUUID(imagedMoment0.getUuid()))
    im shouldBe defined
  }

  it should "findByVideoReferenceUUIDAndElapsedTime" in {
    val im = run(
      _.findByVideoReferenceUUIDAndElapsedTime(
        imagedMoment0.getVideoReferenceUuid(),
        imagedMoment0.getElapsedTime()
      )
    )
    im shouldBe defined
  }

  it should "findByVideoReferenceUUIDAndRecordedDate" in {
    val im = run(
      _.findByVideoReferenceUUIDAndRecordedDate(
        imagedMoment0.getVideoReferenceUuid(),
        imagedMoment0.getRecordedDate()
      )
    )
    im shouldBe defined
  }

  it should "findByVideoReferenceUUIDAndIndex" in {
    val im0 = run(
      _.findByVideoReferenceUUIDAndIndex(
        videoReferenceUUID,
        elapsedTime = Option(imagedMoment0.getElapsedTime())
      )
    )
    im0 shouldBe defined
    val im1 = run(
      _.findByVideoReferenceUUIDAndIndex(
        videoReferenceUUID,
        recordedDate = Option(imagedMoment0.getRecordedDate())
      )
    )
    im1 shouldBe defined
  }

  it should "findByWindowRequest" in {
    val windowRequest =
      WindowRequest(Seq(videoReferenceUUID), imagedMoment0.getUuid(), Duration.ofSeconds(61).toMillis())
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
    val im = run(_.findByUUID(imagedMoment0.getUuid()))
    im shouldBe empty
  }

  it should "delete" in {
    val im = run(_.findAll()).filter(_.getUuid() == imagedMoment1.getUuid())
    run(d => im.foreach(d.delete))
    val imCheck = run(_.findAll()).filter(_.getUuid == imagedMoment1.getUuid)
    imCheck shouldBe empty
  }

  override protected def afterAll(): Unit = {
    daoFactory.cleanup()
  }

}
