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
  var latitude: Option[Double]
  var longitude: Option[Double]
  var depthMeters: Option[Float]
  var altitude: Option[Float]

  /**
   * Coordinate Reference System for latitude and longitude
   */
  var crs: String

  // --- CTDO
  var salinity: Option[Float]
  var temperatureCelsius: Option[Float]
  var oxygenMlL: Option[Float]
  var pressureDbar: Option[Float]

  // --- Transmissometer
  var lightTransmission: Option[Float]

  // -- Camera Pose
  // Camera coordinate system
  var x: Option[Double]
  var y: Option[Double]
  var z: Option[Double]
  var posePositionUnits: String
  var phi: Option[Double]
  var theta: Option[Double]
  var psi: Option[Double]

  def lastUpdated: Option[Instant]

}
