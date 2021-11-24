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

package org.mbari.vars.annotation.model.simple

import com.google.gson.annotations.Expose
import scala.jdk.CollectionConverters._
import java.time.Instant

class TimeHistogram {
  @Expose(serialize = true)
  var binsMin: java.util.List[Instant] = _

  @Expose(serialize = true)
  var binsMax: java.util.List[Instant] = _

  @Expose(serialize = true)
  var values: java.util.List[Int] = _
}

object TimeHistogram {

  def apply(binsMin: Seq[Instant], binsMax: Seq[Instant], values: Seq[Int]): TimeHistogram = {
    val histogram = new TimeHistogram
    histogram.binsMin = binsMin.asJava
    histogram.binsMax = binsMax.asJava
    histogram.values = values.asJava
    histogram
  }
}
