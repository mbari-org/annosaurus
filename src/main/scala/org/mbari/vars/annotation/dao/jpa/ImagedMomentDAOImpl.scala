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
  override def findByVideoReferenceUUID(uuid: UUID): Iterable[ImagedMomentImpl] = ???

  override def findWithImageReferences(videoReferenceUUID: UUID): Iterable[ImagedMomentImpl] = ???

  override def findByUUID(primaryKey: UUID): Option[ImagedMomentImpl] = ???

  override def update(entity: ImagedMomentImpl): ImagedMomentImpl = ???

  override def findAll(): Iterable[ImagedMomentImpl] = ???

  override def delete(entity: ImagedMomentImpl): Unit = ???

  override def close(): Unit = ???

  override def create(entity: ImagedMomentImpl): Unit = ???

  override def deleteByUUID(primaryKey: UUID): Unit = ???

  override def runTransaction[R](fn: (ImagedMomentDAOImpl.this.type) => R)(implicit ec: ExecutionContext): Future[R] = ???
}
