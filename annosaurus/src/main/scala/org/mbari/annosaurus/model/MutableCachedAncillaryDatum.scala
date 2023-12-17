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

package org.mbari.annosaurus.model

import org.mbari.annosaurus.PersistentObject
import java.time.Instant
import java.util.UUID

/** @author
  *   Brian Schlining
  * @since 2016-06-15T16:55:00
  */
@deprecated("Use org.mbari.annosaurus.repository.jpa.CachedAncillaryDatumEntity instead", "2021-11-23T11:00:00")
trait MutableCachedAncillaryDatum extends PersistentObject {

    var uuid: UUID
    var imagedMoment: MutableImagedMoment

    // --- Position
    var latitude: Option[Double]
    var longitude: Option[Double]
    var depthMeters: Option[Double]
    var altitude: Option[Double]

    /** Coordinate Reference System for latitude and longitude
      */
    var crs: String

    // --- CTDO
    var salinity: Option[Double]
    var temperatureCelsius: Option[Double]
    var oxygenMlL: Option[Double]
    var pressureDbar: Option[Double]

    // --- Transmissometer
    var lightTransmission: Option[Double]

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
