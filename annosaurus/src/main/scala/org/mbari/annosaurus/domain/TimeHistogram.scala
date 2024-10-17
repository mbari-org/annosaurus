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

final case class TimeHistogram(binsMin: Seq[Instant], binsMax: Seq[Instant], values: Seq[Int])
    extends ToSnakeCase[TimeHistogramSC]:
    override def toSnakeCase: TimeHistogramSC = TimeHistogramSC(binsMin, binsMax, values)

    def count: Int = values.sum

final case class TimeHistogramSC(bins_min: Seq[Instant], bins_max: Seq[Instant], values: Seq[Int])
    extends ToCamelCase[TimeHistogram]:
    override def toCamelCase: TimeHistogram = TimeHistogram(bins_min, bins_max, values)

    def count: Int = values.sum
