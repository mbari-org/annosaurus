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
import java.time.Duration
import org.mbari.vcr4j.time.Timecode
import java.time.Instant
import org.mbari.annosaurus.repository.jpa.entity.ImagedMomentEntity


final case class ImagedMoment(
    videoReferenceUuid: UUID,
    timecode: Option[String] = None,
    elapsedTimeMillis: Option[Long] = None,
    recordedTimestamp: Option[Instant] = None,
    observations: Seq[Observation] = Nil,
    imageReferences: Seq[ImageReference] = Nil,
    ancillaryData: Option[CachedAncillaryDatum] = None,
    uuid: Option[UUID] = None,
    lastUpdated: Option[java.time.Instant] = None
) extends ToSnakeCase[ImagedMomentSC]
    with ToEntity[ImagedMomentEntity] {
    override def toSnakeCase: ImagedMomentSC =
        ImagedMomentSC(
            videoReferenceUuid,
            timecode,
            elapsedTimeMillis,
            recordedTimestamp,
            observations.map(_.toSnakeCase),
            imageReferences.map(_.toSnakeCase),
            ancillaryData.map(_.toSnakeCase),
            uuid,
            lastUpdated
        )

    override def toEntity: ImagedMomentEntity =
        var entity = new ImagedMomentEntity
        entity.videoReferenceUUID = videoReferenceUuid
        timecode.foreach(tc => entity.timecode = Timecode(tc))
        elapsedTimeMillis.foreach(t => entity.elapsedTime = Duration.ofMillis(t))
        recordedTimestamp.foreach(entity.recordedDate = _)
        observations.foreach(obs => entity.addObservation(obs.toEntity))
        imageReferences.foreach(ir => entity.addImageReference(ir.toEntity))
        ancillaryData.foreach(d => entity.ancillaryDatum = d.toEntity)
        uuid.foreach(entity.uuid = _)
        entity

    lazy val elapsedTime: Option[Duration] = elapsedTimeMillis.map(Duration.ofMillis)
}

object ImagedMoment extends FromEntity[ImagedMomentEntity, ImagedMoment] {
    def from(entity: ImagedMomentEntity, extend: Boolean = false): ImagedMoment = {
        ImagedMoment(
            entity.videoReferenceUUID,
            Option(entity.timecode).map(_.toString()),
            Option(entity.elapsedTime).map(_.toMillis),
            Option(entity.recordedDate),
            entity.observations.map(x => Observation.from(x, false)).toSeq,
            entity.imageReferences.map(x =>  ImageReference.from(x, false)).toSeq,
            Option(entity.ancillaryDatum).map(x => CachedAncillaryDatum.from(x, false)),
            Option(entity.uuid),
            entity.lastUpdated
        )
    }
}

final case class ImagedMomentSC(
    video_reference_uuid: UUID,
    timecode: Option[String] = None,
    elapsed_time_millis: Option[Long] = None,
    recorded_date: Option[Instant] = None, // XXX: This should have been recorded_timestamp
    observations: Seq[ObservationSC] = Nil,
    image_references: Seq[ImageReferenceSC] = Nil,
    ancillary_data: Option[CachedAncillaryDatumSC] = None,
    uuid: Option[UUID] = None,
    last_updated_time: Option[java.time.Instant] = None
) extends ToCamelCase[ImagedMoment] {
    override def toCamelCase: ImagedMoment =
        ImagedMoment(
            video_reference_uuid,
            timecode,
            elapsed_time_millis,
            recorded_date,
            observations.map(_.toCamelCase),
            image_references.map(_.toCamelCase),
            ancillary_data.map(_.toCamelCase),
            uuid,
            last_updated_time
        )
}
