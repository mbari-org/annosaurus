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

class QueryConstraintsResponse[A] {

  @Expose(serialize = true)
  var queryConstraints: QueryConstraints = _

  @Expose(serialize = true)
  var content: A = _
}

object QueryConstraintsResponse {
  def apply[A](queryConstraints: QueryConstraints, content: A): QueryConstraintsResponse[A] = {
    val qc = new QueryConstraintsResponse[A]
    qc.queryConstraints = queryConstraints
    qc.content = content
    qc
  }
}
