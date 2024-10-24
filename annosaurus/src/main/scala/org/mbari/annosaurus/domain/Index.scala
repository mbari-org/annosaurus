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

import org.mbari.annosaurus.repository.jpa.entity.extensions.*
import org.mbari.annosaurus.repository.jpa.entity.{ImagedMomentEntity, IndexEntity}

import java.util.UUID

final case class Index(
    videoReferenceUuid: UUID,
    timecode: Option[String] = None,
    elapsedTimeMillis: Option[Long] = None,
    recordedTimestamp: Option[java.time.Instant] = None,
    uuid: Option[UUID] = None,
    lastUpdated: Option[java.time.Instant] = None
) extends ToSnakeCase[IndexSC]
    with ToEntity[IndexEntity]:

    lazy val elapsedTime              = elapsedTimeMillis.map(java.time.Duration.ofMillis)
    override def toSnakeCase: IndexSC =
        IndexSC(
            videoReferenceUuid,
            timecode,
            elapsedTimeMillis,
            recordedTimestamp,
            uuid,
            lastUpdated
        )

    override def toEntity: IndexEntity =
        val entity = new IndexEntity
        entity.setVideoReferenceUuid(videoReferenceUuid)
        timecode.foreach(tc => entity.setTimecode(org.mbari.vcr4j.time.Timecode(tc)))
        elapsedTimeMillis.foreach(t => entity.setElapsedTime(java.time.Duration.ofMillis(t)))
        recordedTimestamp.foreach(entity.setRecordedTimestamp)
        uuid.foreach(entity.setUuid)
        entity

object Index extends FromEntity[IndexEntity, Index]:
    def from(entity: IndexEntity, extend: Boolean = false): Index =
        Index(
            entity.getVideoReferenceUuid,
            Option(entity.getTimecode).map(_.toString()),
            Option(entity.getElapsedTime).map(_.toMillis),
            Option(entity.getRecordedTimestamp),
            entity.primaryKey,
            entity.lastUpdated
        )

    def fromImagedMomentEntity(entity: ImagedMomentEntity): Index =
        Index(
            entity.getVideoReferenceUuid,
            Option(entity.getTimecode).map(_.toString()),
            Option(entity.getElapsedTime).map(_.toMillis),
            Option(entity.getRecordedTimestamp),
            entity.primaryKey,
            entity.lastUpdated
        )

final case class IndexSC(
    video_reference_uuid: UUID,
    timecode: Option[String] = None,
    elapsed_time_millis: Option[Long] = None,
    recorded_timestamp: Option[java.time.Instant] = None,
    uuid: Option[UUID] = None,
    last_updated: Option[java.time.Instant] = None
) extends ToCamelCase[Index]:
    override def toCamelCase: Index =
        Index(
            video_reference_uuid,
            timecode,
            elapsed_time_millis,
            recorded_timestamp,
            uuid,
            last_updated
        )
