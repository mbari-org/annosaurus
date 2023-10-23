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

package org.mbari.vars.annotation.api.v1

import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.Constants
import org.mbari.vars.annotation.api.WebApiStack
import org.mbari.vars.annotation.controllers.{
  AnnotationController,
  AssociationController,
  BasicDAOFactory,
  ObservationController
}
import org.mbari.vars.annotation.dao.jpa.AssociationImpl
import org.mbari.vars.annotation.model.simple.{
  ConceptAssociationRequest,
  ConceptAssociationResponse
}
import org.mbari.vars.annotation.model.{Annotation, Association}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration => SDuration}

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-09-08T16:48:00
  */
class AssociationV1ApiSpec extends WebApiStack {

  private[this] val timeout            = SDuration(3000, TimeUnit.MILLISECONDS)
  private[this] val videoReferenceUuid = UUID.randomUUID()

  private[this] val associationV1Api = {
    val controller = new AssociationController(daoFactory)
    new AssociationV1Api(controller)
  }

  addServlet(associationV1Api, "/v1/associations")

  var annotation: Annotation   = _
  var association: Association = _

  "AssociationV1Api" should "create" in {

    annotation = {
      val controller = new AnnotationController(daoFactory)
      Await.result(
        controller
          .create(videoReferenceUuid, "Foo", "brian", elapsedTime = Some(Duration.ofMillis(2000))),
        timeout
      )
    }

    post(
      s"/v1/associations/",
      "observation_uuid" -> annotation.observationUuid.toString,
      "link_name"        -> "color",
      "link_value"       -> "red"
    ) {
      status should be(200)
      association = gson.fromJson(body, classOf[AssociationImpl])
      association.linkName should be("color")
      association.linkValue should be("red")
    }
  }

  it should "find by uuid" in {
    get(s"/v1/associations/${association.uuid}") {
      status should be(200)
      val a = gson.fromJson(body, classOf[AssociationImpl])
      a.uuid should be(association.uuid)
      a.linkName should be(association.linkName)
      a.toConcept should be(association.toConcept)
      a.linkValue should be(association.linkValue)
    }
  }

  it should "find by concept association request" in {

    val car  = ConceptAssociationRequest(association.linkName, Seq(annotation.videoReferenceUuid))
    val json = Constants.GSON.toJson(car)
    post(
      "/v1/associations/conceptassociations",
      headers = Map("Content-Type" -> "application/json"),
      body = json.getBytes(StandardCharsets.UTF_8)
    ) {
//      println("---" + body)
      status should be(200)
      val resp = Constants.GSON.fromJson(body, classOf[ConceptAssociationResponse])
      resp.associations should not be empty
    }
  }

  it should "find by videoreference.uuid and linkName" in {
    post(
      s"/v1/associations/",
      "observation_uuid" -> annotation.observationUuid.toString,
      "link_name"        -> "eating",
      "to_concept"       -> "crab"
    ) {
      status should be(200)
    }
    post(
      s"/v1/associations/",
      "observation_uuid" -> annotation.observationUuid.toString,
      "link_name"        -> "eating",
      "to_concept"       -> "filet-o-fish"
    ) {
      status should be(200)
    }
    get(s"/v1/associations/${annotation.videoReferenceUuid}/eating") {
      status should be(200)
      val links = gson.fromJson(body, classOf[Array[AssociationImpl]]).toList
      links.size should be(2)
    }

  }

  it should "update" in {
    put(
      s"/v1/associations/${association.uuid}",
      "link_name"  -> "surface-color",
      "link_value" -> "blue"
    ) {
      status should be(200)
      val a = gson.fromJson(body, classOf[AssociationImpl])
      a.linkName should be("surface-color")
      a.linkValue should be("blue")
    }
  }

  it should "update (move to new observation)" in {
    val newAnno = {
      val controller = new AnnotationController(daoFactory.asInstanceOf[BasicDAOFactory])
      Await.result(
        controller
          .create(UUID.randomUUID(), "Bar", "schlin", elapsedTime = Some(Duration.ofMillis(3000))),
        timeout
      )
    }

    put(
      s"/v1/associations/${association.uuid}",
      "observation_uuid" -> newAnno.observationUuid.toString
    ) {
      status should be(200)
      val a = gson.fromJson(body, classOf[AssociationImpl])
      a.uuid should be(association.uuid)

      val observationController =
        new ObservationController(daoFactory.asInstanceOf[BasicDAOFactory])
      val f      = observationController.findByAssociationUUID(a.uuid)
      val obsOpt = Await.result(f, timeout)
      obsOpt should not be empty
      obsOpt.get.uuid should be(newAnno.observationUuid)
    }

  }

  it should "create with a defined association UUID" in {
    val uuid = UUID.randomUUID()
    post(
      s"/v1/associations/",
      "observation_uuid" -> annotation.observationUuid.toString,
      "link_name"        -> "bounding box",
      "to_concept"       -> "cool thing",
      "link_value"       -> """{"x": 10}""",
      "association_uuid" -> uuid.toString()
    ) {
      status should be(200)
      association = gson.fromJson(body, classOf[AssociationImpl])
      association.linkName should be("bounding box")
      association.toConcept should be("cool thing")
      association.linkValue should be("""{"x": 10}""")
      association.uuid should be(uuid)
    }
  }

  it should "bulk delete" in {
    // TODO implement bulk delete spec
  }

  it should "delete" in {
    delete(s"/v1/associations/${association.uuid}") {
      status should be(204)
    }
  }

}
