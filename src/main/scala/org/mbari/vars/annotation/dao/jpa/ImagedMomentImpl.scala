package org.mbari.vars.annotation.dao.jpa

import java.time.{ Duration, Instant }
import java.util.UUID
import javax.persistence.{ Convert, _ }
import java.util.{ ArrayList => JArrayList, List => JList }

import org.mbari.vars.annotation.model.{ CachedAncillaryDatum, ImageReference, ImagedMoment, Observation }
import org.mbari.vcr4j.time.Timecode

import scala.collection.JavaConverters._

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-16T14:12:00
 */
@Entity(name = "ImagedMoment")
@Table(name = "imaged_moments")
@EntityListeners(value = Array(classOf[TransactionLogger]))
@NamedQueries(Array(
  new NamedQuery(
    name = "ImagedMoment.findAll",
    query = "SELECT i FROM ImagedMoment i"
  ),
  new NamedQuery(
    name = "ImagedMoment.findByVideoReferenceUUID",
    query = "SELECT i FROM ImagedMoment i WHERE i.videoReferenceUUID = :uuid"
  ),
  new NamedQuery(
    name = "ImagedMoment.findWithImageReferences",
    query = "SELECT i FROM ImagedMoment i LEFT JOIN i.javaImageReferences r WHERE i.videoReferenceUUID = :uuid AND r.url NOT NULL"
  ),
  new NamedQuery(
    name = "ImagedMoment.findByUUID",
    query = "SELECT i FROM ImagedMoment i WHERE i.uuid = :uuid"
  ),
  new NamedQuery(
    name = "ImagedMoment.findByVideoReferenceUUIDAndTimecode",
    query = "SELECT i FROM ImagedMoment i WHERE i.timecode = :timecode"
  ),
  new NamedQuery(
    name = "ImagedMoment.findByVideoReferenceUUIDAndElapsedTime",
    query = "SELECT i FROM ImagedMoment i WHERE i.elapsedTime = :elapsedTime"
  ),
  new NamedQuery(
    name = "ImagedMoment.findByVideoReferenceUUIDAndRecordedDate",
    query = "SELECT i FROM ImagedMoment i WHERE i.recordedDate = :recordedDate"
  )
))
class ImagedMomentImpl extends ImagedMoment with JPAPersistentObject {

  @Column(
    name = "elapsed_time_millis",
    nullable = true
  )
  @Convert(converter = classOf[DurationConverter])
  override var elapsedTime: Duration = _

  @Index(name = "idx_recorded_timestamp", columnList = "recorded_timestamp")
  @Column(
    name = "recorded_timestamp",
    nullable = true
  )
  @Temporal(value = TemporalType.TIMESTAMP)
  @Convert(converter = classOf[InstantConverter])
  override var recordedDate: Instant = _

  @Column(
    name = "timecode",
    nullable = true
  )
  @Convert(converter = classOf[TimecodeConverter])
  override var timecode: Timecode = _

  @Column(
    name = "video_reference_uuid",
    nullable = true
  )
  @Convert(converter = classOf[UUIDConverter])
  override var videoReferenceUUID: UUID = _

  @OneToMany(
    targetEntity = classOf[Observation],
    cascade = Array(CascadeType.ALL),
    fetch = FetchType.EAGER,
    mappedBy = "imagedMoment",
    orphanRemoval = true
  )
  protected var javaObservations: JList[Observation] = new JArrayList[Observation]

  override def addObservation(observation: Observation): Unit = {
    javaObservations.add(observation)
    observation.imagedMoment = this
  }

  override def removeObservation(observation: Observation): Unit = {
    javaObservations.remove(observation)
    observation.imagedMoment = null
  }

  override def observations: Iterable[Observation] = javaObservations.asScala

  @OneToMany(
    targetEntity = classOf[ImageReference],
    cascade = Array(CascadeType.ALL),
    fetch = FetchType.EAGER,
    mappedBy = "imagedMoment",
    orphanRemoval = true
  )
  protected var javaImageReferences: JList[ImageReference] = new JArrayList[ImageReference]

  override def addImageReference(imageReference: ImageReference): Unit = {
    javaImageReferences.add(imageReference)
    imageReference.imagedMoment = this
  }

  override def imageReferences: Iterable[ImageReference] = javaImageReferences.asScala

  override def removeImageReference(imageReference: ImageReference): Unit = {
    javaImageReferences.remove(imageReference)
    imageReference.imagedMoment = null
  }

  @OneToOne(
    cascade = Array(CascadeType.ALL),
    optional = true
  )
  @JoinColumn(
    name = "ancillary_datum_uuid",
    nullable = true
  )
  override var ancillaryDatum: CachedAncillaryDatum = _
}

object ImagedMomentImpl {

  def apply(
    videoReferenceUUID: Option[UUID] = None,
    recordedDate: Option[Instant] = None,
    timecode: Option[Timecode] = None,
    elapsedTime: Option[Duration] = None
  ): ImagedMomentImpl = {

    val im = new ImagedMomentImpl
    videoReferenceUUID.foreach(im.videoReferenceUUID = _)
    recordedDate.foreach(im.recordedDate = _)
    timecode.foreach(im.timecode = _)
    elapsedTime.foreach(im.elapsedTime = _)
    im
  }

}
