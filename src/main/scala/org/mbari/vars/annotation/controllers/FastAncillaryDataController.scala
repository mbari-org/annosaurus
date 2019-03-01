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

package org.mbari.vars.annotation.controllers

import java.util.UUID

import javax.persistence.EntityManager
import org.mbari.vars.annotation.model.simple.CachedAncillaryDatumBean
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContext, Future }
import scala.collection.JavaConverters._

/**
 * @author Brian Schlining
 * @since 2019-02-13T14:35:00
 */
class FastAncillaryDataController(entityManager: EntityManager) {

  private[this] val tableName = "ancillary_data"

  private[this] val log = LoggerFactory.getLogger(getClass)

  def createAsync(data: Seq[CachedAncillaryDatumBean])(implicit ec: ExecutionContext): Future[Unit] = Future(createOrUpdate(data))

  def createOrUpdate(data: Seq[CachedAncillaryDatumBean]): Unit = for (d <- data) {
    val ok = createOrUpdate(d)
    if (!ok) {
      val msg = "Failed to create or update ancillary data with imagedMomentUuid = " + d.imagedMomentUuid
      log.error(msg)
      throw new RuntimeException(msg)
    }
  }

  def createOrUpdate(data: CachedAncillaryDatumBean): Boolean =
    if (exists(data)) update(data) else create(data)

  def exists(data: CachedAncillaryDatumBean): Boolean = {
    val sql = s"SELECT uuid FROM $tableName WHERE imaged_moment_uuid = '${data.imagedMomentUuid}'"
    val query = entityManager.createNativeQuery(sql)
    query.setParameter("uuid", data.imagedMomentUuid)
    val n = query.getResultList
      .asScala
      .size
    n > 0
  }

  def create(data: CachedAncillaryDatumBean): Boolean = {
    val uuid = Option(data.uuid).getOrElse(UUID.randomUUID())
    val sqlData = dataAsSql(data) +
      ("uuid" -> s"'$uuid'") +
      ("imaged_moment_uuid" -> s"'${data.imagedMomentUuid}'")
    val keys = sqlData.keySet.toSeq
    val cols = keys.mkString("(", ", ", ")")
    val values = keys.map(k => sqlData(k)).mkString("(", ", ", ")")
    val sql = s"INSERT INTO $tableName $cols VALUES $values"
    val n = entityManager.createNativeQuery(sql)
      .executeUpdate()
    n == 1
  }

  def update(data: CachedAncillaryDatumBean): Boolean = {
    val values = dataAsSql(data)
      .map({ case (a, b) => s"$a = $b" })
      .mkString(", ")
    val sql = s"UPDATE $tableName SET $values WHERE imaged_moment_uuid = '${data.imagedMomentUuid}'"
    val n = entityManager.createNativeQuery(sql)
      .executeUpdate()
    n == 1
  }

  def dataAsSql(datum: CachedAncillaryDatumBean): Map[String, String] = {
    require(datum.imagedMomentUuid != null)
    (datum.altitude.map(v => "altitude" -> s"$v") ::
      Option(datum.crs).map(v => "coordinate_reference_system" -> s"'$v'") ::
      datum.depthMeters.map(v => "depth_meters" -> s"$v") ::
      datum.latitude.map(v => "latitude" -> s"$v") ::
      datum.longitude.map(v => "longitude" -> s"$v") ::
      datum.lightTransmission.map(v => "light_transmission" -> s"$v") ::
      datum.oxygenMlL.map(v => "oxygen_ml_per_l" -> s"$v") ::
      datum.phi.map(v => "phi" -> s"$v") ::
      datum.theta.map(v => "theta" -> s"$v") ::
      datum.psi.map(v => "psi" -> s"$v") ::
      Option(datum.posePositionUnits).map(v => "xyz_position_units" -> s"'$v'") ::
      datum.x.map(v => "x" -> s"$v") ::
      datum.y.map(v => "y" -> s"$v") ::
      datum.z.map(v => "z" -> s"$v") ::
      datum.salinity.map(v => "salinity" -> s"$v") ::
      datum.temperatureCelsius.map(v => "temperature_celsius" -> s"$v") ::
      datum.pressureDbar.map(v => "pressure_dbar" -> s"$v") ::
      Nil).flatten.toMap
  }

}
