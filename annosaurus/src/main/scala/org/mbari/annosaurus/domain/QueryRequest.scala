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

package org.mbari.annosaurus.domain

import java.time.Instant

/**
 * QueryRequest is a case class that represents a query (i.e. SQL) request.
 * @param where
 *   A list of constraints to apply to the query
 * @param select
 *   A list of columns to return
 * @param limit
 *   The maximum number of rows to return
 * @param offset
 *   The number of rows to skip before returning results
 * @param concurrentObservations
 *   Whether to include concurrent observations in the query (i.e. observations that are part of the same moment in time
 *   on a video)
 * @param relatedAssociations
 *   Whether to include related associations in the query (i.e. associations that are related to the observations in the
 *   query)
 * @param distinct
 *   Whether to return distinct rows (default is false)
 * @param strict
 *   Whether to use strict mode or to 'enhance' the query by adding additional columns useful for grouping annotations
 *   if either concurrentObservations or relatedAssociations are true, strict is set to false regardless of the value
 *   passed in. default is true
 * @param orderBy
 *   A list of columns to order the results by
 */
case class QueryRequest(
    select: Option[Seq[String]] = None,
    distinct: Option[Boolean] = None,
    where: Option[Seq[ConstraintRequest]] = None,
    orderBy: Option[Seq[String]] = None,
    limit: Option[Int] = None,
    offset: Option[Int] = None,
    concurrentObservations: Option[Boolean] = None,
    relatedAssociations: Option[Boolean] = None,
    strict: Option[Boolean] = None
)

/**
 * ConstraintRequest is a case class that represents a constraint to apply to a query. It can have exactly 2 fields:
 * column and one of the following fields: between, contains, equals, in, isnull, like, max, min, minmax.
 * @param column The column to apply the constraint to
 * @param between A list of two numeric, or dat2 (as iso8601) values to apply a between constraint
 * @param contains Maps to a SQL 'LIKE' clause with the value wrapped in '%' characters
 * @param equals A value to apply an equals constraint
 * @param in A list of values to apply an 'IN' constraint
 * @param isnull A boolean value to apply an 'IS NULL' constraint. If true, the constraint is 'IS NULL', if false, the
 *               constraint is 'IS NOT NULL'
 * @param like A value to apply a 'LIKE' constraint. You must apply the '%' characters to the value before passing it
 * @param max The maximum value to apply a '<=' constraint
 * @param min The minimum value to apply a '>=' constraint
 * @param minmax A list of 2 numeric values to apply a 'BETWEEN' constraint
 */
case class ConstraintRequest(
    column: String,
    between: Option[Seq[Instant]] = None,
    contains: Option[String] = None,
    equals: Option[String] = None,
    in: Option[Seq[String]] = None,
    isnull: Option[Boolean] = None,
    like: Option[String] = None,
    max: Option[Double] = None,
    min: Option[Double] = None,
    minmax: Option[Seq[Double]] = None
)
