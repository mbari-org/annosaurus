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
import org.checkerframework.checker.units.qual.t

/** @author
  *   Brian Schlining
  * @since 2016-07-09T15:51:00
  */
class AssociationController(
    val daoFactory: JPADAOFactory,
    bus: Subject[Any] = MessageBus.RxSubject
) extends BaseController[AssociationEntity, AssociationDAO[AssociationEntity], Association] {

    type ADAO = AssociationDAO[AssociationEntity]

    private val associationPublisher = new AssociationPublisher(bus)

    override def newDAO(): AssociationDAO[AssociationEntity] = daoFactory.newAssociationDAO()

    override def transform(a: AssociationEntity): Association = Association.from(a, true)

    def create(
        observationUuid: UUID,
        linkName: String,
        toConcept: String,
        linkValue: String,
        mimeType: String,
        associationUuid: Option[UUID] = None
    )(implicit ec: ExecutionContext): Future[Association] = {
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
                    association
            }
        }
        exec(fn).map(entity => {
            val a = transform(entity) // transform after transaction is committed or UUID isn't set
            associationPublisher.publish(a)
            a
        })
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
                    transform(association)
                })
        }

        exec(fn)
    }

    def bulkUpdate(
        associations: Iterable[Association]
    )(implicit ec: ExecutionContext): Future[Iterable[Association]] = {
        def fn(dao: ADAO): Iterable[Association] =
            val validAssociations = associations.filter(_.uuid.isDefined)
            validAssociations.flatMap(a0 => {
                dao
                    .findByUUID(a0.uuid.get)
                    .map(a1 => {
                        a1.setLinkName(a0.linkName)
                        a1.setToConcept(a0.toConcept)
                        a1.setLinkValue(a0.linkValue)
                        a0.mimeType.foreach(a1.setMimeType)
                        transform(a1)
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
        def fn(dao: ADAO): Iterable[Association] = dao.findByLinkName(linkName).map(transform)
        exec(fn)
    }

    def findByLinkNameAndVideoReferenceUuid(linkName: String, videoReferenceUUID: UUID)(implicit
        ec: ExecutionContext
    ): Future[Iterable[Association]] = {
        def fn(dao: ADAO): Iterable[Association] =
            dao.findByLinkNameAndVideoReferenceUUID(linkName, videoReferenceUUID).map(transform)
        exec(fn)
    }

    def findByLinkNameAndVideoReferenceUuidAndConcept(
        linkName: String,
        videoReferenceUUID: UUID,
        concept: Option[String] = None
    )(implicit ec: ExecutionContext): Future[Iterable[Association]] = {
        def fn(dao: ADAO): Iterable[Association] =
            dao.findByLinkNameAndVideoReferenceUUIDAndConcept(linkName, videoReferenceUUID, concept)
                .map(transform)
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
