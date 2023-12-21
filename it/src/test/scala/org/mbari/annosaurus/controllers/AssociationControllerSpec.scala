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
import org.mbari.annosaurus.repository.jpa.entity.{AssociationEntity, ImagedMomentEntity, ObservationEntity}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration as SDuration
import scala.concurrent.{Await, Future}

/**
  * @author Brian Schlining
  * @since 2019-06-05T14:52:00
  */
class AssociationControllerSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory = DerbyTestDAOFactory
  private[this] val controller = new AssociationController(daoFactory)
  private[this] val annotationController = new AnnotationController(daoFactory)
  private[this] val timeout      = SDuration(200, TimeUnit.SECONDS)
  private[this] val recordedDate = Instant.now()

  def exec[R](fn: () => Future[R]): R = Await.result(fn.apply(), timeout)

  "AssociationController" should "find by ConceptAssociationRequest" in {

    // Create the annotations
    val uuids = 0 until 5 map (_ => UUID.randomUUID())
    val as = for {
      uuid <- uuids
      i    <- 0 until 5
    } yield {
      val im  = ImagedMomentEntity(recordedDate = Some(recordedDate), videoReferenceUUID = Some(uuid))
      val obs = ObservationEntity("Cyclops", observer = Some("brian"))
      im.addObservation(obs)
      val ass = AssociationEntity(s"foo-$i", "self", s"$i")
      obs.addAssociation(ass)
      Annotation.from(obs, true)
    }
    val annotations = exec(() => annotationController.bulkCreate(as))
    annotations.size should be(25)

    // Find by request
    val linkName = "foo-0"
    val request  = ConceptAssociationRequest(uuids, linkName)
    val response = exec(() => controller.findByConceptAssociationRequest(request))
//    println(Constants.GSON.toJson(response))
    response.conceptAssociationRequest.linkName should be(linkName)
    response.conceptAssociationRequest.videoReferenceUuids should contain theSameElementsAs uuids
    response.associations.size should be(5)
    response.associations.foreach(a => a.linkName should be(linkName))
    response.associations.foreach(a => a.linkValue should be("0"))

  }

  override protected def afterAll(): Unit = {
    daoFactory.cleanup()
  }
}
