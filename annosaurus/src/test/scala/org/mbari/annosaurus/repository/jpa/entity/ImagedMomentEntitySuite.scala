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

package org.mbari.annosaurus.repository.jpa.entity

import org.mbari.annosaurus.domain.Annotation
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.vcr4j.time.{FrameRates, Timecode}

import java.time.{Duration, Instant}
import java.util.UUID

import scala.jdk.CollectionConverters.*

class ImagedMomentEntitySuite extends munit.FunSuite {

    test("round trip to/from annotations") {
        val now = Instant.now()
        val videoReferenceUuid = UUID.randomUUID()

        val imagedMoment0 = ImagedMomentEntity(videoReferenceUuid, now, null, null)
        imagedMoment0.uuid = UUID.randomUUID()
        val observation0 = ObservationEntity("zero", "brian")
        imagedMoment0.addObservation(observation0)

        val imagedMoment1 = ImagedMomentEntity(
            videoReferenceUuid,
            now.plusSeconds(60),
            null,
            Duration.ofMinutes(1)
        )
        imagedMoment1.uuid = UUID.randomUUID()
        val observation1 = ObservationEntity("one", "brian")
        observation1.setGroup("ROV")
        imagedMoment1.addObservation(observation1)

        val imagedMoment2 =
            ImagedMomentEntity(videoReferenceUuid, null, null, Duration.ofMinutes(2))
        imagedMoment2.uuid = UUID.randomUUID()
        val observation2 = ObservationEntity("two", "kyra")
        observation2.setActivity("descent")
        observation2.setUuid(UUID.randomUUID())
        imagedMoment2.addObservation(observation2)
        val association2 = AssociationEntity("foo", "bar", "baz")
        observation2.addAssociation(association2)

        val imagedMoment3 = ImagedMomentEntity(
            videoReferenceUuid,
            null,
            new Timecode("01:23:45:21", FrameRates.NTSC),
            null
        )
        imagedMoment3.uuid = UUID.randomUUID()
        val observation3 = ObservationEntity("three", "schlin")
        observation3.setDuration(Duration.ofSeconds(10))
        val observation4 = ObservationEntity("four", "brian")
        observation4.setActivity("descent")
        imagedMoment3.addObservation(observation3)
        imagedMoment3.addObservation(observation4)

        val xs = Seq(imagedMoment0, imagedMoment1, imagedMoment2, imagedMoment3)

        val annos = xs.flatMap(o => Annotation.fromImagedMoment(o))
        assertEquals(annos.size, 5)
        val ims = Annotation.toEntities(annos)

        assertEquals(ims.size, 4)
        val obs = ims.flatMap(i => i.getObservations().asScala)
        assertEquals(obs.size, 5)
        obs.find(o => o.getConcept() == "two") match {
            case None => fail("Should have found observation with concept 'two'")
            case Some(o) =>
                assert(o.uuid != null)
                assertEquals(o.associations.size(), 1)
        }
    }

    test("round trip an annotation without a video index") {
        val json =
            """[
              |  {
              |    "concept": "test",
              |    "observer": "brian",
              |    "observation_timestamp": "2019-10-31T21:00:20.131533Z",
              |    "video_reference_uuid": "a9f75399-9bc5-4ff3-934c-0bd72ba4dccb",
              |    "imaged_moment_uuid": "c72cb4d4-b4a5-41e1-d965-4c9f16c29f1e",
              |    "recorded_timestamp": "2019-10-31T21:00:05.275Z",
              |    "group": "ROV",
              |    "activity": "descend",
              |    "associations": [],
              |    "image_references": []
              |  }
              |]""".stripMargin

        val annotations = json
            .reify[Seq[Annotation]]
            .getOrElse(throw new RuntimeException("Failed to parse json"))
        val imagedMoments = Annotation.toEntities(annotations)
        assertEquals(imagedMoments.size, 1)
        val im = imagedMoments.head
        assertEquals(im.getVideoReferenceUuid, UUID.fromString("a9f75399-9bc5-4ff3-934c-0bd72ba4dccb"))
        assertEquals(im.imageReferences.size(), 0)
        assertEquals(im.observations.size, 1)
        val obs = im.getObservations().asScala.head
        assertEquals(obs.concept, "test")
        assertEquals(obs.group, "ROV")
        assertEquals(obs.activity, "descend")
    }
}
