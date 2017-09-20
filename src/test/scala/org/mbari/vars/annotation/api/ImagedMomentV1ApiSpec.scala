package org.mbari.vars.annotation.api

import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.mbari.vars.annotation.controllers.{ AnnotationController, ImagedMomentController }
import org.mbari.vars.annotation.dao.jpa.{ AnnotationImpl, ImagedMomentImpl }
import org.mbari.vars.annotation.model.{ Annotation, ImagedMoment }
import org.mbari.vcr4j.time.Timecode

import scala.concurrent.Await
import scala.concurrent.duration.{ Duration => SDuration }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-09-08T14:24:00
 */
class ImagedMomentV1ApiSpec extends WebApiStack {

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
          elapsedTime = Some(Duration.ofMillis(2000))),
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

  it should "update" in {
    put(
      s"/v1/imagedmoments/${annotation.imagedMomentUuid}",
      "timecode" -> "01:23:45:12",
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

}
