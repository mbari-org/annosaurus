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

package org.mbari.vars.annotation.repository.jdbc

import org.mbari.annosaurus.model.simple.{DepthHistogram, QueryConstraints, TimeHistogram}
import org.slf4j.LoggerFactory

import jakarta.persistence.{EntityManager, EntityManagerFactory}
import scala.jdk.CollectionConverters._
import java.time.Instant

class AnalysisRepository(entityManagerFactory: EntityManagerFactory) {

  private[this] val log = LoggerFactory.getLogger(getClass)

  def depthHistogram(constraints: QueryConstraints, binSizeMeters: Int = 50): DepthHistogram = {
    val select                       = DepthHistogramSQL.selectFromBinSize(binSizeMeters)
    val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val query                        = QueryConstraints.toQuery(constraints, entityManager, select, "")
    val results                      = query.getResultList.asScala.toList
    entityManager.close()
    val values = results
      .head
      .asInstanceOf[Array[Object]]
      .map(s => s.toString.toInt)
    val binsMin = (0 until DepthHistogramSQL.MaxDepth by binSizeMeters).toArray
    val binsMax = binsMin.map(_ + binSizeMeters)
    DepthHistogram(binsMin, binsMax, values)
  }

  def timeHistogram(constraints: QueryConstraints, binSizeDays: Int = 30): TimeHistogram = {
    val now                          = Instant.now()
    val select                       = TimeHistogramSQL.selectFromBinSize(now, binSizeDays)
    val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val query                        = QueryConstraints.toQuery(constraints, entityManager, select, "")
    
    println(query)
    val results                      = query.getResultList.asScala.toList
    entityManager.close()
    val values = results
      .head
      .asInstanceOf[Array[Object]]
      .map(s => s.toString.toInt)
      .toIndexedSeq

    val intervalMillis = binSizeDays * 24 * 60 * 60 * 1000L
    val binsMin =
      (TimeHistogramSQL.MinTime.toEpochMilli until now.toEpochMilli by intervalMillis)
        .map(Instant.ofEpochMilli(_))

    val binsMax = binsMin
      .map(_.plusMillis(intervalMillis))

    TimeHistogram(binsMin, binsMax, values)

  }

}

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

object TimeHistogramSQL {

  val MinTime = Instant.parse("1987-01-01T00:00:00Z")

  def selectFromBinSize(maxInstant: Instant, binSizeDays: Int = 30): String = {
    val intervalMillis = binSizeDays * 24 * 60 * 60 * 1000L
    val start          = MinTime.toEpochMilli
    val end            = maxInstant.toEpochMilli()
    val xs = for (i <- start to end by intervalMillis) yield {
      val j = i + intervalMillis
      val date0 = new java.sql.Date(i)
      val date1 = new java.sql.Date(j)
      s"COUNT(CASE WHEN im.recorded_timestamp >= '$date0' AND im.recorded_timestamp < '$date1' THEN 1 END) AS \"$i-$j\""
    }

    s"""SELECT
       |  ${xs.mkString(",\n  ")}
       |""".stripMargin
  }

  // def selectFromBinSize(binSizeDays: Int = 30): String = {
  //   val xs = for (i <- 0 until 365 by binSizeDays) yield {
  //     val j = i + binSizeDays
  //     s"COUNT(CASE WHEN ad.time_utc >= '$MinTime' AND ad.time_utc < '$j' THEN 1 END) AS [$i-$j]"
  //   }

  //   s"""SELECT
  //      |  ${xs.mkString(",\n  ")}
  //      |""".stripMargin
  // }
}
