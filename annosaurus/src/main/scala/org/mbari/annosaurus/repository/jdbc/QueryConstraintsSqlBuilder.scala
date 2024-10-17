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
import java.time.Instant
import org.mbari.annosaurus.domain.QueryConstraints
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import java.sql.Timestamp
import org.mbari.annosaurus.etc.jdk.Logging.given

object QueryConstraintsSqlBuilder:

    private val log = System.getLogger(getClass.getName)

    /**
     * Generates a SQL template for use to build a query. It's not executable SQL though!
     * @param qc
     * @return
     */
    def toFromWhereSql(qc: QueryConstraints): String =
        import org.mbari.annosaurus.repository.jdbc.AnnotationSQL.*

        val sqlConstraints = List(
            if qc.concepts.nonEmpty then Some("(obs.concept IN (A?) OR ass.to_concept IN (A?))")
            else None,
            if qc.videoReferenceUuids.nonEmpty then Some("im.video_reference_uuid IN (B?)")
            else None,
            if qc.observers.nonEmpty then Some("obs.observer IN (C?)") else None,
            if qc.groups.nonEmpty then Some("obs.observation_group IN (D?)") else None,
            if qc.activities.nonEmpty then Some("obs.activity IN (E?)") else None,
            if qc.missionContacts.nonEmpty then Some("vri.mission_contact IN (F?)") else None,
            qc.minDepth.map(_ => "ad.depth_meters >= ?"),
            qc.maxDepth.map(_ => "ad.depth_meters < ?"),
            qc.minLon.map(_ => "ad.longitude >= ?"),
            qc.maxLon.map(_ => "ad.longitude < ?"),
            qc.minLat.map(_ => "ad.latitude >= ?"),
            qc.maxLat.map(_ => "ad.latitude < ?"),
            qc.minTimestamp.map(_ => "im.recorded_timestamp >= ?"),
            qc.maxTimestamp.map(_ => "im.recorded_timestamp < ?"),
            qc.linkName.map(_ => "ass.link_name = ?"),
            qc.linkValue.map(_ => "ass.link_value = ?"),
            qc.platformName.map(_ => "vri.platform_name = ?"),
            qc.missionId.map(_ => "vri.mission_id = ?")
        ).flatten

        FROM_WITH_ANCILLARY_DATA + " WHERE " + sqlConstraints.mkString(" AND ")

    private def toSql(
        qc: QueryConstraints,
        selectStatement: String,
        orderStatement: String = AnnotationSQL.ORDER
    ): String =
        // import org.mbari.vars.annotation.dao.jdbc.AnnotationSQL._
        val fromWhere = toFromWhereSql(qc)
        selectStatement + fromWhere + orderStatement

    private def toCountSql(qc: QueryConstraints): String =
        val fromWhere = toFromWhereSql(qc)
        val select    = "SELECT COUNT(DISTINCT obs.uuid)"
        select + " " + fromWhere

    def toCountQuery(qc: QueryConstraints, entityManager: EntityManager): Query =
        val sql = toCountSql(qc)
        buildQuery(qc, entityManager, sql)

    /**
     * @param qc
     * @param entityManager
     * @return
     */
    def toQuery(
        qc: QueryConstraints,
        entityManager: EntityManager,
        selectStatement: String = AnnotationSQL.SELECT,
        orderStatment: String = AnnotationSQL.ORDER
    ): Query =
        val sql = toSql(qc, selectStatement, orderStatment)
        log.atDebug.log(() => "SQL: " + sql)
        buildQuery(qc, entityManager, sql)

    private def toGeographicRangeSql(qc: QueryConstraints): String =
        val fromWhere = toFromWhereSql(qc) +
            " AND ad.longitude IS NOT NULL AND ad.latitude IS NOT NULL AND ad.depth_meters IS NOT NULL"
        val select    =
            "SELECT min(ad.latitude), max(ad.latitude), min(ad.longitude), max(ad.longitude), min(ad.depth_meters), max(ad.depth_meters) "
        select + " " + fromWhere

    def toGeographicRangeQuery(qc: QueryConstraints, entityManager: EntityManager): Query =
        val sql = toGeographicRangeSql(qc)
        buildQuery(qc, entityManager, sql)

    private def buildQuery(
        qc: QueryConstraints,
        entityManager: EntityManager,
        base: String
    ): Query =

        // JPA doesn't handle in clauses well. So this is a cludge
        def replaceInClause[A](xs: Iterable[A], sql: String, target: String): String =
            if xs.nonEmpty then
                val s = xs.mkString("('", "','", "')")
                sql.replace(target, s)
            else sql

        val a   = replaceInClause(qc.concepts, base, "(A?)")
        val b   = replaceInClause(qc.videoReferenceUuids, a, "(B?)")
        val c   = replaceInClause(qc.observers, b, "(C?)")
        val d   = replaceInClause(qc.groups, c, "(D?)")
        val e   = replaceInClause(qc.activities, d, "(E?)")
        val sql = replaceInClause(qc.missionContacts, e, "(F?)")

        // Bind the params in the correct order. This is the same order they are found in the SQL.
        // The flattened list excludes empty Options
        def params = List(
            qc.minDepth,
            qc.maxDepth,
            qc.minLon,
            qc.maxLon,
            qc.minLat,
            qc.maxLat,
            qc.minTimestamp,
            qc.maxTimestamp,
            qc.linkName,
            qc.linkValue,
            qc.platformName,
            qc.missionId
        ).flatten
        val query  = entityManager.createNativeQuery(sql)
        for i <- params.indices
        do query.setParameter(i + 1, params(i))
        query.setMaxResults(qc.limit.getOrElse(5000))
        query.setFirstResult(qc.offset.getOrElse(0))
        query
