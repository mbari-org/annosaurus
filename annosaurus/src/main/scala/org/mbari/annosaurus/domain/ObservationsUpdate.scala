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

import java.util.UUID

/**
 * Update multiple observations at once
 * @param observationUuids The UUIDs of the observations to update
 * @param concept The new concept
 * @param observer The new observer
 * @param activity The new activity
 * @param group The new group
 */
case class ObservationsUpdate(
    observationUuids: Seq[UUID],
    concept: Option[String] = None,
    observer: Option[String] = None,
    activity: Option[String] = None,
    group: Option[String] = None
) extends ToSnakeCase[ObservationsUpdateSC] {
    def toSnakeCase: ObservationsUpdateSC = ObservationsUpdateSC(
        observation_uuids = observationUuids,
        concept = concept,
        observer = observer,
        activity = activity,
        group = group
    )
}

case class ObservationsUpdateSC(
    observation_uuids: Seq[UUID],
    concept: Option[String] = None,
    observer: Option[String] = None,
    activity: Option[String] = None,
    group: Option[String] = None
) extends ToCamelCase[ObservationsUpdate] {
    def toCamelCase: ObservationsUpdate = ObservationsUpdate(
        observationUuids = observation_uuids,
        concept = concept,
        observer = observer,
        activity = activity,
        group = group
    )
}
