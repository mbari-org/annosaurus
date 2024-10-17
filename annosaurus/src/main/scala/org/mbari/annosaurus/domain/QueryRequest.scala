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

case class QueryRequest(
    where: Seq[ConstraintRequest],
    select: Option[Seq[String]] = None,
    limit: Option[Int] = None,
    offset: Option[Int] = None,
    concurrentObservations: Option[Boolean] = None,
    relatedAssociations: Option[Boolean] = None,
    distinct: Option[Boolean] = Some(true),
    strict: Option[Boolean] = Some(false),
    orderby: Option[Seq[String]] = None
)

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
