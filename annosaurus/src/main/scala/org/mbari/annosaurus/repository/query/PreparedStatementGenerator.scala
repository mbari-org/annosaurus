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

package org.mbari.annosaurus.repository.query

import org.mbari.annosaurus.etc.jdk.Loggers.{*, given}

import java.sql.PreparedStatement
import scala.util.Try
import org.mbari.annosaurus.DatabaseConfig
object PreparedStatementGenerator:

    val IndexTime        = "index_recorded_timestamp"
    val ObservationUuid  = "observation_uuid"
    val ImagedMomentUuid = "imaged_moment_uuid"

    private val log = System.getLogger(getClass.getName)

    def bind(statement: PreparedStatement, constraints: Seq[Constraint]): Either[Throwable, Unit] =
        Try {
            var idx = 1
            for c <- constraints
            do idx = c.bind(statement, idx)
            log.atDebug.log(s"Bound ${idx - 1} constraints to prepared statement")
        }.toEither

    def buildPreparedStatementTemplateForCounts(
        tableName: String,
        query: Query
    ): String =
        val where = buildWhereClause(tableName, query)
        s"""
          |SELECT COUNT(*)
          |FROM $tableName
          |$where
          |""".stripMargin

    def buildPreparedStatementTemplate(
        tableName: String,
        query: Query,
        databaseConfig: DatabaseConfig
    ): String =
        val select   = buildSelectClause(query)
        val where    = buildWhereClause(tableName, query)
        val distinct = if query.distinct then "DISTINCT" else ""
        val orderBy  = query.orderBy match
            case Some(columns) => columns.mkString(", ")
            case None          =>
                if query.strict then query.select.head
                else s"$IndexTime, $ObservationUuid"
        val limitOffset = buildLimitOffsetClause(query, databaseConfig)

        s"""
          |SELECT $distinct $select
          |FROM $tableName
          |$where
          |ORDER BY $orderBy ASC
          |$limitOffset
          |""".stripMargin

    private def buildSelectClause(query: Query): String =
        if query.strict then query.select.mkString(", ")
        else
            // Add InexTime to the columns if it is not already there so that we can always sort by time
            val resolvedColumns1 =
                if query.select.contains(IndexTime) then query.select else IndexTime +: query.select
            val resolvedColumns2 =
                if resolvedColumns1.contains(ObservationUuid) then resolvedColumns1
                else ObservationUuid +: resolvedColumns1
            resolvedColumns2.mkString(", ")

    private def buildWhereClause(
        tableName: String,
        query: Query
    ): String =
        if query.where.isEmpty then ""
        else
            val wheres = query.where.map(_.toPreparedStatementTemplate).mkString(" AND ")
            if query.concurrentObservations && query.relatedAssociations then s"""WHERE $ObservationUuid IN (
                   |     SELECT $ObservationUuid
                   |     FROM $tableName
                   |     WHERE $ImagedMomentUuid IN (
                   |          SELECT $ImagedMomentUuid
                   |          FROM $tableName
                   |          WHERE $wheres
                   |     )
                   |)
                   |""".stripMargin
            else if query.concurrentObservations then s"""WHERE $ImagedMomentUuid IN (
                   |     SELECT $ImagedMomentUuid
                   |     FROM $tableName
                   |     WHERE $wheres
                   |)
                   |""".stripMargin
            else if query.relatedAssociations then s"""WHERE $ObservationUuid IN (
                   |     SELECT $ObservationUuid
                   |     FROM $tableName
                   |     WHERE $wheres
                   |)
                   |""".stripMargin
            else s"""WHERE $wheres"""

    private def buildLimitOffsetClause(query: Query, databaseConfig: DatabaseConfig): String =
        if (query.limit.isEmpty && query.offset.isEmpty) return ""
        else if databaseConfig.isPostgres then
            val limit  = query.limit.map(l => s"LIMIT $l").getOrElse("")
            val offset = query.offset.map(o => s"OFFSET $o").getOrElse("")
            s"$limit $offset"
        else 
            val offset = query.offset.getOrElse(0)
            val fetch = query.limit match
                case Some(l) => s"FETCH NEXT $l ROWS ONLY"
                case None    => ""
            s"OFFSET $offset ROWS $fetch"
                
