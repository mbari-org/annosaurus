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

package org.mbari.annosaurus.repository.jpa.entity

import com.google.gson.annotations.{Expose, SerializedName}
import jakarta.persistence._
import org.mbari.annosaurus.repository.jpa.{
    DurationConverter,
    InstantConverter,
    JpaEntity,
    TransactionLogger
}

import java.time.{Duration, Instant}
import java.util.{ArrayList => JArrayList, List => JList}
import scala.jdk.CollectionConverters._

/** @author
  *   Brian Schlining
  * @since 2016-06-16T14:12:00
  */
@Entity(name = "Observation")
@Table(
    name = "observations",
    indexes = Array(
        new Index(name = "idx_observations__concept", columnList = "concept"),
        new Index(name = "idx_observations__group", columnList = "observation_group"),
        new Index(name = "idx_observations__activity", columnList = "activity"),
        new Index(name = "idx_observations__imaged_moment_uuid", columnList = "imaged_moment_uuid")
    )
)
@EntityListeners(value = Array(classOf[TransactionLogger]))
@NamedNativeQueries(
    Array(
        new NamedNativeQuery(
            name = "Observation.findAllNames",
            query = "SELECT DISTINCT concept FROM observations ORDER BY concept"
        ),
        new NamedNativeQuery(
            name = "Observation.findAllGroups",
            query = "SELECT DISTINCT observation_group FROM observations ORDER BY observation_group"
        ),
        new NamedNativeQuery(
            name = "Observation.findAllNamesByVideoReferenceUUID",
            query =
                "SELECT DISTINCT concept FROM imaged_moments LEFT JOIN observations ON observations.imaged_moment_uuid = imaged_moments.uuid WHERE imaged_moments.video_reference_uuid = ?1 ORDER BY concept"
        ),
        new NamedNativeQuery(
            name = "Observation.findAllActivities",
            query = "SELECT DISTINCT activity FROM observations ORDER BY activity"
        ),
        new NamedNativeQuery(
            name = "Observation.countByVideoReferenceUUID",
            query =
                "SELECT COUNT(obs.uuid) FROM observations obs RIGHT JOIN imaged_moments im ON obs.imaged_moment_uuid = im.uuid " +
                    "WHERE im.video_reference_uuid = ?1"
        ),
        new NamedNativeQuery(
            name = "Observation.countByVideoReferenceUUIDAndTimestamps",
            query =
                "SELECT COUNT(obs.uuid) FROM observations obs RIGHT JOIN imaged_moments im ON obs.imaged_moment_uuid = im.uuid " +
                    "WHERE im.uuid = ?1 AND im.recorded_timestamp BETWEEN ?2 AND ?3"
        ),
        new NamedNativeQuery(
            name = "Observation.countAllByVideoReferenceUUIDs",
            query =
                "SELECT im.video_reference_uuid, COUNT(obs.uuid) as n FROM observations obs RIGHT JOIN imaged_moments im ON im.uuid = obs.imaged_moment_uuid GROUP BY im.video_reference_uuid ORDER BY n"
        ),
        new NamedNativeQuery(
            name = "Observation.countByConcept",
            query = "SELECT COUNT(*) FROM observations WHERE concept = ?1"
        ),
        new NamedNativeQuery(
            name = "Observation.countByConceptWithImages",
            query = "SELECT COUNT(*) FROM (" +
                "SELECT DISTINCT obs.uuid FROM observations obs " +
                "LEFT JOIN imaged_moments im ON obs.imaged_moment_uuid = im.uuid " +
                "LEFT JOIN image_references ir ON ir.imaged_moment_uuid = im.uuid " +
                "WHERE obs.uuid IS NOT NULL AND obs.concept = ?1 AND ir.url IS NOT NULL) foo"
        ),
        new NamedNativeQuery(
            name = "Observation.updateConcept",
            query = "UPDATE observations SET concept = ?1 WHERE concept = ?2"
        ),
        new NamedNativeQuery(
            name = "Observation.updateImagedMomentUUID",
            query = "UPDATE observations SET imaged_moment_uuid = ?1 WHERE uuid = ?2"
        )
    )
)
@NamedQueries(
    Array(
        new NamedQuery(
            name = "Observation.findAll",
            query = "SELECT o FROM Observation o ORDER BY o.uuid"
        ),
        new NamedQuery(
            name = "Observation.findByMultiRequest",
            query =
                "SELECT o FROM Observation o LEFT JOIN o.imagedMoment i WHERE i.videoReferenceUUID IN :uuids ORDER BY o.uuid"
        ),
        new NamedQuery(
            name = "Observation.countByMultiRequest",
            query =
                "SELECT COUNT(o.uuid) FROM Observation o LEFT JOIN o.imagedMoment i WHERE i.videoReferenceUUID IN :uuids"
        ),
        new NamedQuery(
            name = "Observation.findByConcurrentRequest",
            query =
                "SELECT o FROM Observation o LEFT JOIN o.imagedMoment i WHERE i.videoReferenceUUID IN :uuids AND i.recordedDate BETWEEN :start AND :end ORDER BY o.uuid"
        ),
        new NamedQuery(
            name = "Observation.countByConcurrentRequest",
            query =
                "SELECT COUNT(o.uuid) FROM Observation o LEFT JOIN o.imagedMoment i WHERE i.videoReferenceUUID IN :uuids AND i.recordedDate BETWEEN :start AND :end"
        ),
        new NamedQuery(
            name = "Observation.findByVideoReferenceUUID",
            query =
                "SELECT o FROM Observation o LEFT JOIN o.imagedMoment i WHERE i.videoReferenceUUID = :uuid ORDER BY o.uuid"
        ),
        new NamedQuery(
            name = "Observation.findByVideoReferenceUUIDAndTimestamps",
            query =
                "SELECT o FROM Observation o LEFT JOIN o.imagedMoment i WHERE i.videoReferenceUUID = :uuid AND i.recordedDate BETWEEN :start AND :end ORDER BY i.recordedDate"
        )
    )
)
class ObservationEntity extends JpaEntity {

    @Expose(serialize = true)
    @Column(name = "concept", length = 256)
    var concept: String = _

    @Expose(serialize = true)
    @SerializedName(value = "duration_millis")
    @Column(name = "duration_millis", nullable = true)
    @Convert(converter = classOf[DurationConverter])
    var duration: Duration = _

    @ManyToOne(
        cascade = Array(CascadeType.PERSIST, CascadeType.DETACH),
        optional = false,
        targetEntity = classOf[ImagedMomentEntity]
    )
    @JoinColumn(name = "imaged_moment_uuid", nullable = false, columnDefinition = "CHAR(36)")
    var imagedMoment: ImagedMomentEntity = _

    @Expose(serialize = true)
    @SerializedName(value = "observation_timestamp")
    @Column(name = "observation_timestamp", nullable = false)
    @Temporal(value = TemporalType.TIMESTAMP)
    @Convert(converter = classOf[InstantConverter])
    var observationDate: Instant = Instant.now()

    @Expose(serialize = true)
    @Column(name = "observer", length = 128, nullable = true)
    var observer: String = _

    @Expose(serialize = true)
    @Column(name = "observation_group", nullable = true, length = 128)
    var group: String = _

    @Expose(serialize = true)
    @Column(name = "activity", nullable = true, length = 128)
    var activity: String = _

    @Expose(serialize = true)
    @SerializedName(value = "associations")
    @OneToMany(
        targetEntity = classOf[AssociationEntity],
        cascade = Array(CascadeType.ALL),
        fetch = FetchType.EAGER,
        mappedBy = "observation"
    )
    protected var javaAssociations: JList[AssociationEntity] = new JArrayList[AssociationEntity]

    def addAssociation(association: AssociationEntity): Unit = {
        javaAssociations.add(association.asInstanceOf[AssociationEntity])
        association.observation = this
    }

    def removeAssociation(association: AssociationEntity): Unit = {
        javaAssociations.remove(association)
        association.observation = null
    }

    def associations: Iterable[AssociationEntity] = javaAssociations.asScala

}

object ObservationEntity {

    def apply(
        concept: String,
        duration: Option[Duration] = None,
        observationDate: Option[Instant] = None,
        observer: Option[String] = None,
        group: Option[String] = None,
        activity: Option[String] = None
    ): ObservationEntity = {
        val obs = new ObservationEntity
        obs.concept = concept
        duration.foreach(obs.duration = _)
        observationDate.foreach(obs.observationDate = _)
        observer.foreach(obs.observer = _)
        group.foreach(obs.group = _)
        activity.foreach(obs.activity = _)
        obs
    }

    def apply(observation: ObservationEntity): ObservationEntity = {
        val newObservation = apply(
            observation.concept,
            Option(observation.duration),
            Option(observation.observationDate),
            Option(observation.observer),
            Option(observation.group),
            Option(observation.activity)
        )
        newObservation.uuid = observation.uuid
        observation.associations.foreach(a => newObservation.addAssociation(AssociationEntity(a)))
        newObservation
    }

}
