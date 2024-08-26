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

case class MoveImagedMoments(videoReferenceUuid: UUID, imagedMomentUuids: Seq[UUID])
    extends ToSnakeCase[MoveImagedMomentsSC] {

    def toSnakeCase: MoveImagedMomentsSC = {
        MoveImagedMomentsSC(
            video_reference_uuid = videoReferenceUuid,
            imaged_moment_uuids = imagedMomentUuids
        )
    }

}

case class MoveImagedMomentsSC(video_reference_uuid: UUID, imaged_moment_uuids: Seq[UUID])
    extends ToCamelCase[MoveImagedMoments] {

    def toCamelCase: MoveImagedMoments = {
        MoveImagedMoments(
            videoReferenceUuid = video_reference_uuid,
            imagedMomentUuids = imaged_moment_uuids
        )
    }

}
