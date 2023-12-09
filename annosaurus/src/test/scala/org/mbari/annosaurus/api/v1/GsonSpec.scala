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

import org.mbari.annosaurus.Constants
import org.mbari.vars.annotation.repository.jpa.entity.IndexEntity
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GsonSpec extends AnyFlatSpec with Matchers {

  "Gson" should "parse pythons isodate" in {
    val date = "2019-08-31T23:07:08.510000+00:00"
    val json = s"""{ "recorded_date": "$date"}"""
    val parsed = Constants.GSON.fromJson(json, classOf[IndexEntity])
    println(parsed)
    json should not be (null)
  }

  
}
