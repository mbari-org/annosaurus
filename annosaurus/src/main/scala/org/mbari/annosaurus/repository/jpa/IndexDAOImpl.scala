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

import java.util.UUID
import jakarta.persistence.EntityManager

import org.mbari.annosaurus.repository.IndexDAO
import org.mbari.annosaurus.repository.jpa.entity.IndexEntity
import org.mbari.annosaurus.repository.jpa.entity.ImagedMomentEntity

/**
 * @author
 *   Brian Schlining
 * @since 2019-02-08T08:55:00
 */
class IndexDAOImpl(entityManager: EntityManager) extends BaseDAO[IndexEntity](entityManager) with IndexDAO[IndexEntity]:

    def newPersistentObject(): IndexEntity = new IndexEntity

    override def findByVideoReferenceUuid(
        videoReferenceUuid: UUID,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): Iterable[IndexEntity] =
        findByNamedQuery(
            "Index.findByVideoReferenceUUID",
            Map("uuid" -> videoReferenceUuid),
            limit,
            offset
        )

    // --- These methods are deliberately overridden ---

    override def findAll(
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): Iterable[IndexEntity] =
        ???

    override def deleteByUUID(primaryKey: UUID): Unit = ???

    override def create(entity: IndexEntity): Unit = ???

    override def delete(entity: IndexEntity): Unit = ???
