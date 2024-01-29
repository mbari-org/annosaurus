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

package org.mbari.annosaurus.controllers

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import jakarta.persistence.EntityManager

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import org.mbari.annosaurus.domain.CachedAncillaryDatum
import org.mbari.annosaurus.repository.jpa.extensions.*
import org.mbari.annosaurus.etc.jdk.Logging.{*, given}

/** @author
  *   Brian Schlining
  * @since 2019-02-13T14:35:00
  */
class FastAncillaryDataController(val entityManager: EntityManager) {

    private[this] val tableName = "ancillary_data"

    private val log = System.getLogger(getClass.getName)

    // Needs to be called in a transaction
    def createOrUpdate(data: Seq[CachedAncillaryDatum])(using ec: ExecutionContext): Unit =
        for (d <- data) {
            val ok = createOrUpdate(d)
            if (!ok) {
                val msg =
                    "Failed to create or update ancillary data with imagedMomentUuid = " + d.imagedMomentUuid
                log.atError.log(msg)
                throw new RuntimeException(msg)
            }
        }

    protected def createOrUpdate(data: CachedAncillaryDatum): Boolean =
        if (exists(data)) update(data) else create(data)

    def exists(data: CachedAncillaryDatum): Boolean = {
        data.imagedMomentUuid match
            case None                   => false
            case Some(imagedMomentUuid) =>
                val sql   =
                    s"SELECT uuid FROM $tableName WHERE imaged_moment_uuid = :uuid"
                val query = entityManager.createNativeQuery(sql)
                query.setParameter("uuid", imagedMomentUuid)
                log.atDebug.log(() => "SQL: " + sql)
                val n     = query
                    .getResultList
                    .asScala
                    .size
                n > 0
    }

    /** Creates a new AncillaryData record. Must be called in a transaction!! In general you should
      * use createOrUpdate instead.
      * @param data
      * @return
      */
    def create(data: CachedAncillaryDatum): Boolean = {
        if (data.imagedMomentUuid.isEmpty) false
        else
            val uuid    = data.uuid.getOrElse(UUID.randomUUID())
            val sqlData = dataAsSql(data) +
                ("uuid"               -> s"'$uuid'") +
                ("imaged_moment_uuid" -> s"'${data.imagedMomentUuid.get}'")
            val keys   = sqlData.keySet.toSeq
            val cols   = keys.mkString("(", ", ", ")")
            val values = keys.map(k => sqlData(k)).mkString("(", ", ", ")")
            val sql    = s"INSERT INTO $tableName $cols VALUES $values"
            log.atDebug.log(() => "SQL: " + sql)
            val n      = entityManager
                .createNativeQuery(sql)
                .executeUpdate()
            n == 1
    }

    def update(data: CachedAncillaryDatum): Boolean = {
        val values = dataAsSql(data)
            .map { case (a, b) => s"$a = $b" }
            .mkString(", ")
        val sql    =
            s"UPDATE $tableName SET $values WHERE imaged_moment_uuid = '${data.imagedMomentUuid.get}'"
        val n      = entityManager
            .createNativeQuery(sql)
            .executeUpdate()
        n == 1
    }

    private def dataAsSql(datum: CachedAncillaryDatum): Map[String, String] = {
        require(datum.imagedMomentUuid != null)
        val lastUpdated = Timestamp.from(Instant.now())

        (datum.altitude.map(v => "altitude" -> s"$v") ::
            datum.crs.map(v => "coordinate_reference_system" -> s"'$v'") ::
            datum.depthMeters.map(v => "depth_meters" -> s"$v") ::
            datum.latitude.map(v => "latitude" -> s"$v") ::
            datum.longitude.map(v => "longitude" -> s"$v") ::
            datum.lightTransmission.map(v => "light_transmission" -> s"$v") ::
            datum.oxygenMlL.map(v => "oxygen_ml_per_l" -> s"$v") ::
            datum.phi.map(v => "phi" -> s"$v") ::
            datum.theta.map(v => "theta" -> s"$v") ::
            datum.psi.map(v => "psi" -> s"$v") ::
            datum.posePositionUnits.map(v => "xyz_position_units" -> s"'$v'") ::
            datum.x.map(v => "x" -> s"$v") ::
            datum.y.map(v => "y" -> s"$v") ::
            datum.z.map(v => "z" -> s"$v") ::
            datum.salinity.map(v => "salinity" -> s"$v") ::
            datum.temperatureCelsius.map(v => "temperature_celsius" -> s"$v") ::
            datum.pressureDbar.map(v => "pressure_dbar" -> s"$v") ::
            Some("last_updated_timestamp" -> s"'$lastUpdated'") ::
            Nil).flatten.toMap
    }

}
