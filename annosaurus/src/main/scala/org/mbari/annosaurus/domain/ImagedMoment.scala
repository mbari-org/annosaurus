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

import org.mbari.annosaurus.repository.jpa.entity.ImagedMomentEntity
import org.mbari.annosaurus.repository.jpa.entity.extensions.*
import org.mbari.vcr4j.time.Timecode

import java.time.{Duration, Instant}
import java.util.UUID
import scala.jdk.CollectionConverters.*

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
    with ToEntity[ImagedMomentEntity]:

    def removeForeignKeys(): ImagedMoment = copy(
        lastUpdated = None
    )

    lazy val validTimecode: Option[Timecode] = timecode.map(Timecode(_))
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
        val entity = new ImagedMomentEntity
        entity.setVideoReferenceUuid(videoReferenceUuid)
        timecode.foreach(tc => entity.setTimecode(Timecode(tc)))
        elapsedTimeMillis.foreach(t => entity.setElapsedTime(Duration.ofMillis(t)))
        recordedTimestamp.foreach(entity.setRecordedTimestamp)
        observations.map(_.toEntity).foreach(entity.addObservation)
        imageReferences.map(_.toEntity).foreach(entity.addImageReference)
        ancillaryData.map(_.toEntity).foreach(entity.setAncillaryDatum)
        uuid.foreach(entity.setUuid)
        entity

    lazy val elapsedTime: Option[Duration] = elapsedTimeMillis.map(Duration.ofMillis)

    /**
    * This method is used during integration testing. We store timestamps
    * to millis, but (depending on the JVM) it may have nanosecond resolution
    * in memory
    *
    * @return a copy of this ImagedMoment with observation timestamps rounded to millis
    */
    def roundObservationTimestampsToMillis(): ImagedMoment =
      var newObservations = observations.map(_.roundObservationTimestampToMillis)
      this.copy(observations = newObservations)

object ImagedMoment extends FromEntity[ImagedMomentEntity, ImagedMoment]:
    def from(entity: ImagedMomentEntity, extend: Boolean = false): ImagedMoment =

        val observations =
            if extend && !entity.getObservations().isEmpty()
            then entity.getObservations.asScala.map(Observation.from(_, extend)).toSeq
            else Nil

        // Do not extend image references here. As that would include redundant information
        val imageReferences =
            if extend && !entity.getImageReferences().isEmpty()
            then entity.getImageReferences.asScala.map(ImageReference.from(_, false)).toSeq
            else Nil

        // Do not extend data here. As that would include redundant information
        val data =
            if extend then Option(entity.getAncillaryDatum).map(CachedAncillaryDatum.from(_, false))
            else None

        ImagedMoment(
            entity.getVideoReferenceUuid,
            Option(entity.getTimecode).map(_.toString()),
            Option(entity.getElapsedTime).map(_.toMillis),
            Option(entity.getRecordedTimestamp),
            observations,
            imageReferences,
            data,
            entity.primaryKey,
            entity.lastUpdated
        )

final case class ImagedMomentSC(
    video_reference_uuid: UUID,
    timecode: Option[String] = None,
    elapsed_time_millis: Option[Long] = None,
    recorded_timestamp: Option[Instant] = None, // XXX: This should have been recorded_timestamp
    observations: Seq[ObservationSC] = Nil,
    image_references: Seq[ImageReferenceSC] = Nil,
    ancillary_data: Option[CachedAncillaryDatumSC] = None,
    uuid: Option[UUID] = None,
    last_updated_time: Option[java.time.Instant] = None
) extends ToCamelCase[ImagedMoment]:
    override def toCamelCase: ImagedMoment =
        ImagedMoment(
            video_reference_uuid,
            timecode,
            elapsed_time_millis,
            recorded_timestamp,
            observations.map(_.toCamelCase),
            image_references.map(_.toCamelCase),
            ancillary_data.map(_.toCamelCase),
            uuid,
            last_updated_time
        )
