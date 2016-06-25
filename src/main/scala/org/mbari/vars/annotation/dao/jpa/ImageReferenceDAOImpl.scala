package org.mbari.vars.annotation.dao.jpa

import java.util.UUID
import javax.persistence.EntityManager

import org.mbari.vars.annotation.dao.ImageReferenceDAO

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T17:17:00
 */
class ImageReferenceDAOImpl(entityManager: EntityManager)
    extends BaseDAO[ImageReferenceImpl](entityManager)
    with ImageReferenceDAO[ImageReferenceImpl] {

  override def findAll(): Iterable[ImageReferenceImpl] =
    findByNamedQuery("ImageReference.findAll")
}
