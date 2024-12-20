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

final case class ConceptAssociationResponse(
    conceptAssociationRequest: ConceptAssociationRequest,
    conceptAssociations: Seq[ConceptAssociation]
) extends ToSnakeCase[ConceptAssociationResponseSC]:
    def toSnakeCase: ConceptAssociationResponseSC = ConceptAssociationResponseSC(
        conceptAssociationRequest.toSnakeCase,
        conceptAssociations.map(_.toSnakeCase)
    )

final case class ConceptAssociationResponseSC(
    concept_association_request: ConceptAssociationRequestSC,
    concept_associations: Seq[ConceptAssociationSC]
) extends ToCamelCase[ConceptAssociationResponse]:
    def toCamelCase: ConceptAssociationResponse = ConceptAssociationResponse(
        concept_association_request.toCamelCase,
        concept_associations.map(_.toCamelCase)
    )
