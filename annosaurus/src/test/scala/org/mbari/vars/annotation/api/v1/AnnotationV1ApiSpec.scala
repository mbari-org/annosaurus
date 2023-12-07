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
import org.mbari.vars.annotation.repository.jpa.AnnotationImpl
import org.mbari.vars.annotation.model.Annotation
import org.mbari.vars.annotation.model.simple.{
  ConcurrentRequest,
  ConcurrentRequestCount,
  MultiRequest
}
import org.mbari.vars.annotation.repository.jpa.entity.{AssociationEntity, ImageReferenceEntity}
import org.mbari.vcr4j.time.Timecode

import scala.jdk.CollectionConverters._

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

  addServlet(annotationV1Api, "/v1/annotations")

  var annotation: Annotation = _

  "AnnotationV1Api" should "create with timecode" in {
    post(
      "/v1/annotations",
      "video_reference_uuid" -> UUID.randomUUID().toString,
      "concept"              -> "Nanomia bijuga",
      "observer"             -> "brian",
      "elapsed_time_millis"  -> "12345"
    ) {
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
      "concept"              -> "Squid",
      "observer"             -> "brian",
      "recorded_timestamp"   -> "2017-01-18T22:01:03.41Z"
    ) {
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
      "concept"              -> "Shark",
      "observer"             -> "brian",
      "recorded_timestamp"   -> "2017-01-18T22:01:03.41Z"
    ) {
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
      "concept"         -> "Aegina",
      "duration_millis" -> "2500"
    ) {
      status should be(200)
      val a = gson.fromJson(body, classOf[AnnotationImpl])
      a.concept should be("Aegina")
      a.observer should be(annotation.observer)
      a.elapsedTime should be(annotation.elapsedTime)
      a.timecode should be(null)
      a.duration should be(Duration.ofMillis(2500))
    }
  }

  // it should "update recorded_timestamp" in {
  //   put(
  //     s"/v1/annotations/${annotation.observationUuid}",
  //     "recorded_timestamp"         -> "20190831T230708.510000Z",
  //   ) {
  //     status should be(200)
  //     val a = gson.fromJson(body, classOf[AnnotationImpl])
  //     a.concept should be("Aegina")
  //     a.observer should be(annotation.observer)
  //     a.elapsedTime should be(annotation.elapsedTime)
  //     a.timecode should be(null)
  //     a.duration should be(Duration.ofMillis(2500))
  //     a.recordedTimestamp should be(Instant.parse("2019-08-31T23:07:08.51Z"))
  //   }
  // }

  it should "update with same VideoReferenceUuid" in {
    put(
      s"/v1/annotations/${annotation.observationUuid}",
      "concept"              -> "Aegina",
      "duration_millis"      -> "2500",
      "video_reference_uuid" -> s"${annotation.videoReferenceUuid}"
    ) {
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
  val uuid0        = UUID.randomUUID()
  val uuid1        = UUID.randomUUID()
  val recordedDate = Some(Instant.now())
  val elapsedTime  = Some(Duration.ofSeconds(123))
  val annotations = Seq(
    AnnotationImpl(uuid0, "Nanomia bijuga", "brian", recordedDate = recordedDate),
    AnnotationImpl(uuid0, "bony-eared assfish", "brian", recordedDate = recordedDate),
    AnnotationImpl(uuid1, "Pandalus platyceros", "schlin", elapsedTime = elapsedTime),
    AnnotationImpl(
      uuid1,
      "Peobius",
      "stephalopod",
      elapsedTime = elapsedTime,
      timecode = Some(new Timecode("00:02:34:29", 29.97))
    ),
    AnnotationImpl(
      uuid1,
      "Peobius",
      "stephalopod",
      timecode = Some(new Timecode("00:02:34:29", 29.97))
    )
  )
  var persistedAnnotations: Seq[AnnotationImpl] = _

  it should "bulk create" in {
    val json = Constants.GSON_FOR_ANNOTATION.toJson(annotations.asJava)
    post(
      "/v1/annotations/bulk",
      headers = Map("Content-Type" -> "application/json"),
      body = json.getBytes(StandardCharsets.UTF_8)
    ) {
      status should be(200)
      persistedAnnotations = Constants
        .GSON_FOR_ANNOTATION
        .fromJson(body, classOf[Array[AnnotationImpl]])
        .toSeq
      persistedAnnotations.size should be(5)
    }
  }

  val anno0 = {
    val a0 =
      AnnotationImpl(UUID.randomUUID(), "Nanomia bijuga", "brian", recordedDate = recordedDate)
    val ir = ImageReferenceEntity(new URL("http://www.foo.bar/woot.png"), Option(1920), Option(1080))
    a0.imageReferences = Seq(ir)
    a0
  }

  it should "bulk create with an imagereference" in {
    val annos = Seq(anno0)
    val json  = Constants.GSON_FOR_ANNOTATION.toJson(annos.asJava)
//    print(json)
    post(
      "/v1/annotations/bulk",
      headers = Map("Content-Type" -> "application/json"),
      body = json.getBytes(StandardCharsets.UTF_8)
    ) {
      status should be(200)
      val pas0 = Constants
        .GSON_FOR_ANNOTATION
        .fromJson(body, classOf[Array[AnnotationImpl]])
        .toSeq
      pas0.size should be(1)
      pas0.head.imageReferences.size should be(1)
    }
  }

  val uuid2 = UUID.randomUUID()
  val anno1 = {
    val a0 = AnnotationImpl(uuid2, "Nanomia bijuga", "brian", recordedDate = recordedDate)
    val as = AssociationEntity("linkname", "toconcept", "linkvalue")
    val ir =
      ImageReferenceEntity(new URL("http://www.foo.bar/wootty.png"), Option(1920), Option(1080))
    a0.imageReferences = Seq(ir)
    a0.associations = Seq(as)
    a0
  }

  it should "bulk create with an imagereference AND association" in {
    val annos = Seq(anno1)
    val json  = Constants.GSON_FOR_ANNOTATION.toJson(annos.asJava)
    post(
      "/v1/annotations/bulk",
      headers = Map("Content-Type" -> "application/json"),
      body = json.getBytes(StandardCharsets.UTF_8)
    ) {
      status should be(200)
      val pas0 = Constants
        .GSON_FOR_ANNOTATION
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
      body = json.getBytes(StandardCharsets.UTF_8)
    ) {
      status should be(200)
      val updatedAnnotations = Constants
        .GSON_FOR_ANNOTATION
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

  // Create annotations for concurrent requests
  val startTimestamp = recordedDate.get
  val cUuid0         = UUID.randomUUID()
  val cUuid1         = UUID.randomUUID()
  val concurrentAnnotations = Seq(
    AnnotationImpl(cUuid0, "Nanomia bijuga", "brian", recordedDate = Some(startTimestamp)),
    AnnotationImpl(cUuid0, "bony-eared assfish", "brian", recordedDate = Some(startTimestamp)),
    AnnotationImpl(
      cUuid1,
      "Pandalus platyceros",
      "schlin",
      recordedDate = Some(startTimestamp.plus(Duration.ofSeconds(1)))
    ),
    AnnotationImpl(
      cUuid1,
      "Peobius",
      "stephalopod",
      elapsedTime = elapsedTime,
      recordedDate = Some(startTimestamp.plus(Duration.ofSeconds(2)))
    ),
    AnnotationImpl(
      cUuid1,
      "Peobius",
      "stephalopod",
      recordedDate = Some(startTimestamp.plus(Duration.ofSeconds(100)))
    )
  )

  it should "count by concurrent request" in {

    // Create the concurrent annotations
    val jsonC = Constants.GSON.toJson(concurrentAnnotations.asJava)
    post(
      "/v1/annotations/bulk",
      headers = Map("Content-Type" -> "application/json"),
      body = jsonC.getBytes(StandardCharsets.UTF_8)
    ) {
      status should be(200)
    }

    val start = recordedDate.get
    val end   = start.plus(Duration.ofSeconds(5))
    val cr    = ConcurrentRequest(start, end, Seq(cUuid0, cUuid1))
    val json  = Constants.GSON.toJson(cr)
    post(
      "/v1/annotations/concurrent/count",
      headers = Map("Content-Type" -> "application/json"),
      body = json.getBytes(StandardCharsets.UTF_8)
    ) {

      status should be(200)
      val count = Constants.GSON.fromJson(body, classOf[ConcurrentRequestCount])
      // ONe of the concurrent annotations is outside the date range. This was deliberate
      // So we expect one less annotation
      count.count should be(concurrentAnnotations.size - 1)
    }

  }

  it should "find by concurrent request" in {
    val start = recordedDate.get
    val end   = start.plus(Duration.ofSeconds(5))
    val cr    = ConcurrentRequest(start, end, Seq(cUuid0, cUuid1))
    val json  = Constants.GSON.toJson(cr)

    post(
      "/v1/annotations/concurrent",
      headers = Map("Content-Type" -> "application/json"),
      body = json.getBytes(StandardCharsets.UTF_8)
    ) {

      status should be(200)

      val concurrentAnnos = Constants
        .GSON
        .fromJson(body, classOf[Array[AnnotationImpl]])
        .toSeq

      concurrentAnnos.size should be(4)
//      println(concurrentAnnos)

    }
  }

  it should "find by multi request" in {
    val r    = MultiRequest(Seq(cUuid0, cUuid1))
    val json = Constants.GSON.toJson(r)

    post(
      "/v1/annotations/multi",
      headers = Map("Content-Type" -> "application/json"),
      body = json.getBytes(StandardCharsets.UTF_8)
    ) {

      status should be(200)

      val annos = Constants
        .GSON
        .fromJson(body, classOf[Array[AnnotationImpl]])
        .toSeq

      annos.size should be(5)
//      println(annos)

    }
  }

}
