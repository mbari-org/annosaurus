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

import java.time.{Duration, Instant}
import java.util.UUID
import io.reactivex.rxjava3.subjects.Subject
import org.mbari.annosaurus.messaging.{AnnotationPublisher, MessageBus}
import org.mbari.annosaurus.repository.{NotFoundInDatastoreException, ObservationDAO}

import scala.concurrent.{ExecutionContext, Future}
import org.mbari.annosaurus.repository.jpa.entity.{ImagedMomentEntity, ObservationEntity}
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import org.mbari.annosaurus.domain.Observation

/** @author
  *   Brian Schlining
  * @since 2016-06-25T20:33:00
  */
class ObservationController(
    val daoFactory: JPADAOFactory,
    bus: Subject[Any] = MessageBus.RxSubject
) extends BaseController[ObservationEntity, ObservationDAO[ObservationEntity]] {

    type ODAO = ObservationDAO[ObservationEntity]

    private[this] val annotationPublisher = new AnnotationPublisher(bus)

    override def newDAO(): ODAO = daoFactory.newObservationDAO()

    def create(
        imagedMomentUUID: UUID,
        concept: String,
        observer: String,
        observationDate: Instant = Instant.now(),
        duration: Option[Duration] = None,
        group: Option[String] = None
    )(implicit ec: ExecutionContext): Future[ObservationEntity] = {

        def fn(dao: ODAO): ObservationEntity = {
            val imDao = daoFactory.newImagedMomentDAO(dao)
            imDao.findByUUID(imagedMomentUUID) match {
                case None               =>
                    throw new NotFoundInDatastoreException(
                        s"ImagedMoment with UUID of $imagedMomentUUID not found"
                    )
                case Some(imagedMoment) =>
                    val observation = new ObservationEntity(
                        concept,
                        duration.orNull,
                        observationDate,
                        observer,
                        group.orNull,
                        null
                    )
                    observation.setImagedMoment(imagedMoment)
                    annotationPublisher.publish(Observation.from(observation))
                    observation
            }
        }

        exec(fn)
    }

    def update(
        uuid: UUID,
        concept: Option[String] = None,
        observer: Option[String] = None,
        observationDate: Instant = Instant.now(),
        duration: Option[Duration] = None,
        group: Option[String] = None,
        activity: Option[String] = None,
        imagedMomentUUID: Option[UUID] = None
    )(implicit ec: ExecutionContext): Future[Option[ObservationEntity]] = {

        def fn(dao: ODAO): Option[ObservationEntity] = {
            // --- 1. Does uuid exist?
            val observation = dao.findByUUID(uuid)

            observation.map(obs => {
                concept.foreach(obs.setConcept)
                observer.foreach(obs.setObserver)
                obs.setObservationTimestamp(observationDate)
                duration.foreach(obs.setDuration)
                group.foreach(obs.setGroup)
                activity.foreach(obs.setActivity)
                for {
                    imUUID <- imagedMomentUUID
                    imDao   = daoFactory.newImagedMomentDAO(dao)
                    newIm  <- imDao.findByUUID(imUUID)
                } {
                    obs.getImagedMoment.removeObservation(obs)
                    newIm.addObservation(obs)
                }

                annotationPublisher.publish(Observation.from(obs))
                obs
            })
        }

        exec(fn)

    }

    def findAllConcepts(implicit ec: ExecutionContext): Future[Iterable[String]] = {
        def fn(dao: ODAO): Iterable[String] = dao.findAllConcepts()
        exec(fn)
    }

    def findAllGroups(implicit ec: ExecutionContext): Future[Iterable[String]] = {
        def fn(dao: ODAO): Iterable[String] = dao.findAllGroups()
        exec(fn)
    }

    def findAllActivities(implicit ec: ExecutionContext): Future[Iterable[String]] = {
        def fn(dao: ODAO): Iterable[String] = dao.findAllActivities()
        exec(fn)
    }

    def findAllConceptsByVideoReferenceUUID(
        uuid: UUID
    )(implicit ec: ExecutionContext): Future[Iterable[String]] = {
        def fn(dao: ODAO): Iterable[String] = dao.findAllConceptsByVideoReferenceUUID(uuid)
        exec(fn)
    }

    def findByVideoReferenceUUID(uuid: UUID, limit: Option[Int] = None, offset: Option[Int] = None)(
        implicit ec: ExecutionContext
    ): Future[Iterable[ObservationEntity]] = {
        def fn(dao: ODAO): Iterable[ObservationEntity] =
            dao.findByVideoReferenceUUID(uuid, limit, offset)
        exec(fn)
    }

    def findByAssociationUUID(
        uuid: UUID
    )(implicit ec: ExecutionContext): Future[Option[ObservationEntity]] = {
        def fn(dao: ODAO): Option[ObservationEntity] = {
            val adao = daoFactory.newAssociationDAO(dao)
            adao.findByUUID(uuid).map(_.getObservation)
        }
        exec(fn)
    }

    /** This controller will also delete the [[MutableImagedMoment]] if it is empty (i.e. no
      * observations or other imageReferences)
      *
      * @param uuid
      * @param ec
      * @return
      */
    override def delete(uuid: UUID)(implicit ec: ExecutionContext): Future[Boolean] = {
        def fn(dao: ODAO) = deleteFunction(dao, uuid)
        exec(fn)
    }

    def deleteDuration(
        uuid: UUID
    )(implicit ec: ExecutionContext): Future[Option[ObservationEntity]] = {
        def fn(dao: ODAO): Option[ObservationEntity] =
            dao
                .findByUUID(uuid)
                .map(obs => {
                    obs.setDuration(null)
                    obs
                })
        exec(fn)
    }

    def bulkDelete(uuids: Iterable[UUID])(implicit ec: ExecutionContext): Future[Boolean] = {
        def fn(dao: ODAO): Boolean =
            uuids.map(deleteFunction(dao, _)).toSeq.forall(b => b)
        exec(fn)
    }

    def countByConcept(concept: String)(implicit ec: ExecutionContext): Future[Int] = {
        def fn(dao: ODAO): Int = dao.countByConcept(concept)
        exec(fn)
    }

    def countByConceptWithImages(concept: String)(implicit ec: ExecutionContext): Future[Int] = {
        def fn(dao: ODAO): Int = dao.countByConceptWithImages(concept)
        exec(fn)
    }

    def countByVideoReferenceUUID(uuid: UUID)(implicit ec: ExecutionContext): Future[Int] = {
        def fn(dao: ODAO): Int = dao.countByVideoReferenceUUID(uuid)
        exec(fn)
    }

    def countByVideoReferenceUUIDAndTimestamps(uuid: UUID, start: Instant, end: Instant)(implicit
        ec: ExecutionContext
    ): Future[Int] = {
        def fn(dao: ODAO): Int = dao.countByVideoReferenceUUIDAndTimestamps(uuid, start, end)
        exec(fn)
    }

    def countAllGroupByVideoReferenceUUID()(implicit ec: ExecutionContext): Future[Map[UUID, Int]] =
        exec(dao => dao.countAllByVideoReferenceUuids())

    def updateConcept(oldConcept: String, newConcept: String)(implicit
        ec: ExecutionContext
    ): Future[Int] = {
        def fn(dao: ODAO): Int = dao.updateConcept(oldConcept, newConcept)
        exec(fn)
    }

    private def deleteFunction(dao: ODAO, uuid: UUID): Boolean = {
        dao.findByUUID(uuid) match {
            case None              => false
            case Some(observation) =>
                val imagedMoment = observation.getImagedMoment
                // If this is the only observation and there are no imagerefs, delete the imagemoment
                if (imagedMoment.getObservations.size == 1 && imagedMoment.getImageReferences.isEmpty) {
                    val imDao = daoFactory.newImagedMomentDAO(dao)
                    imDao.delete(imagedMoment)
                }
                else {
                    dao.delete(observation)
                }
                true
        }
    }

}
