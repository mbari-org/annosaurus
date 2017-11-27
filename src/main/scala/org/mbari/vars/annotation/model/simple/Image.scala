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

import java.net.URL
import java.time.{ Duration, Instant }
import java.util.UUID

import com.google.gson.annotations.{ Expose, SerializedName }
import org.mbari.vars.annotation.model.ImageReference
import org.mbari.vcr4j.time.Timecode

/**
 * Created by brian on 7/14/16.
 */
class Image {

  @Expose(serialize = true)
  var imageReferenceUuid: UUID = _

  @Expose(serialize = true)
  var format: String = _

  @Expose(serialize = true)
  var width: Int = _

  @Expose(serialize = true)
  var height: Int = _

  @Expose(serialize = true)
  var url: URL = _

  @Expose(serialize = true)
  var description: String = _

  @Expose(serialize = true)
  var videoReferenceUuid: UUID = _

  @Expose(serialize = true)
  var imagedMomentUuid: UUID = _

  @Expose(serialize = true)
  var timecode: Timecode = _

  @Expose(serialize = true)
  @SerializedName(value = "elapsed_time_millis")
  var elapsedTime: Duration = _

  @Expose(serialize = true)
  var recordedTimestamp: Instant = _

}

object Image {

  def apply(imageReference: ImageReference): Image = {
    val i = new Image
    i.imageReferenceUuid = imageReference.uuid
    i.format = imageReference.format
    i.width = imageReference.width
    i.height = imageReference.height
    i.url = imageReference.url
    i.description = imageReference.description
    i.videoReferenceUuid = imageReference.imagedMoment.videoReferenceUUID
    i.imagedMomentUuid = imageReference.imagedMoment.uuid
    i.timecode = imageReference.imagedMoment.timecode
    i.elapsedTime = imageReference.imagedMoment.elapsedTime
    i.recordedTimestamp = imageReference.imagedMoment.recordedDate
    i
  }
}
