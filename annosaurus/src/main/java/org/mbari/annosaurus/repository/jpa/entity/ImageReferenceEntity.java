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

import java.net.URL;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.Objects;
import org.hibernate.annotations.UuidGenerator;
import org.mbari.annosaurus.repository.jpa.TransactionLogger;
import org.mbari.annosaurus.repository.jpa.URLConverter;

@Entity(name = "ImageReference")
@Table(
        name = "image_references",
        indexes = {
                @Index(name = "idx_image_references__url", columnList = "url"),
                @Index(
                        name = "idx_image_references__imaged_moment_uuid",
                        columnList = "imaged_moment_uuid"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_image_references__url",
                        columnNames = {"url"}
                )
        }
)
@EntityListeners({TransactionLogger.class})
@NamedQueries(
        {
                @NamedQuery(
                        name = "ImageReference.findAll",
                        query = "SELECT r FROM ImageReference r ORDER BY r.url"
                ),
                @NamedQuery(
                        name = "ImageReference.findByImageName",
                        query = "SELECT r FROM ImageReference r WHERE TRIM(r.url) LIKE :name ORDER BY r.url"
                ),
                @NamedQuery(
                        name = "ImageReference.findByURL",
                        query = "SELECT r FROM ImageReference r WHERE r.url = :url ORDER BY r.url"
                ),
                @NamedQuery(
                        name = "ImageReference.findDTOByVideoReferenceUuid",
                        query =
                                "SELECT new org.mbari.annosaurus.repository.jpa.entity.ImageReferenceDTO(r.url, r.width, r.height, r.format, r.description, r.uuid, im.uuid) FROM ImageReference r LEFT JOIN ImagedMoment im WHERE im.videoReferenceUuid = :uuid ORDER BY r.url"
                ),
                @NamedQuery(
                        name = "ImageReference.findDTOByVideoReferenceUuidBetweenDates",
                        query =
                                "SELECT new org.mbari.annosaurus.repository.jpa.entity.ImageReferenceDTO(r.url, r.width, r.height, r.format, r.description, r.uuid, im.uuid) FROM ImageReference r JOIN ImagedMoment im WHERE im.videoReferenceUuid = :uuid AND im.recordedTimestamp BETWEEN :start AND :end ORDER BY r.url"
                ),
                @NamedQuery(
                        name = "ImageReference.findDTOByConcurrentRequest",
                        query =
                                "SELECT new org.mbari.annosaurus.repository.jpa.entity.ImageReferenceDTO(r.url, r.width, r.height, r.format, r.description, r.uuid, im.uuid) FROM ImageReference r JOIN ImagedMoment im WHERE im.videoReferenceUuid IN :uuids AND im.recordedTimestamp BETWEEN :start AND :end ORDER BY r.url"
                ),
                @NamedQuery(
                        name = "ImageReference.findDTOByMultiRequest",
                        query =
                                "SELECT new org.mbari.annosaurus.repository.jpa.entity.ImageReferenceDTO(r.url, r.width, r.height, r.format, r.description, r.uuid, im.uuid) FROM ImageReference r JOIN ImagedMoment im WHERE im.videoReferenceUuid IN :uuids ORDER BY r.url"
                ),
                @NamedQuery(
                        name = "ImageReference.findDTOByImagedMomentUuids",
                        query =
                                "SELECT new org.mbari.annosaurus.repository.jpa.entity.ImageReferenceDTO(r.url, r.width, r.height, r.format, r.description, r.uuid, im.uuid) FROM ImageReference r LEFT JOIN ImagedMoment im WHERE im.uuid IN :uuids ORDER BY r.url"
                )
        }
)
// @org.hibernate.envers.Audited
public class ImageReferenceEntity implements IPersistentObject {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "uuid", nullable = false, updatable = false)
    UUID uuid;

    /** Optimistic lock to prevent concurrent overwrites */
    @Version
    @Column(name = "last_updated_timestamp")
    protected Timestamp lastUpdatedTime;

    @Column(name = "description", length = 256, nullable = true)
    String description;

    @Column(name = "url", unique = true, length = 1024, nullable = false)
    @Convert(converter = URLConverter.class)
    URL url;

    @ManyToOne(
            cascade = {CascadeType.PERSIST, CascadeType.DETACH},
            optional = false
    )
    @JoinColumn(
            name = "imaged_moment_uuid",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_image_references__imaged_moment_uuid")
    )
    ImagedMomentEntity imagedMoment;

    @Column(name = "height_pixels", nullable = true)
    Integer height;

    @Column(name = "width_pixels", nullable = true)
    Integer width;

    @Column(name = "format", length = 64, nullable = true)
    String format;

    public ImageReferenceEntity() {
    }

    public ImageReferenceEntity(URL url) {
        this.url = url;
    }

    public ImageReferenceEntity(URL url, Integer height, Integer width, String format, String description) {
        this.url = url;
        this.description = description;
        this.height = height;
        this.width = width;
        this.format = format;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public ImagedMomentEntity getImagedMoment() {
        return imagedMoment;
    }

    public void setImagedMoment(ImagedMomentEntity imagedMoment) {
        this.imagedMoment = imagedMoment;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return "ImageReferenceEntity{" +
                "uuid=" + uuid +
                ", description='" + description + '\'' +
                ", url=" + url +
                ", height=" + height +
                ", width=" + width +
                ", format='" + format + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof ImageReferenceEntity)) return false;

        ImageReferenceEntity that = (ImageReferenceEntity) o;

        return Objects.equals(uuid, that.uuid) &&
                Objects.equals(url, that.url);
    }
}
