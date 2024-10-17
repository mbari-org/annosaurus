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

import org.mbari.vcr4j.time.Timecode

import java.time.{Duration, Instant}
import java.util.UUID

case class AnnotationUpdate(
    observationUuid: Option[UUID] = None,
    videoReferenceUuid: Option[UUID] = None,
    concept: Option[String] = None,
    observer: Option[String] = None,
    observationTimestamp: Option[Instant] = None,
    timecode: Option[String] = None,
    elapsedTimeMillis: Option[Long] = None,
    recordedTimestamp: Option[Instant] = None,
    durationMillis: Option[Long] = None,
    group: Option[String] = None,
    activity: Option[String] = None
) extends ToSnakeCase[AnnotationUpdateSC]:

    lazy val elapsedTime: Option[Duration]   = elapsedTimeMillis.map(Duration.ofMillis)
    lazy val duration: Option[Duration]      = durationMillis.map(Duration.ofMillis)
    lazy val validTimecode: Option[Timecode] = timecode.map(Timecode(_))

    override def toSnakeCase: AnnotationUpdateSC =
        AnnotationUpdateSC(
            observationUuid,
            videoReferenceUuid,
            concept,
            observer,
            observationTimestamp,
            timecode,
            elapsedTimeMillis,
            recordedTimestamp,
            durationMillis,
            group,
            activity
        )

    def toAnnotation: Annotation =
        Annotation(
            observationUuid = observationUuid,
            videoReferenceUuid = videoReferenceUuid,
            concept = concept,
            observer = observer,
            observationTimestamp = observationTimestamp,
            timecode = timecode,
            elapsedTimeMillis = elapsedTimeMillis,
            recordedTimestamp = recordedTimestamp,
            durationMillis = durationMillis,
            group = group,
            activity = activity
        )

case class AnnotationUpdateSC(
    observation_uuid: Option[UUID] = None,
    video_reference_uuid: Option[UUID] = None,
    concept: Option[String] = None,
    observer: Option[String] = None,
    observation_timestamp: Option[Instant] = None,
    timecode: Option[String] = None,
    elapsed_time_millis: Option[Long] = None,
    recorded_timestamp: Option[Instant] = None,
    duration_millis: Option[Long] = None,
    group: Option[String] = None,
    activity: Option[String] = None
) extends ToCamelCase[AnnotationUpdate]:

    override def toCamelCase: AnnotationUpdate =
        AnnotationUpdate(
            observation_uuid,
            video_reference_uuid,
            concept,
            observer,
            observation_timestamp,
            timecode,
            elapsed_time_millis,
            recorded_timestamp,
            duration_millis,
            group,
            activity
        )
