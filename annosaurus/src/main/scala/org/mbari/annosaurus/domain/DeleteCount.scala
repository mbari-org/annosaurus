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

final case class DeleteCount(
    videoReferenceUuid: UUID,
    ancillaryDataCount: Int = 0,
    imageReferenceCount: Int = 0,
    associationCount: Int = 0,
    observationCount: Int = 0,
    imagedMomentCount: Int = 0,
    errorMessage: Option[String] = None
) extends ToSnakeCase[DeleteCountSC] {
    override def toSnakeCase: DeleteCountSC = DeleteCountSC(
        videoReferenceUuid,
        ancillaryDataCount,
        imageReferenceCount,
        associationCount,
        observationCount,
        imagedMomentCount,
        errorMessage
    )
}

final case class DeleteCountSC(
    video_reference_uuid: UUID,
    ancillary_data_count: Int = 0,
    image_reference_count: Int = 0,
    association_count: Int = 0,
    observation_count: Int = 0,
    imaged_moment_count: Int = 0,
    error_message: Option[String] = None
) extends ToCamelCase[DeleteCount] {
    override def toCamelCase: DeleteCount = DeleteCount(
        video_reference_uuid,
        ancillary_data_count,
        image_reference_count,
        association_count,
        observation_count,
        imaged_moment_count,
        error_message
    )
}
