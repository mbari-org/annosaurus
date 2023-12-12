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

package org.mbari.annosaurus.model

import org.mbari.annosaurus.PersistentObject
import java.time.{Duration, Instant}
import java.util.UUID

/** @author
  *   Brian Schlining
  * @since 2016-06-15T16:53:00
  */
trait MutableObservation extends PersistentObject {

    var uuid: UUID
    var imagedMoment: ImagedMoment

    /** A Concept is the term used for the annotation. For science purposes, this is typically the
      * name of the object, such as a species name
      */
    var concept: String

    /** The duration can be used to capture how long an observation existed.
      */
    var duration: Duration

    /** At MBARI, we have set-up mulitple databases for different annotations. For example, one
      * database for ROV video tapes, another for video files, and yet another images. Going
      * forward, we could store all of these in the same database, but organize them using this
      * group tag.
      */
    var group: String

    /** At MBARI, we track what the camera is doing when the observation is made. For example,
      * transect, descent, ascent, etc.
      */
    var activity: String

    /** An ID for the person or software that made the observation
      */
    var observer: String

    /** The date that the observation was made
      */
    var observationDate: Instant
    def lastUpdated: Option[Instant]
    def addAssociation(association: Association): Unit
    def removeAssociation(association: Association): Unit
    def associations: Iterable[Association]
}
