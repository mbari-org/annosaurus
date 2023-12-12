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

package org.mbari.annosaurus.api.v1

import org.mbari.annosaurus.etc.circe.CirceCodecs
import org.scalatra.ScalatraServlet

import org.mbari.annosaurus.model.simple.HealthStatus
import org.mbari.annosaurus.etc.circe.CirceCodecs.{given, *}

class HealthApi extends ScalatraServlet {

  before() {
    contentType = "application/json"
  }

  get("/") {
    HealthStatus.default.stringify
  }

}
