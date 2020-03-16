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

class ConceptAssociation {

  /**
    * This is the association UUID
    */
  @Expose(serialize = true)
  var uuid: UUID = _

  @Expose(serialize = true)
  var videoReferenceUuid: UUID = _

  @Expose(serialize = true)
  var concept: String = _

  @Expose(serialize = true)
  var linkName: String = _

  @Expose(serialize = true)
  var toConcept: String = _

  @Expose(serialize = true)
  var linkValue: String = _

  @Expose(serialize = true)
  var mimeType: String = _

}

object ConceptAssociation {
  def apply(
      uuid: UUID,
      videoReferenceUuid: UUID,
      concept: String,
      linkName: String,
      toConcept: String,
      linkValue: String,
      mimeType: String
  ): ConceptAssociation = {
    val ca = new ConceptAssociation
    ca.uuid = uuid
    ca.videoReferenceUuid = videoReferenceUuid
    ca.concept = concept
    ca.linkName = linkName
    ca.toConcept = toConcept
    ca.linkValue = linkValue
    ca.mimeType = mimeType
    ca
  }
}
