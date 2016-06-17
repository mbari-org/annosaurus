package org.mbari.vars.annotation.model

import java.net.URL
import java.time.Instant
import java.util.UUID

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-15T16:54:00
 */
trait ImageReference {

  var uuid: UUID
  var imagedMoment: ImagedMoment

  /**
   * This is essentially the mimetype
   */
  var format: String

  /**
   * Image width in pixels
   */
  var width: Int

  /**
   * Image height in pixels
   */
  var height: Int
  var url: URL
  var description: String
  def lastUpdated: Option[Instant]

}
