package org.mbari.vars.annotation.model

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.PersistentObject

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-15T16:53:00
 */
trait Observation extends PersistentObject {

  var uuid: UUID
  var imagedMoment: ImagedMoment
  var concept: String
  var duration: Duration
  var observer: String
  var observationDate: Instant
  def lastUpdated: Option[Instant]
  def addAssociation(association: Association): Unit
  def removeAssociation(association: Association): Unit
  def associations: Iterable[Association]
}
