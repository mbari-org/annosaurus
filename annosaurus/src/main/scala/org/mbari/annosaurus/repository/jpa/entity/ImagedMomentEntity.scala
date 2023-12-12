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
import org.mbari.annosaurus.Constants
import org.mbari.annosaurus.model._
import org.mbari.annosaurus.repository.jpa._
import org.mbari.vcr4j.time.Timecode

import java.time.{Duration, Instant}
import java.util.{UUID, ArrayList => JArrayList, List => JList}
import scala.collection.mutable
import scala.jdk.CollectionConverters._

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-06-16T14:12:00
  */
@Entity(name = "ImagedMoment")
@Table(
  name = "imaged_moments",
  indexes = Array(
    new Index(
      name = "idx_imaged_moments__video_reference_uuid",
      columnList = "video_reference_uuid"
    ),
    new Index(
      name = "idx_imaged_moments__recorded_timestamp",
      columnList = "recorded_timestamp"
    ),
    new Index(
      name = "idx_imaged_moments__elapsed_time",
      columnList = "elapsed_time_millis"
    ),
    new Index(name = "idx_imaged_moments__timecode", columnList = "timecode")
  )
)
@EntityListeners(value = Array(classOf[TransactionLogger]))
@NamedNativeQueries(
  Array(
    new NamedNativeQuery(
      name = "ImagedMoment.findAllVideoReferenceUUIDs",
      query =
        "SELECT DISTINCT video_reference_uuid FROM imaged_moments ORDER BY video_reference_uuid ASC"
    ),
    new NamedNativeQuery(
      name = "ImagedMoment.findVideoReferenceUUIDsModifiedBetweenDates",
      query = "SELECT DISTINCT video_reference_uuid FROM imaged_moments im LEFT JOIN " +
        "observations obs ON obs.imaged_moment_uuid = im.uuid WHERE " +
        "im.last_updated_timestamp BETWEEN ?1 AND ?2 OR " +
        "obs.last_updated_timestamp BETWEEN ?1 AND ?2 " +
        "ORDER BY video_reference_uuid ASC"
    ),
    new NamedNativeQuery(
      name = "ImagedMoment.countByConcept",
      query = "SELECT COUNT(*) FROM imaged_moments im LEFT JOIN " +
        "observations obs ON obs.imaged_moment_uuid = im.uuid WHERE " +
        "obs.concept = ?1"
    ),
    new NamedNativeQuery(
      name = "ImagedMoment.countByConceptWithImages",
      query = "SELECT COUNT(*) FROM imaged_moments im LEFT JOIN " +
        "observations obs ON obs.imaged_moment_uuid = im.uuid RIGHT JOIN " +
        "image_references ir ON ir.imaged_moment_uuid = im.uuid " +
        "WHERE obs.concept = ?1 AND ir.url IS NOT NULL"
    ),
    new NamedNativeQuery(
      name = "ImagedMoment.countBetweenUpdatedDates",
      query = "SELECT COUNT(*) FROM imaged_moments im LEFT JOIN " +
        "observations obs ON obs.imaged_moment_uuid = im.uuid WHERE " +
        "im.last_updated_timestamp BETWEEN ?1 AND ?2 OR " +
        "obs.last_updated_timestamp BETWEEN ?1 AND ?2"
    ),
    new NamedNativeQuery(
      name = "ImageMoment.updateRecordedTimestampByObservationUuid",
      query = "UPDATE imaged_moments SET recorded_timestamp = ?1 WHERE " +
        "uuid IN (SELECT obs.imaged_moment_uuid FROM observations obs WHERE obs.uuid = ?2)"
    ),
    new NamedNativeQuery(
      name = "ImagedMoment.countAllByVideoReferenceUUIDs",
      query =
        "SELECT video_reference_uuid, COUNT(uuid) as n FROM imaged_moments GROUP BY video_reference_uuid ORDER BY n"
    ),
    new NamedNativeQuery(
      name = "ImagedMoment.countModifiedBeforeDate",
      query =
        "SELECT COUNT(*) FROM imaged_moments WHERE video_reference_uuid = ?1 AND last_updated_timestamp < ?2"
    ),
    new NamedNativeQuery(
      name = "ImagedMoment.countByVideoReferenceUUID",
      query = "SELECT COUNT(*) FROM imaged_moments WHERE video_reference_uuid = ?1"
    ),
    new NamedNativeQuery(
      name = "ImagedMoment.countByVideoReferenceUUIDWithImages",
      query = "SELECT COUNT(DISTINCT i.uuid) FROM imaged_moments i " + 
        "INNER JOIN image_references ir ON ir.imaged_moment_uuid = i.uuid " +
        "WHERE ir.url IS NOT NULL AND video_reference_uuid = ?1"
    ),
    new NamedNativeQuery(
      name = "ImagedMoment.countAll",
      query = "SELECT COUNT(*) FROM imaged_moments"
    ),
    new NamedNativeQuery(
      name = "ImagedMoment.countWithImages",
      query = "SELECT COUNT(DISTINCT i.uuid) FROM imaged_moments i " + 
        "INNER JOIN image_references ir ON ir.imaged_moment_uuid = i.uuid " +
        "WHERE ir.url IS NOT NULL"
    ),
    new NamedNativeQuery(
      name = "ImagedMoment.countByLinkName",
      query = "SELECT COUNT(DISTINCT i.uuid) FROM imaged_moments i " + 
        "INNER JOIN observations o ON o.imaged_moment_uuid = i.uuid " +
        "INNER JOIN associations a ON a.observation_uuid = o.uuid " +
        "WHERE a.link_name = ?1"
    )
  )
)
@NamedQueries(
  Array(
    new NamedQuery(
      name = "ImagedMoment.findAll",
      query = "SELECT i FROM ImagedMoment i ORDER BY i.uuid"
    ),
    new NamedQuery(
      name = "ImagedMoment.findWithImages",
      query = "SELECT i FROM ImagedMoment i " +
        "LEFT JOIN i.javaImageReferences ir " +
        "WHERE ir.url IS NOT NULL"
    ),
    new NamedQuery(
      name = "ImagedMoment.findByLinkName",
      query = "SELECT i FROM ImagedMoment i " + 
        "INNER JOIN i.javaObservations o " +
        "INNER JOIN o.javaAssociations a " +
        "WHERE a.linkName = :linkName"
    ),
    new NamedQuery(
      name = "ImagedMoment.findByConcept",
      query = "SELECT i FROM ImagedMoment i LEFT JOIN i.javaObservations o WHERE " +
        "o.concept = :concept ORDER BY i.uuid"
    ),
    new NamedQuery(
      name = "ImagedMoment.findByConceptWithImages",
      query = "SELECT i FROM ImagedMoment i LEFT JOIN i.javaObservations o " +
        "LEFT JOIN i.javaImageReferences ir " +
        "WHERE ir.url IS NOT NULL AND o.concept = :concept ORDER BY i.uuid"
    ),
    new NamedQuery(
      name = "ImagedMoment.findBetweenUpdatedDates",
      query = "SELECT i FROM ImagedMoment i LEFT JOIN i.javaObservations o WHERE " +
        "i.lastUpdatedTime BETWEEN :start AND :end OR " +
        "o.lastUpdatedTime BETWEEN :start AND :end ORDER BY i.uuid"
    ),
    new NamedQuery(
      name = "ImagedMoment.findByVideoReferenceUUID",
      query = "SELECT i FROM ImagedMoment i WHERE i.videoReferenceUUID = :uuid ORDER BY i.uuid"
    ),
    new NamedQuery(
      name = "ImagedMoment.findByVideoReferenceUUIDAndTimestamps",
      query = "SELECT i FROM ImagedMoment i WHERE i.videoReferenceUUID = :uuid AND " +
        "i.recordedDate BETWEEN :start AND :end ORDER BY i.recordedDate"
    ),
    new NamedQuery(
      name = "ImagedMoment.findWithImageReferences",
      query =
        "SELECT i FROM ImagedMoment i LEFT JOIN i.javaImageReferences r WHERE i.videoReferenceUUID = :uuid ORDER BY i.uuid"
    ),
    new NamedQuery(
      name = "ImagedMoment.findByObservationUUID",
      query =
        "SELECT i FROM ImagedMoment i LEFT JOIN i.javaObservations o WHERE o.uuid = :uuid ORDER BY i.uuid"
    ),
    new NamedQuery(
      name = "ImagedMoment.findByUUID",
      query = "SELECT i FROM ImagedMoment i WHERE i.uuid = :uuid ORDER BY i.uuid"
    ),
    new NamedQuery(
      name = "ImagedMoment.findByVideoReferenceUUIDAndTimecode",
      query =
        "SELECT i FROM ImagedMoment i WHERE i.timecode = :timecode AND i.videoReferenceUUID = :uuid ORDER BY i.uuid"
    ),
    new NamedQuery(
      name = "ImagedMoment.findByVideoReferenceUUIDAndElapsedTime",
      query =
        "SELECT i FROM ImagedMoment i WHERE i.elapsedTime = :elapsedTime AND i.videoReferenceUUID = :uuid ORDER BY i.uuid"
    ),
    new NamedQuery(
      name = "ImagedMoment.findByVideoReferenceUUIDAndRecordedDate",
      query =
        "SELECT i FROM ImagedMoment i WHERE i.recordedDate = :recordedDate AND i.videoReferenceUUID = :uuid ORDER BY i.uuid"
    ),
    new NamedQuery(
      name = "ImagedMoment.deleteByVideoReferenceUUID",
      query = "DELETE FROM ImagedMoment i WHERE i.videoReferenceUUID = :uuid"
    ),
    new NamedQuery(
      name = "ImagedMoment.findByWindowRequest",
      query =
        "SELECT i from ImagedMoment i WHERE i.videoReferenceUUID IN :uuids AND i.recordedDate BETWEEN :start AND :end"
    ),
    new NamedQuery(
      name = "ImagedMoment.findByImageReferenceUUID",
      query =
        "SELECT i FROM ImagedMoment i LEFT JOIN i.javaImageReferences r WHERE r.uuid = :uuid ORDER BY i.uuid"
    )
  )
)
class ImagedMomentEntity extends MutableImagedMoment with JPAPersistentObject {

  @Expose(serialize = true)
  @Column(name = "elapsed_time_millis", nullable = true)
  @Convert(converter = classOf[DurationConverter])
  var elapsedTime: Duration = _

  @Expose(serialize = true)
  @Column(name = "recorded_timestamp", nullable = true)
  @Temporal(value = TemporalType.TIMESTAMP)
  @Convert(converter = classOf[InstantConverter])
  var recordedDate: Instant = _

  @Expose(serialize = true)
  @Column(name = "timecode", nullable = true)
  @Convert(converter = classOf[TimecodeConverter])
  var timecode: Timecode = _

  @Expose(serialize = true)
  @SerializedName(value = "video_reference_uuid")
  @Column(
    name = "video_reference_uuid",
    nullable = true,
    columnDefinition = "CHAR(36)"
  )
  @Convert(converter = classOf[UUIDConverter])
  var videoReferenceUUID: UUID = _

  @Expose(serialize = true)
  @SerializedName(value = "observations")
  @OneToMany(
    targetEntity = classOf[ObservationEntity],
    cascade = Array(CascadeType.ALL),
    fetch = FetchType.EAGER,
    mappedBy = "imagedMoment",
    orphanRemoval = true
  )
  var javaObservations: JList[ObservationEntity] =
    new JArrayList[ObservationEntity]

  override def addObservation(observation: MutableObservation): Unit = {
    javaObservations.add(observation.asInstanceOf[ObservationEntity])
    observation.imagedMoment = this
  }

  override def removeObservation(observation: MutableObservation): Unit = {
    javaObservations.remove(observation)
    observation.imagedMoment = null
  }

  override def observations: Iterable[MutableObservation] = javaObservations.asScala

  @Expose(serialize = true)
  @SerializedName(value = "image_references")
  @OneToMany(
    targetEntity = classOf[ImageReferenceEntity],
    cascade = Array(CascadeType.ALL),
    fetch = FetchType.LAZY,
    mappedBy = "imagedMoment",
    orphanRemoval = true
  )
  protected var javaImageReferences: JList[ImageReferenceEntity] =
    new JArrayList[ImageReferenceEntity]

  override def addImageReference(imageReference: MutableImageReference): Unit = {
    javaImageReferences.add(imageReference.asInstanceOf[ImageReferenceEntity])
    imageReference.imagedMoment = this
  }

  override def imageReferences: Iterable[MutableImageReference] =
    javaImageReferences.asScala

  override def removeImageReference(imageReference: MutableImageReference): Unit = {
    javaImageReferences.remove(imageReference)
    imageReference.imagedMoment = null
  }

  @Expose(serialize = true)
  @SerializedName(value = "ancillary_data")
  @OneToOne(
    mappedBy = "imagedMoment",
    cascade = Array(CascadeType.ALL),
    optional = true,
    fetch = FetchType.LAZY,
    targetEntity = classOf[CachedAncillaryDatumEntity]
  )
  protected var _ancillaryDatum: MutableCachedAncillaryDatum = _

  def ancillaryDatum: MutableCachedAncillaryDatum = _ancillaryDatum
  def ancillaryDatum_=(ad: MutableCachedAncillaryDatum): Unit = {
    if (_ancillaryDatum != null) _ancillaryDatum.imagedMoment = null
    _ancillaryDatum = ad
    ad.imagedMoment = this
  }

  override def toString: String = Constants.GSON.toJson(this)
}

object ImagedMomentEntity {

  def apply(
      videoReferenceUUID: Option[UUID] = None,
      recordedDate: Option[Instant] = None,
      timecode: Option[Timecode] = None,
      elapsedTime: Option[Duration] = None
  ): ImagedMomentEntity = {

    val im = new ImagedMomentEntity
    videoReferenceUUID.foreach(im.videoReferenceUUID = _)
    recordedDate.foreach(im.recordedDate = _)
    timecode.foreach(im.timecode = _)
    elapsedTime.foreach(im.elapsedTime = _)
    im
  }

  def apply(imagedMoment: MutableImagedMoment): ImagedMomentEntity = {
    val newImagedMoment = apply(
      Option(imagedMoment.videoReferenceUUID),
      Option(imagedMoment.recordedDate),
      Option(imagedMoment.timecode),
      Option(imagedMoment.elapsedTime)
    )
    newImagedMoment.uuid = imagedMoment.uuid
    Option(imagedMoment.ancillaryDatum)
      .foreach(ad => newImagedMoment.ancillaryDatum = CachedAncillaryDatumEntity(ad))
    imagedMoment.observations.foreach(obs => newImagedMoment.addObservation(ObservationEntity(obs)))
    imagedMoment
      .imageReferences
      .foreach(ir => newImagedMoment.addImageReference(ImageReferenceEntity(ir)))
    newImagedMoment
  }

  /**
    * Map a group of annotations to the equivalent group of imagedMOments
    * @param annotations
    * @return
    */
  def apply(annotations: Seq[MutableAnnotation]): Seq[ImagedMomentEntity] = {
    val moments = new mutable.ArrayBuffer[ImagedMomentEntity]()
    // -- 1st pass create moments
    for (a <- annotations) {

      // Grab the correct imageMoment
      val imagedMoment = moments
        .filter(i => i.videoReferenceUUID == a.videoReferenceUuid)
        .find(i =>
          (a.imagedMomentUuid != null && i.uuid == a.imagedMomentUuid)
            || (a.recordedTimestamp != null && i.recordedDate == a.recordedTimestamp)
            || (a.elapsedTime != null && i.elapsedTime == a.elapsedTime)
            || (a.timecode != null && i.timecode == a.timecode)
        )
        .getOrElse {
          val i = ImagedMomentEntity(
            Some(a.videoReferenceUuid),
            Option(a.recordedTimestamp),
            Option(a.timecode),
            Option(a.elapsedTime)
          )
          Option(a.imagedMomentUuid).foreach(uuid => i.uuid = uuid)
          moments.append(i)
          i
        }

      // Add the observation
      val o = ObservationEntity(
        a.concept,
        Option(a.duration),
        Option(a.observationTimestamp),
        Option(a.observer),
        Option(a.group),
        Option(a.activity)
      )
      Option(a.observationUuid).foreach(uuid => o.uuid = uuid)
      imagedMoment.addObservation(o)

      // Add the associations
      a.associations.foreach(ass => o.addAssociation(ass))

      // Add the images (if needed)
      a.imageReferences
        .foreach(img => {
          imagedMoment
            .imageReferences
            .find(i => i.url == img.url) match {
            case None    => imagedMoment.addImageReference(img)
            case Some(_) => // Do nothing. Image already exists
          }

        })

    }
    moments.toSeq

  }

}
