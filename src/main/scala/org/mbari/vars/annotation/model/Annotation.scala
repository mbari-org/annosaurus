package org.mbari.vars.annotation.model

import java.time.{Duration, Instant}
import java.util.UUID

import org.mbari.vcr4j.time.Timecode

/**
  * @author Brian Schlining
  * @since 2017-09-20T10:39:00
  */
trait Annotation {
  
  var observationUuid: UUID
  var concept: String
  var observer: String
  var observationTimestamp: Instant
  var videoReferenceUuid: UUID
  var imagedMomentUuid: UUID
  var timecode: Timecode
  var elapsedTime: Duration
  var recordedTimestamp: Instant
  var duration: Duration
  var group: String
  var activity: String

}
