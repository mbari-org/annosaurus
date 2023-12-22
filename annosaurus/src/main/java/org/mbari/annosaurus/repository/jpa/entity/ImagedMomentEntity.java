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
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import org.mbari.annosaurus.domain.ImagedMoment;
import org.mbari.annosaurus.repository.jpa.DurationConverter;
import org.mbari.annosaurus.repository.jpa.InstantConverter;
import org.mbari.annosaurus.repository.jpa.TimecodeConverter;
import org.mbari.annosaurus.repository.jpa.TransactionLogger;
import org.mbari.vcr4j.time.Timecode;

@Entity(name = "ImagedMoment")
@Table(
        name = "imaged_moments",
        indexes = {
                @Index(
                        name = "idx_imaged_moments__video_reference_uuid",
                        columnList = "video_reference_uuid"
                ),
                @Index(
                        name = "idx_imaged_moments__recorded_timestamp",
                        columnList = "recorded_timestamp"
                ),
                @Index(
                        name = "idx_imaged_moments__elapsed_time",
                        columnList = "elapsed_time_millis"
                ),
                @Index(name = "idx_imaged_moments__timecode", columnList = "timecode")
        }
)
@EntityListeners({TransactionLogger.class})
@NamedNativeQueries(
        {
                @NamedNativeQuery(
                        name = "ImagedMoment.findAllVideoReferenceUUIDs",
                        query =
                                "SELECT DISTINCT video_reference_uuid FROM imaged_moments ORDER BY video_reference_uuid ASC"
                ),
                @NamedNativeQuery(
                        name = "ImagedMoment.findVideoReferenceUUIDsModifiedBetweenDates",
                        query = "SELECT DISTINCT video_reference_uuid FROM imaged_moments im LEFT JOIN " +
                                "observations obs ON obs.imaged_moment_uuid = im.uuid WHERE " +
                                "im.last_updated_timestamp BETWEEN ?1 AND ?2 OR " +
                                "obs.last_updated_timestamp BETWEEN ?1 AND ?2 " +
                                "ORDER BY video_reference_uuid ASC"
                ),
                @NamedNativeQuery(
                        name = "ImagedMoment.countByConcept",
                        query = "SELECT COUNT(*) FROM imaged_moments im LEFT JOIN " +
                                "observations obs ON obs.imaged_moment_uuid = im.uuid WHERE " +
                                "obs.concept = ?1"
                ),
                @NamedNativeQuery(
                        name = "ImagedMoment.countByConceptWithImages",
                        query = "SELECT COUNT(*) FROM imaged_moments im LEFT JOIN " +
                                "observations obs ON obs.imaged_moment_uuid = im.uuid RIGHT JOIN " +
                                "image_references ir ON ir.imaged_moment_uuid = im.uuid " +
                                "WHERE obs.concept = ?1 AND ir.url IS NOT NULL"
                ),
                @NamedNativeQuery(
                        name = "ImagedMoment.countBetweenUpdatedDates",
                        query = "SELECT COUNT(*) FROM imaged_moments im LEFT JOIN " +
                                "observations obs ON obs.imaged_moment_uuid = im.uuid WHERE " +
                                "im.last_updated_timestamp BETWEEN ?1 AND ?2 OR " +
                                "obs.last_updated_timestamp BETWEEN ?1 AND ?2"
                ),
                @NamedNativeQuery(
                        name = "ImageMoment.updateRecordedTimestampByObservationUuid",
                        query = "UPDATE imaged_moments SET recorded_timestamp = ?1 WHERE " +
                                "uuid IN (SELECT obs.imaged_moment_uuid FROM observations obs WHERE obs.uuid = ?2)"
                ),
                @NamedNativeQuery(
                        name = "ImagedMoment.countAllByVideoReferenceUUIDs",
                        query =
                                "SELECT video_reference_uuid, COUNT(uuid) as n FROM imaged_moments GROUP BY video_reference_uuid ORDER BY n"
                ),
                @NamedNativeQuery(
                        name = "ImagedMoment.countModifiedBeforeDate",
                        query =
                                "SELECT COUNT(*) FROM imaged_moments WHERE video_reference_uuid = ?1 AND last_updated_timestamp < ?2"
                ),
                @NamedNativeQuery(
                        name = "ImagedMoment.countByVideoReferenceUUID",
                        query = "SELECT COUNT(*) FROM imaged_moments WHERE video_reference_uuid = ?1"
                ),
                @NamedNativeQuery(
                        name = "ImagedMoment.countByVideoReferenceUUIDWithImages",
                        query = "SELECT COUNT(DISTINCT i.uuid) FROM imaged_moments i " +
                                "INNER JOIN image_references ir ON ir.imaged_moment_uuid = i.uuid " +
                                "WHERE ir.url IS NOT NULL AND video_reference_uuid = ?1"
                ),
                @NamedNativeQuery(
                        name = "ImagedMoment.countAll",
                        query = "SELECT COUNT(*) FROM imaged_moments"
                ),
                @NamedNativeQuery(
                        name = "ImagedMoment.countWithImages",
                        query = "SELECT COUNT(DISTINCT i.uuid) FROM imaged_moments i " +
                                "INNER JOIN image_references ir ON ir.imaged_moment_uuid = i.uuid " +
                                "WHERE ir.url IS NOT NULL"
                ),
                @NamedNativeQuery(
                        name = "ImagedMoment.countByLinkName",
                        query = "SELECT COUNT(DISTINCT i.uuid) FROM imaged_moments i " +
                                "INNER JOIN observations o ON o.imaged_moment_uuid = i.uuid " +
                                "INNER JOIN associations a ON a.observation_uuid = o.uuid " +
                                "WHERE a.link_name = ?1"
                )
        }
)
@NamedQueries(
        {
                @NamedQuery(
                        name = "ImagedMoment.findAll",
                        query = "SELECT i FROM ImagedMoment i ORDER BY i.uuid"
                ),
                @NamedQuery(
                        name = "ImagedMoment.findWithImages",
                        query = "SELECT i FROM ImagedMoment i " +
                                "LEFT JOIN i.javaImageReferences ir " +
                                "WHERE ir.url IS NOT NULL"
                ),
                @NamedQuery(
                        name = "ImagedMoment.findByLinkName",
                        query = "SELECT i FROM ImagedMoment i " +
                                "INNER JOIN i.javaObservations o " +
                                "INNER JOIN o.javaAssociations a " +
                                "WHERE a.linkName = :linkName"
                ),
                @NamedQuery(
                        name = "ImagedMoment.findByConcept",
                        query = "SELECT i FROM ImagedMoment i LEFT JOIN i.javaObservations o WHERE " +
                                "o.concept = :concept ORDER BY i.uuid"
                ),
                @NamedQuery(
                        name = "ImagedMoment.findByConceptWithImages",
                        query = "SELECT i FROM ImagedMoment i LEFT JOIN i.javaObservations o " +
                                "LEFT JOIN i.javaImageReferences ir " +
                                "WHERE ir.url IS NOT NULL AND o.concept = :concept ORDER BY i.uuid"
                ),
                @NamedQuery(
                        name = "ImagedMoment.findBetweenUpdatedDates",
                        query = "SELECT i FROM ImagedMoment i LEFT JOIN i.javaObservations o WHERE " +
                                "i.lastUpdatedTime BETWEEN :start AND :end OR " +
                                "o.lastUpdatedTime BETWEEN :start AND :end ORDER BY i.uuid"
                ),
                @NamedQuery(
                        name = "ImagedMoment.findByVideoReferenceUUID",
                        query =
                                "SELECT i FROM ImagedMoment i WHERE i.videoReferenceUUID = :uuid ORDER BY i.uuid"
                ),
                @NamedQuery(
                        name = "ImagedMoment.findByVideoReferenceUUIDAndTimestamps",
                        query = "SELECT i FROM ImagedMoment i WHERE i.videoReferenceUUID = :uuid AND " +
                                "i.recordedDate BETWEEN :start AND :end ORDER BY i.recordedDate"
                ),
                @NamedQuery(
                        name = "ImagedMoment.findWithImageReferences",
                        query =
                                "SELECT i FROM ImagedMoment i LEFT JOIN i.javaImageReferences r WHERE i.videoReferenceUUID = :uuid ORDER BY i.uuid"
                ),
                @NamedQuery(
                        name = "ImagedMoment.findByObservationUUID",
                        query =
                                "SELECT i FROM ImagedMoment i LEFT JOIN i.javaObservations o WHERE o.uuid = :uuid ORDER BY i.uuid"
                ),
                @NamedQuery(
                        name = "ImagedMoment.findByUUID",
                        query = "SELECT i FROM ImagedMoment i WHERE i.uuid = :uuid ORDER BY i.uuid"
                ),
                @NamedQuery(
                        name = "ImagedMoment.findByVideoReferenceUUIDAndTimecode",
                        query =
                                "SELECT i FROM ImagedMoment i WHERE i.timecode = :timecode AND i.videoReferenceUUID = :uuid ORDER BY i.uuid"
                ),
                @NamedQuery(
                        name = "ImagedMoment.findByVideoReferenceUUIDAndElapsedTime",
                        query =
                                "SELECT i FROM ImagedMoment i WHERE i.elapsedTime = :elapsedTime AND i.videoReferenceUUID = :uuid ORDER BY i.uuid"
                ),
                @NamedQuery(
                        name = "ImagedMoment.findByVideoReferenceUUIDAndRecordedDate",
                        query =
                                "SELECT i FROM ImagedMoment i WHERE i.recordedDate = :recordedDate AND i.videoReferenceUUID = :uuid ORDER BY i.uuid"
                ),
                @NamedQuery(
                        name = "ImagedMoment.deleteByVideoReferenceUUID",
                        query = "DELETE FROM ImagedMoment i WHERE i.videoReferenceUUID = :uuid"
                ),
                @NamedQuery(
                        name = "ImagedMoment.findByWindowRequest",
                        query =
                                "SELECT i from ImagedMoment i WHERE i.videoReferenceUUID IN :uuids AND i.recordedDate BETWEEN :start AND :end"
                ),
                @NamedQuery(
                        name = "ImagedMoment.findByImageReferenceUUID",
                        query =
                                "SELECT i FROM ImagedMoment i LEFT JOIN i.javaImageReferences r WHERE r.uuid = :uuid ORDER BY i.uuid"
                ),
                @NamedQuery(
                        name = "ImagedMoment.findImageByConceptWithImages",
                        query = "SELECT new org.mbari.annosaurus.repository.jpa.entity.ImageDTO(i.uuid, i.elapsedTime, i.videoReferenceUUID, i.recordedDate, i.timecode, ir.description, ir.format, ir.height, ir.width, ir.url, ir.uuid) " +
                                "FROM ImagedMoment i LEFT JOIN i.javaObservations o LEFT JOIN i.javaImageReferences ir " +
                                "WHERE ir.url IS NOT NULL AND o.concept = :concept ORDER BY i.uuid"
                ),
                @NamedQuery(
                        name = "ImagedMoment.findImageByToConceptWithImages",
                        query = "SELECT new org.mbari.annosaurus.repository.jpa.entity.ImageDTO(i.uuid, i.elapsedTime, i.videoReferenceUUID, i.recordedDate, i.timecode, ir.description, ir.format, ir.height, ir.width, ir.url, ir.uuid) " +
                                "FROM ImagedMoment i LEFT JOIN i.javaObservations o LEFT JOIN i.javaImageReferences ir LEFT JOIN o.javaAssociations a " +
                                "WHERE ir.url IS NOT NULL AND a.toConcept = :concept ORDER BY i.uuid"
                ),
                @NamedQuery(
                        name = "ImagedMoment.findImageByVideoReferenceUUID",
                        query =
                                "SELECT new org.mbari.annosaurus.repository.jpa.entity.ImageDTO(i.uuid, i.elapsedTime, i.videoReferenceUUID, i.recordedDate, i.timecode, ir.description, ir.format, ir.height, ir.width, ir.url, ir.uuid) " +
                                        "FROM ImagedMoment i LEFT JOIN i.javaImageReferences ir " +
                                        "WHERE ir.url IS NOT NULL AND i.videoReferenceUUID = :uuid ORDER BY i.uuid"
                )
        }
)
@org.hibernate.envers.Audited
public class ImagedMomentEntity implements IPersistentObject {

    @Id
    @Column(name = "uuid", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID uuid;

    /** Optimistic lock to prevent concurrent overwrites */
    @Version
    @Column(name = "last_updated_time")
    protected Timestamp lastUpdatedTime;

    @Column(name = "elapsed_time_millis", nullable = true)
    @Convert(converter = DurationConverter.class)
    Duration elapsedTime;

    @Column(name = "recorded_timestamp", nullable = true)
    @Temporal(value = TemporalType.TIMESTAMP)
    @Convert(converter = InstantConverter.class)
    Instant recordedDate;

    @Column(name = "timecode", nullable = true)
    @Convert(converter = TimecodeConverter.class)
    Timecode timecode;

    @Column(
            name = "video_reference_uuid",
            nullable = true,
            columnDefinition = "uuid-char"
    )
    UUID videoReferenceUuid;

    @OneToMany(
            targetEntity = ObservationEntity.class,
            cascade = {CascadeType.ALL},
            fetch = FetchType.EAGER,
            mappedBy = "imagedMoment",
            orphanRemoval = true
    )
    Set<ObservationEntity> observations = new HashSet<>();

    @OneToMany(
            targetEntity = ImageReferenceEntity.class,
            cascade = {CascadeType.ALL},
            fetch = FetchType.LAZY,
            mappedBy = "imagedMoment",
            orphanRemoval = true
    )
    Set<ImageReferenceEntity> imageReferences = new HashSet<>();

    @OneToOne(
            mappedBy = "imagedMoment",
            cascade = {CascadeType.ALL},
            optional = true,
            fetch = FetchType.LAZY,
            targetEntity = CachedAncillaryDatumEntity.class
    )
    CachedAncillaryDatumEntity ancillaryDatum;

    public ImagedMomentEntity() {

    }

    public ImagedMomentEntity(UUID videoReferenceUuid, Instant recordedTimestamp, Timecode timecode, Duration elapsedTime) {
        this.videoReferenceUuid = videoReferenceUuid;
        this.recordedDate = recordedTimestamp;
        this.timecode = timecode;
        this.elapsedTime = elapsedTime;
    }

    public ImagedMomentEntity(ImagedMomentEntity that) {
        this.videoReferenceUuid = that.videoReferenceUuid;
        this.recordedDate = that.recordedDate;
        this.timecode = that.timecode;
        this.elapsedTime = that.elapsedTime;
        this.uuid = that.uuid;
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

    public Duration getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(Duration elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public Instant getRecordedDate() {
        return recordedDate;
    }

    public void setRecordedDate(Instant recordedDate) {
        this.recordedDate = recordedDate;
    }

    public Timecode getTimecode() {
        return timecode;
    }

    public void setTimecode(Timecode timecode) {
        this.timecode = timecode;
    }

    public UUID getVideoReferenceUuid() {
        return videoReferenceUuid;
    }

    public void setVideoReferenceUuid(UUID videoReferenceUuid) {
        this.videoReferenceUuid = videoReferenceUuid;
    }

    public Set<ObservationEntity> getObservations() {
        return observations;
    }

    public void addObservation(ObservationEntity obs) {
        if (obs != null) {
            observations.add(obs);
            obs.setImagedMoment(this);
        }
    }

    public void removeObservation(ObservationEntity obs) {
        if (obs != null) {
            observations.remove(obs);
            obs.setImagedMoment(null);
        }
    }

    public void setObservations(Set<ObservationEntity> observations) {
        this.observations = observations;
    }

    public Set<ImageReferenceEntity> getImageReferences() {
        return imageReferences;
    }

    public void addImageReference(ImageReferenceEntity ir) {
        if (ir != null) {
            imageReferences.add(ir);
            ir.setImagedMoment(this);
        }
    }

    public void removeImageReference(ImageReferenceEntity ir) {
        if (ir != null) {
            imageReferences.remove(ir);
            ir.setImagedMoment(null);
        }
    }

    public void setImageReferences(Set<ImageReferenceEntity> imageReferences) {
        this.imageReferences = imageReferences;
    }

    public CachedAncillaryDatumEntity getAncillaryDatum() {
        return ancillaryDatum;
    }

    public void setAncillaryDatum(CachedAncillaryDatumEntity ancillaryDatum) {
        if (this.ancillaryDatum != null) {
            this.ancillaryDatum.setImagedMoment(null);
        }
        if (ancillaryDatum != null) {
            ancillaryDatum.setImagedMoment(this);
        }
        this.ancillaryDatum = ancillaryDatum;
    }

    public String toString() {
        var sb = new StringBuilder();
        sb.append("ImagedMomentEntity(");
        sb.append("uuid=" + uuid);
        sb.append(", videoReferenceUUID=" + videoReferenceUuid);
        sb.append(", recordedDate=" + recordedDate);
        sb.append(", timecode=" + timecode);
        sb.append(", elapsedTime=" + elapsedTime);
        sb.append(")");
        return sb.toString();
    }



}
