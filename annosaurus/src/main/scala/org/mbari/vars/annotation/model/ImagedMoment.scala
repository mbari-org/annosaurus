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

package org.mbari.vars.annotation.model

import java.time.{Duration, Instant}
import java.util.UUID

import org.mbari.vars.annotation.PersistentObject
import org.mbari.vcr4j.time.Timecode

/**
  * An [[ImagedMoment]] is an index back to a particular video-file. The index can
  * be one or more of the following:
  * 1. timecode (if the video has a time-code track)
  * 2. elapsedTime - The elapsed time from the start or the video until the frame containing the obseration
  * 3. recordedDate - The moment in time when the frame of video was recorded.
  *
  * @author Brian Schlining
  * @since 2016-06-15T16:54:00
  */
trait ImagedMoment extends PersistentObject {

  var uuid: UUID

  /**
    * videoReferenceUUID is a UUID that could point to a video-reference in
    * the `video-asset-manager`. In turn, that could be used to reference another
    * version of the same video segment. Alternately, this could also point at
    * __any__ UUID in any MAM system. Exactly how it is used is not hard-coded.
    */
  var videoReferenceUUID: UUID

  /**
    *
    */
  var timecode: Timecode
  var recordedDate: Instant
  var elapsedTime: Duration
  def lastUpdated: Option[Instant]
  def addObservation(observation: Observation): Unit
  def removeObservation(observation: Observation): Unit
  def observations: Iterable[Observation]
  def addImageReference(imageReference: ImageReference): Unit
  def removeImageReference(imageReference: ImageReference): Unit
  def imageReferences: Iterable[ImageReference]
  var ancillaryDatum: CachedAncillaryDatum
  def isEmpty: Boolean = observations.isEmpty && imageReferences.isEmpty

}
