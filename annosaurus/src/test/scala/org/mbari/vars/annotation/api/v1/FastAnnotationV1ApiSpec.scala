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
import java.util.{ArrayList => JArrayList, List => JList}
import java.util.concurrent.TimeUnit
import java.util.UUID
import org.mbari.vars.annotation.api.WebApiStack
import org.mbari.vars.annotation.Constants
import org.mbari.vars.annotation.controllers.AnnotationController
import org.mbari.vars.annotation.controllers.TestEntityFactory
import org.mbari.vars.annotation.repository.ImagedMomentDAO
import org.mbari.vars.annotation.repository.jpa.AnnotationImpl
import org.mbari.vars.annotation.repository.jpa.ImagedMomentEntity
import org.mbari.vars.annotation.repository.jpa.JPADAOFactory
import org.mbari.vars.annotation.repository.jpa.TestDAOFactory
import org.mbari.vars.annotation.model.simple.QueryConstraints
import org.mbari.vars.annotation.model.simple.QueryConstraintsResponse
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters._

class FastAnnotationV1ApiSpec extends WebApiStack {

  private val jpaDaoFactory = daoFactory.asInstanceOf[JPADAOFactory]
  private val controller    = new AnnotationController(daoFactory)
  private val api           = new FastAnnotationV1Api(jpaDaoFactory)
  private val entityFactory = new TestEntityFactory(jpaDaoFactory)

  private type IMDAO = ImagedMomentDAO[ImagedMomentEntity]
  private val dao               = jpaDaoFactory.newImagedMomentDAO()
  private[this] val timeout     = Duration(2, TimeUnit.SECONDS)
  def run[R](fn: IMDAO => R): R = Await.result(dao.runTransaction(fn), timeout)

  addServlet(api, "/v1/fast")

  private val videoReferenceUuid = UUID.randomUUID()

  class ConcreteQCR extends QueryConstraintsResponse[JList[AnnotationImpl]]

  "FastAnnotationV1Api" should "set up correctly" in {

    val vriDao = jpaDaoFactory.newCachedVideoReferenceInfoDAO(dao)

    val im  = entityFactory.createImagedMoment(3, videoReferenceUuid, "FastTest")
    val imD = entityFactory.createImagedMoment(3, UUID.randomUUID(), "Dummy")
    val vri = vriDao.newPersistentObject()
    vri.videoReferenceUUID = videoReferenceUuid
    vri.missionContact = "Brian Schlining"
    vri.platformName = "Doc Ricketts"
    vri.missionId = "Doc Ricketts 0123"

    run(d => {
      d.create(im)
      d.create(imD)
      val vDao = jpaDaoFactory.newCachedVideoReferenceInfoDAO(d)
      vDao.create(vri)
    })

    val qc0   = QueryConstraints(missionContacts = List("Brian Schlining"))
    val json0 = Constants.GSON.toJson(qc0)
    post(
      "/v1/fast/",
      headers = Map("Content-Type" -> "application/json"),
      body = json0.getBytes(StandardCharsets.UTF_8)
    ) {
      status should be(200)
      val qcr = Constants
        .GSON_FOR_ANNOTATION
        .fromJson(body, classOf[ConcreteQCR])
      qcr.content.size should be(3)
      qcr
        .content
        .forEach(a => {
          // a.uuid should not be null
          a.videoReferenceUuid should be(videoReferenceUuid)
        })
    }

    val qc1   = QueryConstraints(missionId = Some(vri.missionId))
    val json1 = Constants.GSON.toJson(qc1)
    post(
      "/v1/fast/",
      headers = Map("Content-Type" -> "application/json"),
      body = json1.getBytes(StandardCharsets.UTF_8)
    ) {
      status should be(200)
      val qcr = Constants
        .GSON_FOR_ANNOTATION
        .fromJson(body, classOf[ConcreteQCR])
      qcr.content.size should be(3)
      qcr
        .content
        .forEach(a => {
          // a.uuid should not be null
          a.videoReferenceUuid should be(videoReferenceUuid)
        })
    }

    val qc2   = QueryConstraints(platformName = Some(vri.platformName))
    val json2 = Constants.GSON.toJson(qc2)
    post(
      "/v1/fast/",
      headers = Map("Content-Type" -> "application/json"),
      body = json2.getBytes(StandardCharsets.UTF_8)
    ) {
      status should be(200)
      val qcr = Constants
        .GSON_FOR_ANNOTATION
        .fromJson(body, classOf[ConcreteQCR])
      qcr.content.size should be(3)
      qcr
        .content
        .forEach(a => {
          // a.uuid should not be null
          a.videoReferenceUuid should be(videoReferenceUuid)
        })
    }

    val qc3   = QueryConstraints(concepts = List("FastTest"), missionId = Some(vri.missionId))
    val json3 = Constants.GSON.toJson(qc3)
    post(
      "/v1/fast/",
      headers = Map("Content-Type" -> "application/json"),
      body = json3.getBytes(StandardCharsets.UTF_8)
    ) {
      status should be(200)
      val qcr = Constants
        .GSON_FOR_ANNOTATION
        .fromJson(body, classOf[ConcreteQCR])
      qcr.content.size should be(3)
      qcr
        .content
        .forEach(a => {
          // a.uuid should not be null
          a.videoReferenceUuid should be(videoReferenceUuid)
        })
    }

  }

}
