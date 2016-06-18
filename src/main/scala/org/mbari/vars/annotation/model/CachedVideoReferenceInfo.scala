package org.mbari.vars.annotation.model

import java.time.Instant
import java.util.UUID

import org.mbari.vars.annotation.PersistentObject

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-15T16:55:00
 */
trait CachedVideoReferenceInfo extends PersistentObject {

  var uuid: UUID
  var videoReferenceUUID: UUID
  var platformName: String
  var missionID: String
  var missionContact: String
  def lastUpdated: Option[Instant]

}
