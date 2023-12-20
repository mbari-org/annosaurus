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

import org.mbari.annosaurus.repository.AssociationDAO
import org.mbari.annosaurus.repository.jpa.entity.AssociationEntity

import scala.jdk.CollectionConverters._
import org.mbari.annosaurus.domain.{ConceptAssociation, ConceptAssociationRequest}

/** @author
  *   Brian Schlining
  * @since 2016-06-17T17:11:00
  */
class AssociationDAOImpl(entityManager: EntityManager)
    extends BaseDAO[AssociationEntity](entityManager)
    with AssociationDAO[AssociationEntity] {

    override def newPersistentObject(): AssociationEntity = new AssociationEntity

    override def newPersistentObject(
        linkName: String,
        toConcept: Option[String],
        linkValue: Option[String],
        mimeType: Option[String]
    ): AssociationEntity = {
        val a = new AssociationEntity
        a.linkName = linkName
        toConcept.foreach(a.toConcept = _)
        linkValue.foreach(a.linkValue = _)
        mimeType.foreach(a.mimeType = _)
        a
    }

    override def newPersistentObject(association: AssociationEntity): AssociationEntity =
        AssociationEntity(association)

    override def findByLinkName(linkName: String): Iterable[AssociationEntity] =
        findByNamedQuery("Association.findByLinkName", Map("linkName" -> linkName))

    //  override def findByLinkNameAndVideoReferenceUUID(linkName: String, videoReferenceUUID: UUID): Iterable[AssociationImpl] =
    //    findByNamedQuery(
    //      "MutableAssociation.findByLinkNameAndVideoReferenceUUID",
    //      Map("linkName" -> linkName, "videoReferenceUuid" -> videoReferenceUUID))

    override def findByLinkNameAndVideoReferenceUUID(
        linkName: String,
        videoReferenceUUID: UUID
    ): Iterable[AssociationEntity] = {
        findByLinkNameAndVideoReferenceUUIDAndConcept(linkName, videoReferenceUUID, None)
    }

    def findByLinkNameAndVideoReferenceUUIDAndConcept(
        linkName: String,
        videoReferenceUUID: UUID,
        concept: Option[String] = None
    ): Iterable[AssociationEntity] = {
        // HACK We are experiencing performance issues with the JPQL query. This
        // version is native SQL. Faster, but type casting is not pretty
        val query  = entityManager.createNamedQuery("Association.findByLinkNameAndVideoReference")
        // if (DatabaseProductName.isPostgreSQL()) {
        //   query.setParameter(1, videoReferenceUUID)
        // }
        // else {
        //   query.setParameter(1, videoReferenceUUID.toString)
        // }
        setUuidParameter(query, 1, videoReferenceUUID)
        query.setParameter(2, linkName)
        // Concept -> MutableAssociation map
        val tuples = query
            .getResultList
            .asScala
            .map(obj => obj.asInstanceOf[Array[Object]])
            .map(obj =>
                obj(0) -> {
                    val ass = newPersistentObject(
                        obj(2).asInstanceOf[String],
                        Option(obj(3).asInstanceOf[String]),
                        Option(obj(4).asInstanceOf[String]),
                        Option(obj(5).asInstanceOf[String])
                    )

                    ass.uuid = UUID.fromString(obj(1).toString)
                    ass
                }
            )

        // Filter for a particular concept name
        concept match {
            case None    => tuples.map(_._2)
            case Some(c) => tuples.filter(_._1 == c).map(_._2)
        }
    }

    def findByConceptAssociationRequest(
        request: ConceptAssociationRequest
    ): Iterable[ConceptAssociation] = {

        findByTypedNamedQuery[ConceptAssociation](
            "Association.findByConceptAssociationRequest",
            Map(
                "uuids"    -> request.videoReferenceUuids.toList.asJava,
                "linkName" -> request.linkName
            )
        )

    }

    override def findAll(
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): Iterable[AssociationEntity] =
        findByNamedQuery("Association.findAll", limit = limit, offset = offset)

    override def countByToConcept(toConcept: String): Long = {
        // val query = entityManager.createNativeQuery("MutableAssociation.countByToConcept")
        val query = entityManager.createNamedQuery("Association.countByToConcept")
        query.setParameter(1, toConcept)
        query
            .getResultList
            .asScala
            .map(_.toString().toLong)
            .head

    }

    override def updateToConcept(oldToConcept: String, newToConcept: String): Int = {
        val query = entityManager.createNamedQuery("Association.updateToConcept")
        query.setParameter(1, newToConcept)
        query.setParameter(2, oldToConcept)
        query.executeUpdate()
    }
}
