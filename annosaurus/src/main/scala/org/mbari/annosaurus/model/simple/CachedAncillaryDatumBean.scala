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

package org.mbari.annosaurus.model.simple

import java.time.Instant
import java.util.UUID

import com.google.gson.annotations.Expose
import org.mbari.annosaurus.model.{MutableCachedAncillaryDatum, MutableImagedMoment}

/**
  * @author Brian Schlining
  * @since 2017-11-09T12:55:00
  */
class CachedAncillaryDatumBean extends MutableCachedAncillaryDatum {

  @Expose(serialize = true)
  var uuid: UUID = _

  var imagedMoment: MutableImagedMoment = _

  @Expose(serialize = true)
  var imagedMomentUuid: UUID = _

  @Expose(serialize = true)
  var recordedTimestamp: Option[Instant] = None

  @Expose(serialize = true)
  var latitude: Option[Double] = None

  @Expose(serialize = true)
  var longitude: Option[Double] = None

  @Expose(serialize = true)
  var depthMeters: Option[Double] = None

  @Expose(serialize = true)
  var altitude: Option[Double] = None

  /**
    * Coordinate Reference System for latitude and longitude
    */
  @Expose(serialize = true)
  var crs: String = _

  @Expose(serialize = true)
  var salinity: Option[Double] = None

  @Expose(serialize = true)
  var temperatureCelsius: Option[Double] = None

  @Expose(serialize = true)
  var oxygenMlL: Option[Double] = None

  @Expose(serialize = true)
  var pressureDbar: Option[Double] = None

  @Expose(serialize = true)
  var lightTransmission: Option[Double] = None

  @Expose(serialize = true)
  var x: Option[Double] = None

  @Expose(serialize = true)
  var y: Option[Double] = None

  @Expose(serialize = true)
  var z: Option[Double] = None

  @Expose(serialize = true)
  var posePositionUnits: String = _

  @Expose(serialize = true)
  var phi: Option[Double] = None

  @Expose(serialize = true)
  var theta: Option[Double] = None

  @Expose(serialize = true)
  var psi: Option[Double] = None

  val lastUpdated: Option[Instant] = None

  override def primaryKey = Option(uuid)

}

object CachedAncillaryDatumBean {

  def apply(datum: MutableCachedAncillaryDatum): CachedAncillaryDatumBean = {
    datum match {
      case c: CachedAncillaryDatumBean => c
      case c =>
        val d = new CachedAncillaryDatumBean
        d.uuid = c.uuid
        //d.imagedMoment = c.imagedMoment
        d.latitude = c.latitude
        d.longitude = c.longitude
        d.depthMeters = c.depthMeters
        d.altitude = c.altitude
        d.crs = c.crs
        d.salinity = c.salinity
        d.temperatureCelsius = c.temperatureCelsius
        d.oxygenMlL = c.oxygenMlL
        d.pressureDbar = c.pressureDbar
        d.lightTransmission = c.lightTransmission
        d.x = c.x
        d.y = c.y
        d.z = c.z
        d.posePositionUnits = c.posePositionUnits
        d.phi = c.phi
        d.theta = c.theta
        d.psi = c.psi
        if (c.imagedMoment != null) {
          d.imagedMomentUuid = c.imagedMoment.uuid
          d.recordedTimestamp = Option(c.imagedMoment.recordedDate)
        }
        d
    }
  }

}
