package org.mbari.vars.annotation.dao.jpa

import java.util.UUID
import javax.persistence.EntityManager

import org.mbari.vars.annotation.dao.AssociationDAO

import scala.collection.JavaConverters._

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

  override def newPersistentObject(linkName: String, toConcept: Option[String], linkValue: Option[String]): AssociationImpl = {
    val a = new AssociationImpl
    a.linkName = linkName
    toConcept.foreach(a.toConcept = _)
    linkValue.foreach(a.linkValue = _)
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

  override def countByToConcept(toConcept: String): Int = {
    val query = entityManager.createNativeQuery("Association.countByToConcept")
    query.setParameter(1, toConcept)
    query.getResultList
          .asScala
          .map(_.asInstanceOf[Int])
        .head
  }

  override def updateToConcept(oldToConcept: String, newToConcept: String): Int = {
    val query = entityManager.createNativeQuery("Association.updateToConcept")
    query.setParameter(1, newToConcept)
    query.setParameter(2, oldToConcept)
  }
}
