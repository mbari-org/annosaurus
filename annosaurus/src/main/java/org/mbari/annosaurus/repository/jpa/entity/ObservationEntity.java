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
import org.mbari.annosaurus.repository.jpa.TransactionLogger;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity(name = "Observation")
@Table(
        name = "observations",
        indexes = {
                @Index(name = "idx_observations__concept", columnList = "concept"),
                @Index(name = "idx_observations__group", columnList = "observation_group"),
                @Index(name = "idx_observations__activity", columnList = "activity"),
                @Index(name = "idx_observations__imaged_moment_uuid", columnList = "imaged_moment_uuid")
        }
)
@EntityListeners({TransactionLogger.class})
@NamedNativeQueries(
        {
                @NamedNativeQuery(
                        name = "Observation.findAllNames",
                        query = "SELECT DISTINCT concept FROM observations ORDER BY concept"
                ),
                @NamedNativeQuery(
                        name = "Observation.findAllGroups",
                        query = "SELECT DISTINCT observation_group FROM observations ORDER BY observation_group"
                ),
                @NamedNativeQuery(
                        name = "Observation.findAllNamesByVideoReferenceUUID",
                        query =
                                "SELECT DISTINCT concept FROM imaged_moments LEFT JOIN observations ON observations.imaged_moment_uuid = imaged_moments.uuid WHERE imaged_moments.video_reference_uuid = ?1 ORDER BY concept"
                ),
                @NamedNativeQuery(
                        name = "Observation.findAllActivities",
                        query = "SELECT DISTINCT activity FROM observations ORDER BY activity"
                ),
                @NamedNativeQuery(
                        name = "Observation.countByVideoReferenceUUID",
                        query =
                                "SELECT COUNT(obs.uuid) FROM observations obs RIGHT JOIN imaged_moments im ON obs.imaged_moment_uuid = im.uuid " +
                                        "WHERE im.video_reference_uuid = ?1"
                ),
                @NamedNativeQuery(
                        name = "Observation.countByVideoReferenceUUIDAndTimestamps",
                        query =
                                "SELECT COUNT(obs.uuid) FROM observations obs RIGHT JOIN imaged_moments im ON obs.imaged_moment_uuid = im.uuid " +
                                        "WHERE im.uuid = ?1 AND im.recorded_timestamp BETWEEN ?2 AND ?3"
                ),
                @NamedNativeQuery(
                        name = "Observation.countAllByVideoReferenceUUIDs",
                        query =
                                "SELECT im.video_reference_uuid, COUNT(obs.uuid) as n FROM observations obs RIGHT JOIN imaged_moments im ON im.uuid = obs.imaged_moment_uuid GROUP BY im.video_reference_uuid ORDER BY n"
                ),
                @NamedNativeQuery(
                        name = "Observation.countByConcept",
                        query = "SELECT COUNT(*) FROM observations WHERE concept = ?1"
                ),
                @NamedNativeQuery(
                        name = "Observation.countByConceptWithImages",
                        query = "SELECT COUNT(*) FROM (" +
                                "SELECT DISTINCT obs.uuid FROM observations obs " +
                                "LEFT JOIN imaged_moments im ON obs.imaged_moment_uuid = im.uuid " +
                                "LEFT JOIN image_references ir ON ir.imaged_moment_uuid = im.uuid " +
                                "WHERE obs.uuid IS NOT NULL AND obs.concept = ?1 AND ir.url IS NOT NULL) foo"
                ),
                @NamedNativeQuery(
                        name = "Observation.updateConcept",
                        query = "UPDATE observations SET concept = ?1 WHERE concept = ?2"
                ),
                @NamedNativeQuery(
                        name = "Observation.updateImagedMomentUUID",
                        query = "UPDATE observations SET imaged_moment_uuid = ?1 WHERE uuid = ?2"
                )
        }
)
@NamedQueries(
        {
                @NamedQuery(
                        name = "Observation.findAll",
                        query = "SELECT o FROM Observation o ORDER BY o.uuid"
                ),
                @NamedQuery(
                        name = "Observation.findByMultiRequest",
                        query =
                                "SELECT o FROM Observation o LEFT JOIN o.imagedMoment i WHERE i.videoReferenceUuid IN :uuids ORDER BY o.uuid"
                ),
                @NamedQuery(
                        name = "Observation.countByMultiRequest",
                        query =
                                "SELECT COUNT(o.uuid) FROM Observation o LEFT JOIN o.imagedMoment i WHERE i.videoReferenceUuid IN :uuids"
                ),
                @NamedQuery(
                        name = "Observation.findByConcurrentRequest",
                        query =
                                "SELECT o FROM Observation o LEFT JOIN o.imagedMoment i WHERE i.videoReferenceUuid IN :uuids AND i.recordedTimestamp BETWEEN :start AND :end ORDER BY o.uuid"
                ),
                @NamedQuery(
                        name = "Observation.countByConcurrentRequest",
                        query =
                                "SELECT COUNT(o.uuid) FROM Observation o LEFT JOIN o.imagedMoment i WHERE i.videoReferenceUuid IN :uuids AND i.recordedTimestamp BETWEEN :start AND :end"
                ),
                @NamedQuery(
                        name = "Observation.findByVideoReferenceUUID",
                        query =
                                "SELECT o FROM Observation o LEFT JOIN o.imagedMoment i WHERE i.videoReferenceUuid = :uuid ORDER BY o.uuid"
                ),
                @NamedQuery(
                        name = "Observation.findByVideoReferenceUUIDAndTimestamps",
                        query =
                                "SELECT o FROM Observation o LEFT JOIN o.imagedMoment i WHERE i.videoReferenceUuid = :uuid AND i.recordedTimestamp BETWEEN :start AND :end ORDER BY i.recordedTimestamp"
                )
        }
)
// @org.hibernate.envers.Audited
public class ObservationEntity implements IPersistentObject {

    @Id
    @Column(name = "uuid", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID uuid;

    /** Optimistic lock to prevent concurrent overwrites */
    @Version
    @Column(name = "last_updated_time")
    protected Timestamp lastUpdatedTime;

    @Column(name = "concept", length = 256)
    String concept;

    @Column(name = "duration_millis", nullable = true)
    @Convert(converter = DurationConverter.class)
    Duration duration;

//     @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(
            cascade = {CascadeType.PERSIST, CascadeType.DETACH},
            optional = false
    )
    @JoinColumn(name = "imaged_moment_uuid", nullable = false, foreignKey = @ForeignKey(name = "fk_observations__imaged_moment_uuid"))
    ImagedMomentEntity imagedMoment;

    @Column(name = "observation_timestamp", nullable = false)
    @Temporal(value = TemporalType.TIMESTAMP)
    Instant observationTimestamp = Instant.now();

    @Column(name = "observer", length = 128, nullable = true)
    String observer;

    @Column(name = "observation_group", nullable = true, length = 128)
    String group;

    @Column(name = "activity", nullable = true, length = 128)
    String activity;

    @OneToMany(
            targetEntity = AssociationEntity.class,
            cascade = {CascadeType.ALL},
            fetch = FetchType.LAZY,
            mappedBy = "observation"
    )
    Set<AssociationEntity> associations = new HashSet<>();

    public ObservationEntity() {
    }

    public ObservationEntity(String concept, Duration duration, Instant observationTimestamp, String observer, String group, String activity) {
        this.concept = concept;
        this.duration = duration;
        this.observationTimestamp = observationTimestamp;
        this.observer = observer;
        this.group = group;
        this.activity = activity;
    }

    public ObservationEntity(String concept, String observer) {
        this.concept = concept;
        this.observer = observer;
        this.observationTimestamp = Instant.now();
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

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public ImagedMomentEntity getImagedMoment() {
        return imagedMoment;
    }

    public void setImagedMoment(ImagedMomentEntity imagedMoment) {
        this.imagedMoment = imagedMoment;
    }

    public Instant getObservationTimestamp() {
        return observationTimestamp;
    }

    public void setObservationTimestamp(Instant observationTimestamp) {
        this.observationTimestamp = observationTimestamp;
    }

    public String getObserver() {
        return observer;
    }

    public void setObserver(String observer) {
        this.observer = observer;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public Set<AssociationEntity> getAssociations() {
        return associations;
    }

    public void setAssociations(Set<AssociationEntity> associations) {
        this.associations = associations;
    }

    public void addAssociation(AssociationEntity ass) {
        associations.add(ass);
        ass.setObservation(this);
    }

    public void removeAssociation(AssociationEntity ass) {
        associations.remove(ass);
        ass.setObservation(null);
    }

    @Override
    public String toString() {
        return "ObservationEntity2{" +
                "uuid=" + uuid +
                ", concept='" + concept + '\'' +
                ", duration=" + duration +
                ", imagedMoment=" + imagedMoment +
                ", observationDate=" + observationTimestamp +
                ", observer='" + observer + '\'' +
                ", group='" + group + '\'' +
                ", activity='" + activity + '\'' +
                '}';
    }
}
