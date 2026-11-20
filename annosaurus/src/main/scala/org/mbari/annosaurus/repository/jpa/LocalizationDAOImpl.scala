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

package org.mbari.annosaurus.repository.jpa

import jakarta.persistence.EntityManager
import org.mbari.annosaurus.repository.LocalizationDAO
import org.mbari.annosaurus.repository.jpa.entity.LocalizationEntity

import java.util.UUID

class LocalizationDAOImpl(entityManager: EntityManager)
    extends BaseDAO[LocalizationEntity](entityManager)
    with LocalizationDAO[LocalizationEntity]:

    override def newPersistentObject(): LocalizationEntity = new LocalizationEntity

    override def findAll(limit: Option[Int] = None, offset: Option[Int] = None): Iterable[LocalizationEntity] =
        findByNamedQuery("Localization.findAll", limit = limit, offset = offset)

    override def findByObservationUuid(uuid: UUID): Seq[LocalizationEntity] =
        findByNamedQuery("Localization.findByObservationUuid", Map("uuid" -> uuid))

    override def findByObservationUuidAndType(uuid: UUID, localizationType: String): Option[LocalizationEntity] =
        findByNamedQuery(
            "Localization.findByObservationUuidAndType",
            Map("uuid" -> uuid, "localizationType" -> localizationType)
        ).headOption

    override def findByVideoReferenceUuid(
        uuid: UUID,
        limit: Option[Int],
        offset: Option[Int]
    ): Seq[LocalizationEntity] =
        findByNamedQuery("Localization.findByVideoReferenceUuid", Map("uuid" -> uuid), limit, offset)
