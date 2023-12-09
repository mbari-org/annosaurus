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

package org.mbari.annosaurus.model.simple

import com.google.gson.annotations.Expose
import scala.jdk.CollectionConverters._

class DepthHistogram {

  @Expose(serialize = true)
  var binsMin: java.util.List[Int] = _

  @Expose(serialize = true)
  var binsMax: java.util.List[Int] = _

  @Expose(serialize = true)
  var values: java.util.List[Int] = _

}

object DepthHistogram {
  def apply(binsMin: Array[Int], binsMax: Array[Int], values: Array[Int]): DepthHistogram = {
    val obj = new DepthHistogram
    obj.binsMin = binsMin.toList.asJava
    obj.binsMax = binsMax.toList.asJava
    obj.values = values.toList.asJava
    obj
  }
}
