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

import java.util
import java.util.{UUID, List => JList}

import scala.collection.JavaConverters._

import com.google.gson.annotations.Expose

/**
  * Data class for information needed to request all annotations from multiple
  * videos in a single REST call
 * @author Brian Schlining
 * @since 2019-07-10T14:10:00
 */
class MultiRequest {

  @Expose(serialize = true)
  var videoReferenceUuids: JList[UUID] = new util.ArrayList[UUID]()

  def uuids: List[UUID] = videoReferenceUuids.asScala.toList

}

object MultiRequest {
  def apply(videoReferenceUuids: Seq[UUID]): MultiRequest = {
    val mr = new MultiRequest
    videoReferenceUuids.foreach(uuid => mr.videoReferenceUuids.add(uuid))
    mr
  }
}
