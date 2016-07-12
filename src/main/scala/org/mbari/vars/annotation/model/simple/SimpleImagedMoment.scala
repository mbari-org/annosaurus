package org.mbari.vars.annotation.model.simple

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.model.ImagedMoment
import org.mbari.vcr4j.time.Timecode

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-07-11T14:58:00
 */
case class SimpleImagedMoment(
  uuid: UUID,
  videoReferenceUuid: UUID,
  timecode: Timecode,
  elapsedTime: Duration,
  recordedDate: Instant,
  observations: Iterable[SimpleObservation],
  imageReferences: Iterable[SimpleImageReference],
  ancillaryDatum: SimpleAncillaryDatum
)

object SimpleImagedMoment {

  def apply(imagedMoment: ImagedMoment): SimpleImagedMoment =
    new SimpleImagedMoment(imagedMoment.uuid, imagedMoment.videoReferenceUUID,
      imagedMoment.timecode, imagedMoment.elapsedTime, imagedMoment.recordedDate,
      imagedMoment.observations.map(SimpleObservation(_)),
      imagedMoment.imageReferences.map(SimpleImageReference(_)),
      SimpleAncillaryDatum(imagedMoment.ancillaryDatum))
}

