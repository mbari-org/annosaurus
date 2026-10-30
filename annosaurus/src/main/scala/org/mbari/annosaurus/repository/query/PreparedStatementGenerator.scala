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

import org.mbari.annosaurus.DatabaseConfig
import org.mbari.annosaurus.etc.jdbc.Databases
import org.mbari.annosaurus.etc.jdk.Loggers.given

import java.sql.PreparedStatement
import scala.util.Try
object PreparedStatementGenerator:

    val IndexTime        = "index_recorded_timestamp"
    val ObservationUuid  = "observation_uuid"
    val ImagedMomentUuid = "imaged_moment_uuid"

    private val log = System.getLogger(getClass.getName)

    /** Column kinds that need a typed bind instead of the default string bind of `equals` / `in`. */
    enum ColumnKind:
        case Boolean, Uuid

    /**
     * Binds the constraints. `columnKinds` (lower-case column name -> kind, from the view's metadata) switches `equals`
     * and `in` values of boolean and uuid columns to a typed bind: PostgreSQL rejects `boolean = varchar`, so the string
     * "true" cannot be bound as is. A value that does not parse as the column's type falls back to the original string
     * bind, so the database reports the same error it always did. Every other constraint, and every column not in
     * `columnKinds`, binds exactly as before.
     */
    def bind(
        statement: PreparedStatement,
        constraints: Seq[Constraint],
        columnKinds: Map[String, ColumnKind] = Map.empty
    ): Either[Throwable, Unit] =
        Try {
            var idx = 1
            for c <- constraints
            do idx = bindOne(statement, c, idx, columnKinds)
            log.atDebug.log(s"Bound ${idx - 1} constraints to prepared statement")
        }.toEither

    /** The typed value for a string bound against a column of the given kind; None means "bind the string as is". */
    private[query] def typedValue(kind: Option[ColumnKind], value: Any): Option[Any] =
        (kind, value) match
            case (Some(ColumnKind.Boolean), s: String) =>
                s.trim.toLowerCase match
                    case "true" | "1"  => Some(java.lang.Boolean.TRUE)
                    case "false" | "0" => Some(java.lang.Boolean.FALSE)
                    case _             => None
            case (Some(ColumnKind.Uuid), s: String)    => Try(java.util.UUID.fromString(s.trim)).toOption
            case _                                     => None

    private def setTyped(statement: PreparedStatement, idx: Int, value: Any): Unit =
        value match
            case b: java.lang.Boolean => statement.setBoolean(idx, b)
            case other                => statement.setObject(idx, other)

    private def bindOne(
        statement: PreparedStatement,
        c: Constraint,
        idx: Int,
        columnKinds: Map[String, ColumnKind]
    ): Int =
        def kindOf(column: String) = columnKinds.get(column.trim.toLowerCase)
        c match
            case Constraint.Equals(column, value) =>
                typedValue(kindOf(column), value) match
                    case Some(v) =>
                        setTyped(statement, idx, v)
                        idx + 1
                    case None    => c.bind(statement, idx)
            case Constraint.In(column, values)    =>
                val typed = values.map(v => typedValue(kindOf(column), v))
                if typed.forall(_.isDefined) then
                    for (v, i) <- typed.flatten.zipWithIndex do setTyped(statement, idx + i, v)
                    idx + typed.size
                else c.bind(statement, idx)
            case _                                => c.bind(statement, idx)

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
        val select      = buildSelectClause(query)
        val where       = buildWhereClause(tableName, query)
        val distinct    = if query.distinct then "DISTINCT" else ""
        val orderBy     = query.orderBy match
            case Some(columns) => columns.mkString(", ")
            case None          =>
                if query.strict then query.select.head // strict = true is the default. So this is the default ordering
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
        if query.limit.isEmpty && query.offset.isEmpty then return ""
        else if databaseConfig.databaseType == Databases.DatabaseType.PostgreSQL then
            val limit  = query.limit.map(l => s"LIMIT $l").getOrElse("")
            val offset = query.offset.map(o => s"OFFSET $o").getOrElse("")
            s"$limit $offset"
        else
            val offset = query.offset.getOrElse(0)
            val fetch  = query.limit match
                case Some(l) => s"FETCH NEXT $l ROWS ONLY"
                case None    => ""
            s"OFFSET $offset ROWS $fetch"
