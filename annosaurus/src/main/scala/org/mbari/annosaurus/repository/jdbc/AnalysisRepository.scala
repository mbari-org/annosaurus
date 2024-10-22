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

import jakarta.persistence.{EntityManager, EntityManagerFactory}
import org.hibernate.jpa.QueryHints
import org.mbari.annosaurus.domain.{DepthHistogram, QueryConstraints, TimeHistogram}
import org.slf4j.LoggerFactory

import java.time.Instant
import scala.jdk.CollectionConverters.*

class AnalysisRepository(entityManagerFactory: EntityManagerFactory):

    private val log = LoggerFactory.getLogger(getClass)

    def depthHistogram(constraints: QueryConstraints, binSizeMeters: Int = 50): DepthHistogram =
        val select                       = DepthHistogramSQL.selectFromBinSize(binSizeMeters)
        val entityManager: EntityManager = entityManagerFactory.createEntityManager()
        val query                        = QueryConstraintsSqlBuilder.toQuery(constraints, entityManager, select, "")
        query.setHint(QueryHints.HINT_READONLY, true)
        val results                      = query.getResultList.iterator().next()
        entityManager.close()
        val values                       = results
            .asInstanceOf[Array[Object]]
            .map(s => s.asInt.getOrElse(0))
            .toList

        val binsMin = (0 until DepthHistogramSQL.MaxDepth by binSizeMeters).toList
        val binsMax = binsMin.map(_ + binSizeMeters)
        DepthHistogram(binsMin, binsMax, values)

    def timeHistogram(constraints: QueryConstraints, binSizeDays: Int = 30): TimeHistogram =
        val start                        = constraints.minTimestamp.getOrElse(TimeHistogramSQL.MinTime)
        val now                          = constraints.maxTimestamp.getOrElse(Instant.now())
        val select                       = TimeHistogramSQL.selectFromBinSize(start, now, binSizeDays)
        val entityManager: EntityManager = entityManagerFactory.createEntityManager()
        val query                        = QueryConstraintsSqlBuilder.toQuery(constraints, entityManager, select, "")
        query.setHint(QueryHints.HINT_READONLY, true)
        val results                      = query.getResultList.iterator().next()
        entityManager.close()
        val values                       = results
            .asInstanceOf[Array[Object]]
            .map(s => s.asInt.getOrElse(0))
            .toList

        val intervalMillis = binSizeDays * 24 * 60 * 60 * 1000L
        val binsMin        =
            (start.toEpochMilli until now.toEpochMilli by intervalMillis)
                .map(Instant.ofEpochMilli)

        val binsMax = binsMin
            .map(_.plusMillis(intervalMillis))

        TimeHistogram(binsMin, binsMax, values)
