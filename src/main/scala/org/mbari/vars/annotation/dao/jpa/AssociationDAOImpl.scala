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

import org.mbari.vars.annotation.dao.AssociationDAO

import scala.collection.JavaConverters._

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T17:11:00
 */
class AssociationDAOImpl(entityManager: EntityManager)
  extends BaseDAO[AssociationImpl](entityManager)
  with AssociationDAO[AssociationImpl] {

  override def newPersistentObject(): AssociationImpl = new AssociationImpl

  override def newPersistentObject(
    linkName: String,
    toConcept: Option[String],
    linkValue: Option[String],
    mimeType: Option[String]): AssociationImpl = {
    val a = new AssociationImpl
    a.linkName = linkName
    toConcept.foreach(a.toConcept = _)
    linkValue.foreach(a.linkValue = _)
    mimeType.foreach(a.mimeType = _)
    a
  }

  override def findByLinkName(linkName: String): Iterable[AssociationImpl] =
    findByNamedQuery("Association.findByLinkName", Map("linkName" -> linkName))

  override def findByLinkNameAndVideoReferenceUUID(linkName: String, videoReferenceUUID: UUID): Iterable[AssociationImpl] =
    findByNamedQuery(
      "Association.findByLinkNameAndVideoReferenceUUID",
      Map("linkName" -> linkName, "videoReferenceUuid" -> videoReferenceUUID))

  override def findAll(): Iterable[AssociationImpl] =
    findByNamedQuery("Association.findAll")

  override def findAll(limit: Int, offset: Int): Iterable[AssociationImpl] =
    findByNamedQuery("Association.findAll", limit = Some(limit), offset = Some(offset))

  override def countByToConcept(toConcept: String): Int = {
    //val query = entityManager.createNativeQuery("Association.countByToConcept")
    val query = entityManager.createNamedQuery("Association.countByToConcept")
    query.setParameter(1, toConcept)
    query.getResultList
      .asScala
      .map(_.asInstanceOf[Int])
      .head

  }

  override def updateToConcept(oldToConcept: String, newToConcept: String): Int = {
    val query = entityManager.createNamedQuery("Association.updateToConcept")
    query.setParameter(1, newToConcept)
    query.setParameter(2, oldToConcept)
    query.executeUpdate()
  }
}
