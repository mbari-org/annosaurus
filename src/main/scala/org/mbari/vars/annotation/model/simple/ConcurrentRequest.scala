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

import java.time.Instant
import java.util
import java.util.{UUID, List => JList}

import com.google.gson.annotations.Expose

import scala.collection.JavaConverters._

/**
  * Data class enacpsulating information for concurrent requests. That is a request
  * of annotations from multiple videos between a given time span
  */
class ConcurrentRequest {

  @Expose(serialize = true)
  var startTimestamp: Instant = _

  @Expose(serialize = true)
  var endTimestamp: Instant = _

  @Expose(serialize = true)
  var videoReferenceUuids: JList[UUID] = new util.ArrayList[UUID]()

  def uuids: List[UUID] = videoReferenceUuids.asScala.toList

}

object ConcurrentRequest {
  def apply(
      startTimestamp: Instant,
      endTimestamp: Instant,
      videoReferenceUuids: Seq[UUID]
  ): ConcurrentRequest = {
    val cr = new ConcurrentRequest
    cr.startTimestamp = startTimestamp
    cr.endTimestamp = endTimestamp
    videoReferenceUuids.foreach(uuid => cr.videoReferenceUuids.add(uuid))
    cr
  }
}
