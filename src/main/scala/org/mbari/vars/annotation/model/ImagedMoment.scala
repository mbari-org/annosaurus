package org.mbari.vars.annotation.model

import java.time.{Duration, Instant}
import java.util.UUID

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-06-15T16:54:00
  */
trait ImagedMoment {

  var uuid: UUID = _
  var videoReferenceUUID: UUID = _
  var timecode: String = _
  var recordedDate: Instant = _
  var elapsedTime: Duration = _
  var lastUpdated: Instant = _
  def addObservation(observation: Observation): Unit
  def removeObservation(observation: Observation): Unit
  def observations: Iterable[Observation]

}

