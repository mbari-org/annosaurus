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

case class AnnotationCreate(
    videoReferenceUuid: UUID,
    concept: String,
    activity: Option[String] = None,
    durationMillis: Option[Long] = None,
    elapsedTimeMillis: Option[Long] = None,
    group: Option[String] = None,
    imagedMomentUuid: Option[UUID] = None,
    observationTimestamp: Option[Instant] = None,
    observationUuid: Option[UUID] = None,
    observer: Option[String] = None,
    recordedTimestamp: Option[Instant] = None,
    timecode: Option[String] = None
) extends ToSnakeCase[AnnotationCreateSC] {

    def toAnnotation: Annotation = {
        Annotation(
            activity = activity,
            concept = Some(concept),
            durationMillis = durationMillis,
            elapsedTimeMillis = elapsedTimeMillis,
            group = group,
            imagedMomentUuid = imagedMomentUuid,
            observationTimestamp = observationTimestamp,
            observationUuid = observationUuid,
            observer = observer,
            recordedTimestamp = recordedTimestamp,
            timecode = timecode,
            videoReferenceUuid = Some(videoReferenceUuid)
        )
    }

    def toSnakeCase: AnnotationCreateSC = {
        AnnotationCreateSC(
            activity = activity,
            concept = concept,
            duration_millis = durationMillis,
            elapsed_time_millis = elapsedTimeMillis,
            group = group,
            imaged_moment_uuid = imagedMomentUuid,
            observation_timestamp = observationTimestamp,
            observation_uuid = observationUuid,
            observer = observer,
            recorded_timestamp = recordedTimestamp,
            timecode = timecode,
            video_reference_uuid = videoReferenceUuid
        )
    }

}

object AnnotationCreate {
    def fromAnnotation(a: Annotation): AnnotationCreate = {
        // TODO - hack. Forcing get on concept and videoReferenceUuid
        AnnotationCreate(
            activity = a.activity,
            concept = a.concept.get,
            durationMillis = a.durationMillis,
            elapsedTimeMillis = a.elapsedTimeMillis,
            group = a.group,
            imagedMomentUuid = a.imagedMomentUuid,
            observationTimestamp = a.observationTimestamp,
            observationUuid = a.observationUuid,
            observer = a.observer,
            recordedTimestamp = a.recordedTimestamp,
            timecode = a.timecode,
            videoReferenceUuid = a.videoReferenceUuid.get
        )
    }
}

case class AnnotationCreateSC(
    video_reference_uuid: UUID,
    concept: String,
    activity: Option[String] = None,
    duration_millis: Option[Long] = None,
    elapsed_time_millis: Option[Long] = None,
    group: Option[String] = None,
    imaged_moment_uuid: Option[UUID] = None,
    observation_timestamp: Option[Instant] = None,
    observation_uuid: Option[UUID] = None,
    observer: Option[String] = None,
    recorded_timestamp: Option[Instant] = None,
    timecode: Option[String] = None
) extends ToCamelCase[AnnotationCreate] {

    def toCamelCase: AnnotationCreate = {
        AnnotationCreate(
            activity = activity,
            concept = concept,
            durationMillis = duration_millis,
            elapsedTimeMillis = elapsed_time_millis,
            group = group,
            imagedMomentUuid = imaged_moment_uuid,
            observationTimestamp = observation_timestamp,
            observationUuid = observation_uuid,
            observer = observer,
            recordedTimestamp = recorded_timestamp,
            timecode = timecode,
            videoReferenceUuid = video_reference_uuid
        )
    }
}
