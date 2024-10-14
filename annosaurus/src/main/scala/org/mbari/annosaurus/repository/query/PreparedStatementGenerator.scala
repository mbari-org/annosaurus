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

import org.mbari.annosaurus.etc.jdk.Logging.{*, given}

import java.sql.PreparedStatement
import scala.util.Try
object PreparedStatementGenerator {

    val ObservationUuid = "observation_uuid"
    val ImagedMomentUuid = "imaged_moment_uuid"

    private val log = System.getLogger(getClass.getName)

    def bind(statement: PreparedStatement, constraints: Seq[Constraint]): Either[Throwable, Unit] =
        Try {
            var idx = 1
            for c <- constraints
            do idx = c.bind(statement, idx)
            log.atDebug.log(s"Bound ${idx - 1} constraints to prepared statement")
        }.toEither

    def buildPreparedStatementTemplate(
        tableName: String,
        querySelects: Seq[String],
        constraints: Seq[Constraint],
        includeConcurrentObservations: Boolean = false,
        includeRelatedAssociations: Boolean = false
    ): String =

        val select = (ObservationUuid +: querySelects).mkString(", ")
        val where = buildWhereClause(tableName, constraints, includeConcurrentObservations, includeRelatedAssociations)

        s"""
          |SELECT $select
          |FROM $tableName
          |WHERE $where
          |""".stripMargin


    private def buildWhereClause(tableName: String,
                         constraints: Seq[Constraint],
                         includeConcurrentObservations: Boolean = false,
                         includeRelatedAssociations: Boolean = false): String =
        val wheres  = constraints.map(_.toPreparedStatementTemplate).mkString(" AND ")
        if includeConcurrentObservations && includeRelatedAssociations then
            s"""WHERE $ObservationUuid IN (
               |     SELECT $ObservationUuid
               |     FROM $tableName
               |     WHERE $ImagedMomentUuid IN (
               |          SELECT $ImagedMomentUuid
               |          FROM $tableName
               |          WHERE $wheres
               |     )
               |)
               |""".stripMargin
        else if includeConcurrentObservations then
            s"""WHERE $ImagedMomentUuid IN (
               |     SELECT $ImagedMomentUuid
               |     FROM $tableName
               |     WHERE $wheres
               |)
               |""".stripMargin
        else if includeRelatedAssociations then
            s"""WHERE $ObservationUuid IN (
               |     SELECT $ObservationUuid
               |     FROM $tableName
               |     WHERE $wheres
               |)
               |""".stripMargin
        else
            s"""WHERE $wheres"""
}
