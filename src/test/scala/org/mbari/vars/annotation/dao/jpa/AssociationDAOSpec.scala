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

package org.mbari.vars.annotation.dao.jpa

import java.time.{Duration, Instant}
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.dao.{AssociationDAO}
import org.mbari.vars.annotation.model.Association
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{Await}
import scala.concurrent.duration.{Duration => SDuration}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-06-28T09:49:00
  */
class AssociationDAOSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory = TestDAOFactory.Instance

  private[this] val timeout            = SDuration(2, TimeUnit.SECONDS)
  private[this] val imDao              = daoFactory.newImagedMomentDAO()
  private[this] val obsDao             = daoFactory.newObservationDAO(imDao)
  private[this] val dao                = daoFactory.newAssociationDAO(imDao)
  private[this] val videoReferenceUUID = UUID.randomUUID()
  private[this] val now                = Instant.now()
  private[this] val imagedMoment0 =
    ImagedMomentImpl(Some(videoReferenceUUID), Some(now), elapsedTime = Some(Duration.ofMinutes(1)))
  private[this] val concept = "Grimpoteuthis"
  private[this] val observation0 =
    ObservationImpl(concept, observationDate = Some(now), observer = Some("brian"))
  private[this] val association0 =
    AssociationImpl("surface-color", Association.TO_CONCEPT_SELF, "red")
  private[this] val association1 =
    AssociationImpl("image-quality", Association.TO_CONCEPT_SELF, "mega-awesome!!")

  private type ADAO = AssociationDAO[AssociationImpl]
  def run[R](fn: ADAO => R): R = Await.result(dao.runTransaction(fn), timeout)

  "AssociationDAOImpl" should "create" in {
    imagedMoment0.addObservation(observation0)
    observation0.addAssociation(association0)
    run(_.create(association0))
    association0.uuid should not be null

    observation0.addAssociation(association1)
    run(_.create(association0))
  }

  it should "update" in {
    run(d => {
      val ass = d.findByUUID(association0.uuid)
      ass shouldBe defined
      ass.get.linkValue = "blue"
    })

    val ass = run(_.findByUUID(association0.uuid)).head
    ass.linkValue should be("blue")
  }

  it should "findByUUID" in {
    val ass = run(_.findByUUID(association0.uuid))
    ass shouldBe defined
  }

  it should "findAll" in {
    val ass = run(_.findAll())
      .filter(_.observation.uuid == observation0.uuid)

    ass.size should be(2)
  }

  it should "findByLinkName" in {
    val ass = run(_.findByLinkName("surface-color"))
    ass.size should be(1)
  }

  it should "findByLinkNameAndVideoReferenceUUID when matches are present" in {
    val ass = run(_.findByLinkNameAndVideoReferenceUUID("surface-color", videoReferenceUUID))
    ass.size should be(1)
  }

  it should "findByLinkNameAndVideoReferenceUUID when matches are absent" in {
    val ass = run(_.findByLinkNameAndVideoReferenceUUID("bottom-color", videoReferenceUUID))
    ass.size should be(0)
  }

  it should "findByLinkNameAndVideoReferenceUUIDAndConcept when matches are present" in {
    val ass = run(
      _.findByLinkNameAndVideoReferenceUUIDAndConcept(
        "surface-color",
        videoReferenceUUID,
        Some("Grimpoteuthis")
      )
    )
    ass.size should be(1)
  }

  it should "findByLinkNameAndVideoReferenceUUIDAndConcept when matches are absent" in {
    val assNon = run(
      _.findByLinkNameAndVideoReferenceUUIDAndConcept(
        "surface-color",
        videoReferenceUUID,
        Some("Nanomia")
      )
    )
    assNon.size should be(0)
  }

  it should "delete" in {
    val ass = run(_.findAll())
      .filter(_.uuid == association1.uuid)
    run(d => ass.foreach(d.delete))

    val assCheck = run(_.findAll()).filter(_.uuid == association1.uuid)
    assCheck shouldBe empty
  }

  override protected def afterAll(): Unit = {
    daoFactory.cleanup()
  }

}
