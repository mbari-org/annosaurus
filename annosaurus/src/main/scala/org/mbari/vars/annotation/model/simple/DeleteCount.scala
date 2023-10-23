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

import java.util.UUID

import com.google.gson.annotations.Expose

/**
  * @author Brian Schlining
  * @since 2019-10-28T16:57:00
  */
class DeleteCount {
  @Expose(serialize = true)
  var videoReferenceUuid: UUID = _

  @Expose(serialize = true)
  var ancillaryDataCount: Int = 0

  @Expose(serialize = true)
  var imageReferenceCount: Int = 0

  @Expose(serialize = true)
  var associationCount: Int = 0

  @Expose(serialize = true)
  var observationCount: Int = 0

  @Expose(serialize = true)
  var imagedMomentCount: Int = 0

  @Expose(serialize = true)
  var errorMessage: String = _
}

object DeleteCount {
  def apply(
      videoReferenceUuid: UUID,
      imagedMomentCount: Int,
      imageReferenceCount: Int,
      observationCount: Int,
      associationCount: Int,
      ancillaryDataCount: Int
  ): DeleteCount = {
    val d = new DeleteCount
    d.videoReferenceUuid = videoReferenceUuid
    d.imagedMomentCount = imagedMomentCount
    d.imageReferenceCount = imageReferenceCount
    d.observationCount = observationCount
    d.associationCount = associationCount
    d.ancillaryDataCount = ancillaryDataCount
    d
  }

  def apply(videoReferenceUuid: UUID): DeleteCount = {
    val d = new DeleteCount
    d.videoReferenceUuid = videoReferenceUuid
    d
  }
}
