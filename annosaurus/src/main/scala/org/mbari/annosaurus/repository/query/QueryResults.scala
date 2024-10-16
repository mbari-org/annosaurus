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

import java.sql.ResultSet
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

type QueryResults = List[(JDBC.Metadata, Seq[Any])]

object QueryResults {

    def fromResultSet(rs: ResultSet): QueryResults =
        val metadata = JDBC.Metadata.fromResultSet(rs)
        val results = ListBuffer[ListBuffer[Any]]()
        val numColumns = metadata.size
        var isNew = true
        while rs.next() do
            for i <- 1 to numColumns do
                if (isNew) results += ListBuffer[Any]()
                val column = results(i - 1)
                column += rs.getObject(i)
            isNew = false
        val columnData = results.result().map(_.result()) // Turn into immutable
        metadata.zip(columnData).toList


    def toTsv(queryResults: QueryResults): String =
        val header = queryResults.map(_._1.columnName).mkString("\t")
        val columnData = queryResults.map(_._2)
        val numRows      = columnData.headOption.map(_.size).getOrElse(0)
        val numCols     = columnData.size
        val sb = new StringBuilder(header)
        sb.append("\n")
        for i <- 0 until numRows do
            for j <- 0 until numCols do
                sb.append(columnData(j)(i))
                if j < numCols - 1 then sb.append("\t")
            sb.append("\n")
        sb.result()

}
