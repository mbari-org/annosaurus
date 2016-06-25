package org.mbari.vars.annotation.dao

import java.util.UUID

import org.mbari.vars.annotation.model.Observation

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T16:10:00
 */
trait ObservationDAO[T <: Observation] extends DAO[T] {

  /**
   *
   * @return Order sequence of all concept names used
   */
  def findAllNames(): Seq[String]

  def findAllNamesByVideoReferenceUUID(uuid: UUID): Seq[String]

}
