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
import org.mbari.annosaurus.repository.jpa.entity.extensions.*
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{Duration, Instant}
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration as SDuration
import scala.concurrent.{Await, Future}

/**
  * @author Brian Schlining
  * @since 2017-11-09T15:19:00
  */
class CachedAncillaryDatumControllerSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory = DerbyTestDAOFactory
  private[this] val controller = new CachedAncillaryDatumController(
    daoFactory
  )
  private[this] val imagedMomentController = new ImagedMomentController(
    daoFactory
  )
  private[this] val timeout            = SDuration(200, TimeUnit.SECONDS)
  private[this] val recordedDate       = Instant.now()
  private[this] val videoReferenceUuid = UUID.randomUUID()

  private[this] val imagedMoments = {

    val dao = daoFactory.newImagedMomentDAO()
    val ims = (0 until 4).map(i =>
      dao.newPersistentObject(
        videoReferenceUuid,
        elapsedTime = Some(Duration.ofMillis(1000 + i * 10 * 1000)),
        recordedDate = Some(recordedDate.plusSeconds(10 * i))
      )
    )
    exec(() => dao.runTransaction(d => ims.map(d.create)))
    ims
  }

  def exec[R](fn: () => Future[R]): R = Await.result(fn.apply(), timeout)

  "CacheAncillaryDatumController" should "create" in {
    val im  = imagedMoments.head
    val cad = exec(() => controller.create(im.getUuid, 36.3, -122.345, 1078))
    cad should not be (null)
    cad.getUuid() should not be (null)
    cad.getImagedMoment().getUuid() should be(im.getUuid())

    cad.getDepthMeters() should be(1078)
    cad.lastUpdated should not be None
  }

  it should "bulk create datums" in {
    val cads = imagedMoments.map(i => {
      CachedAncillaryDatum(
        imagedMomentUuid = Some(i.getUuid()),
        latitude = Some(math.random() * 90),
        longitude = Some(math.random() * 180),
        depthMeters = Some(1000)
      )
    })

    exec(() => controller.bulkCreateOrUpdate(cads))

    imagedMoments.foreach(im => {
      val maybeMoment = exec(() => imagedMomentController.findByUUID(im.getUuid()))
      maybeMoment should not be None
      val i = maybeMoment.get
      i.getAncillaryDatum().getDepthMeters() should be(1000)
      i.getAncillaryDatum().lastUpdated should not be None
    })
  }

  it should "merge" in {

    // --- Remove
    val minEpochMillis = imagedMoments.map(_.getRecordedTimestamp.toEpochMilli).min

    val cads = imagedMoments
      .zipWithIndex
      .map({
        case (im, idx) =>
          //val ts = im.recordedDate.plusMillis(1000)
          val ts = Instant.ofEpochMilli(minEpochMillis + idx * 10 * 1000 + 1000)
          CachedAncillaryDatum(
            recordedTimestamp = Some(ts),
            latitude = Some(90),
            longitude = Some(180),
            depthMeters = Some(2000),
            imagedMomentUuid = Some(im.getUuid()),
            lightTransmission = Some(50),
            temperatureCelsius = Some(4),
            salinity = Some(33.5f),
            crs = Some("EPSG:4326")
          )

      })

    exec(() => controller.merge(cads, videoReferenceUuid, Duration.ofMillis(15000)))

    imagedMoments.foreach(im => {
      val maybeMoment = exec(() => imagedMomentController.findByUUID(im.getUuid()))
      maybeMoment should not be None
      val i  = maybeMoment.get
      val ad = i.getAncillaryDatum()
//      println(im.recordedDate)

      ad.getDepthMeters() should be(2000)
      ad.getSalinity() should be(33.5f)
      ad.getLightTransmission() should be(50)
    })

  }

  override protected def afterAll(): Unit = {
    daoFactory.cleanup()
  }

}
