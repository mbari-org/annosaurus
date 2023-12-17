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

package org.mbari.annosaurus.repository.jdbc

import com.google.gson.annotations.Expose
import org.mbari.annosaurus.repository.jpa.MutableAnnotationImpl

/** @author
  *   Brian Schlining
  * @since 2019-10-22T14:34:00
  */
class MutableAnnotationExt extends MutableAnnotationImpl {
    @Expose(serialize = true)
    var ancillaryData: AncillaryDatumExt = _

    override def equals(obj: Any): Boolean = {
        if (!obj.isInstanceOf[MutableAnnotationExt]) false
        else {
            val other: MutableAnnotationExt = obj.asInstanceOf[MutableAnnotationExt]
            other.observationUuid != null &&
            this.observationUuid != null &&
            other.observationUuid == this.observationUuid
        }
    }

    override def hashCode(): Int = this.observationUuid.hashCode()

}
