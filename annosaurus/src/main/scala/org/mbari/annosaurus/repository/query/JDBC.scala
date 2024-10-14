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

import java.sql.{Connection, PreparedStatement, ResultSet}
import scala.collection.mutable.ListBuffer
import scala.util.Using

object JDBC {
    case class Metadata(columnName: String,
                        columnType: String,
                        columnSize: Int,
                        columnLabel: String,
                        columnClassName: String)
    object Metadata {
        def fromResultSet(rs: ResultSet): Seq[Metadata] = {
            val metadata = rs.getMetaData
            val n = metadata.getColumnCount
            for
                i <- 1 to n
                columnName = metadata.getColumnName(i)
                columnType = metadata.getColumnTypeName(i)
                columnSize = metadata.getColumnDisplaySize(i)
                columnLabel = metadata.getColumnLabel(i)
                columnClassName = metadata.getColumnClassName(i)
            yield {
                JDBC.Metadata(columnName, columnType, columnSize, columnLabel, columnClassName)
            }
        }
    }
}


class JDBC(user: String, password: String, url: String, driver: String) {

    def this(config: DatabaseConfig) = this(config.user, config.password, config.url, config.driver)

    def newConnection(): Connection = {
        Class.forName(driver)
        java.sql.DriverManager.getConnection(url, user, password)
    }

    def runQuery[T](sql: String, f: ResultSet => T): Either[Throwable, T] = {
        Using.Manager(use =>
            val conn = use(newConnection())
            conn.setReadOnly(true)
            val stmt = use(conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY))
            val rs = use(stmt.executeQuery(sql))
            f(rs)
        ).toEither
    }

    def runPreparedStatement(statement: PreparedStatement, f: ResultSet => Unit): Either[Throwable, Unit] = {
        Using.Manager(use =>
            val rs = use(statement.executeQuery())
            f(rs)
        ).toEither
    }

    def listColumnsMetadata(viewName: String): Either[Throwable, Seq[JDBC.Metadata]] = {
        val sql = s"SELECT * FROM $viewName LIMIT 1"
        runQuery(sql, rs => JDBC.Metadata.fromResultSet(rs))
    }

    def findDistinct[A](viewName: String, columName: String, transform: Object => Option[A]): Either[Throwable, Seq[A]] =
        val sql = s"SELECT DISTINCT $columName FROM $viewName"
        runQuery(sql, rs => {
            val values = ListBuffer.newBuilder[A]
            while (rs.next()) {
                transform(rs.getObject(columName)) match
                    case Some(value) => values += value
                    case None        => ()
            }
            values.result().toSeq
        })

    def countDistinct(viewName: String, columnName: String): Either[Throwable, Int] = {
        val sql = s"SELECT COUNT(DISTINCT $columnName) FROM $viewName"
        runQuery(sql, rs => {
            rs.next()
            rs.getInt(1)
        })
    }

    def findMinMax[A](viewName: String, columnName: String, transform: Object => Option[A]): Either[Throwable, Option[(A, A)]] = {
        val sql = s"SELECT MIN($columnName), MAX($columnName) FROM $viewName WHERE $columnName IS NOT NULL"
        runQuery(sql, rs => {
            rs.next()
            val minOpt = transform(rs.getObject(1))
            val maxOpt = transform(rs.getObject(2))
            for {
                min <- minOpt
                max <- maxOpt
            } yield (min, max)
        })
    }
}