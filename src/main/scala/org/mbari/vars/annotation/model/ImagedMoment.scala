package org.mbari.vars.annotation.model

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vcr4j.time.Timecode

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-15T16:54:00
 */
trait ImagedMoment {

  var uuid: UUID
  var videoReferenceUUID: UUID
  var timecode: Timecode
  var recordedDate: Instant
  var elapsedTime: Duration
  def lastUpdated: Option[Instant]
  def addObservation(observation: Observation): Unit
  def removeObservation(observation: Observation): Unit
  def observations: Iterable[Observation]
  def addImageReference(imageReference: ImageReference): Unit
  def removeImageReference(imageReference: ImageReference): Unit
  def imageReferences: Iterable[ImageReference]
  var ancillaryDatum: CachedAncillaryDatum

}

