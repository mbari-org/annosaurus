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
import org.hibernate.annotations.UuidGenerator;
import org.mbari.annosaurus.repository.jpa.TransactionLogger;
import org.mbari.annosaurus.repository.jpa.TransactionNotifier;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * A localization (bounding box, point, line, polygon or mask) on an observation. Coordinates are
 * normalized to [0, 1]. {@code vertices} is stored as JSON text: an interleaved array [x0, y0, x1, y1, ...].
 */
@Entity(name = "Localization")
@Table(
        name = "localizations",
        indexes = {
                @Index(name = "idx_localizations__observation_uuid", columnList = "observation_uuid"),
                @Index(name = "idx_localizations__localization_type", columnList = "localization_type")
        }
)
@EntityListeners({TransactionLogger.class, TransactionNotifier.class})
@NamedQueries(
        {
                @NamedQuery(
                        name = "Localization.findAll",
                        query = "SELECT l FROM Localization l ORDER BY l.uuid"
                ),
                @NamedQuery(
                        name = "Localization.findByObservationUuid",
                        query = "SELECT l FROM Localization l WHERE l.observation.uuid = :uuid ORDER BY l.uuid"
                ),
                @NamedQuery(
                        name = "Localization.findByObservationUuidAndType",
                        query = "SELECT l FROM Localization l WHERE l.observation.uuid = :uuid AND l.localizationType = :localizationType"
                ),
                @NamedQuery(
                        name = "Localization.findByVideoReferenceUuid",
                        query = "SELECT l FROM Localization l JOIN l.observation o JOIN o.imagedMoment im WHERE im.videoReferenceUuid = :uuid ORDER BY l.uuid"
                )
        }
)
@org.hibernate.envers.Audited
public class LocalizationEntity implements IPersistentObject {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "uuid", nullable = false, updatable = false)
    UUID uuid;

    /** Optimistic lock to prevent concurrent overwrites */
    @Version
    @Column(name = "last_updated_timestamp")
    protected Timestamp lastUpdatedTime;

    @ManyToOne(
            cascade = {CascadeType.PERSIST, CascadeType.DETACH},
            optional = false,
            fetch = FetchType.LAZY,
            targetEntity = ObservationEntity.class
    )
    @JoinColumn(
            name = "observation_uuid",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_localizations__observation_uuid")
    )
    ObservationEntity observation;

    @Column(name = "localization_type", length = 32, nullable = false)
    String localizationType;

    @Column(name = "x", nullable = false)
    Double x;

    @Column(name = "y", nullable = false)
    Double y;

    @Column(name = "width", nullable = false)
    Double width;

    @Column(name = "height", nullable = false)
    Double height;

    /** JSON array text: [x0, y0, x1, y1, ...] */
    @Column(name = "vertices", nullable = true)
    String vertices;

    @Column(name = "mask_encoding", length = 32, nullable = true)
    String maskEncoding;

    @Column(name = "mask_data", nullable = true)
    String maskData;

    @Column(name = "mask_width_pixels", nullable = true)
    Integer maskWidthPixels;

    @Column(name = "mask_height_pixels", nullable = true)
    Integer maskHeightPixels;

    @Column(name = "generator", length = 128, nullable = true)
    String generator;

    @Column(name = "confidence", nullable = true)
    Float confidence;

    @Column(name = "observer", length = 128, nullable = true)
    String observer;

    @Column(name = "observation_timestamp", nullable = true)
    Instant observationTimestamp;

    @Column(name = "verifier", length = 128, nullable = true)
    String verifier;

    @Column(name = "verification_timestamp", nullable = true)
    Instant verificationTimestamp;

    @Column(name = "comment", length = 1024, nullable = true)
    String comment;

    @Column(name = "part", length = 128, nullable = true)
    String part;

    @Column(name = "training", nullable = false)
    boolean training = false;

    @Column(name = "truncated", nullable = false)
    boolean truncated = false;

    @Column(name = "difficult", nullable = false)
    boolean difficult = false;

    public LocalizationEntity() {
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

    public ObservationEntity getObservation() {
        return observation;
    }

    public void setObservation(ObservationEntity observation) {
        this.observation = observation;
    }

    public String getLocalizationType() {
        return localizationType;
    }

    public void setLocalizationType(String localizationType) {
        this.localizationType = localizationType;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public String getVertices() {
        return vertices;
    }

    public void setVertices(String vertices) {
        this.vertices = vertices;
    }

    public String getMaskEncoding() {
        return maskEncoding;
    }

    public void setMaskEncoding(String maskEncoding) {
        this.maskEncoding = maskEncoding;
    }

    public String getMaskData() {
        return maskData;
    }

    public void setMaskData(String maskData) {
        this.maskData = maskData;
    }

    public Integer getMaskWidthPixels() {
        return maskWidthPixels;
    }

    public void setMaskWidthPixels(Integer maskWidthPixels) {
        this.maskWidthPixels = maskWidthPixels;
    }

    public Integer getMaskHeightPixels() {
        return maskHeightPixels;
    }

    public void setMaskHeightPixels(Integer maskHeightPixels) {
        this.maskHeightPixels = maskHeightPixels;
    }

    public String getGenerator() {
        return generator;
    }

    public void setGenerator(String generator) {
        this.generator = generator;
    }

    public Float getConfidence() {
        return confidence;
    }

    public void setConfidence(Float confidence) {
        this.confidence = confidence;
    }

    public String getObserver() {
        return observer;
    }

    public void setObserver(String observer) {
        this.observer = observer;
    }

    public Instant getObservationTimestamp() {
        return observationTimestamp;
    }

    public void setObservationTimestamp(Instant observationTimestamp) {
        this.observationTimestamp = observationTimestamp;
    }

    public String getVerifier() {
        return verifier;
    }

    public void setVerifier(String verifier) {
        this.verifier = verifier;
    }

    public Instant getVerificationTimestamp() {
        return verificationTimestamp;
    }

    public void setVerificationTimestamp(Instant verificationTimestamp) {
        this.verificationTimestamp = verificationTimestamp;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getPart() {
        return part;
    }

    public void setPart(String part) {
        this.part = part;
    }

    public boolean isTraining() {
        return training;
    }

    public void setTraining(boolean training) {
        this.training = training;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public boolean isDifficult() {
        return difficult;
    }

    public void setDifficult(boolean difficult) {
        this.difficult = difficult;
    }

    @Override
    public String toString() {
        return "LocalizationEntity{" +
                "uuid=" + uuid +
                ", localizationType='" + localizationType + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                ", generator='" + generator + '\'' +
                '}';
    }

    /**
     * Equal when both have the same database id. Transient instances are only equal to themselves.
     * The hash code is constant so an instance keeps its bucket in a HashSet when its uuid is
     * assigned on persist (the observation's localizations collection is a Set).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocalizationEntity that = (LocalizationEntity) o;
        return uuid != null && uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
