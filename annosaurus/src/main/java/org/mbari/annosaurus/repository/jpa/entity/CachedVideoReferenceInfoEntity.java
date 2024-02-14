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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.mbari.annosaurus.repository.jpa.TransactionLogger;
import org.mbari.annosaurus.repository.jpa.UUIDConverter;

import java.sql.Timestamp;
import java.util.UUID;

@Entity(name = "CachedVideoReferenceInfo")
@Table(
        name = "video_reference_information",
        indexes = {
                @Index(
                        name = "idx_video_reference_information__video_reference_uuid",
                        columnList = "video_reference_uuid"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_video_reference_information__video_reference_uuid",
                        columnNames = {"video_reference_uuid"}
                )
        }
)
@EntityListeners({TransactionLogger.class})
@NamedNativeQueries(
        {
                @NamedNativeQuery(
                        name = "VideoReferenceInfo.findAllVideoReferenceUUIDs",
                        query =
                                "SELECT DISTINCT video_reference_uuid FROM video_reference_information ORDER BY video_reference_uuid ASC"
                ),
                @NamedNativeQuery(
                        name = "VideoReferenceInfo.findAllMissionContacts",
                        query =
                                "SELECT DISTINCT mission_contact FROM video_reference_information ORDER BY mission_contact ASC"
                ),
                @NamedNativeQuery(
                        name = "VideoReferenceInfo.findAllPlatformNames",
                        query =
                                "SELECT DISTINCT platform_name FROM video_reference_information ORDER BY platform_name ASC"
                ),
                @NamedNativeQuery(
                        name = "VideoReferenceInfo.findAllMissionIDs",
                        query =
                                "SELECT DISTINCT mission_id FROM video_reference_information ORDER BY mission_id ASC"
                )
        }
)
@NamedQueries(
        {
                @NamedQuery(
                        name = "VideoReferenceInfo.findAll",
                        query = "SELECT v FROM CachedVideoReferenceInfo v ORDER BY v.uuid"
                ),
                @NamedQuery(
                        name = "VideoReferenceInfo.findByVideoReferenceUUID",
                        query =
                                "SELECT v FROM CachedVideoReferenceInfo v WHERE v.videoReferenceUuid = :uuid ORDER BY v.uuid"
                ),
                @NamedQuery(
                        name = "VideoReferenceInfo.findByPlatformName",
                        query =
                                "SELECT v FROM CachedVideoReferenceInfo v WHERE v.platformName = :name ORDER BY v.uuid"
                ),
                @NamedQuery(
                        name = "VideoReferenceInfo.findByMissionID",
                        query =
                                "SELECT v FROM CachedVideoReferenceInfo v WHERE v.missionId = :id ORDER BY v.uuid"
                ),
                @NamedQuery(
                        name = "VideoReferenceInfo.findByMissionContact",
                        query =
                                "SELECT v FROM CachedVideoReferenceInfo v WHERE v.missionContact = :contact ORDER BY v.uuid"
                )
        }
)
public class CachedVideoReferenceInfoEntity implements IPersistentObject {

    @Id
    @Column(name = "uuid", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID uuid;

    /** Optimistic lock to prevent concurrent overwrites */
    @Version
    @Column(name = "last_updated_timestamp")
    protected Timestamp lastUpdatedTime;

    /** typically this will be the chief scientist
     */
    @Column(name = "mission_contact", nullable = true, length = 64)
    String missionContact;

    @Column(name = "platform_name", nullable = false, length = 64)
    String platformName;

    @Basic(optional = false)
    @Column(
            name = "video_reference_uuid",
            unique = true
    )
    @JdbcTypeCode(SqlTypes.UUID)
    UUID videoReferenceUuid;

    @Column(name = "mission_id", nullable = false, length = 256)
    String missionId;

    public CachedVideoReferenceInfoEntity() {}

    public CachedVideoReferenceInfoEntity(UUID videoReferenceUuid,
                                          String missionId,
                                          String platformName,
                                          String missionContact) {
        this.videoReferenceUuid = videoReferenceUuid;
        this.missionId = missionId;
        this.platformName = platformName;
        this.missionContact = missionContact;
    }

    public CachedVideoReferenceInfoEntity(CachedVideoReferenceInfoEntity that) {
        this.videoReferenceUuid = that.videoReferenceUuid;
        this.missionId = that.missionId;
        this.platformName = that.platformName;
        this.missionContact = that.missionContact;
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
    
    public String getMissionContact() {
        return missionContact;
    }

    public void setMissionContact(String missionContact) {
        this.missionContact = missionContact;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public UUID getVideoReferenceUuid() {
        return videoReferenceUuid;
    }

    public void setVideoReferenceUuid(UUID videoReferenceUuid) {
        this.videoReferenceUuid = videoReferenceUuid;
    }

    public String getMissionId() {
        return missionId;
    }

    public void setMissionId(String missionId) {
        this.missionId = missionId;
    }
}
