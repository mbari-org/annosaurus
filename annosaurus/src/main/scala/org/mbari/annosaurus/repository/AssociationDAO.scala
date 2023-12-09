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

package org.mbari.annosaurus.repository

import org.mbari.annosaurus.model.Association
import org.mbari.annosaurus.model.simple.{ConceptAssociation, ConceptAssociationRequest}

import java.util.UUID

/** @author
  *   Brian Schlining
  * @since 2016-06-17T16:08:00
  */
trait AssociationDAO[T <: Association] extends DAO[T] {

    def newPersistentObject(
        linkName: String,
        toConcept: Option[String] = Some(Association.TO_CONCEPT_SELF),
        linkValue: Option[String] = Some(Association.LINK_VALUE_NIL),
        mimeType: Option[String] = Some("text/plain")
    ): T

    def newPersistentObject(association: Association): T

    def findByLinkName(linkName: String): Iterable[T]

    def findByLinkNameAndVideoReferenceUUID(linkName: String, videoReferenceUUID: UUID): Iterable[T]

    def findByLinkNameAndVideoReferenceUUIDAndConcept(
        linkName: String,
        videoReferenceUUID: UUID,
        concept: Option[String] = None
    ): Iterable[T]

    def findByConceptAssociationRequest(
        request: ConceptAssociationRequest
    ): Iterable[ConceptAssociation]

    def countByToConcept(toConcept: String): Long

    def updateToConcept(oldToConcept: String, newToConcept: String): Int

}
