package org.mbari.vars.annotation.model

import java.time.{Duration, Instant}
import java.util.UUID

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-06-15T16:53:00
  */
trait Observation {

  var uuid: UUID = _
  var imagedMoment: ImagedMoment = _
  var concept: String = _
  var duration: Duration = _
  var observer: String = _
  var observationDate: Instant = _
  var lastUpdated: Instant = _
}
