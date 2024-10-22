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

import io.prometheus.metrics.shaded.com_google_protobuf_3_25_3.Timestamp

import java.time.Instant

object TimeHistogramSQL:

    val MinTime = Instant.parse("1987-01-01T00:00:00Z")

    def selectFromBinSize(
        minInstant: Instant,
        maxInstant: Instant,
        binSizeDays: Int = 30
    ): String =
        val intervalMillis = binSizeDays * 24 * 60 * 60 * 1000L
        val start          = minInstant.toEpochMilli
        val end            = maxInstant.toEpochMilli
        val xs             = for (i <- start to end by intervalMillis) yield
            val j     = i + intervalMillis
//            val date0 = new java.sql.Date(i)
//            val date1 = new java.sql.Date(j)
            val date0 = Instant.ofEpochMilli(i)
            val date1 = Instant.ofEpochMilli(j)
            s"COUNT(CASE WHEN im.recorded_timestamp >= '$date0' AND im.recorded_timestamp < '$date1' THEN 1 END) AS \"$i-$j\""

        s"""SELECT
       |  ${xs.mkString(",\n  ")}
       |""".stripMargin
