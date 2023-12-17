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

package org.mbari.annosaurus.model.simple

import org.mbari.annosaurus.model.MutableObservation

import java.time.{Duration, Instant}
import java.util.UUID

/** @author
  *   Brian Schlining
  * @since 2016-07-11T15:06:00
  */
case class SimpleObservation(
    uuid: UUID,
    concept: String,
    duration: Duration,
    group: String,
    activity: String,
    observer: String,
    observationDate: Instant,
    assocations: Iterable[SimpleAssociation]
)

object SimpleObservation {

    def apply(obs: MutableObservation): SimpleObservation =
        new SimpleObservation(
            obs.uuid,
            obs.concept,
            obs.duration,
            obs.group,
            obs.activity,
            obs.observer,
            obs.observationDate,
            obs.associations.map(SimpleAssociation(_))
        )
}
