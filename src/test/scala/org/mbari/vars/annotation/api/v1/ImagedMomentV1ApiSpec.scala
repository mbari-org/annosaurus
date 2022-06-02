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
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.api.WebApiStack
import org.mbari.vars.annotation.controllers.{AnnotationController, ImagedMomentController, ImageController, AssociationController}
import org.mbari.vars.annotation.dao.jpa.ImagedMomentImpl
import org.mbari.vars.annotation.model.Annotation
import org.mbari.vars.annotation.model.simple.{WindowRequest, Count}

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
        controller.create(
          UUID.randomUUID(),
          "Foo",
          "brian",
          elapsedTime = Some(Duration.ofMillis(2000)),
          recordedDate = Some(Instant.now())
        ),
        SDuration(3000, TimeUnit.MILLISECONDS)
      )
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
    val windowRequest = WindowRequest(
      Seq(annotation.videoReferenceUuid),
      annotation.imagedMomentUuid,
      Duration.ofSeconds(10)
    )
    val json = gson.toJson(windowRequest)
    post(
      s"/v1/imagedmoments/windowrequest",
      body = json,
      headers = Map("Content-Type" -> "application/json")
    ) {

      status should be(200)
      val im = gson.fromJson(body, classOf[Array[ImagedMomentImpl]]).toList
      im.size should be > 0
    }
  }

  it should "count last updated imagedmoments between timestamps" in {
    get(s"/v1/imagedmoments/modified/count/$startTimestamp/${Instant.now()}") {
      status should be(200)
//      println(body)
    }
  }

  it should "count all imagedmoments" in {
    get("/v1/imagedmoments/count/all") {
      status should be(200)
      val count = gson.fromJson(body, classOf[Count])
      count.count.toInt should be > 0
    }
  }

  it should "update" in {
    put(
      s"/v1/imagedmoments/${annotation.imagedMomentUuid}",
      "timecode"            -> "01:23:45:12",
      "elapsed_time_millis" -> "22222"
    ) {
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

  it should "count and find with images" in {

    val imageController = new ImageController(daoFactory)
    
    // Create some images at the same recorded date (will map to same imaged moment)
    // Make sure they have different UUIDs
    val recordedDate = Some(Instant.now())
    val numImages = 10
    val videoReferenceUUID = UUID.randomUUID()
    val images = (1 to numImages).map { i =>
      Await.result(
        imageController.create(
          videoReferenceUUID,
          new URL(s"http://foo.bar/image-${i}.png"),
          recordedDate = recordedDate
        ),
        SDuration(3000, TimeUnit.MILLISECONDS)
      )
    }

    val targetImagedMomentUUID = images.head.imagedMomentUuid

    // Check imaged moment count = 1
    get("/v1/imagedmoments/count/images") {
      status should be(200)
      val count = gson.fromJson(body, classOf[Count])
      count.count.toInt should be(1)
    }

    // Find imaged moment, check imaged moment UUID and image URLs match
    get("/v1/imagedmoments/find/images") {
      status should be(200)
      val im = gson.fromJson(body, classOf[Array[ImagedMomentImpl]]).toList
      im.size should be > 0
      val i = im.head
      i.uuid should be(targetImagedMomentUUID)
      
      // Check # of images matches
      i.imageReferences.size should be(numImages)
    }

  }

  it should "count and find by link name" in {

    val linkName = "foo"
    val toConcept = "self"
    val linkValue = "bar"

    val annotation = {
      val annotationController = new AnnotationController(daoFactory)
      Await.result(
        annotationController.create(
          UUID.randomUUID(),  // video reference UUID
          "Bar",
          "kbarnard",
          elapsedTime = Some(Duration.ofMillis(42)),
          recordedDate = Some(Instant.now())
        ),
        SDuration(3000, TimeUnit.MILLISECONDS)
      )
    }

    val association = {
      val associationController = new AssociationController(daoFactory)
      Await.result(
        associationController.create(
          annotation.observationUuid,
          linkName,
          toConcept,
          linkValue,
          "text/plain"
        ),
        SDuration(3000, TimeUnit.MILLISECONDS)
      )
    }

    // Check count > 0 for given link name
    get(s"/v1/imagedmoments/count/linkname/${linkName}") {
      status should be(200)
      val count = gson.fromJson(body, classOf[Count])
      count.count.toInt should be > 0
    }

    // Find imaged moment, check UUID and link name match
    get(s"/v1/imagedmoments/find/linkname/${linkName}") {
      status should be(200)

      val im = gson.fromJson(body, classOf[Array[ImagedMomentImpl]]).toList
      im.size should be > 0
      
      val i = im.head
      i.uuid should be(annotation.imagedMomentUuid)

      // Pull out the association
      val o = i.observations.head
      val a = o.associations.head

      a.uuid should be(association.uuid)
      a.linkName should be(linkName)
      a.linkValue should be(linkValue)
    }

  }

}
