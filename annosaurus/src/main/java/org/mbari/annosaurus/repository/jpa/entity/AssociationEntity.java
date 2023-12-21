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
import org.mbari.annosaurus.repository.jpa.TransactionLogger;

import java.sql.Timestamp;
import java.util.UUID;

@Entity(name = "Association")
@Table(
        name = "associations",
        indexes = {
                @Index(name = "idx_associations__link_name", columnList = "link_name"),
                @Index(name = "idx_associations__link_value", columnList = "link_value"),
                @Index(name = "idx_associations__to_concept", columnList = "to_concept"),
                @Index(name = "idx_associations__observation_uuid", columnList = "observation_uuid")
        }
)
@EntityListeners({TransactionLogger.class})
@NamedNativeQueries(
        {
                @NamedNativeQuery(
                        name = "Association.findAllToConcepts",
                        query = "SELECT DISTINCT to_concept FROM associations ORDER BY to_concept"
                ),
                @NamedNativeQuery(
                        name = "Association.countByToConcept",
                        query = "SELECT COUNT(*) FROM associations WHERE to_concept = ?1"
                ),
                @NamedNativeQuery(
                        name = "Association.findByLinkNameAndVideoReference",
                        query = "SELECT o.concept, a.uuid, link_name, to_concept, link_value, mime_type " +
                                "FROM associations a LEFT JOIN observations o ON a.observation_uuid = o.uuid LEFT JOIN " +
                                "imaged_moments i ON o.imaged_moment_uuid = i.uuid WHERE i.video_reference_uuid = ?1 AND a.link_name = ?2"
                ),
                @NamedNativeQuery(
                        name = "Association.updateToConcept",
                        query = "UPDATE associations SET to_concept = ?1 WHERE to_concept = ?2"
                )
        }
)
@NamedQueries(
        {
                @NamedQuery(
                        name = "Association.findAll",
                        query = "SELECT a FROM Association a ORDER BY a.uuid"
                ),
                @NamedQuery(
                        name = "Association.findByLinkName",
                        query = "SELECT a FROM Association a WHERE a.linkName = :linkName ORDER BY a.uuid"
                ),
                @NamedQuery(
                        name = "Association.findByLinkNameAndVideoReferenceUUID",
                        query = "SELECT a FROM Association a INNER JOIN FETCH a.observation o INNER JOIN FETCH o.imagedMoment im WHERE im.videoReferenceUUID = :videoReferenceUuid AND a.linkName = :linkName ORDER BY a.uuid"
                ),
                 @NamedQuery(
                     name = "Association.findByConceptAssociationRequest",
                     query = "SELECT new org.mbari.annosaurus.repository.jpa.entity.ConceptAssociationDTO(a.linkName, a.toConcept, a.linkValue, a.mimeType, a.uuid, o.concept, im.videoReferenceUUID) FROM Association a RIGHT JOIN a.observation o RIGHT JOIN o.imagedMoment im WHERE im.videoReferenceUUID IN :uuids AND a.linkName = :linkName ORDER BY a.uuid"
                 ),
                 @NamedQuery(
                     name = "Association.findDTOByVideoReferenceUuid",
                     query = "SELECT new org.mbari.annosaurus.repository.jpa.entity.AssociationDTO(a.linkName, a.toConcept, a.linkValue, a.mimeType, a.uuid, o.uuid, im.uuid) FROM Association a RIGHT JOIN a.observation o RIGHT JOIN o.imagedMoment im WHERE im.videoReferenceUUID = :videoReferenceUuid AND a.linkName = :linkName ORDER BY a.uuid"
                 ),
                 @NamedQuery(
                     name = "Association.findDTOByVideoReferenceUuidBetweenDates",
                     query = "SELECT new org.mbari.annosaurus.repository.jpa.entity.AssociationDTO(a.linkName, a.toConcept, a.linkValue, a.mimeType, a.uuid, o.uuid, im.uuid) FROM Association a RIGHT JOIN a.observation o RIGHT JOIN o.imagedMoment im WHERE im.videoReferenceUUID = :videoReferenceUuid AND im.recordedTimestamp BETWEEN :start AND :end ORDER BY a.uuid"
                 ),
                 @NamedQuery(
                     name = "Association.findDTOByConcurrentRequest",
                     query = "SELECT new org.mbari.annosaurus.repository.jpa.entity.AssociationDTO(a.linkName, a.toConcept, a.linkValue, a.mimeType, a.uuid, o.uuid, im.uuid) FROM Association a RIGHT JOIN a.observation o RIGHT JOIN o.imagedMoment im WHERE im.videoReferenceUUID IN :uuids AND im.recordedTimestamp BETWEEN :start AND :end ORDER BY a.uuid"
                 ),
                 @NamedQuery(
                     name = "Association.findCDTOByMultiRequest",
                     query = "SELECT new org.mbari.annosaurus.repository.jpa.entity.AssociationDTO(a.linkName, a.toConcept, a.linkValue, a.mimeType, a.uuid, o.uuid, im.uuid) FROM Association a RIGHT JOIN a.observation o RIGHT JOIN o.imagedMoment im WHERE im.videoReferenceUUID IN :uuids ORDER BY a.uuid"
                 ),
                 @NamedQuery(
                     name = "Association.findDTOByObservationUuids",
                     query = "SELECT new org.mbari.annosaurus.repository.jpa.entity.AssociationDTO(a.linkName, a.toConcept, a.linkValue, a.mimeType, a.uuid, o.uuid, im.uuid) FROM Association a RIGHT JOIN a.observation o RIGHT JOIN o.imagedMoment im WHERE o.uuid IN :uuids ORDER BY a.uuid"
                 ),
                 @NamedQuery(
                     name = "Association.findDTOByLinkNameAndValue",
                     query = "SELECT new org.mbari.annosaurus.repository.jpa.entity.AssociationDTO(a.linkName, a.toConcept, a.linkValue, a.mimeType, a.uuid, o.uuid, im.uuid) FROM Association a RIGHT JOIN a.observation o RIGHT JOIN o.imagedMoment im WHERE a.linkName = :linkName AND a.linkValue = :linkValue ORDER BY a.uuid"
                 )

        }
)
@org.hibernate.envers.Audited
public class AssociationEntity implements IPersistentObject {

    @Id
    @Column(name = "uuid", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID uuid;

    /** Optimistic lock to prevent concurrent overwrites */
    @Version
    @Column(name = "last_updated_time")
    protected Timestamp lastUpdatedTime;

    @Column(name = "link_name", length = 128, nullable = false)
    String linkName;

    @Column(name = "link_value", length = 1024, nullable = true)
    String linkValue;

    @ManyToOne(
            cascade = {CascadeType.PERSIST, CascadeType.DETACH},
            optional = false,
            targetEntity = ObservationEntity.class
    )
    @JoinColumn(name = "observation_uuid", nullable = false)
    ObservationEntity observation;

    @Column(name = "to_concept", length = 128, nullable = true)
    String toConcept;

    /** The mime-type of the linkValue
     */
    @Column(name = "mime_type", length = 64, nullable = false)
    String mimeType = "text/plain";

    public AssociationEntity() {

    }

    public AssociationEntity(String linkName, String toConcept, String linkValue, String mimeType) {
        this.linkName = linkName;
        this.toConcept = toConcept;
        this.linkValue = linkValue;
        this.mimeType = mimeType;
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

    public String getLinkName() {
        return linkName;
    }

    public void setLinkName(String linkName) {
        this.linkName = linkName;
    }

    public String getLinkValue() {
        return linkValue;
    }

    public void setLinkValue(String linkValue) {
        this.linkValue = linkValue;
    }

    public ObservationEntity getObservation() {
        return observation;
    }

    public void setObservation(ObservationEntity observation) {
        this.observation = observation;
    }

    public String getToConcept() {
        return toConcept;
    }

    public void setToConcept(String toConcept) {
        this.toConcept = toConcept;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public String toString() {
        return "AssociationEntity{" +
                "uuid=" + uuid +
                ", linkName='" + linkName + '\'' +
                ", linkValue='" + linkValue + '\'' +
                ", toConcept='" + toConcept + '\'' +
                ", mimeType='" + mimeType + '\'' +
                '}';
    }
}
