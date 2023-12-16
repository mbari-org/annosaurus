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
import org.mbari.annosaurus.repository.jpa.entity.ImagedMomentEntity

final case class Annotation(
    observationUuid: UUID,
    concept: String,
    observer: String,
    observationTimestamp: String,
    videoReferenceUuid: UUID,
    imagedMomentUuid: UUID,
    timecode: String,
    elapsedTime: String,
    recordedTimestamp: String,
    duration: String,
    group: String,
    activity: String,
    associations: Seq[Association],
    imageReferences: Seq[ImageReference]
)

final case class AnnotationSC(
    observation_uuid: UUID,
    concept: String,
    observer: String,
    observation_timestamp: String,
    video_reference_uuid: UUID,
    imaged_moment_uuid: UUID,
    timecode: String,
    elapsed_time: String,
    recorded_timestamp: String,
    duration: String,
    group: String,
    activity: String,
    associations: Seq[AssociationSC],
    image_references: Seq[ImageReferenceSC]
) {

    def toCamelCase: Annotation = Annotation(
        observation_uuid,
        concept,
        observer,
        observation_timestamp,
        video_reference_uuid,
        imaged_moment_uuid,
        timecode,
        elapsed_time,
        recorded_timestamp,
        duration,
        group,
        activity,
        associations.map(_.toCamelCase),
        image_references.map(_.toCamelCase)
    )
}


