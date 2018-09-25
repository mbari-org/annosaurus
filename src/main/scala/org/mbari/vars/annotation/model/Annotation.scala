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

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vcr4j.time.Timecode

/**
 * @author Brian Schlining
 * @since 2017-09-20T15:58:00
 */
trait Annotation {

  var observationUuid: UUID
  var concept: String
  var observer: String
  var observationTimestamp: Instant
  var videoReferenceUuid: UUID
  var imagedMomentUuid: UUID
  var timecode: Timecode
  var elapsedTime: Duration
  var recordedTimestamp: Instant
  var duration: Duration
  var group: String
  var activity: String
  var associations: Seq[Association]
  var imageReferences: Seq[ImageReference]

}
