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

package org.mbari.annosaurus.repository.jdbc

import java.util.UUID
import org.mbari.annosaurus.domain.CachedAncillaryDatum
import org.mbari.annosaurus.domain.Annotation
import org.mbari.annosaurus.domain.CachedAncillaryDatumSC

object AncillaryDatumSQL {

    def resultListToAnncillaryData(rows: List[_]): Seq[CachedAncillaryDatum] = {
        for {
            row <- rows
        } yield {
            val xs = row.asInstanceOf[Array[Object]]

            CachedAncillaryDatum(
                uuid = xs(0).asUUID,
                altitude = xs(1).asDouble,
                crs = xs(2).asString,
                depthMeters = xs(3).asDouble,
                latitude = xs(4).asDouble,
                longitude = xs(5).asDouble,
                oxygenMlL = xs(6).asDouble,
                phi = xs(7).asDouble,
                posePositionUnits = xs(8).asString,
                pressureDbar = xs(9).asDouble,
                psi = xs(10).asDouble,
                salinity = xs(11).asDouble,
                temperatureCelsius = xs(12).asDouble,
                theta = xs(13).asDouble,
                x = xs(14).asDouble,
                y = xs(15).asDouble,
                z = xs(16).asDouble,
                lightTransmission = xs(17).asDouble,
                imagedMomentUuid = xs(18).asUUID
            )
        }
    }

    private def toDouble(obj: Number): Option[Double] =
        if (obj != null) Some(obj.doubleValue())
        else None

    val SELECT: String =
        """ SELECT
      |  ad.uuid AS ancillary_data_uuid,
      |  ad.altitude,
      |  ad.coordinate_reference_system,
      |  ad.depth_meters,
      |  ad.latitude,
      |  ad.longitude,
      |  ad.oxygen_ml_per_l,
      |  ad.phi,
      |  ad.xyz_position_units,
      |  ad.pressure_dbar,
      |  ad.psi,
      |  ad.salinity,
      |  ad.temperature_celsius,
      |  ad.theta,
      |  ad.x,
      |  ad.y,
      |  ad.z,
      |  ad.light_transmission,
      |  im.uuid as imaged_moment_uuid
    """.stripMargin

    val FROM: String =
        """ FROM
      |  ancillary_data ad LEFT JOIN
      |  imaged_moments im ON ad.imaged_moment_uuid = im.uuid
    """.stripMargin

    val ORDER: String = " ORDER BY ad.uuid"

    val byVideoReferenceUuid: String = SELECT + FROM + " WHERE im.video_reference_uuid = ?" + ORDER

    val byImagedMomentUuid: String = SELECT + FROM + " WHERE im.uuid IN (?)" + ORDER

    val byVideoReferenceUuidBetweenDates: String = SELECT + FROM +
        " WHERE im.video_reference_uuid = ? AND im.recorded_timestamp BETWEEN ? AND ? " + ORDER

    val byConcurrentRequest: String = SELECT + FROM +
        " WHERE im.video_reference_uuid IN (?) AND im.recorded_timestamp BETWEEN ? AND ?" + ORDER

    val byMultiRequest: String = SELECT + FROM + " WHERE im.video_reference_uuid IN (?)" + ORDER

    val deleteByVideoReferenceUuid: String =
        """ DELETE FROM ancillary_data WHERE EXISTS (
      |   SELECT
      |     *
      |   FROM
      |     imaged_moments im
      |   WHERE
      |     im.video_reference_uuid = ? AND
      |     im.uuid = ancillary_data.imaged_moment_uuid
      | )
      |""".stripMargin

    def join(
        annotations: Seq[Annotation],
        data: Seq[CachedAncillaryDatum]
    ): Seq[Annotation] = {
        val resolvedAnnos = for {
            d <- data
        } yield {
            annotations
                .filter(anno => anno.imagedMomentUuid == d.imagedMomentUuid)
                .map(anno => anno.copy(ancillaryData = Some(d)))
        }
        resolvedAnnos.flatten.toSeq
    }

}
