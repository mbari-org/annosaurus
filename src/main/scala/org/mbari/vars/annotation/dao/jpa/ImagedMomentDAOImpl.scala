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
    findByNamedQuery("ImagedMoment.findByVideoReferenceUUIDAndElapsedTime", Map("elapsedTime" -> elapsedTime)).headOption

  override def findByVideoReferenceUUIDAndTimecode(uuid: UUID, timecode: Timecode): Option[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findByVideoReferenceUUIDAndTimecode", Map("timecode" -> timecode)).headOption

  override def findByVideoReferenceUUIDAndRecordedDate(uuid: UUID, recordedDate: Instant): Option[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findByVideoReferenceUUIDAndRecordedDate", Map("recordedDate" -> recordedDate)).headOption

  override def findByObservationUUID(uuid: UUID): Option[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findByObservationUUID", Map("uuid" -> uuid)).headOption
}
