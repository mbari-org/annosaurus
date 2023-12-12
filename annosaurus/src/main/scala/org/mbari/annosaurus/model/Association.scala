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

package org.mbari.annosaurus.model

import org.mbari.annosaurus.PersistentObject

import java.time.Instant
import java.util.UUID

/** @author
  *   Brian Schlining
  * @since 2016-06-15T16:54:00
  */
trait Association extends PersistentObject {

    var uuid: UUID
    var observation: MutableObservation
    var linkName: String
    var toConcept: String
    var linkValue: String

    /** The mime-type of the linkValue
      */
    var mimeType: String
    def lastUpdated: Option[Instant]

}

object Association {

    val LINK_VALUE_NIL = "nil"

    val TO_CONCEPT_SELF = "self"

    val SEPARATOR = " | "

    def asString(association: Association): String = {
        association.linkName + SEPARATOR +
            Option(association.toConcept).getOrElse(TO_CONCEPT_SELF) + SEPARATOR +
            Option(association.linkValue).getOrElse(LINK_VALUE_NIL)
    }

}
