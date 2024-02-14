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

object DepthHistogramSQL {

    val MaxDepth: Int = 4000

    def selectFromBinSize(binSizeMeters: Int = 50): String = {
        val xs = for (i <- 0 until MaxDepth by binSizeMeters) yield {
            val j = i + binSizeMeters
            s"COUNT(CASE WHEN ad.depth_meters >= $i AND ad.depth_meters < $j THEN 1 END) AS \"$i-$j\""
        }

        s"""SELECT
       |  ${xs.mkString(",\n  ")}
       |""".stripMargin
    }

}
