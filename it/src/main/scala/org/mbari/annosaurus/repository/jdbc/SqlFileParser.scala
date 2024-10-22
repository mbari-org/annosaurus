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

import jakarta.persistence.EntityManager
import org.mbari.annosaurus.etc.jdk.Loggers.given

import java.net.URL
import java.sql.Connection

object SqlFileParser:

    def parseFile(sqlFile: String): Seq[String] =
        val sql = scala.io.Source.fromResource(sqlFile).mkString
        parseStatements(sql)

    def parseUrl(url: URL): Seq[String] =
        val sql = scala.io.Source.fromURL(url).mkString
        parseStatements(sql)

    def parseStatements(sql: String): Seq[String] =
        sql.split(";").toSeq.map(_.trim()).filter(_.nonEmpty)

        // val lines = sql.split("\n")
        // val statements = lines.foldLeft(Seq.empty[String]) { (acc, line) =>
        //     if line.trim.startsWith("--") then acc
        //     else if line.trim.endsWith(";") then acc :+ line
        //     else acc.init :+ (acc.last + line)
        // }
        // statements.map(_.trim).filterNot(_.isEmpty)

object SqlRunner:

    private val log = System.getLogger(getClass.getName)

    def run(sqlUrl: URL, connection: Connection): Unit =
        val statements = SqlFileParser.parseUrl(sqlUrl)
        run(statements, connection)

    def run(statements: Seq[String], connection: Connection): Unit =
        statements.foreach { sql =>
            val statement = connection.createStatement()
            statement.execute(sql)
            statement.close()
        }

    def run(sqlUrl: URL, entityManager: EntityManager): Unit =
        val statements = SqlFileParser.parseUrl(sqlUrl)
        run(statements, entityManager)

    def run(statements: Seq[String], entityManager: EntityManager): Unit =
        val transaction = entityManager.getTransaction()

        statements.foreach { sql =>
            log.atInfo.log(s"Running: $sql")
            transaction.begin()
            val statement = entityManager.createNativeQuery(sql)
            statement.executeUpdate()
            transaction.commit()
        }
