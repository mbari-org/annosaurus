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

package org.mbari.vars.annotation.model.simple

import java.time.Instant
import java.util.UUID

import com.google.gson.annotations.{ Expose, JsonAdapter }
import org.mbari.vars.annotation.model.{ CachedAncillaryDatum, ImagedMoment }

/**
 * @author Brian Schlining
 * @since 2017-11-09T12:55:00
 */
class CachedAncillaryDatumBean extends CachedAncillaryDatum {

  @Expose(serialize = true)
  override var uuid: UUID = _

  override var imagedMoment: ImagedMoment = _

  @Expose(serialize = true)
  var imagedMomentUuid: UUID = _

  @Expose(serialize = true)
  var recordedTimestamp: Option[Instant] = None

  @Expose(serialize = true)
  override var latitude: Option[Double] = None

  @Expose(serialize = true)
  override var longitude: Option[Double] = None

  @Expose(serialize = true)
  override var depthMeters: Option[Double] = None

  @Expose(serialize = true)
  override var altitude: Option[Double] = None
  /**
   * Coordinate Reference System for latitude and longitude
   */
  @Expose(serialize = true)
  override var crs: String = _

  @Expose(serialize = true)
  override var salinity: Option[Double] = None

  @Expose(serialize = true)
  override var temperatureCelsius: Option[Double] = None

  @Expose(serialize = true)
  override var oxygenMlL: Option[Double] = None

  @Expose(serialize = true)
  override var pressureDbar: Option[Double] = None

  @Expose(serialize = true)
  override var lightTransmission: Option[Double] = None

  @Expose(serialize = true)
  override var x: Option[Double] = None

  @Expose(serialize = true)
  override var y: Option[Double] = None

  @Expose(serialize = true)
  override var z: Option[Double] = None

  @Expose(serialize = true)
  override var posePositionUnits: String = _

  @Expose(serialize = true)
  override var phi: Option[Double] = None

  @Expose(serialize = true)
  override var theta: Option[Double] = None

  @Expose(serialize = true)
  override var psi: Option[Double] = None

  override val lastUpdated: Option[Instant] = None

  override def primaryKey = Option(uuid)

}
