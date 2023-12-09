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

import org.mbari.annosaurus.Constants
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.Instant
import java.util.UUID

class QueryConstraintsSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {
  private val gson = Constants.GSON

  "QueryConstraints" should "serialize to/from json" in {
    val concepts = List("foo", "bar", "baz")
    val qc       = QueryConstraints(concepts)
    val json     = gson.toJson(qc)
    println(json)
    val qc1 = QueryConstraints.fromJson(json)
    qc1.conceptSeq() should contain theSameElementsAs concepts

  }

  it should "serialize to/from more complicated json" in {
    val concepts   = List("foo", "bar", "baz")
    val uuids      = List(UUID.randomUUID(), UUID.randomUUID())
    val observers  = List("brian", "schlin")
    val groups     = List("ROV", "AUV")
    val activities = List("descent", "transect")
    val qc = QueryConstraints(
      concepts,
      uuids,
      observers,
      groups,
      activities,
      Some(0),
      Some(90),
      Some(-20),
      Some(20),
      Some(-20),
      Some(20),
      Some(Instant.EPOCH),
      Some(Instant.now()),
      Some("eating"),
      Some("Aegina")
    )
    val json = gson.toJson(qc)
    println(json)
    val qc1 = QueryConstraints.fromJson(json)
    qc1.conceptSeq() should contain theSameElementsAs concepts
  }

}
