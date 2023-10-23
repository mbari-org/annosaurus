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

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.dao.jpa.{
  AnnotationImpl,
  AssociationImpl,
  ImagedMomentImpl,
  ObservationImpl,
  TestDAOFactory
}
import org.mbari.vars.annotation.model.simple.ConceptAssociationRequest
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration => SDuration}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author Brian Schlining
  * @since 2019-06-05T14:52:00
  */
class AssociationControllerSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  private[this] val daoFactory = TestDAOFactory.Instance
  private[this] val controller = new AssociationController(daoFactory.asInstanceOf[BasicDAOFactory])
  private[this] val annotationController = new AnnotationController(
    daoFactory.asInstanceOf[BasicDAOFactory]
  )
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
      val im  = ImagedMomentImpl(recordedDate = Some(recordedDate), videoReferenceUUID = Some(uuid))
      val obs = ObservationImpl("Cyclops", observer = Some("brian"))
      im.addObservation(obs)
      val ass = AssociationImpl(s"foo-$i", "self", s"$i")
      obs.addAssociation(ass)
      AnnotationImpl(obs)
    }
    val annotations = exec(() => annotationController.bulkCreate(as))
    annotations.size should be(25)

    // Find by request
    val linkName = "foo-0"
    val request  = ConceptAssociationRequest(linkName, uuids)
    val response = exec(() => controller.findByConceptAssociationRequest(request))
//    println(Constants.GSON.toJson(response))
    response.conceptAssociationRequest.linkName should be(linkName)
    response.conceptAssociationRequest.uuids should contain theSameElementsAs uuids
    response.associations.size should be(5)
    response.associations.foreach(a => a.linkName should be(linkName))
    response.associations.foreach(a => a.linkValue should be("0"))

  }

  override protected def afterAll(): Unit = {
    daoFactory.cleanup()
  }
}
