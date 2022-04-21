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
import org.mbari.vars.annotation.model.Association
import org.mbari.vars.annotation.model.simple.{ConceptAssociation, ConceptAssociationRequest}

import scala.jdk.CollectionConverters._

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
      mimeType: Option[String]
  ): AssociationImpl = {
    val a = new AssociationImpl
    a.linkName = linkName
    toConcept.foreach(a.toConcept = _)
    linkValue.foreach(a.linkValue = _)
    mimeType.foreach(a.mimeType = _)
    a
  }

  override def newPersistentObject(association: Association): AssociationImpl =
    AssociationImpl(association)

  override def findByLinkName(linkName: String): Iterable[AssociationImpl] =
    findByNamedQuery("Association.findByLinkName", Map("linkName" -> linkName))

  //  override def findByLinkNameAndVideoReferenceUUID(linkName: String, videoReferenceUUID: UUID): Iterable[AssociationImpl] =
  //    findByNamedQuery(
  //      "Association.findByLinkNameAndVideoReferenceUUID",
  //      Map("linkName" -> linkName, "videoReferenceUuid" -> videoReferenceUUID))

  override def findByLinkNameAndVideoReferenceUUID(
      linkName: String,
      videoReferenceUUID: UUID
  ): Iterable[AssociationImpl] = {
    findByLinkNameAndVideoReferenceUUIDAndConcept(linkName, videoReferenceUUID, None)
  }

  def findByLinkNameAndVideoReferenceUUIDAndConcept(
      linkName: String,
      videoReferenceUUID: UUID,
      concept: Option[String] = None
  ): Iterable[AssociationImpl] = {
    // HACK We are experiencing performance issues with the JPQL query. This
    // version is native SQL. Faster, but type casting is not pretty
    val query = entityManager.createNamedQuery("Association.findByLinkNameAndVideoReference")
    if (DatabaseProductName.isPostgres()) {
      query.setParameter(1, videoReferenceUUID)
    }
    else {
      query.setParameter(1, videoReferenceUUID.toString)
    }
    query.setParameter(2, linkName)
    // Concept -> Association map
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

    val sql =
      s"""
        |SELECT
        |  a.uuid,
        |  i.video_reference_uuid,
        |  o.concept,
        |  a.link_name,
        |  a.to_concept,
        |  a.link_value,
        |  a.mime_type
        |FROM
        |  associations a RIGHT JOIN
        |  observations o ON a.observation_uuid = o.uuid RIGHT JOIN
        |  imaged_moments i ON o.imaged_moment_uuid = i.uuid
        |WHERE
        |  i.video_reference_uuid IN ${request.uuids.mkString("('", "','", "')")} AND
        |  a.link_name = '${request.linkName}'
      """.stripMargin
    val query = entityManager.createNativeQuery(sql)
    // HACK: Dropping into SQL. Fast with ugly typecasting
    // a.uuid, i.video_reference_uuid, o.concept, a.link_name, a.to_concept, a.link_value, a.mime_type
    query
      .getResultList
      .asScala
      .map(obj => obj.asInstanceOf[Array[Object]])
      .map(obj => {
//        obj.foreach(println)
        val uuid               = UUID.fromString(obj(0).toString())
        val videoReferenceUuid = UUID.fromString(obj(1).toString())
        val conept             = obj(2).asInstanceOf[String]
        val linkName           = obj(3).asInstanceOf[String]
        val toConcept          = obj(4).asInstanceOf[String]
        val linkValue          = obj(5).asInstanceOf[String]
        val mimeType           = obj(6).asInstanceOf[String]
        ConceptAssociation(
          uuid,
          videoReferenceUuid,
          conept,
          linkName,
          toConcept,
          linkValue,
          mimeType
        )
      })
  }

  override def findAll(
      limit: Option[Int] = None,
      offset: Option[Int] = None
  ): Iterable[AssociationImpl] =
    findByNamedQuery("Association.findAll", limit = limit, offset = offset)

  override def countByToConcept(toConcept: String): Long = {
    //val query = entityManager.createNativeQuery("Association.countByToConcept")
    val query = entityManager.createNamedQuery("Association.countByToConcept")
    query.setParameter(1, toConcept)
    query
      .getResultList
      .asScala
      .map(_.asInstanceOf[Long])
      .head

  }

  override def updateToConcept(oldToConcept: String, newToConcept: String): Int = {
    val query = entityManager.createNamedQuery("Association.updateToConcept")
    query.setParameter(1, newToConcept)
    query.setParameter(2, oldToConcept)
    query.executeUpdate()
  }
}
