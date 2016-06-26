package org.mbari.vars.annotation.dao.jpa

import java.util.UUID
import javax.persistence.EntityManager

import org.mbari.vars.annotation.dao.ObservationDAO

import scala.collection.JavaConverters._

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T17:10:00
 */
class ObservationDAOImpl(entityManager: EntityManager)
    extends BaseDAO[ObservationImpl](entityManager)
    with ObservationDAO[ObservationImpl] {

  override def newPersistentObject(): ObservationImpl = new ObservationImpl

  /**
   *
   * @return Order sequence of all concept names used
   */
  override def findAllNames(): Seq[String] = entityManager.createNamedQuery("Observation.findAllNames")
    .getResultList
    .asScala
    .map(_.toString)

  override def findAll(): Iterable[ObservationImpl] =
    findByNamedQuery("Observation.findAll")

  override def findAllNamesByVideoReferenceUUID(uuid: UUID): Seq[String] = {
    val query = entityManager.createNamedQuery("Observation.findAllNamesByVideoReferenceUUID")
    query.setParameter(1, UUIDConverter.uuidToString(uuid))
    query.getResultList
      .asScala
      .map(_.toString)
  }
}
