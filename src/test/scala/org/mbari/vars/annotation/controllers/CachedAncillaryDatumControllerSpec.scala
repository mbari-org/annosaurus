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

import java.time.{ Duration, Instant }
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.dao.jpa.{ ImagedMomentImpl, TestDAOFactory }
import org.mbari.vars.annotation.model.simple.CachedAncillaryDatumBean
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers }

import scala.concurrent.duration.{ Duration => SDuration }
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author Brian Schlining
 * @since 2017-11-09T15:19:00
 */
class CachedAncillaryDatumControllerSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory = TestDAOFactory.Instance
  private[this] val controller = new CachedAncillaryDatumController(daoFactory.asInstanceOf[BasicDAOFactory])
  private[this] val imagedMomentController = new ImagedMomentController(daoFactory.asInstanceOf[BasicDAOFactory])
  private[this] val timeout = SDuration(200, TimeUnit.SECONDS)
  private[this] val recordedDate = Instant.now()

  private[this] val imagedMoments = {
    val videoReferenceUuid = UUID.randomUUID()
    val dao = daoFactory.newImagedMomentDAO()
    val ims = (0 until 10).map(i => dao.newPersistentObject(
      videoReferenceUuid,
      elapsedTime = Some(Duration.ofMillis(math.round(math.random() * 10000L)))
    ))
    dao.runTransaction(d => {
      ims.foreach(dao.create)
    })
    ims
  }

  def exec[R](fn: () => Future[R]): R = Await.result(fn.apply(), timeout)

  "CacheAncillaryDatumController" should "create" in {
    val im = imagedMoments.head
    val cad = exec(() => controller.create(im.uuid, 36.3, -122.345, 1078))
    cad should not be (null)
    cad.uuid should not be (null)
    cad.imagedMoment.uuid should be(im.uuid)
    cad.depthMeters should not be None
    cad.depthMeters.get should be(1078)
  }

  it should "bulk create datums" in {
    val cads = imagedMoments.map(i => {
      val c = new CachedAncillaryDatumBean
      c.imagedMomentUuid = i.uuid
      c.latitude = Some(math.random() * 90)
      c.longitude = Some(math.random() * 180)
      c.depthMeters = Some(1000)
      //c.uuid = UUID.randomUUID()
      c
    })

    exec(() => controller.bulkCreateOrUpdate(cads))

    imagedMoments.foreach(im => {
      val maybeMoment = exec(() => imagedMomentController.findByUUID(im.uuid))
      maybeMoment should not be None
      val i = maybeMoment.get
      i.ancillaryDatum.depthMeters should not be None
      i.ancillaryDatum.depthMeters.get should be(1000)
    })
  }

}
