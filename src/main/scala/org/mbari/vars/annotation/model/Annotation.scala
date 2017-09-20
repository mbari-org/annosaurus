package org.mbari.vars.annotation.model

import java.time.{ Duration, Instant }
import java.util.UUID

import com.google.gson.annotations.{ Expose, SerializedName }
import org.mbari.vcr4j.time.Timecode

/**
 * @author Brian Schlining
 * @since 2017-09-20T15:58:00
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
  var associations: Seq[Association]
  var imageReferences: Seq[ImageReference]

}
