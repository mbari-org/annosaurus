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

import java.util.UUID

import org.mbari.vars.annotation.model.CachedAncillaryDatum

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-07-11T14:55:00
 */
case class SimpleAncillaryDatum(
    uuid: UUID,
    latitude: Option[Double],
    longitude: Option[Double],
    depthMeters: Option[Double],
    salinity: Option[Double],
    temperatureCelsius: Option[Double],
    oxygenMlL: Option[Double],
    pressureDbar: Option[Double])

object SimpleAncillaryDatum {

  def apply(datum: CachedAncillaryDatum): SimpleAncillaryDatum =
    new SimpleAncillaryDatum(datum.uuid, datum.latitude, datum.longitude, datum.depthMeters,
      datum.salinity, datum.temperatureCelsius, datum.oxygenMlL, datum.pressureDbar)

}
