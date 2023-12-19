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

import java.util.UUID

import org.mbari.annosaurus.repository.jpa.entity.CachedAncillaryDatumEntity

final case class CachedAncillaryDatum(
    latitude: Option[Double] = None,
    longitude: Option[Double] = None,
    depthMeters: Option[Double] = None,
    altitude: Option[Double] = None,
    crs: Option[String] = None,
    salinity: Option[Double] = None,
    temperatureCelsius: Option[Double] = None,
    oxygenMlL: Option[Double] = None,
    pressureDbar: Option[Double] = None,
    lightTransmission: Option[Double] = None,
    x: Option[Double] = None,
    y: Option[Double] = None,
    z: Option[Double] = None,
    posePositionUnits: Option[String] = None,
    phi: Option[Double] = None,
    theta: Option[Double] = None,
    psi: Option[Double] = None,
    uuid: Option[UUID] = None,
    lastUpdated: Option[java.time.Instant] = None,
    imagedMomentUuid: Option[UUID] = None,  // extend
    recordedTimestamp: Option[java.time.Instant] = None //extend
) extends ToSnakeCase[CachedAncillaryDatumSC]
    with ToEntity[CachedAncillaryDatumEntity] {
    override def toSnakeCase: CachedAncillaryDatumSC =
        CachedAncillaryDatumSC(
            latitude,
            longitude,
            depthMeters,
            altitude,
            crs,
            salinity,
            temperatureCelsius,
            oxygenMlL,
            pressureDbar,
            lightTransmission,
            x,
            y,
            z,
            posePositionUnits,
            phi,
            theta,
            psi,
            uuid,
            lastUpdated,
            imagedMomentUuid,
            recordedTimestamp
        )

    override def toEntity: CachedAncillaryDatumEntity =
        var entity = new CachedAncillaryDatumEntity
        entity.latitude = latitude
        entity.longitude = longitude
        entity.depthMeters = depthMeters
        entity.altitude = altitude
        entity.crs = crs.orNull
        entity.salinity = salinity
        entity.temperatureCelsius = temperatureCelsius
        entity.oxygenMlL = oxygenMlL
        entity.pressureDbar = pressureDbar
        entity.lightTransmission = lightTransmission
        entity.x = x
        entity.y = y
        entity.z = z
        entity.posePositionUnits = posePositionUnits.orNull
        entity.phi = phi
        entity.theta = theta
        entity.psi = psi
        entity.uuid = uuid.orNull
        // NOTE: We can't set he lastUpdated field because it's set by the database driver
        entity
}

object CachedAncillaryDatum extends FromEntity[CachedAncillaryDatumEntity, CachedAncillaryDatum] {
    def from(entity: CachedAncillaryDatumEntity, extend: Boolean = false): CachedAncillaryDatum =
        val opt = if extend && entity.imagedMoment != null then entity.imagedMoment.primaryKey else None
        val rt = if extend && entity.imagedMoment != null then Option(entity.imagedMoment.recordedDate) else None
        CachedAncillaryDatum(
            entity.latitude,
            entity.longitude,
            entity.depthMeters,
            entity.altitude,
            Option(entity.crs),
            entity.salinity,
            entity.temperatureCelsius,
            entity.oxygenMlL,
            entity.pressureDbar,
            entity.lightTransmission,
            entity.x,
            entity.y,
            entity.z,
            Option(entity.posePositionUnits),
            entity.phi,
            entity.theta,
            entity.psi,
            Option(entity.uuid),
            entity.lastUpdated,
            opt,
            rt
        )
}

final case class CachedAncillaryDatumSC(
    latitude: Option[Double] = None,
    longitude: Option[Double] = None,
    depth_meters: Option[Double] = None,
    altitude: Option[Double] = None,
    crs: Option[String] = None,
    salinity: Option[Double] = None,
    temperature_celsius: Option[Double] = None,
    oxygen_ml_l: Option[Double] = None,
    pressure_dbar: Option[Double] = None,
    light_transmission: Option[Double] = None,
    x: Option[Double] = None,
    y: Option[Double] = None,
    z: Option[Double] = None,
    pose_position_units: Option[String] = None,
    phi: Option[Double] = None,
    theta: Option[Double] = None,
    psi: Option[Double] = None,
    uuid: Option[UUID] = None,
    last_updated_time: Option[java.time.Instant] = None,
    imaged_moment_uuid: Option[UUID] = None,
    recorded_timestamp: Option[java.time.Instant] = None
) extends ToCamelCase[CachedAncillaryDatum] {
    override def toCamelCase: CachedAncillaryDatum =
        CachedAncillaryDatum(
            latitude,
            longitude,
            depth_meters,
            altitude,
            crs,
            salinity,
            temperature_celsius,
            oxygen_ml_l,
            pressure_dbar,
            light_transmission,
            x,
            y,
            z,
            pose_position_units,
            phi,
            theta,
            psi,
            uuid,
            last_updated_time,
            imaged_moment_uuid,
            recorded_timestamp
        )
}
