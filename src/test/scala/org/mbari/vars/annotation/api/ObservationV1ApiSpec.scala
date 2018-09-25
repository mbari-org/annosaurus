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

package org.mbari.vars.annotation.api

import java.nio.charset.StandardCharsets
import java.time.{ Duration, Instant }
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.Constants
import org.mbari.vars.annotation.controllers.ObservationController
import org.mbari.vars.annotation.dao.jpa.{ AnnotationImpl, AssociationImpl, ImagedMomentImpl, ObservationImpl }
import org.mbari.vars.annotation.model.Observation

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.{ Duration => SDuration }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-09-13T14:31:00
 */
class ObservationV1ApiSpec extends WebApiStack {

  private[this] val timeout = SDuration(3000, TimeUnit.MILLISECONDS)

  private[this] val observationV1Api = {
    val controller = new ObservationController(daoFactory)
    new ObservationV1Api(controller)
  }

  protected[this] override val gson = Constants.GSON_FOR_ANNOTATION

  private[this] val path = "/v1/observations"

  addServlet(observationV1Api, path)

  var observation: Observation = _

  "ObservationV1Api" should "find by uuid" in {

    // --- create an observation
    val dao = daoFactory.newObservationDAO()
    val imagedMoment = ImagedMomentImpl(Some(UUID.randomUUID()), Some(Instant.now()))
    observation = ObservationImpl("rocketship", observer = Some("brian"), group = Some("ROV"),
      activity = Some("transect"))
    imagedMoment.addObservation(observation)
    val f = dao.runTransaction(d => d.create(observation))
    f.onComplete(t => dao.close())
    Await.result(f, timeout)

    // --- find it via the web api
    get(s"$path/${observation.uuid}") {
      status should be(200)
      val obs = gson.fromJson(body, classOf[ObservationImpl])
      obs.observer should be("brian")
      obs.group should be("ROV")
      obs.activity should be("transect")
      obs.lastUpdated should not be empty
      //obs.observationDate should not be (null)

    }
  }

  it should "find by videoreference" in {
    val dao = daoFactory.newObservationDAO()
    val newObs = ObservationImpl("submarine", observer = Some("schlin"), group = Some("AUV"),
      activity = Some("descent"))
    val f = dao.runTransaction(d => {
      dao.findByUUID(observation.uuid) match {
        case None => fail(s"Unable to find observation with uuid of ${observation.uuid}")
        case Some(obs) => obs.imagedMoment.addObservation(newObs)
      }
    })
    f.onComplete(t => dao.close())
    Await.result(f, timeout)

    get(s"$path/videoreference/${observation.imagedMoment.videoReferenceUUID}") {
      status should be(200)
      val obs = gson.fromJson(body, classOf[Array[ObservationImpl]])
      obs.size should be(2)
    }
  }

  it should "find by association" in {
    val dao = daoFactory.newObservationDAO()
    val assDao = daoFactory.newAssociationDAO(dao)
    val association = assDao.newPersistentObject("eating", Some("cake"))
    val f = dao.runTransaction(d => {
      dao.findByUUID(observation.uuid) match {
        case None => fail(s"Unable to find observation with uuid of ${observation.uuid}")
        case Some(obs) => obs.addAssociation(association)
      }
    })
    f.onComplete(t => dao.close())
    Await.result(f, timeout)

    get(s"$path/association/${association.uuid}") {
      status should be(200)
      val obs = gson.fromJson(body, classOf[AssociationImpl])
      obs.uuid should be(observation.uuid)
    }
  }

  it should "find all names" in {
    // --- create another imagedmoment with a different video-reference uuid
    val dao = daoFactory.newObservationDAO()
    val imagedMoment = ImagedMomentImpl(Some(UUID.randomUUID()), Some(Instant.now()))
    val obs = ObservationImpl("squid", observer = Some("aine"), group = Some("Image:Benthic Rover"),
      activity = Some("transit"))
    imagedMoment.addObservation(obs)
    val f = dao.runTransaction(d => d.create(obs))
    f.onComplete(t => dao.close())
    Await.result(f, timeout)

    // --- find all names
    get(s"$path/concepts") {
      status should be(200)
      println(body)
      val concepts = gson.fromJson(body, classOf[Array[String]])
      concepts should contain theSameElementsAs Array("squid", "submarine", "rocketship")
    }
  }

  it should "find all names for a video reference" in {
    get(s"$path/concepts/${observation.imagedMoment.videoReferenceUUID}") {
      status should be(200)
      val concepts = gson.fromJson(body, classOf[Array[String]])
      println(body)
      concepts should contain theSameElementsAs Array("submarine", "rocketship")
    }
  }

  it should "update" in {
    put(
      s"$path/${observation.uuid}",
      "concept" -> "shoe",
      "duration_millis" -> "3200",
      "activity" -> "ascent") {
        status should be(200)
        val obs = gson.fromJson(body, classOf[ObservationImpl])
        obs.concept should be("shoe")
        obs.duration should be(Duration.ofMillis(3200))
        obs.activity should be("ascent")
        obs.uuid should be(observation.uuid)
      }
  }

  it should "delete" in {
    delete(s"$path/${observation.uuid}") {
      status should be(204)
    }
  }

  it should "bulk delete" in {
    val dao = daoFactory.newObservationDAO()
    val imagedMoment = ImagedMomentImpl(Some(UUID.randomUUID()), Some(Instant.now()))
    val obs0 = ObservationImpl("rocketship", observer = Some("brian"), group = Some("ROV"),
      activity = Some("transect"))
    val obs1 = ObservationImpl("giant dragon", observer = Some("brian"), group = Some("AUV"),
      activity = Some("transect"))
    imagedMoment.addObservation(obs0)
    imagedMoment.addObservation(obs1)
    val f = dao.runTransaction(d => {
      d.create(obs0)
      d.create(obs1)
    })
    f.onComplete(t => dao.close())
    Await.result(f, timeout)

    val annos = Seq(obs0, obs1).map(AnnotationImpl(_))
      .map(_.observationUuid)
      .asJava
    val json = Constants.GSON_FOR_ANNOTATION
      .toJson(annos)
      .getBytes(StandardCharsets.UTF_8)
    post(
      s"$path/delete",
      body = json,
      headers = Map("Content-Type" -> "application/json")) {
        status should be(200)
      }
  }

}
