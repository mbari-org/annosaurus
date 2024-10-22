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

import jakarta.persistence.{EntityManager, Query}
import org.mbari.annosaurus.domain.ObservationsUpdate

/**
 * @author
 *   Brian Schlining
 * @since 2019-10-28T16:39:00
 */
object ObservationSQL:

    val countAll: String = "SELECT COUNT(*) FROM observations"

    val deleteByVideoReferenceUuid: String =
        """ DELETE FROM observations WHERE EXISTS (
      |   SELECT
      |     *
      |   FROM
      |     imaged_moments im
      |   WHERE
      |     im.video_reference_uuid = ? AND
      |     im.uuid = observations.imaged_moment_uuid
      | )
      |""".stripMargin

    val updateGroup: String =
        """ UPDATE observations
      | SET
      |   observation_group = ?
      | WHERE
      |   uuid IN (?)
      |""".stripMargin

    val updateActivity: String =
        """ UPDATE observations
      | SET
      |   activity = ?
      | WHERE
      |   uuid IN (?)
      |""".stripMargin

    val updateConcept: String =
        """ UPDATE observations
        | SET
        |   concept = ?
        | WHERE
        |   uuid IN (?)
        |""".stripMargin

    val updateObserver: String =
        """ UPDATE observations
        | SET
        |   observer = ?
        | WHERE
        |   uuid IN (?)
        |""".stripMargin

    def buildUpdates(update: ObservationsUpdate, entityManager: EntityManager): Seq[Query] =
        val uuidsString = update.observationUuids.mkString("('", "','", "')")

        // Helper function to build a query. It replaces the first (?) with the uuidsStrings
        // and then sets the parameter for the value
        def build(sql: String, value: Option[String]): Option[Query] =
            value.map { v =>
                val sql2  = sql.replace("(?)", uuidsString)
                val query = entityManager.createNativeQuery(sql2)
                query.setParameter(1, v)
                query
            }

        // Map of the sql and the value to set. We'll build a query for each value that
        // is not an Option
        val params = (updateObserver -> update.observer)
            :: (updateGroup    -> update.group)
            :: (updateConcept  -> update.concept)
            :: (updateActivity -> update.activity)
            :: Nil
        params.flatMap(p => build(p._1, p._2))
