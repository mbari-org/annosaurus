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

type QueryResults = Map[JDBC.Metadata, Seq[Any]]

object QueryResults {

    def fromResultSet(rs: ResultSet): QueryResults =
        val metadata = JDBC.Metadata.fromResultSet(rs)
        val map = scala.collection.mutable.Map[JDBC.Metadata, mutable.ListBuffer[Any]]()
        while rs.next() do
            metadata.foreach { m =>
                val list = map.getOrElseUpdate(m, mutable.ListBuffer())
                list += rs.getObject(m.columnName)
            }
        val data = map.map { case (k, v) => k -> v.result() }.toMap
        data


}
