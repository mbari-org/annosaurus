package org.mbari.vars.annotation.dao.jpa

import java.net.URL
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

  override def newPersistentObject(): ImageReferenceImpl = new ImageReferenceImpl

  override def newPersistentObject(
    url: URL,
    description: Option[String] = None,
    heightPixels: Option[Int] = None,
    widthPixels: Option[Int] = None,
    format: Option[String] = None
  ): ImageReferenceImpl = {
    val imageReference = newPersistentObject()
    imageReference.url = url
    description.foreach(imageReference.description = _)
    heightPixels.foreach(imageReference.height = _)
    widthPixels.foreach(imageReference.width = _)
    format.foreach(imageReference.format = _)
    imageReference
  }

  override def findAll(): Iterable[ImageReferenceImpl] =
    findByNamedQuery("ImageReference.findAll")

  override def findAll(limit: Int, offset: Int): Iterable[ImageReferenceImpl] =
    findByNamedQuery("ImageReference.findAll", limit = Some(limit), offset = Some(offset))

  override def findByURL(url: URL): Option[ImageReferenceImpl] =
    findByNamedQuery("ImageReference.findByURL", Map("url" -> url)).headOption
}
