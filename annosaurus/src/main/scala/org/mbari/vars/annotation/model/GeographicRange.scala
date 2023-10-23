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

import com.google.gson.annotations.Expose

class GeographicRange {

  @Expose(serialize = true)
  var minLatitude: Double = _

  @Expose(serialize = true)
  var maxLatitude: Double = _

  @Expose(serialize = true)
  var minLongitude: Double = _

  @Expose(serialize = true)
  var maxLongitude: Double = _

  @Expose(serialize = true)
  var minDepthMeters: Double = _

  @Expose(serialize = true)
  var maxDepthMeters: Double = _

}

object GeographicRange {
  def apply(
      minLatitude: Double,
      maxLatitude: Double,
      minLongitude: Double,
      maxLongitude: Double,
      minDepthMeters: Double,
      maxDepthMeters: Double
  ): GeographicRange = {
    val gr = new GeographicRange()
    gr.minLatitude = minLatitude
    gr.maxLatitude = maxLatitude
    gr.minLongitude = minLongitude
    gr.maxLongitude = maxLongitude
    gr.minDepthMeters = minDepthMeters
    gr.maxDepthMeters = maxDepthMeters
    gr
  }

  val Empty: GeographicRange =
    apply(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN)
}
