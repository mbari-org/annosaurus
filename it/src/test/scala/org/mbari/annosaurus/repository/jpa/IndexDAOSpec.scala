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

import org.mbari.annosaurus.repository.jpa.entity.{ImagedMomentEntity, IndexEntity}
import org.mbari.annosaurus.repository.{ImagedMomentDAO, IndexDAO}
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
  * @author Brian Schlining
  * @since 2019-02-08T09:18:00
  */
class IndexDAOSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  private type IMDAO = ImagedMomentDAO[ImagedMomentEntity]
  private type IDAO  = IndexDAO[IndexEntity]
  private[this] val daoFactory         = DerbyTestDAOFactory
  private[this] val timeout            = SDuration(2, TimeUnit.SECONDS)
  private[this] val imDao              = daoFactory.newImagedMomentDAO()
  private[this] val dao                = daoFactory.newIndexDAO(imDao)
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

  def runIm[R](fn: IMDAO => R): R = Await.result(imDao.runTransaction(fn), timeout)

  def runId[R](fn: IDAO => R): R = Await.result(dao.runTransaction(fn), timeout)

  "ImagedMomentDAOImpl" should "create for spec setup" in {
    runIm(_.create(imagedMoment0))
    imagedMoment0.getUuid() should not be null

    // --- Add a second
    runIm(_.create(imagedMoment1))
  }

  it should "findByVideoReferenceUuid" in {
    val im = dao.findByVideoReferenceUuid(videoReferenceUUID)
    im should not be empty
    im.size should be(2)
    //im.foreach(println)
  }

  it should "update" in {
    val timecode = new Timecode(2345, 29.97)
    runId(d => {
      val id = d
        .findByVideoReferenceUuid(videoReferenceUUID)
        .find(_.getUuid() == imagedMoment0.getUuid())
      id shouldBe defined
      id.get.setTimecode(timecode)
    })

    val imagedMoment = runId(_.findByVideoReferenceUuid(videoReferenceUUID))
      .find(_.getUuid() == imagedMoment0.getUuid())
    imagedMoment should not be empty
    imagedMoment.get.getTimecode() should not be null
    imagedMoment.get.getTimecode().toString should be(timecode.toString)
  }

  override protected def afterAll(): Unit = {
    daoFactory.cleanup()
  }

}
