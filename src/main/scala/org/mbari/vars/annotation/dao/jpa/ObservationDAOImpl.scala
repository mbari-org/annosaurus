package org.mbari.vars.annotation.dao.jpa

import java.time.{ Duration, Instant }
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

  override def newPersistentObject(
    concept: String,
    observer: String,
    observationDate: Instant = Instant.now(),
    group: Option[String] = None,
    duration: Option[Duration] = None
  ): ObservationImpl = {

    val observation = newPersistentObject()
    observation.concept = concept
    observation.observer = observer
    observation.observationDate = observationDate
    group.foreach(observation.group = _)
    duration.foreach(observation.duration = _)
    observation
  }

  override def findByVideoReferenceUUID(uuid: UUID, limit: Option[Int] = None, offset: Option[Int] = None): Iterable[ObservationImpl] =
    findByNamedQuery("Observation.findByVideoReferenceUUID", Map("uuid" -> uuid))

  /**
   *
   * @return Order sequence of all concept names used
   */
  override def findAllNames(): Seq[String] = entityManager.createNamedQuery("Observation.findAllNames")
    .getResultList
    .asScala
    .filter(_ != null)
    .map(_.toString)

  override def findAll(): Iterable[ObservationImpl] =
    findByNamedQuery("Observation.findAll")

  override def findAll(limit: Int, offset: Int): Iterable[ObservationImpl] =
    findByNamedQuery("Observation.findAll", limit = Some(limit), offset = Some(offset))

  override def findAllConceptsByVideoReferenceUUID(uuid: UUID): Seq[String] = {
    val query = entityManager.createNamedQuery("Observation.findAllNamesByVideoReferenceUUID")
    query.setParameter(1, UUIDConverter.uuidToString(uuid))
    query.getResultList
      .asScala
      .map(_.toString)
  }
}
