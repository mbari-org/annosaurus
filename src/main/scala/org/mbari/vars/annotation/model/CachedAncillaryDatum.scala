/*
 * Copyright 2017 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
