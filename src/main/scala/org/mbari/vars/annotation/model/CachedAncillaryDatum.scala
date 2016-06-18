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
trait CachedAncillaryDatum extends PersistentObject {

  var uuid: UUID
  var imagedMoment: ImagedMoment

  // --- Position
  var latitude: Double
  var longitude: Double
  var depthMeters: Float
  var altitude: Float

  /**
   * Coordinate Reference System for latitude and longitude
   */
  var crs: String

  // --- CTDO
  var salinity: Float
  var temperatureCelsius: Float
  var oxygenMgL: Float
  var pressureDbar: Float

  // -- Camera Pose
  // Camera coordinate system
  var x: Double
  var y: Double
  var z: Double
  var posePositionUnits: String
  var phi: Double
  var theta: Double
  var psi: Double

  def lastUpdated: Option[Instant]

}
