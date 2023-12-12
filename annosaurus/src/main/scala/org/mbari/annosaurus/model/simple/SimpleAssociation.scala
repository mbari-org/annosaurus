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

import org.mbari.annosaurus.model.MutableAssociation

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-07-11T14:54:00
  */
case class SimpleAssociation(uuid: UUID, linkName: String, toConcept: String, linkValue: String)

object SimpleAssociation {
  def apply(association: MutableAssociation): SimpleAssociation =
    new SimpleAssociation(
      association.uuid,
      association.linkName,
      association.toConcept,
      association.linkValue
    )
}
