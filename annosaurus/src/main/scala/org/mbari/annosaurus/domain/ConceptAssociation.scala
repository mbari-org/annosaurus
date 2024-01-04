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

import java.util.UUID
import org.mbari.annosaurus.repository.jpa.entity.AssociationEntity
import org.mbari.annosaurus.repository.jpa.entity.ConceptAssociationDTO

final case class ConceptAssociation(
    uuid: UUID,
    videoReferenceUuid: UUID,
    concept: String,
    linkName: String,
    toConcept: String,
    linkValue: String,
    mimeType: String
) extends ToSnakeCase[ConceptAssociationSC] {
    def toSnakeCase: ConceptAssociationSC = ConceptAssociationSC(
        uuid,
        videoReferenceUuid,
        concept,
        linkName,
        toConcept,
        linkValue,
        mimeType
    )
}

object ConceptAssociation extends FromEntity[AssociationEntity, ConceptAssociation] {
    def from(entity: AssociationEntity, extend: Boolean = false): ConceptAssociation = {
        ConceptAssociation(
            entity.getUuid,
            entity.getObservation.getImagedMoment.getVideoReferenceUuid,
            entity.getObservation.getConcept,
            entity.getLinkName,
            entity.getToConcept,
            entity.getLinkValue,
            entity.getMimeType
        )
    }

    def fromDto(dto: ConceptAssociationDTO): ConceptAssociation =
        ConceptAssociation(
            dto.uuid,
            dto.videoReferenceUuid,
            dto.concept,
            dto.linkName,
            dto.toConcept,
            dto.linkValue,
            dto.mimeType
        )
}

final case class ConceptAssociationSC(
    uuid: UUID,
    video_reference_uuid: UUID,
    concept: String,
    link_name: String,
    to_concept: String,
    link_value: String,
    mime_type: String
) extends ToCamelCase[ConceptAssociation] {
    def toCamelCase: ConceptAssociation = ConceptAssociation(
        uuid,
        video_reference_uuid,
        concept,
        link_name,
        to_concept,
        link_value,
        mime_type
    )
}
