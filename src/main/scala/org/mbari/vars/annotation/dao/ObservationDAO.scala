package org.mbari.vars.annotation.dao

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.model.Observation

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T16:10:00
 */
trait ObservationDAO[T <: Observation] extends DAO[T] {

  def newPersistentObject(
    concept: String,
    observer: String,
    observationDate: Instant = Instant.now(),
    group: Option[String] = None,
    duration: Option[Duration] = None
  ): T

  def findByVideoReferenceUUID(uuid: UUID, limit: Option[Int] = None, offset: Option[Int] = None): Iterable[T]

  /**
   *
   * @return Order sequence of all concept names used
   */
  def findAllConcepts(): Seq[String]

  def findAllConceptsByVideoReferenceUUID(uuid: UUID): Seq[String]

}
