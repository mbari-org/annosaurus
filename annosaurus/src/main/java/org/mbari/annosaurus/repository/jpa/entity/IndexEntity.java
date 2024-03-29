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

import jakarta.persistence.*;
import org.mbari.annosaurus.repository.jpa.DurationConverter;
import org.mbari.annosaurus.repository.jpa.TimecodeConverter;
import org.mbari.vcr4j.time.Timecode;
import org.mbari.annosaurus.repository.jpa.TransactionLogger;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "Index")
@Table(name = "imaged_moments")
@EntityListeners({TransactionLogger.class})
@NamedQueries(
        {
                @NamedQuery(
                        name = "Index.findByVideoReferenceUUID",
                        query = "SELECT i FROM Index i WHERE i.videoReferenceUuid = :uuid ORDER BY i.uuid"
                )
        }
)
public class IndexEntity implements IPersistentObject {

    @Id
    @Column(name = "uuid", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID uuid;

    /** Optimistic lock to prevent concurrent overwrites */
    @Version
    @Column(name = "last_updated_timestamp")
    protected Timestamp lastUpdatedTime;

    @Basic(optional = false)
    @Column(
            name = "video_reference_uuid"
    )
    @JdbcTypeCode(SqlTypes.UUID)
    UUID videoReferenceUuid;

    @Column(name = "elapsed_time_millis", nullable = true)
    @Convert(converter = DurationConverter.class)
    Duration elapsedTime;

    @Column(name = "recorded_timestamp", nullable = true)
    @Temporal(value = TemporalType.TIMESTAMP)
    Instant recordedTimestamp;

    @Column(name = "timecode", nullable = true)
    @Convert(converter = TimecodeConverter.class)
    Timecode timecode;

    @Transient
    CachedAncillaryDatumEntity ancillaryDatum;

    public IndexEntity() {
    }

    public IndexEntity(UUID uuid, UUID videoReferenceUuid, Duration elapsedTime, Instant recordedTimestamp, Timecode timecode) {
        this.uuid = uuid;
        this.videoReferenceUuid = videoReferenceUuid;
        this.elapsedTime = elapsedTime;
        this.recordedTimestamp = recordedTimestamp;
        this.timecode = timecode;
    }

    public IndexEntity(ImagedMomentEntity imagedMomentEntity) {
        this.uuid = imagedMomentEntity.getUuid();
        this.videoReferenceUuid = imagedMomentEntity.getVideoReferenceUuid();
        this.elapsedTime = imagedMomentEntity.getElapsedTime();
        this.recordedTimestamp = imagedMomentEntity.getRecordedTimestamp();
        this.timecode = imagedMomentEntity.getTimecode();
        this.lastUpdatedTime = imagedMomentEntity.getLastUpdatedTime();
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Timestamp getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    public void setLastUpdatedTime(Timestamp lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }

    public UUID getVideoReferenceUuid() {
        return videoReferenceUuid;
    }

    public void setVideoReferenceUuid(UUID videoReferenceUuid) {
        this.videoReferenceUuid = videoReferenceUuid;
    }

    public Duration getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(Duration elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public Instant getRecordedTimestamp() {
        return recordedTimestamp;
    }

    public void setRecordedTimestamp(Instant recordedTimestamp) {
        this.recordedTimestamp = recordedTimestamp;
    }

    public Timecode getTimecode() {
        return timecode;
    }

    public void setTimecode(Timecode timecode) {
        this.timecode = timecode;
    }

    public CachedAncillaryDatumEntity getAncillaryDatum() {
        return ancillaryDatum;
    }

    public void setAncillaryDatum(CachedAncillaryDatumEntity ancillaryDatum) {
        this.ancillaryDatum = ancillaryDatum;
    }

    
}
