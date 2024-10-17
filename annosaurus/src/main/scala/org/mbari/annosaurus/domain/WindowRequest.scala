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

import java.time.Duration
import java.util.UUID

final case class WindowRequest(
    videoReferenceUuids: Seq[UUID],
    imagedMomentUuid: UUID,
    windowMillis: Long
) extends ToSnakeCase[WindowRequestSC]:
    def toSnakeCase: WindowRequestSC = WindowRequestSC(
        videoReferenceUuids,
        imagedMomentUuid,
        windowMillis
    )

    val window: Duration = Duration.ofMillis(windowMillis)

final case class WindowRequestSC(
    video_reference_uuids: Seq[UUID],
    imaged_moment_uuid: UUID,
    window_millis: Long
) extends ToCamelCase[WindowRequest]:
    def toCamelCase: WindowRequest = WindowRequest(
        video_reference_uuids,
        imaged_moment_uuid,
        window_millis
    )
