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

package org.mbari.vars.annotation.controllers

import java.util.UUID

import io.reactivex.subjects.Subject
import org.mbari.vars.annotation.dao.{AssociationDAO, NotFoundInDatastoreException}
import org.mbari.vars.annotation.messaging.{AssociationPublisher, MessageBus}
import org.mbari.vars.annotation.model.Association
import org.mbari.vars.annotation.model.simple.{
  ConceptAssociation,
  ConceptAssociationRequest,
  ConceptAssociationResponse
}

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-07-09T15:51:00
  */
class AssociationController(
    val daoFactory: BasicDAOFactory,
    bus: Subject[Any] = MessageBus.RxSubject
) extends BaseController[Association, AssociationDAO[Association]] {

  type ADAO = AssociationDAO[Association]

  private[this] val associationPublisher = new AssociationPublisher(bus)

  override def newDAO(): AssociationDAO[Association] = daoFactory.newAssociationDAO()

  def create(
      observationUuid: UUID,
      linkName: String,
      toConcept: String,
      linkValue: String,
      mimeType: String,
      associationUuid: Option[UUID] = None
  )(implicit ec: ExecutionContext): Future[Association] = {
    def fn(dao: ADAO): Association = {
      val obsDao = daoFactory.newObservationDAO(dao)
      obsDao.findByUUID(observationUuid) match {
        case None =>
          throw new NotFoundInDatastoreException(
            s"Observation with UUID of $observationUuid not found"
          )
        case Some(observation) =>
          val association =
            dao.newPersistentObject(linkName, Some(toConcept), Some(linkValue), Some(mimeType))
          associationUuid.foreach(uuid => association.uuid = uuid)
          observation.addAssociation(association)
          associationPublisher.publish(association)
          association
      }
    }
    exec(fn)
  }

  def update(
      uuid: UUID,
      observationUUID: Option[UUID] = None,
      linkName: Option[String] = None,
      toConcept: Option[String] = None,
      linkValue: Option[String] = None,
      mimeType: Option[String] = None
  )(implicit ec: ExecutionContext): Future[Option[Association]] = {

    def fn(dao: ADAO): Option[Association] = {
      dao
        .findByUUID(uuid)
        .map(association => {
          linkName.foreach(association.linkName = _)
          toConcept.foreach(association.toConcept = _)
          linkValue.foreach(association.linkValue = _)
          mimeType.foreach(association.mimeType = _)
          // Move to new observation if it exists
          for {
            obsUUID <- observationUUID
            obsDao  = daoFactory.newObservationDAO(dao)
            obs     <- obsDao.findByUUID(obsUUID)
          } {
            association.observation.removeAssociation(association)
            obs.addAssociation(association)
          }
          associationPublisher.publish(association)
          association
        })
    }

    exec(fn)
  }

  def bulkUpdate(
      associations: Iterable[Association]
  )(implicit ec: ExecutionContext): Future[Iterable[Association]] = {
    def fn(dao: ADAO): Iterable[Association] =
      associations.flatMap(a0 => {
        dao
          .findByUUID(a0.uuid)
          .map(a1 => {
            a1.linkName = a0.linkName
            a1.toConcept = a0.toConcept
            a1.linkValue = a0.linkValue
            a1.mimeType = a0.mimeType
            a1
          })
      })
    exec(fn)
  }

  def bulkDelete(uuids: Iterable[UUID])(implicit ec: ExecutionContext): Future[Unit] = {
    def fn(dao: ADAO): Unit = uuids.foreach(uuid => dao.deleteByUUID(uuid))
    exec(fn)
  }

  def findByLinkName(
      linkName: String
  )(implicit ec: ExecutionContext): Future[Iterable[Association]] = {
    def fn(dao: ADAO): Iterable[Association] = dao.findByLinkName(linkName)
    exec(fn)
  }

  def findByLinkNameAndVideoReferenceUUID(linkName: String, videoReferenceUUID: UUID)(
      implicit ec: ExecutionContext
  ): Future[Iterable[Association]] = {
    def fn(dao: ADAO): Iterable[Association] =
      dao.findByLinkNameAndVideoReferenceUUID(linkName, videoReferenceUUID)
    exec(fn)
  }

  def findByLinkNameAndVideoReferenceUUIDAndConcept(
      linkName: String,
      videoReferenceUUID: UUID,
      concept: Option[String] = None
  )(implicit ec: ExecutionContext): Future[Iterable[Association]] = {
    def fn(dao: ADAO): Iterable[Association] =
      dao.findByLinkNameAndVideoReferenceUUIDAndConcept(linkName, videoReferenceUUID, concept)
    exec(fn)
  }

  def findByConceptAssociationRequest(
      request: ConceptAssociationRequest
  )(implicit ec: ExecutionContext): Future[ConceptAssociationResponse] = {
    def fn(dao: ADAO): Iterable[ConceptAssociation] = dao.findByConceptAssociationRequest(request)
    exec(fn).map(ca => ConceptAssociationResponse(request, ca.toSeq))
  }

  def countByToConcept(concept: String)(implicit ec: ExecutionContext): Future[Long] = {
    def fn(dao: ADAO): Long = dao.countByToConcept(concept)
    exec(fn)
  }

  def updateToConcept(oldToConcept: String, newToConcept: String)(
      implicit ec: ExecutionContext
  ): Future[Int] = {
    def fn(dao: ADAO): Int = dao.updateToConcept(oldToConcept, newToConcept)
    exec(fn)
  }

}
