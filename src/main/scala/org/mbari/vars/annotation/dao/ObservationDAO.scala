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

  /**
    * @return Ordered sequence of all activities used.
    */
  def findAllActivities(): Seq[String]

  /**
    * @return Ordered sequence of all groups used.
    */
  def findAllGroups(): Seq[String]


  def findAllConceptsByVideoReferenceUUID(uuid: UUID): Seq[String]

  def countByConcept(name: String): Int

  def countByVideoReferenceUUID(uuid: UUID): Int

  def updateConcept(oldName: String, newName: String): Int

  /**
   * Move an observation to a different imaged moment efficeintly
   * @param imagedMomentUuid The image moment we want to move to
   * @param observationUuid The observation to move
   * @return The number of records affected. Should be 1
   */
  def changeImageMoment(imagedMomentUuid: UUID, observationUuid: UUID): Int

}
