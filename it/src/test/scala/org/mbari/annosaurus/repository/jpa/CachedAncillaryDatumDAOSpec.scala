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

import org.mbari.annosaurus.repository.CachedAncillaryDatumDAO
import org.mbari.annosaurus.repository.jpa.entity.{CachedAncillaryDatumEntity, ImagedMomentEntity}
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
  * @since 2016-06-28T15:39:00
  */
class CachedAncillaryDatumDAOSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory = DerbyTestDAOFactory

  private[this] val timeout            = SDuration(2, TimeUnit.SECONDS)
  private[this] val imDao              = daoFactory.newImagedMomentDAO()
  private[this] val dao                = daoFactory.newCachedAncillaryDatumDAO(imDao)
  private[this] val videoReferenceUUID = UUID.randomUUID()
  private[this] val now                = Instant.now()
  private[this] val imagedMoment0 =
    ImagedMomentEntity(Some(videoReferenceUUID), Some(now), elapsedTime = Some(Duration.ofMinutes(1)))
  private[this] val ancillaryDatum0 = CachedAncillaryDatumEntity(36.234, 122.0011, 666)
  private[this] val newTemp         = 3.2

  private type CADAO = CachedAncillaryDatumDAO[CachedAncillaryDatumEntity]
  def run[R](fn: CADAO => R): R = Await.result(dao.runTransaction(fn), timeout)

  "CachedAncillaryDatumDAOImpl" should "create" in {
    imagedMoment0.ancillaryDatum = ancillaryDatum0
    run(_.create(ancillaryDatum0))
    ancillaryDatum0.uuid should not be null
  }

  it should "update" in {
    run(d => {
      val ad = d.findByUUID(ancillaryDatum0.uuid)
      ad shouldBe defined
      ad.get.temperatureCelsius = Some(newTemp)
    })

    val datum = run(d => d.findByUUID(ancillaryDatum0.uuid)).head
    datum.temperatureCelsius should not be None
    datum.temperatureCelsius.get should be(newTemp +- 0.000001d)
  }

  it should "findAll" in {
//    val datum = run(_.findAll()).filter(_.imagedMoment.uuid == imagedMoment0.uuid)
//    datum.size should be(1)
    run(_.findAll()).size should be > 0
  }

  it should "delete" in {
    run(d => {
      val datum = d.findByUUID(ancillaryDatum0.uuid)
      d.delete(datum.get)
    })

    val datCheck = run(_.findByUUID(ancillaryDatum0.uuid))
    datCheck shouldBe empty
  }

  override protected def beforeAll(): Unit = daoFactory.cleanup()

  override protected def afterAll(): Unit = {
    daoFactory.cleanup()
  }

}
