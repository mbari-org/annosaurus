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

import java.time.{Duration, Instant}
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.api.WebApiStack
import org.mbari.vars.annotation.controllers.{AnnotationController, ImagedMomentController}
import org.mbari.vars.annotation.dao.jpa.ImagedMomentImpl
import org.mbari.vars.annotation.model.Annotation
import org.mbari.vars.annotation.model.simple.WindowRequest

import scala.concurrent.Await
import scala.concurrent.duration.{Duration => SDuration}

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-09-08T14:24:00
 */
class ImagedMomentV1ApiSpec extends WebApiStack {

  private[this] val startTimestamp = Instant.now()

  private[this] val imagedMomentV1Api = {
    val controller = new ImagedMomentController(daoFactory)
    new ImagedMomentV1Api(controller)
  }

  addServlet(imagedMomentV1Api, "/v1/imagedmoments")

  "ImageMomentV1Api" should "return an empty JSON array when the database is empty" in {
    get("/v1/imagedmoments") {
      status should be(200)
      body should equal("[]")
    }
  }

  var annotation: Annotation = _

  it should "find by uuid" in {

    annotation = {
      val controller = new AnnotationController(daoFactory)
      Await.result(
        controller.create(UUID.randomUUID(), "Foo", "brian",
          elapsedTime = Some(Duration.ofMillis(2000)), recordedDate = Some(Instant.now())),
        SDuration(3000, TimeUnit.MILLISECONDS))
    }

    get(s"/v1/imagedmoments/${annotation.imagedMomentUuid}") {
      status should be(200)
      val im = gson.fromJson(body, classOf[ImagedMomentImpl])
      im.elapsedTime should be(annotation.elapsedTime)
      im.videoReferenceUUID should be(annotation.videoReferenceUuid)
      im.uuid should be(annotation.imagedMomentUuid)
    }

  }

  it should "find all videoreferences" in {
    get("/v1/imagedmoments/videoreference") {
      status should be(200)
      val uuids = gson.fromJson(body, classOf[Array[UUID]]).toList
      uuids should contain(annotation.videoReferenceUuid)
    }
  }

  it should "find by videoreference" in {
    get(s"/v1/imagedmoments/videoreference/${annotation.videoReferenceUuid}") {
      status should be(200)
      val im = gson.fromJson(body, classOf[Array[ImagedMomentImpl]]).toList
      im.size should be(1)
      val i = im.head
      i.videoReferenceUUID should be(annotation.videoReferenceUuid)
      i.uuid should be(annotation.imagedMomentUuid)
    }
  }

  it should "find last updated imagedmoments between timestamps" in {
    get(s"/v1/imagedmoments/modified/$startTimestamp/${Instant.now()}") {
      status should be(200)
      val im = gson.fromJson(body, classOf[Array[ImagedMomentImpl]]).toList
      im.size should be > 0
    }
  }

  it should "find by window reference" in {
    val windowRequest = WindowRequest(Seq(annotation.videoReferenceUuid),
      annotation.imagedMomentUuid,
      Duration.ofSeconds(10))
    val json = gson.toJson(windowRequest)
    post(s"/v1/imagedmoments/windowrequest",
      body = json,
      headers = Map("Content-Type" -> "application/json")) {

      status should be (200)
      val im = gson.fromJson(body, classOf[Array[ImagedMomentImpl]]).toList
      im.size should be > 0
    }
  }

  it should "count last updated imagedmoments between timestamps" in {
    get(s"/v1/imagedmoments/modified/count/$startTimestamp/${Instant.now()}") {
      status should be(200)
      println(body)
    }
  }

  it should "update" in {
    put(
      s"/v1/imagedmoments/${annotation.imagedMomentUuid}",
      "timecode" -> "01:23:45:12",
      "elapsed_time_millis" -> "22222") {
        status should be(200)
        val im = gson.fromJson(body, classOf[ImagedMomentImpl])
        im.elapsedTime should be(Duration.ofMillis(22222))
        im.timecode.toString should be("01:23:45:12")
      }
  }

  it should "delete" in {
    delete(s"/v1/imagedmoments/${annotation.imagedMomentUuid}") {
      status should be(204)
    }
  }

}
