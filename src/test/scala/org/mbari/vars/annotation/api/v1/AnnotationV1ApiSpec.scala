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

import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.{Duration, Instant}
import java.util.UUID

import org.mbari.vars.annotation.Constants
import org.mbari.vars.annotation.api.WebApiStack
import org.mbari.vars.annotation.controllers.AnnotationController
import org.mbari.vars.annotation.dao.jpa.{AnnotationImpl, AssociationImpl, ImageReferenceImpl}
import org.mbari.vars.annotation.model.Annotation
import org.mbari.vcr4j.time.Timecode

import scala.collection.JavaConverters._

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

  "AnnotationV1Api" should "create with timecode" in {
    post(
      "/v1/annotations",
      "video_reference_uuid" -> UUID.randomUUID().toString,
      "concept" -> "Nanomia bijuga",
      "observer" -> "brian",
      "elapsed_time_millis" -> "12345") {
        status should be(200)
        annotation = gson.fromJson(body, classOf[AnnotationImpl])
        annotation.concept should be("Nanomia bijuga")
        annotation.observer should be("brian")
        annotation.elapsedTime should be(Duration.ofMillis(12345))
      }
  }

  it should "create with recorded timestamp" in {
    post(
      "/v1/annotations",
      "video_reference_uuid" -> UUID.randomUUID().toString,
      "concept" -> "Squid",
      "observer" -> "brian",
      "recorded_timestamp" -> "2017-01-18T22:01:03.41Z") {
        status should be(200)
        val a = gson.fromJson(body, classOf[AnnotationImpl])
        a.concept should be("Squid")
        a.observer should be("brian")
        a.recordedTimestamp should be(Instant.parse("2017-01-18T22:01:03.41Z"))
      }
  }

  it should "create with existing recorded timestamp" in {
    post(
      "/v1/annotations",
      "video_reference_uuid" -> UUID.randomUUID().toString,
      "concept" -> "Shark",
      "observer" -> "brian",
      "recorded_timestamp" -> "2017-01-18T22:01:03.41Z") {
        status should be(200)
        val a = gson.fromJson(body, classOf[AnnotationImpl])
        a.concept should be("Shark")
        a.observer should be("brian")
        a.recordedTimestamp should be(Instant.parse("2017-01-18T22:01:03.41Z"))
      }
  }

  it should "get by observation uuid" in {
    get(s"/v1/annotations/${annotation.observationUuid}") {
      status should be(200)
      val a = gson.fromJson(body, classOf[AnnotationImpl])
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
      val as = gson.fromJson(body, classOf[Array[AnnotationImpl]])
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
      "duration_millis" -> "2500") {
        status should be(200)
        val a = gson.fromJson(body, classOf[AnnotationImpl])
        a.concept should be("Aegina")
        a.observer should be(annotation.observer)
        a.elapsedTime should be(annotation.elapsedTime)
        a.timecode should be(null)
        a.duration should be(Duration.ofMillis(2500))
      }
  }

  it should "update with same VideoReferenceUuid" in {
    put(
      s"/v1/annotations/${annotation.observationUuid}",
      "concept" -> "Aegina",
      "duration_millis" -> "2500",
      "video_reference_uuid" -> s"${annotation.videoReferenceUuid}") {
        status should be(200)
        val a = gson.fromJson(body, classOf[AnnotationImpl])
        a.concept should be("Aegina")
        a.observer should be(annotation.observer)
        a.elapsedTime should be(annotation.elapsedTime)
        a.timecode should be(null)
        a.duration should be(Duration.ofMillis(2500))
      }
  }

  // Set up for bulk methods
  val uuid0 = UUID.randomUUID()
  val uuid1 = UUID.randomUUID()
  val recordedDate = Some(Instant.now())
  val elapsedTime = Some(Duration.ofSeconds(123))
  val annotations = Seq(
    AnnotationImpl(uuid0, "Nanomia bijuga", "brian", recordedDate = recordedDate),
    AnnotationImpl(uuid0, "bony-eared assfish", "brian", recordedDate = recordedDate),
    AnnotationImpl(uuid1, "Pandalus platyceros", "schlin", elapsedTime = elapsedTime),
    AnnotationImpl(uuid1, "Peobius", "stephalopod", elapsedTime = elapsedTime, timecode = Some(new Timecode("00:02:34:29", 29.97))),
    AnnotationImpl(uuid1, "Peobius", "stephalopod", timecode = Some(new Timecode("00:02:34:29", 29.97))))
  var persistedAnnotations: Seq[AnnotationImpl] = _

  it should "bulk create" in {
    val json = Constants.GSON_FOR_ANNOTATION.toJson(annotations.asJava)
    post(
      "/v1/annotations/bulk",
      headers = Map("Content-Type" -> "application/json"),
      body = json.getBytes(StandardCharsets.UTF_8)) {
        status should be(200)
        persistedAnnotations = Constants.GSON_FOR_ANNOTATION
          .fromJson(body, classOf[Array[AnnotationImpl]])
          .toSeq
        persistedAnnotations.size should be(5)
      }
  }

  val anno0 = {
    val a0 = AnnotationImpl(uuid0, "Nanomia bijuga", "brian", recordedDate = recordedDate)
    val ir = ImageReferenceImpl(new URL("http://www.foo.bar/woot.png"), Option(1920), Option(1080))
    a0.imageReferences = Seq(ir)
    a0
  }

  it should "bulk create with an imagereference" in {
    val annos = Seq(anno0)
    val json = Constants.GSON_FOR_ANNOTATION.toJson(annos.asJava)
    post(
      "/v1/annotations/bulk",
      headers = Map("Content-Type" -> "application/json"),
      body = json.getBytes(StandardCharsets.UTF_8)) {
        status should be(200)
        var pas0 = Constants.GSON_FOR_ANNOTATION
          .fromJson(body, classOf[Array[AnnotationImpl]])
          .toSeq
        pas0.size should be(1)
        pas0.head.imageReferences.size should be(1)
      }
  }

  val uuid2 = UUID.randomUUID()
  val anno1 = {
    val a0 = AnnotationImpl(uuid2, "Nanomia bijuga", "brian", recordedDate = recordedDate)
    val as = AssociationImpl("linkname", "toconcept", "linkvalue")
    val ir = ImageReferenceImpl(new URL("http://www.foo.bar/wootty.png"), Option(1920), Option(1080))
    a0.imageReferences = Seq(ir)
    a0.associations = Seq(as)
    a0
  }

  it should "bulk create with an imagereference AND association" in {
    val annos = Seq(anno1)
    val json = Constants.GSON_FOR_ANNOTATION.toJson(annos.asJava)
    post(
      "/v1/annotations/bulk",
      headers = Map("Content-Type" -> "application/json"),
      body = json.getBytes(StandardCharsets.UTF_8)) {
        status should be(200)
        var pas0 = Constants.GSON_FOR_ANNOTATION
          .fromJson(body, classOf[Array[AnnotationImpl]])
          .toSeq
        pas0.size should be(1)
        pas0.head.imageReferences.size should be(1)
        pas0.head.associations.size should be(1)
      }
  }

  it should "bulk update" in {
    val uuid2 = UUID.randomUUID()
    persistedAnnotations.foreach(a => {
      a.videoReferenceUuid = uuid2
      a.observer = "carolina"
    })
    val json = Constants.GSON_FOR_ANNOTATION.toJson(persistedAnnotations.asJava)
    put(
      "/v1/annotations/bulk",
      headers = Map("Content-Type" -> "application/json"),
      body = json.getBytes(StandardCharsets.UTF_8)) {
        status should be(200)
        val updatedAnnotations = Constants.GSON_FOR_ANNOTATION
          .fromJson(body, classOf[Array[AnnotationImpl]])
          .toSeq
        updatedAnnotations.size should be(5)
        //println(body)
        for (a <- updatedAnnotations) {
          a.videoReferenceUuid should be(uuid2)
          a.observer should be("carolina")
        }
      }
  }

}
