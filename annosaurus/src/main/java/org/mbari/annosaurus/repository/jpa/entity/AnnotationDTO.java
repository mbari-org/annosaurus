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

package org.mbari.annosaurus.repository.jpa.entity;

import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.time.Duration;

record AnnotationDTO(
    UUID observationUuid,
    String concept,
    String observer,
    Instant observationTimestamp,
    UUID videoReferenceUuid,
    UUID imagedMomentUuid,
    String timecode,
    Duration elapsedTime,
    Instant recordedTimestamp,
    Duration duration,
    String group,
    String activity,
    List<AssociationDTO> associations,
    List<ImageReferenceDTO> imageReferences
) {

    public AnnotationDTO(UUID observationUuid,
    String concept,
    String observer,
    Instant observationTimestamp,
    UUID videoReferenceUuid,
    UUID imagedMomentUuid,
    String timecode,
    Duration elapsedTime,
    Instant recordedTimestamp,
    Duration duration,
    String group,
    String activity) {
        this(observationUuid, concept, observer, observationTimestamp, videoReferenceUuid, imagedMomentUuid, timecode, elapsedTime, recordedTimestamp, duration, group, activity, List.of(), List.of());
    }

}
