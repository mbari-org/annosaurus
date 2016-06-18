package org.mbari.vars.annotation.dao.jpa

import java.util.UUID
import javax.persistence.EntityManager

import org.mbari.vars.annotation.dao.CachedAncillaryDatumDAO

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T17:12:00
 */
class CachedAncillaryDatumDAOImpl(entityManager: EntityManager)
    extends BaseDAO[CachedAncillaryDatumImpl](entityManager)
    with CachedAncillaryDatumDAO[CachedAncillaryDatumImpl] {

  override def deleteByUUID(primaryKey: UUID): Unit = ???

  override def findAll(): Iterable[CachedAncillaryDatumImpl] = ???
}
