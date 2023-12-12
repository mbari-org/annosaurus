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

import java.util.UUID

import com.google.gson.annotations.Expose
import org.mbari.annosaurus.model.MutableAssociation

/**
  * This is a vewi of an association that includes the observationUUid. It's used
  * for serializing messages to external applications that need the UUID included
  *
  *
  * @author Brian Schlining
  * @since 2020-03-04T13:36:00
  */
case class ExtendedAssociation(
    @Expose(serialize = true) uuid: UUID,
    @Expose(serialize = true) observationUuid: UUID,
    @Expose(serialize = true) linkName: String,
    @Expose(serialize = true) toConcept: String = "self",
    @Expose(serialize = true) linkValue: String = "nil",
    @Expose(serialize = true) mimeType: String = "text/plain"
) {
  val objectType: String = "Association"
}

object ExtendedAssociation {
  def apply(a: MutableAssociation): ExtendedAssociation = {
    require(a.observation != null, "Can not extend an association without an observation")
    ExtendedAssociation(
      a.uuid,
      a.observation.uuid,
      a.linkName,
      a.toConcept,
      a.linkValue,
      a.mimeType
    )
  }
}
