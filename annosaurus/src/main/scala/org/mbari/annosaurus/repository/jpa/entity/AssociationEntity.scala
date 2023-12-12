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

import com.google.gson.annotations.Expose
import jakarta.persistence._
import org.mbari.annosaurus.model._
import org.mbari.annosaurus.repository.jpa.{JPAPersistentObject, TransactionLogger}

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-06-17T11:36:00
  */
@Entity(name = "Association")
@Table(
  name = "associations",
  indexes = Array(
    new Index(name = "idx_associations__link_name", columnList = "link_name"),
    new Index(name = "idx_associations__link_value", columnList = "link_value"),
    new Index(name = "idx_associations__to_concept", columnList = "to_concept"),
    new Index(name = "idx_associations__observation_uuid", columnList = "observation_uuid")
  )
)
@EntityListeners(value = Array(classOf[TransactionLogger]))
@NamedNativeQueries(
  Array(
    new NamedNativeQuery(
      name = "Association.findAllToConcepts",
      query = "SELECT DISTINCT to_concept FROM associations ORDER BY to_concept"
    ),
    new NamedNativeQuery(
      name = "Association.countByToConcept",
      query = "SELECT COUNT(*) FROM associations WHERE to_concept = ?1"
    ),
    new NamedNativeQuery(
      name = "Association.findByLinkNameAndVideoReference",
      query = "SELECT o.concept, a.uuid, link_name, to_concept, link_value, mime_type " +
        "FROM associations a LEFT JOIN observations o ON a.observation_uuid = o.uuid LEFT JOIN " +
        "imaged_moments i ON o.imaged_moment_uuid = i.uuid WHERE i.video_reference_uuid = ?1 AND a.link_name = ?2"
    ),
    new NamedNativeQuery(
      name = "Association.updateToConcept",
      query = "UPDATE associations SET to_concept = ?1 WHERE to_concept = ?2"
    )
  )
)
@NamedQueries(
  Array(
    new NamedQuery(
      name = "Association.findAll",
      query = "SELECT a FROM Association a ORDER BY a.uuid"
    ),
    new NamedQuery(
      name = "Association.findByLinkName",
      query = "SELECT a FROM Association a WHERE a.linkName = :linkName ORDER BY a.uuid"
    ),
    new NamedQuery(
      name = "Association.findByLinkNameAndVideoReferenceUUID",
      query = "SELECT a FROM Association a INNER JOIN FETCH a.observation o " +
        "INNER JOIN FETCH o.imagedMoment im WHERE im.videoReferenceUUID = :videoReferenceUuid " +
        "AND a.linkName = :linkName ORDER BY a.uuid"
    )
  )
)
class AssociationEntity extends MutableAssociation with JPAPersistentObject {

  @Expose(serialize = true)
  @Column(name = "link_name", length = 128, nullable = false)
  var linkName: String = _

  @Expose(serialize = true)
  @Column(name = "link_value", length = 1024, nullable = true)
  var linkValue: String = _

  @Expose(serialize = false)
  @ManyToOne(
    cascade = Array(CascadeType.PERSIST, CascadeType.DETACH),
    optional = false,
    targetEntity = classOf[ObservationEntity]
  )
  @JoinColumn(name = "observation_uuid", nullable = false, columnDefinition = "CHAR(36)")
  var observation: MutableObservation = _

  @Expose(serialize = true)
  @Column(name = "to_concept", length = 128, nullable = true)
  var toConcept: String = _

  /**
    * The mime-type of the linkValue
    */
  @Expose(serialize = true)
  @Column(name = "mime_type", length = 64, nullable = false)
  var mimeType: String = "text/plain"

  override def toString: String = MutableAssociation.asString(this)

}

object AssociationEntity {

  def apply(linkName: String, toConcept: String, linkValue: String): AssociationEntity = {
    val a = new AssociationEntity
    a.linkName = linkName
    a.toConcept = toConcept
    a.linkValue = linkValue
    a
  }

  def apply(
      linkName: String,
      toConcept: String,
      linkValue: String,
      mimetype: String
  ): AssociationEntity = {
    val a = new AssociationEntity
    a.linkName = linkName
    a.toConcept = toConcept
    a.linkValue = linkValue
    a.mimeType = mimetype
    a
  }

  def apply(
      linkName: String,
      toConcept: Option[String] = None,
      linkValue: Option[String] = None
  ): AssociationEntity = {
    val a = new AssociationEntity
    a.linkName = linkName
    toConcept.foreach(a.toConcept = _)
    linkValue.foreach(a.linkValue = _)
    a
  }

  def apply(v: MutableAssociation): AssociationEntity = {
    val a = new AssociationEntity
    a.linkName = v.linkName
    a.toConcept = v.toConcept
    a.linkValue = v.linkValue
    a.mimeType = v.mimeType
    a.uuid = v.uuid
    a
  }

}
