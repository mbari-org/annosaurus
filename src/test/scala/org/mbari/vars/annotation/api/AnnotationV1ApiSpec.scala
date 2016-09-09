package org.mbari.vars.annotation.api

import java.time.Duration
import java.util
import java.util.{ UUID, List => JList }

import org.mbari.vars.annotation.Constants
import org.mbari.vars.annotation.controllers.{ AnnotationController, ImagedMomentController }
import org.mbari.vars.annotation.model.simple.Annotation

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-09-08T10:47:00
 */
class AnnotationV1ApiSpec extends WebApiStack {

  private[this] val annotationV1Api = {
    val controller = new AnnotationController(daoFactory)
    new AnnotationV1Api(controller)
  }

  protected[this] override val gson = Constants.GSON_FOR_ANNOTATION

  addServlet(annotationV1Api, "/v1/annotations")

  var annotation: Annotation = _

  "AnnotationV1API" should "create" in {
    post(
      "/v1/annotations",
      "video_reference_uuid" -> UUID.randomUUID().toString,
      "concept" -> "Nanomia bijuga",
      "observer" -> "brian",
      "elapsed_time_millis" -> "12345"
    ) {
        status should be(200)
        annotation = gson.fromJson(body, classOf[Annotation])
        annotation.concept should be("Nanomia bijuga")
        annotation.observer should be("brian")
        annotation.elapsedTime should be(Duration.ofMillis(12345))
      }
  }

  it should "get by observation uuid" in {
    get(s"/v1/annotations/${annotation.observationUuid}") {
      status should be(200)
      val a = gson.fromJson(body, classOf[Annotation])
      a.concept should be(annotation.concept)
      a.observer should be(annotation.observer)
      a.elapsedTime should be(annotation.elapsedTime)
      a.videoReferenceUuid should be(annotation.videoReferenceUuid)
      a.timecode should be(null)
      a.duration should be(null)
    }
  }

  it should "get by videoreference uuid" in {
    get(s"/v1/annotations/videoreference/${annotation.videoReferenceUuid}") {
      status should be(200)
      val as = gson.fromJson(body, classOf[Array[Annotation]])
      as.size should be(1)
      val a = as(0)
      a.concept should be(annotation.concept)
      a.observer should be(annotation.observer)
      a.elapsedTime should be(annotation.elapsedTime)
      a.timecode should be(null)
    }
  }

  it should "update" in {
    put(
      s"/v1/annotations/${annotation.observationUuid}",
      "concept" -> "Aegina",
      "duration_millis" -> "2500"
    ) {
        status should be(200)
        val a = gson.fromJson(body, classOf[Annotation])
        a.concept should be("Aegina")
        a.observer should be(annotation.observer)
        a.elapsedTime should be(annotation.elapsedTime)
        a.timecode should be(null)
        a.duration should be(Duration.ofMillis(2500))
      }
  }

}
