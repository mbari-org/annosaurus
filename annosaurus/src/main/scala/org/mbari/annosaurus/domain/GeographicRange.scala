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

package org.mbari.annosaurus.domain

case class GeographicRange(
    minLatitude: Double,
    maxLatitude: Double,
    minLongitude: Double,
    maxLongitude: Double,
    minDepthMeters: Double,
    maxDepthMeters: Double
) extends ToSnakeCase[GeographicRangeSC]:
    def toSnakeCase: GeographicRangeSC = GeographicRangeSC(
        minLatitude,
        maxLatitude,
        minLongitude,
        maxLongitude,
        minDepthMeters,
        maxDepthMeters
    )

case class GeographicRangeSC(
    min_latitude: Double,
    max_latitude: Double,
    min_longitude: Double,
    max_longitude: Double,
    min_depth_meters: Double,
    max_depth_meters: Double
) extends ToCamelCase[GeographicRange]:
    def toCamelCase: GeographicRange = GeographicRange(
        min_latitude,
        max_latitude,
        min_longitude,
        max_longitude,
        min_depth_meters,
        max_depth_meters
    )
