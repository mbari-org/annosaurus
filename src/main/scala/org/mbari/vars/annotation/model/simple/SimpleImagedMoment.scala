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

