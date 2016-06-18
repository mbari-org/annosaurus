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

  override def findByLinkName(linkName: String): Iterable[AssociationImpl] = ???

  override def findAll(): Iterable[AssociationImpl] = ???

  override def deleteByUUID(primaryKey: UUID): Unit = ???
}
