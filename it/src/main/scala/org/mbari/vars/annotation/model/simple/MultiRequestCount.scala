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

/**
  * @author Brian Schlining
  * @since 2019-07-10T14:51:00
  */
class MultiRequestCount {

  @Expose(serialize = true)
  var multiRequest: MultiRequest = _

  @Expose(serialize = true)
  var count: Long = 0
}

object MultiRequestCount {
  def apply(multiRequest: MultiRequest, count: Long): MultiRequestCount = {
    val m = new MultiRequestCount
    m.multiRequest = multiRequest
    m.count = count
    m
  }
}
