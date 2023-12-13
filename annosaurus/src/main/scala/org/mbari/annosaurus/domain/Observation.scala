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

package org.mbari.annosaurus.domain

import java.time.{Duration, Instant}
import java.util.UUID

final case class Observation(
    concept: String,
    durationMillis: Option[Long] = None,
    group: Option[String] = None,
    activity: Option[String] = None,
    observer: Option[String] = None,
    observationTimestamp: Option[Instant] = None,
    associations: Seq[Association] = Nil,
    uuid: Option[UUID] = None,
    lastUpdated: Option[Instant] = None
    
) {
    def toSnakeCase: ObservationSC =
        ObservationSC(
            concept,
            durationMillis,
            group,
            activity,
            observer,
            observationTimestamp,
            associations.map(_.toSnakeCase),
            uuid,
            lastUpdated
        )
        
    lazy val duration: Option[Duration] = durationMillis.map(Duration.ofMillis)
}

final case class ObservationSC(
    concept: String,
    duration_millis: Option[Long] = None,
    group: Option[String] = None,
    activity: Option[String] = None,
    observer: Option[String] = None,
    observation_timestamp: Option[Instant] = None,
    associations: Seq[AssociationSC] = Nil,
    uuid: Option[UUID] = None,
    last_updated_time: Option[Instant] = None,
) {
    def toCamelCase: Observation =
        Observation(
            concept,
            duration_millis,
            group,
            activity,
            observer,
            observation_timestamp,
            associations.map(_.toCamelCase),
            uuid,
            last_updated_time
        )
}
