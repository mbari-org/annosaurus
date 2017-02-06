package org.mbari.vars.annotation.dao.jpa

import java.util.UUID
import javax.persistence.EntityManager

import org.mbari.vars.annotation.dao.AssociationDAO

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T17:11:00
 */
class AssociationDAOImpl(entityManager: EntityManager)
    extends BaseDAO[AssociationImpl](entityManager)
    with AssociationDAO[AssociationImpl] {

  override def newPersistentObject(): AssociationImpl = new AssociationImpl

  override def newPersistentObject(
    linkName: String,
    toConcept: Option[String],
    linkValue: Option[String],
    mimeType: Option[String]
  ): AssociationImpl = {
    val a = new AssociationImpl
    a.linkName = linkName
    toConcept.foreach(a.toConcept = _)
    linkValue.foreach(a.linkValue = _)
    mimeType.foreach(a.mimeType = _)
    a
  }

  override def findByLinkName(linkName: String): Iterable[AssociationImpl] =
    findByNamedQuery("Association.findByLinkName", Map("linkName" -> linkName))

  override def findByLinkNameAndVideoReferenceUUID(linkName: String, videoReferenceUUID: UUID): Iterable[AssociationImpl] =
    findByNamedQuery(
      "Association.findByLinkNameAndVideoReferenceUUID",
      Map("linkName" -> linkName, "videoReferenceUuid" -> videoReferenceUUID)
    )

  override def findAll(): Iterable[AssociationImpl] =
    findByNamedQuery("Association.findAll")

  override def findAll(limit: Int, offset: Int): Iterable[AssociationImpl] =
    findByNamedQuery("Association.findAll", limit = Some(limit), offset = Some(offset))
}
