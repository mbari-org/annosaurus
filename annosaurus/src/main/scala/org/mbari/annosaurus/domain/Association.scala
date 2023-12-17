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

package org.mbari.annosaurus.domain

import org.mbari.annosaurus.repository.jpa.entity.AssociationEntity
import java.util.UUID
import org.mbari.annosaurus.model.MutableAssociation

case class Association(
    linkName: String,
    toConcept: String,
    linkValue: String,
    mimeType: Option[String] = None,
    uuid: Option[UUID] = None,
    lastUpdated: Option[java.time.Instant] = None
) extends ToSnakeCase[AssociationSC]
    with ToEntity[AssociationEntity] {

    override def toSnakeCase: AssociationSC =
        AssociationSC(linkName, toConcept, linkValue, mimeType, uuid, lastUpdated)

    def toEntity: AssociationEntity = {
        val a = AssociationEntity(linkName, toConcept, linkValue, mimeType.orNull)
        uuid.foreach(a.uuid = _)
        a
    }
}

object Association extends FromEntity[MutableAssociation, Association] {
    def from(entity: MutableAssociation): Association =
        Association(
            entity.linkName,
            entity.toConcept,
            entity.linkValue,
            Option(entity.mimeType),
            Option(entity.uuid),
            entity.lastUpdated
        )
}

case class AssociationSC(
    link_name: String,
    to_concept: String,
    link_value: String,
    mime_type: Option[String] = None,
    uuid: Option[UUID] = None,
    last_updated_time: Option[java.time.Instant] = None
) extends ToCamelCase[Association] {

    def toCamelCase: Association =
        Association(link_name, to_concept, link_value, mime_type, uuid, last_updated_time)
}
