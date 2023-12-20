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

import java.time.Instant
import java.util.UUID

final case class ConcurrentRequest(
    startTimestamp: Instant,
    endTimestamp: Instant,
    videoReferenceUuids: Seq[UUID]
) extends ToSnakeCase[ConcurrentRequestSC] {
    def toSnakeCase: ConcurrentRequestSC = ConcurrentRequestSC(
        startTimestamp,
        endTimestamp,
        videoReferenceUuids
    )
}

final case class ConcurrentRequestSC(
    start_timestamp: Instant,
    end_timestamp: Instant,
    video_reference_uuids: Seq[UUID]
) extends ToCamelCase[ConcurrentRequest] {
    def toCamelCase: ConcurrentRequest = ConcurrentRequest(
        start_timestamp,
        end_timestamp,
        video_reference_uuids
    )
}
