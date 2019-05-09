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

package org.mbari.vars.annotation.dao.jpa

import java.time.{Duration, Instant}

import javax.persistence._
import java.util.{ArrayList => JArrayList, List => JList}

import com.google.gson.annotations.{Expose, SerializedName}
//import org.eclipse.persistence.annotations.{BatchFetch, BatchFetchType}

import scala.collection.JavaConverters._
import org.mbari.vars.annotation.model.{Association, ImagedMoment, Observation}

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-16T14:12:00
 */
@Entity(name = "Observation")
@Table(name = "observations", indexes = Array(
  new Index(name = "idx_observations__concept", columnList = "concept"),
  new Index(name = "idx_observations__group", columnList = "observation_group"),
  new Index(name = "idx_observations__activity", columnList = "activity"),
  new Index(name = "idx_observations__imaged_moment_uuid", columnList = "imaged_moment_uuid")))
@EntityListeners(value = Array(classOf[TransactionLogger]))
@NamedNativeQueries(Array(
  new NamedNativeQuery(
    name = "Observation.findAllNames",
    query = "SELECT DISTINCT concept FROM observations ORDER BY concept"),
  new NamedNativeQuery(
    name = "Observation.findAllGroups",
    query = "SELECT DISTINCT observation_group FROM observations ORDER BY observation_group"),
  new NamedNativeQuery(
    name = "Observation.findAllNamesByVideoReferenceUUID",
    query = "SELECT DISTINCT concept FROM imaged_moments LEFT JOIN observations ON observations.imaged_moment_uuid = imaged_moments.uuid WHERE imaged_moments.video_reference_uuid = ?1 ORDER BY concept"),
  new NamedNativeQuery(
    name = "Observation.findAllActivities",
    query = "SELECT DISTINCT activity FROM observations ORDER BY activity"),
  new NamedNativeQuery(
    name = "Observation.countByVideoReferenceUUID",
    query = "SELECT COUNT(uuid) FROM observations WHERE imaged_moment_uuid IN " +
      "(SELECT DISTINCT im.uuid FROM imaged_moments im LEFT JOIN observations obs " +
      "ON obs.imaged_moment_uuid = im.uuid WHERE " +
      "im.video_reference_uuid = ?1)"),
  new NamedNativeQuery(
    name = "Observation.countAllByVideoReferenceUUIDs",
    query = "SELECT im.video_reference_uuid, COUNT(obs.uuid) as n FROM observations obs RIGHT JOIN imaged_moments im ON im.uuid = obs.imaged_moment_uuid GROUP BY im.video_reference_uuid ORDER BY n"),
  new NamedNativeQuery(
    name = "Observation.countByConcept",
    query = "SELECT COUNT(*) FROM observations WHERE concept = ?1"),
  new NamedNativeQuery(
    name = "Observation.updateConcept",
    query = "UPDATE observations SET concept = ?1 WHERE concept = ?2"),
  new NamedNativeQuery(
    name = "Observation.updateImagedMomentUUID",
    query = "UPDATE observations SET imaged_moment_uuid = ?1 WHERE uuid = ?2")))
@NamedQueries(Array(
  new NamedQuery(
    name = "Observation.findAll",
    query = "SELECT o FROM Observation o ORDER BY o.uuid"),
  new NamedQuery(
    name = "Observation.findByVideoReferenceUUID",
    query = "SELECT o FROM Observation o JOIN o.imagedMoment i WHERE i.videoReferenceUUID = :uuid ORDER BY o.uuid")))
class ObservationImpl extends Observation with JPAPersistentObject {

  @Expose(serialize = true)
  @Column(
    name = "concept",
    length = 256)
  override var concept: String = _

  @Expose(serialize = true)
  @SerializedName(value = "duration_millis")
  @Column(
    name = "duration_millis",
    nullable = true)
  @Convert(converter = classOf[DurationConverter])
  override var duration: Duration = _

  @ManyToOne(
    cascade = Array(CascadeType.PERSIST, CascadeType.DETACH),
    optional = false,
    targetEntity = classOf[ImagedMomentImpl])
  @JoinColumn(
    name = "imaged_moment_uuid",
    nullable = false,
    columnDefinition = "CHAR(36)")
  override var imagedMoment: ImagedMoment = _

  @Expose(serialize = true)
  @SerializedName(value = "observation_timestamp")
  @Column(
    name = "observation_timestamp",
    nullable = false)
  @Temporal(value = TemporalType.TIMESTAMP)
  @Convert(converter = classOf[InstantConverter])
  override var observationDate: Instant = Instant.now()

  @Expose(serialize = true)
  @Column(
    name = "observer",
    length = 128,
    nullable = true)
  override var observer: String = _

  @Expose(serialize = true)
  @Column(
    name = "observation_group",
    nullable = true,
    length = 128)
  override var group: String = _

  @Expose(serialize = true)
  @Column(
    name = "activity",
    nullable = true,
    length = 128)
  override var activity: String = _

  @Expose(serialize = true)
  @SerializedName(value = "associations")
  @OneToMany(
    targetEntity = classOf[AssociationImpl],
    cascade = Array(CascadeType.ALL),
    fetch = FetchType.EAGER,
    mappedBy = "observation",
    orphanRemoval = true)
//  @BatchFetch(value = BatchFetchType.JOIN)
  protected var javaAssociations: JList[AssociationImpl] = new JArrayList[AssociationImpl]

  override def addAssociation(association: Association): Unit = {
    javaAssociations.add(association.asInstanceOf[AssociationImpl])
    association.observation = this
  }

  override def removeAssociation(association: Association): Unit = {
    javaAssociations.remove(association)
    association.observation = null
  }

  override def associations: Iterable[Association] = javaAssociations.asScala

}

object ObservationImpl {

  def apply(
    concept: String,
    duration: Option[Duration] = None,
    observationDate: Option[Instant] = None,
    observer: Option[String] = None,
    group: Option[String] = None,
    activity: Option[String] = None): ObservationImpl = {
    val obs = new ObservationImpl
    obs.concept = concept
    duration.foreach(obs.duration = _)
    observationDate.foreach(obs.observationDate = _)
    observer.foreach(obs.observer = _)
    group.foreach(obs.group = _)
    activity.foreach(obs.activity = _)
    obs
  }

}
