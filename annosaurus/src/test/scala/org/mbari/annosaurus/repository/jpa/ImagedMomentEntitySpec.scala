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

package org.mbari.annosaurus.repository.jpa

import org.mbari.annosaurus.Constants
import org.mbari.annosaurus.repository.jpa.entity.{AssociationEntity, ImagedMomentEntity, ObservationEntity}
import org.mbari.vcr4j.time.{FrameRates, Timecode}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{Duration, Instant}
import java.util.UUID

class ImagedMomentEntitySpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  "ImagedMomentImpl" should "round trip to/from annotations" in {
    val now                = Instant.now()
    val videoReferenceUuid = UUID.randomUUID()

    val imagedMoment0 = ImagedMomentEntity(Some(videoReferenceUuid), recordedDate = Some(now))
    val observation0  = ObservationEntity("zero")
    imagedMoment0.addObservation(observation0)

    val imagedMoment1 = ImagedMomentEntity(
      Some(videoReferenceUuid),
      recordedDate = Some(now.plusSeconds(60)),
      elapsedTime = Some(Duration.ofMinutes(1))
    )
    val observation1 = ObservationEntity("one", group = Some("ROV"))
    imagedMoment1.addObservation(observation1)

    val imagedMoment2 =
      ImagedMomentEntity(Some(videoReferenceUuid), elapsedTime = Some(Duration.ofMinutes(2)))
    val observation2 = ObservationEntity("two", activity = Some("transect"))
    observation2.uuid = UUID.randomUUID()
    imagedMoment2.addObservation(observation2)
    val association2 = AssociationEntity("foo", linkValue = Some("bar"))
    observation2.addAssociation(association2)

    val imagedMoment3 = ImagedMomentEntity(
      Some(videoReferenceUuid),
      timecode = Some(new Timecode("01:23:45:21", FrameRates.NTSC))
    )
    val observation3 = ObservationEntity("three", duration = Some(Duration.ofSeconds(10)))
    val observation4 = ObservationEntity("four", activity = Some("descent"))
    imagedMoment3.addObservation(observation3)
    imagedMoment3.addObservation(observation4)

    val xs = Seq(imagedMoment0, imagedMoment1, imagedMoment2, imagedMoment3)

    val annos = xs.flatMap(o => MutableAnnotationImpl(o))
    annos.size should be(5)
//    println(annos)
    val ims = ImagedMomentEntity(annos)

//    print(ims)
    ims.size should be(4)
    val obs = ims.flatMap(i => i.observations)
    obs.size should be(5)
    obs.find(o => o.concept == "two") match {
      case None => fail()
      case Some(o) =>
        o.uuid should not be null
        o.associations.size should be(1)
    }

  }

  it should "round trip an annotation without a video index" in {
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

    val annotations   = Constants.GSON.fromJson(json, classOf[Array[MutableAnnotationImpl]])
    val imagedMoments = ImagedMomentEntity(annotations)
    imagedMoments should have size (1)
    val im = imagedMoments.head
    im.videoReferenceUUID should be(UUID.fromString("a9f75399-9bc5-4ff3-934c-0bd72ba4dccb"))
    im.imageReferences should have size (0)
    im.observations should have size (1)
    val obs = im.observations.head
    obs.concept should be("test")
    obs.group should be("ROV")
    obs.activity should be("descend")

  }

}
