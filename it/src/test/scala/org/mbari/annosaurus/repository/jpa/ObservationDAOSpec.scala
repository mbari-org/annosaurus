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

import org.mbari.annosaurus.repository.ObservationDAO
import org.mbari.annosaurus.repository.jpa.entity.{ImagedMomentEntity, ObservationEntity}
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
  * @since 2016-06-28T08:44:00
  */
class ObservationDAOSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory = DerbyTestDAOFactory

  private[this] val timeout            = SDuration(2, TimeUnit.SECONDS)
  private[this] val imDao              = daoFactory.newImagedMomentDAO()
  private[this] val dao                = daoFactory.newObservationDAO(imDao)
  private[this] val videoReferenceUUID = UUID.randomUUID()
  private[this] val now                = Instant.now()
  private[this] val imagedMoment0 =
    ImagedMomentEntity(videoReferenceUUID, now, null, Duration.ofMinutes(1))
  private[this] val concept = "Grimpoteuthis"
  val newConcept            = "Aegina"
  private[this] val observation0 =
    ObservationEntity(concept, "brian")
  private[this] val observation1 =
    ObservationEntity(concept, "kyra")

  private type ODAO = ObservationDAO[ObservationEntity]
  def run[R](fn: ODAO => R): R = Await.result(dao.runTransaction(fn), timeout)

  "ObservationDAOImpl" should "create" in {
    imagedMoment0.addObservation(observation0)
    run(_.create(observation0))
    observation0.getUuid() should not be null

    // -- Add a second
    imagedMoment0.addObservation(observation1)
    run(_.create(observation1))
  }

  it should "update" in {

    val duration = Duration.ofMillis(1234)
    run(d => {
      val obs = d.findByUUID(observation0.getUuid())
      obs shouldBe defined
      obs.get.setConcept(newConcept)
      obs.get.setDuration(duration)
    })

    val obs = run(_.findByUUID(observation0.getUuid())).head
    obs.getConcept() should be(newConcept)
    obs.getDuration() should be(duration)

  }

  it should "findByUUID" in {
    val obs = run(_.findByUUID(observation0.getUuid()))
    obs shouldBe defined
  }

  it should "findAll" in {
    val all = run(_.findAll())
    all.size should be >= 2
  }

  it should "findAllConcepts" in {
    val names = run(_.findAllConcepts())
    names should contain allOf (concept, newConcept)
  }

  it should "findAllNamesByVideoReferenceUUID" in {
    val names = run(_.findAllConceptsByVideoReferenceUUID(imagedMoment0.getVideoReferenceUuid()))
    names should contain allOf (concept, newConcept)
  }

  it should "countByVideoReferenceUUID" in {
    val count = run(_.countByVideoReferenceUUID(imagedMoment0.getVideoReferenceUuid()))
    count should be >= 2
  }

  it should "deleteByUUID" in {
    run(_.deleteByUUID(observation0.getUuid()))
    val obs = run(_.findByUUID(observation0.getUuid()))
    obs shouldBe empty
  }

  it should "delete" in {
    val obs = run(_.findAll()).filter(_.getUuid() == observation1.getUuid())
    run(d => obs.foreach(d.delete))
    val obsCheck = run(_.findAll()).filter(_.getUuid() == observation1.getUuid())
    obsCheck shouldBe empty
  }

  override protected def afterAll(): Unit = {
    daoFactory.cleanup()
  }

}
