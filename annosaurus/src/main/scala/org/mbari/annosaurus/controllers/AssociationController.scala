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

package org.mbari.annosaurus.controllers

import java.util.UUID

import io.reactivex.rxjava3.subjects.Subject
import org.mbari.annosaurus.messaging.{AssociationPublisher, MessageBus}

import org.mbari.annosaurus.repository.{AssociationDAO, NotFoundInDatastoreException}

import scala.concurrent.{ExecutionContext, Future}
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import org.mbari.annosaurus.repository.jpa.entity.AssociationEntity
import org.mbari.annosaurus.domain.{
    Association,
    ConceptAssociation,
    ConceptAssociationRequest,
    ConceptAssociationResponse
}

/** @author
  *   Brian Schlining
  * @since 2016-07-09T15:51:00
  */
class AssociationController(
    val daoFactory: JPADAOFactory,
    bus: Subject[Any] = MessageBus.RxSubject
) extends BaseController[AssociationEntity, AssociationDAO[AssociationEntity]] {

    type ADAO = AssociationDAO[AssociationEntity]

    private[this] val associationPublisher = new AssociationPublisher(bus)

    override def newDAO(): AssociationDAO[AssociationEntity] = daoFactory.newAssociationDAO()

    def create(
        observationUuid: UUID,
        linkName: String,
        toConcept: String,
        linkValue: String,
        mimeType: String,
        associationUuid: Option[UUID] = None
    )(implicit ec: ExecutionContext): Future[AssociationEntity] = {
        def fn(dao: ADAO): AssociationEntity = {
            val obsDao = daoFactory.newObservationDAO(dao)
            obsDao.findByUUID(observationUuid) match {
                case None              =>
                    throw new NotFoundInDatastoreException(
                        s"MutableObservation with UUID of $observationUuid not found"
                    )
                case Some(observation) =>
                    val association =
                        dao.newPersistentObject(
                            linkName,
                            Some(toConcept),
                            Some(linkValue),
                            Some(mimeType)
                        )
                    associationUuid.foreach(association.setUuid)
                    observation.addAssociation(association)
                    associationPublisher.publish(Association.from(association))
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
    )(implicit ec: ExecutionContext): Future[Option[AssociationEntity]] = {

        def fn(dao: ADAO): Option[AssociationEntity] = {
            dao
                .findByUUID(uuid)
                .map(association => {
                    linkName.foreach(association.setLinkName)
                    toConcept.foreach(association.setToConcept)
                    linkValue.foreach(association.setLinkValue)
                    mimeType.foreach(association.setMimeType)
                    // Move to new observation if it exists
                    for {
                        obsUUID <- observationUUID
                        obsDao   = daoFactory.newObservationDAO(dao)
                        obs     <- obsDao.findByUUID(obsUUID)
                    } {
                        association.getObservation.removeAssociation(association)
                        obs.addAssociation(association)
                    }
                    associationPublisher.publish(Association.from(association))
                    association
                })
        }

        exec(fn)
    }

    def bulkUpdate(
        associations: Iterable[AssociationEntity]
    )(implicit ec: ExecutionContext): Future[Iterable[AssociationEntity]] = {
        def fn(dao: ADAO): Iterable[AssociationEntity] =
            associations.flatMap(a0 => {
                dao
                    .findByUUID(a0.getUuid)
                    .map(a1 => {
                        a1.setLinkName(a0.getLinkName)
                        a1.setToConcept(a0.getToConcept)
                        a1.setLinkValue(a0.getLinkValue)
                        a1.setMimeType(a0.getMimeType)
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
    )(implicit ec: ExecutionContext): Future[Iterable[AssociationEntity]] = {
        def fn(dao: ADAO): Iterable[AssociationEntity] = dao.findByLinkName(linkName)
        exec(fn)
    }

    def findByLinkNameAndVideoReferenceUUID(linkName: String, videoReferenceUUID: UUID)(implicit
        ec: ExecutionContext
    ): Future[Iterable[AssociationEntity]] = {
        def fn(dao: ADAO): Iterable[AssociationEntity] =
            dao.findByLinkNameAndVideoReferenceUUID(linkName, videoReferenceUUID)
        exec(fn)
    }

    def findByLinkNameAndVideoReferenceUUIDAndConcept(
        linkName: String,
        videoReferenceUUID: UUID,
        concept: Option[String] = None
    )(implicit ec: ExecutionContext): Future[Iterable[AssociationEntity]] = {
        def fn(dao: ADAO): Iterable[AssociationEntity] =
            dao.findByLinkNameAndVideoReferenceUUIDAndConcept(linkName, videoReferenceUUID, concept)
        exec(fn)
    }

    def findByConceptAssociationRequest(
        request: ConceptAssociationRequest
    )(implicit ec: ExecutionContext): Future[ConceptAssociationResponse] = {
        def fn(dao: ADAO): Iterable[ConceptAssociation] =
            dao.findByConceptAssociationRequest(request)
        exec(fn).map(ca => ConceptAssociationResponse(request, ca.toSeq))
    }

    def countByToConcept(concept: String)(implicit ec: ExecutionContext): Future[Long] = {
        def fn(dao: ADAO): Long = dao.countByToConcept(concept)
        exec(fn)
    }

    def updateToConcept(oldToConcept: String, newToConcept: String)(implicit
        ec: ExecutionContext
    ): Future[Int] = {
        def fn(dao: ADAO): Int = dao.updateToConcept(oldToConcept, newToConcept)
        exec(fn)
    }

}
