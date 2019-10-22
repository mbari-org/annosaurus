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

package org.mbari.vars.annotation.dao.jdbc

import java.util.UUID

import org.mbari.vars.annotation.model.Annotation


object AncillaryDatumSQL {

  def resultListToAnncillaryData(rows: List[_]): Seq[AncillaryDatumExt] = {
    for {
      row <- rows
    } yield {
      val xs = row.asInstanceOf[Array[Object]]
      val a = new AncillaryDatumExt
      a.uuid = UUID.fromString(xs(0).toString)
      a.altitude = toDouble(xs(1).asInstanceOf[Number])
      Option(xs(2)).foreach(v => a.crs = v.toString)
      a.depthMeters = toDouble(xs(3).asInstanceOf[Number])
      a.latitude = toDouble(xs(4).asInstanceOf[Number])
      a.longitude = toDouble(xs(5).asInstanceOf[Number])
      a.oxygenMlL = toDouble(xs(6).asInstanceOf[Number])
      a.phi = toDouble(xs(7).asInstanceOf[Number])
      Option(xs(8)).foreach(v => a.posePositionUnits = v.toString)
      a.pressureDbar = toDouble(xs(9).asInstanceOf[Number])
      a.psi = toDouble(xs(10).asInstanceOf[Number])
      a.salinity = toDouble(xs(11).asInstanceOf[Number])
      a.temperatureCelsius = toDouble(xs(12).asInstanceOf[Number])
      a.theta = toDouble(xs(13).asInstanceOf[Number])
      a.x = toDouble(xs(14).asInstanceOf[Number])
      a.y = toDouble(xs(15).asInstanceOf[Number])
      a.z = toDouble(xs(16).asInstanceOf[Number])
      a.lightTransmission = toDouble(xs(17).asInstanceOf[Number])
      a.imagedMomentUuid = UUID.fromString(xs(18).toString)
      a
    }
  }

  private def toDouble(obj: Number): Option[Double] = if (obj != null) Some(obj.doubleValue())
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


  def join(annotations: Seq[AnnotationExt], data: Seq[AncillaryDatumExt]): Seq[Annotation] = {
    for {
      d <- data
    } {
      annotations.filter(anno => anno.imagedMomentUuid == d.imagedMomentUuid)
        .foreach(anno => anno.ancillaryData = d)
    }
    annotations
  }

}
