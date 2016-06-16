package org.mbari.vars.annotation.model

import java.time.{ Duration, Instant }
import java.util.UUID

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-15T16:54:00
 */
trait ImagedMoment {

  var uuid: UUID
  var videoReferenceUUID: UUID
  var timecode: String
  var recordedDate: Instant
  var elapsedTime: Duration
  def lastUpdated: Option[Instant]
  def addObservation(observation: Observation): Unit
  def removeObservation(observation: Observation): Unit
  def observations: Iterable[Observation]

}

