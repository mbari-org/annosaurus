package org.mbari.vars.annotation.dao.jpa

import java.time.{Duration, Instant}
import java.util.UUID

import org.mbari.vcr4j.time.{FrameRates, Timecode}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class ImagedMomentImplSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  "ImagedMomentImpl" should "round trip to/from annotations" in {
    val now  = Instant.now()
    val videoReferenceUuid = UUID.randomUUID()

    val imagedMoment0 = ImagedMomentImpl(Some(videoReferenceUuid), recordedDate = Some(now))
    val observation0 =  ObservationImpl("zero")
    imagedMoment0.addObservation(observation0)

    val imagedMoment1 = ImagedMomentImpl(Some(videoReferenceUuid), recordedDate = Some(now.plusSeconds(60)), elapsedTime = Some(Duration.ofMinutes(1)))
    val observation1 = ObservationImpl("one", group  = Some("ROV"))
    imagedMoment1.addObservation(observation1)

    val imagedMoment2 = ImagedMomentImpl(Some(videoReferenceUuid), elapsedTime = Some(Duration.ofMinutes(2)))
    val observation2 = ObservationImpl("two", activity = Some("transect"))
    observation2.uuid = UUID.randomUUID()
    imagedMoment2.addObservation(observation2)
    val association2 = AssociationImpl("foo", linkValue = Some("bar"))
    observation2.addAssociation(association2)

    val imagedMoment3 = ImagedMomentImpl(Some(videoReferenceUuid), timecode = Some(new Timecode("01:23:45:21",  FrameRates.NTSC)))
    val observation3 = ObservationImpl("three", duration = Some(Duration.ofSeconds(10)))
    val observation4 =  ObservationImpl("four",  activity = Some("descent"))
    imagedMoment3.addObservation(observation3)
    imagedMoment3.addObservation(observation4)

    val xs = Seq(imagedMoment0, imagedMoment1, imagedMoment2, imagedMoment3)

    val annos = xs.flatMap(o => AnnotationImpl(o))
    annos.size should be (5)
    println(annos)
    val ims = ImagedMomentImpl(annos)

    print(ims)
    ims.size should be (4)
    val obs = ims.flatMap(i => i.observations)
    obs.size should be (5)
    obs.find(o => o.concept == "two") match {
      case None => fail()
      case Some(o) =>
        o.uuid should not be null
        o.associations.size should be (1)
    }

  }

}
