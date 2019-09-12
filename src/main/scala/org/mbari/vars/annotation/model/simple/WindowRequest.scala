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


import java.time.Duration
import java.util
import java.util.{UUID, List => JList}

import com.google.gson.annotations.Expose

import scala.collection.JavaConverters._
/**
 * @author Brian Schlining
 * @since 2019-09-12T14:27:00
 */
class WindowRequest {

  @Expose(serialize = true)
  var videoReferenceUuids: JList[UUID] = new util.ArrayList[UUID]()

  def uuids: List[UUID] = videoReferenceUuids.asScala.toList

  @Expose(serialize = true)
  var imagedMomentUuid: UUID = _

  @Expose(serialize = true)
  var window: Duration = _

}

object WindowRequest {
  def apply(videoReferenceUuids: Seq[UUID], imagedMomentUuid: UUID, window: Duration): WindowRequest = {
    val wr = new WindowRequest
    videoReferenceUuids.foreach(uuid => wr.videoReferenceUuids.add(uuid))
    wr.imagedMomentUuid = imagedMomentUuid
    wr.window = window
    wr
  }
}
