package org.mbari.vars.annotation.dao.jpa

import java.util.UUID
import javax.persistence.EntityManager

import org.mbari.vars.annotation.dao.ImagedMomentDAO

import scala.concurrent.{ ExecutionContext, Future }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T16:34:00
 */
class ImagedMomentDAOImpl(entityManager: EntityManager)
    extends BaseDAO[ImagedMomentImpl](entityManager)
    with ImagedMomentDAO[ImagedMomentImpl] {
  override def findByVideoReferenceUUID(uuid: UUID): Iterable[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findByVideoReferenceUUID", Map("uuid" -> uuid))

  override def findWithImageReferences(videoReferenceUUID: UUID): Iterable[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findWithImageReferences", Map("uuid" -> videoReferenceUUID))

  override def findByUUID(primaryKey: UUID): Option[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findByUUID", Map("uuid" -> primaryKey)).headOption

  override def findAll(): Iterable[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findAll")

}
