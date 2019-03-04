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

package org.mbari.vars.annotation.dao.jpa

import java.util.UUID

import javax.persistence.EntityManager
import org.mbari.vars.annotation.dao.IndexDAO
import org.mbari.vars.annotation.model.ImagedMoment

/**
 * @author Brian Schlining
 * @since 2019-02-08T08:55:00
 */
class IndexDAOImpl(entityManager: EntityManager)
  extends BaseDAO[IndexImpl](entityManager)
  with IndexDAO[IndexImpl] {

  def newPersistentObject(): IndexImpl = new IndexImpl

  override def findByUUID(uuid: UUID): Option[IndexImpl] =
    findByNamedQuery("Index.findByUuid", Map("uuid" -> uuid))
      .headOption

  override def findByVideoReferenceUuid(
    videoReferenceUuid: UUID,
    limit: Option[Int] = None,
    offset: Option[Int] = None): Iterable[ImagedMoment] =
    findByNamedQuery("Index.findByVideoReferenceUUID", Map("uuid" -> videoReferenceUuid), limit, offset)

  // --- These methods are deliberately overridden ---

  override def findAll(): Iterable[IndexImpl] = ???

  override def findAll(limit: Int, offset: Int): Iterable[IndexImpl] = ???

  override def deleteByUUID(primaryKey: UUID): Unit = ???

  override def create(entity: IndexImpl): Unit = ???

  override def delete(entity: IndexImpl): Unit = ???
}
