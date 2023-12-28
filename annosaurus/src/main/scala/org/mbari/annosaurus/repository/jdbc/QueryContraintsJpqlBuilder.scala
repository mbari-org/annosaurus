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


object QueryContraintsJpqlBuilder {

    def buildJpql(select: JpqlSelect, from: JpqlFrom, where: JpqlWhere, order: JpqlOrder): String =
        s"$select $from $where $order"

    def buildJpql(select: JpqlSelect, from: JpqlFrom, order: JpqlOrder): String =
        s"$select $from $order"

    object Annotation {

        val Order: JpqlOrder = JpqlOrder("ORDER BY obs.observationTimestamp")

        val Select: JpqlSelect = JpqlSelect("SELECT new org.mbari.annosaurus.repository.jpa.entity.AnnotationDTO(o.uuid, o.concept, o.observer, o.observationTimestamp, i.videoReferenceUuid, i.uuid, i.timecode, i.elapsedTime, i.recordedTimestamp, o.duration, o.group, o.activity)")

        val SelectCount: JpqlSelect = JpqlSelect("SELECT COUNT(o)")

        // https://www.baeldung.com/jpa-join-types
        val From: JpqlFrom = JpqlFrom("FROM ImagedMoment i JOIN i.observations o JOIN CachedVideoReferenceInfo vi ON i.videoReferenceUuid = vi.videoReferenceUuid")

        val FromWithImages: JpqlFrom = JpqlFrom(s"$From JOIN i.imageReferences ir")

        val FromWithImagesAndAssociations: JpqlFrom = JpqlFrom(s"$FromWithImages JOIN o.associations a")

        val FromWithData: JpqlFrom = JpqlFrom(s"$FromWithImagesAndAssociations JOIN i.ancillaryData d")


        val QueryAll: String = buildJpql(Select, From, Order)

        val QueryByVideoReferenceUuid: String = buildJpql(Select, From, JpqlWhere("WHERE i.videoReferenceUuid = :videoReferenceUuid"), Order)

        val QueryByConcept: String = buildJpql(Select, From, JpqlWhere("WHERE o.concept = :concept"), Order)

        val QueryByConceptWithImage: String = buildJpql(Select, FromWithImages, JpqlWhere("WHERE ir.url IS NOT NULL and o.concept = :concept"), Order)

        val QueryBetweenDates: String = buildJpql(Select, From, JpqlWhere("WHERE i.recordedTimestamp BETWEEN :start AND :end"), Order)

        val QueryByVideoReferenceUuidBetweenDates: String = buildJpql(Select, From,
            JpqlWhere("WHERE i.videoReferenceUuid = :uuid AND i.recordedTimestamp BETWEEN :start AND :end"), Order)

        val QueryByVideoReferenceUuidsBetweenDates: String = buildJpql(Select, From,
            JpqlWhere("WHERE i.videoReferenceUuid IN :uuids AND i.recordedTimestamp BETWEEN :start AND :end"), Order)

        val QueryByVideoReferenceUuids: String = buildJpql(Select, From,
            JpqlWhere("WHERE i.videoReferenceUuid IN :uuids"), Order)

        val QueryByImagedMomentUuids: String = buildJpql(Select, From,
            JpqlWhere("WHERE i.uuid in :uuids"), Order)

        val QueryByToConceptWithImages: String = buildJpql(Select, FromWithImagesAndAssociations,
            JpqlWhere("WHERE a.toConcept = :toConcept AND ir.url IS NOT NULL"), Order)


    }

}
