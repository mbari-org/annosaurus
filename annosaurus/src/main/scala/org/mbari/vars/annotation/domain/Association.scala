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

package org.mbari.vars.annotation.domain

import org.mbari.vars.annotation.repository.jpa.AssociationEntity

import java.util.UUID

case class Association(uuid: UUID, link_name: String, to_concept: String, link_value: String, mime_type: String) {
    def toEntity: AssociationEntity = {
        val a = AssociationEntity(link_name, to_concept, link_value, mime_type)
        a.uuid = uuid
        a
    }
}
