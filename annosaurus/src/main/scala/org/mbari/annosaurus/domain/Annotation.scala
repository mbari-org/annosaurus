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
import org.mbari.vcr4j.time.Timecode
import java.time.Duration
import java.time.Instant

final case class Annotation(
    activity: Option[String] = None,
    ancillaryData: Option[CachedAncillaryDatum] = None,
    associations: Seq[Association] = Nil,
    concept: Option[String] = None,
    durationMillis: Option[Long] = None,
    elapsedTimeMillis: Option[Long] = None,
    group: Option[String] = None,
    imagedMomentUuid: Option[UUID] = None,
    imageReferences: Seq[ImageReference] = Nil,
    observationTimestamp: Option[Instant] = None,
    observationUuid: Option[UUID] = None,
    observer: Option[String] = None,
    recordedTimestamp: Option[Instant] = None,
    timecode: Option[String] = None,
    videoReferenceUuid: Option[UUID] = None
) extends ToSnakeCase[AnnotationSC] {

    override def toSnakeCase: AnnotationSC =
        AnnotationSC(
            activity,
            ancillaryData.map(_.toSnakeCase),
            associations.map(_.toSnakeCase),
            concept,
            durationMillis,
            elapsedTimeMillis,
            group,
            imagedMomentUuid,
            imageReferences.map(_.toSnakeCase),
            observationTimestamp,
            observationUuid,
            observer,
            recordedTimestamp,
            timecode,
            videoReferenceUuid
        )

}

final case class AnnotationSC(
    activity: Option[String] = None,
    ancillary_data: Option[CachedAncillaryDatumSC] = None,
    associations: Seq[AssociationSC] = Nil,
    concept: Option[String] = None,
    duration_millis: Option[Long] = None,
    elapsed_time_millis: Option[Long] = None,
    group: Option[String] = None,
    imaged_moment_uuid: Option[UUID] = None,
    image_references: Seq[ImageReferenceSC] = Nil,
    observation_timestamp: Option[Instant] = None,
    observation_uuid: Option[UUID] = None,
    observer: Option[String] = None,
    recorded_timestamp: Option[Instant] = None,
    timecode: Option[String] = None,
    video_reference_uuid: Option[UUID] = None
) extends ToCamelCase[Annotation] {

    override def toCamelCase: Annotation =
        Annotation(
            activity,
            ancillary_data.map(_.toCamelCase),
            associations.map(_.toCamelCase),
            concept,
            duration_millis,
            elapsed_time_millis,
            group,
            imaged_moment_uuid,
            image_references.map(_.toCamelCase),
            observation_timestamp,
            observation_uuid,
            observer,
            recorded_timestamp,
            timecode,
            video_reference_uuid
        )
}
