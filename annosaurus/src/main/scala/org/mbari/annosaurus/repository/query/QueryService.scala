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
import org.mbari.annosaurus.repository.jdbc.*
import org.mbari.annosaurus.domain.{Annotation, Association}
import org.mbari.annosaurus.etc.jdk.Logging.{*, given}

import java.sql.ResultSet
import scala.collection.mutable.ListBuffer
import scala.util.Using

class QueryService(databaseConfig: DatabaseConfig, viewName: String) {

    val jdbc               = new JDBC(databaseConfig)
    val AnnotationViewName = "annotations"
    private val log        = System.getLogger(getClass.getName)

    def findAllConceptNames(): Either[Throwable, Seq[String]] =
        jdbc.findDistinct(viewName, "concept", stringConverter)

    def findAssociationsForConcepts(concepts: Seq[String]): Either[Throwable, Seq[Association]] = {
        val sql = s"""
           | SELECT DISTINCT
           |   link_name,
           |   to_concept,
           |   link_value
           | FROM
           |   associations AS a JOIN
           |   observations AS o ON o.uuid = a.observation_uuid
           | WHERE
           |   concept IN (${concepts.map(s => s"'$s'").mkString(",")})
           |""".stripMargin
        jdbc.runQuery(
            sql,
            rs => {
                val associations = ListBuffer.newBuilder[Association]
                while (rs.next()) {
                    val linkName  = rs.getString("link_name")
                    val toConcept = rs.getString("to_concept")
                    val linkValue = rs.getString("link_value")
                    associations += Association(linkName, toConcept, linkValue)
                }
                associations.result().toSeq.sortBy(_.linkName)
            }
        )

    }

    def count(query: Query): Either[Throwable, Int] =
        val sql = PreparedStatementGenerator.buildPreparedStatementTemplateForCounts(
            viewName,
            query.constraints,
            query.concurrentObservations.getOrElse(false),
            query.relatedAssociations.getOrElse(false)
        )
        log.atDebug.log(s"Running query: $sql")
        Using
            .Manager(use =>
                val conn = use(jdbc.newConnection())
                conn.setReadOnly(true)
                val stmt = use(
                    conn.prepareStatement(
                        sql,
                        ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY
                    )
                )
                PreparedStatementGenerator.bind(stmt, query.constraints)
                val rs = stmt.executeQuery()
                //                val rs   = use(stmt.executeQuery(sql))
                if rs.next() then rs.getInt(1) else 0
            )
            .toEither

    def query(
        query: Query
    ): Either[Throwable, QueryResults] =
        val sql = PreparedStatementGenerator.buildPreparedStatementTemplate(
            viewName,
            query.columns,
            query.constraints,
            query.concurrentObservations.getOrElse(false),
            query.relatedAssociations.getOrElse(false)
        )
        log.atDebug.log(s"Running query: $sql")
        Using
            .Manager(use =>
                val conn = use(jdbc.newConnection())
                conn.setReadOnly(true)
                val stmt = use(
                    conn.prepareStatement(
                        sql,
                        ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY
                    )
                )
                query.limit.foreach(stmt.setMaxRows)
                query.offset.foreach(stmt.setFetchSize)
                PreparedStatementGenerator.bind(stmt, query.constraints)
                val rs = stmt.executeQuery()
//                val rs   = use(stmt.executeQuery(stmt))
                QueryResults.fromResultSet(rs)
            )
            .toEither

}
