package org.mbari.vars.annotation.model

import java.time.Instant
import java.util.UUID

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-15T16:55:00
 */
trait CachedMissionInfo {

  var uuid: UUID
  var videoSequenceUUID: UUID
  var platformName: UUID
  var missionID: String
  var missionContact: String
  def lastUpdated: Option[Instant]

}
