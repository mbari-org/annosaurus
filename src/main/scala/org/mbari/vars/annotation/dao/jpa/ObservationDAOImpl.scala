package org.mbari.vars.annotation.dao.jpa

import java.util.UUID
import javax.persistence.EntityManager

import org.mbari.vars.annotation.dao.ObservationDAO

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T17:10:00
 */
class ObservationDAOImpl(entityManager: EntityManager)
    extends BaseDAO[ObservationImpl](entityManager)
    with ObservationDAO[ObservationImpl] {
  /**
   *
   * @return Order sequence of all concept names used
   */
  override def findAllNames(): Seq[String] = ???

  override def findAll(): Iterable[ObservationImpl] = ???

  override def deleteByUUID(primaryKey: UUID): Unit = ???
}
