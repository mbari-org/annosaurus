package org.mbari.vars.annotation.dao.jpa

import java.time.{ Duration, Instant }
import java.util.UUID
import javax.persistence.EntityManager

import org.mbari.vars.annotation.dao.ImagedMomentDAO
import org.mbari.vcr4j.time.Timecode
import scala.collection.JavaConverters._

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T16:34:00
 */
class ImagedMomentDAOImpl(entityManager: EntityManager)
    extends BaseDAO[ImagedMomentImpl](entityManager)
    with ImagedMomentDAO[ImagedMomentImpl] {

  override def newPersistentObject(): ImagedMomentImpl = new ImagedMomentImpl

  def newPersistentObject(
    videoReferenceUUID: UUID,
    timecode: Option[Timecode] = None,
    elapsedTime: Option[Duration] = None,
    recordedDate: Option[Instant] = None
  ): ImagedMomentImpl = {
    val imagedMoment = new ImagedMomentImpl
    imagedMoment.videoReferenceUUID = videoReferenceUUID
    timecode.foreach(imagedMoment.timecode = _)
    elapsedTime.foreach(imagedMoment.elapsedTime = _)
    recordedDate.foreach(imagedMoment.recordedDate = _)
    imagedMoment
  }

  override def findAllVideoReferenceUUIDs(limit: Option[Int] = None, offset: Option[Int] = None): Iterable[UUID] = {
    val query = entityManager.createNamedQuery("ImagedMoment.findAllVideoReferenceUUIDs")
    limit.foreach(query.setMaxResults)
    offset.foreach(query.setFirstResult)
    query.getResultList
      .asScala
      .map(s => UUID.fromString(s.toString))
  }

  override def findByVideoReferenceUUID(uuid: UUID, limit: Option[Int], offset: Option[Int]): Iterable[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findByVideoReferenceUUID", Map("uuid" -> uuid), limit, offset)

  override def findWithImageReferences(videoReferenceUUID: UUID): Iterable[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findWithImageReferences", Map("uuid" -> videoReferenceUUID))

  override def findByUUID(primaryKey: UUID): Option[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findByUUID", Map("uuid" -> primaryKey)).headOption

  override def findAll(): Iterable[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findAll")

  override def findAll(limit: Int, offset: Int): Iterable[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findAll", limit = Some(limit), offset = Some(offset))

  override def findByVideoReferenceUUIDAndElapsedTime(uuid: UUID, elapsedTime: Duration): Option[ImagedMomentImpl] =
    findByNamedQuery(
      "ImagedMoment.findByVideoReferenceUUIDAndElapsedTime",
      Map("elapsedTime" -> elapsedTime, "uuid" -> uuid)
    ).headOption

  override def findByVideoReferenceUUIDAndTimecode(uuid: UUID, timecode: Timecode): Option[ImagedMomentImpl] =
    findByNamedQuery(
      "ImagedMoment.findByVideoReferenceUUIDAndTimecode",
      Map("timecode" -> timecode, "uuid" -> uuid)
    ).headOption

  override def findByVideoReferenceUUIDAndRecordedDate(uuid: UUID, recordedDate: Instant): Option[ImagedMomentImpl] =
    findByNamedQuery(
      "ImagedMoment.findByVideoReferenceUUIDAndRecordedDate",
      Map("recordedDate" -> recordedDate, "uuid" -> uuid)
    ).headOption

  override def findByObservationUUID(uuid: UUID): Option[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findByObservationUUID", Map("uuid" -> uuid)).headOption

  /**
   * A bulk delete operation. This will delete all annotation related data for a single video.
   * (which is identified via its uuid (e.g. videoReferenceUUID)
   *
   * @param uuid The UUID of the VideoReference. WARNING!! All annotation data associated to
   *             this videoReference will be deleted.
   */
  override def deleteByVideoReferenceUUUID(uuid: UUID): Int =
    executeNamedQuery("ImagedMoment.deleteByVideoReferenceUUID", Map("uuid" -> uuid))
}
