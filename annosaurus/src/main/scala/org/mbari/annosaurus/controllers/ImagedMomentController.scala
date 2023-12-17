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

import java.io.Closeable
import java.time.{Duration, Instant}
import java.util.UUID
//
//import org.mbari.vars.annotation.dao.jdbc.JdbcRepository
//import org.mbari.vars.annotation.dao.jpa.JPADAOFactory
import org.mbari.annosaurus.model.MutableImagedMoment
import org.mbari.annosaurus.model.simple.WindowRequest
import org.mbari.annosaurus.repository.{DAO, ImagedMomentDAO, NotFoundInDatastoreException}
import org.mbari.vcr4j.time.Timecode
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

/** @author
  *   Brian Schlining
  * @since 2016-06-17T16:06:00
  */
class ImagedMomentController(val daoFactory: BasicDAOFactory)
    extends BaseController[MutableImagedMoment, ImagedMomentDAO[MutableImagedMoment]] {

    protected type IMDAO = ImagedMomentDAO[MutableImagedMoment]

//  // HACK. Assumes daoFactory is JPA
//  private[this] val jdbcRepository = new JdbcRepository(
//    daoFactory.asInstanceOf[JPADAOFactory].entityManagerFactory
//  )

    override def newDAO(): IMDAO = daoFactory.newImagedMomentDAO()

    override def findAll(limit: Option[Int] = None, offset: Option[Int] = None)(implicit
        ec: ExecutionContext
    ): Future[Iterable[MutableImagedMoment]] =
        exec(d => d.findAll(limit, offset))

    def countAll()(implicit
        ec: ExecutionContext
    ): Future[Int] =
        exec(d => d.countAll())

    def countByVideoReferenceUUIDWithImages(videoReferenceUUID: UUID)(implicit
        ec: ExecutionContext
    ): Future[Int] =
        exec(d => d.countByVideoReferenceUUIDWithImages(videoReferenceUUID))

    def findWithImages(limit: Option[Int] = None, offset: Option[Int] = None)(implicit
        ec: ExecutionContext
    ): Future[Iterable[MutableImagedMoment]] =
        exec(d => d.findWithImages(limit, offset))

    def countWithImages()(implicit
        ec: ExecutionContext
    ): Future[Int] =
        exec(d => d.countWithImages())

    def findByLinkName(linkName: String, limit: Option[Int] = None, offset: Option[Int] = None)(
        implicit ec: ExecutionContext
    ): Future[Iterable[MutableImagedMoment]] =
        exec(d => d.findByLinkName(linkName, limit, offset))

    def countByLinkName(linkName: String)(implicit
        ec: ExecutionContext
    ): Future[Int] =
        exec(d => d.countByLinkName(linkName))

    def findAllVideoReferenceUUIDs(limit: Option[Int] = None, offset: Option[Int] = None)(implicit
        ec: ExecutionContext
    ): Future[Iterable[UUID]] =
        exec(d => d.findAllVideoReferenceUUIDs(limit, offset))

    def findByVideoReferenceUUID(uuid: UUID, limit: Option[Int] = None, offset: Option[Int] = None)(
        implicit ec: ExecutionContext
    ): Future[Iterable[MutableImagedMoment]] =
        exec(d => d.findByVideoReferenceUUID(uuid, limit, offset))

    /** @param uuid
      * @param limit
      * @param offset
      * @return
      *   A butple of a closeable, and a stream. When the stream is done being processed invoke the
      *   closeable
      */
    def streamByVideoReferenceUUID(
        uuid: UUID,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): (Closeable, java.util.stream.Stream[MutableImagedMoment]) = {
        val dao = daoFactory.newImagedMomentDAO()
        (() => dao.close(), dao.streamByVideoReferenceUUID(uuid, limit, offset))
    }

    def findByImageReferenceUUID(
        uuid: UUID
    )(implicit ec: ExecutionContext): Future[Option[MutableImagedMoment]] = {
        def fn(dao: IMDAO): Option[MutableImagedMoment] = {
            val irDao = daoFactory.newImageReferenceDAO(dao)
            irDao.findByUUID(uuid).map(_.imagedMoment)
        }
        exec(fn)
    }

    def findByObservationUUID(
        uuid: UUID
    )(implicit ec: ExecutionContext): Future[Option[MutableImagedMoment]] = {
        def fn(dao: IMDAO): Option[MutableImagedMoment] = {
            val obsDao = daoFactory.newObservationDAO(dao)
            obsDao.findByUUID(uuid).map(_.imagedMoment)
        }
        exec(fn)
    }

    def findWithImageReferences(
        videoReferenceUUID: UUID
    )(implicit ec: ExecutionContext): Future[Iterable[MutableImagedMoment]] =
        exec(d => d.findWithImageReferences(videoReferenceUUID))

    def findBetweenUpdatedDates(
        start: Instant,
        end: Instant,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    )(implicit ec: ExecutionContext): Future[Seq[MutableImagedMoment]] = {
        val imDao = daoFactory.newImagedMomentDAO()
        val f     = imDao.runTransaction(d => d.findBetweenUpdatedDates(start, end, limit, offset))
        f.onComplete(_ => imDao.close())
        f.map(_.toSeq)
    }

    def streamBetweenUpdatedDates(
        start: Instant,
        end: Instant,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): (Closeable, java.util.stream.Stream[MutableImagedMoment]) = {
        val dao = daoFactory.newImagedMomentDAO()
        (() => dao.close(), dao.streamBetweenUpdatedDates(start, end, limit, offset))
    }

    def streamVideoReferenceUuidsBetweenUpdatedDates(
        start: Instant,
        end: Instant,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): (Closeable, java.util.stream.Stream[UUID]) = {
        val dao = daoFactory.newImagedMomentDAO()
        (
            () => dao.close(),
            dao.streamVideoReferenceUuidsBetweenUpdatedDates(start, end, limit, offset)
        )
    }

    def countBetweenUpdatedDates(start: Instant, end: Instant)(implicit
        ec: ExecutionContext
    ): Future[Int] = {
        val imDao = daoFactory.newImagedMomentDAO()
        val f     = imDao.runTransaction(d => d.countBetweenUpdatedDates(start, end))
        f.onComplete(_ => imDao.close())
        f
    }

    def countAllGroupByVideoReferenceUUID()(implicit ec: ExecutionContext): Future[Map[UUID, Int]] =
        exec(dao => dao.countAllByVideoReferenceUuids())

    def countByVideoReferenceUuid(uuid: UUID)(implicit ec: ExecutionContext): Future[Int] =
        exec(dao => dao.countByVideoReferenceUUID(uuid))

    def findByConcept(concept: String, limit: Option[Int] = None, offset: Option[Int] = None)(
        implicit ec: ExecutionContext
    ): Future[Iterable[MutableImagedMoment]] = {
        val imDao = daoFactory.newImagedMomentDAO()
        val f     = imDao.runTransaction(d => d.findByConcept(concept, limit, offset))
        f.onComplete(_ => imDao.close())
        f
    }

    def streamByConcept(
        concept: String,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): (Closeable, java.util.stream.Stream[MutableImagedMoment]) = {
        val dao = daoFactory.newImagedMomentDAO()
        (() => dao.close(), dao.streamByConcept(concept, limit, offset))
    }

    def countByConcept(concept: String)(implicit ec: ExecutionContext): Future[Int] = {
        val imDao = daoFactory.newImagedMomentDAO()
        val f     = imDao.runTransaction(d => d.countByConcept(concept))
        f.onComplete(_ => imDao.close())
        f
    }

    def findByConceptWithImages(
        concept: String,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    )(implicit ec: ExecutionContext): Future[Iterable[MutableImagedMoment]] = {
        val imDao = daoFactory.newImagedMomentDAO()
        val f     = imDao.runTransaction(d => d.findByConceptWithImages(concept, limit, offset))
        f.onComplete(_ => imDao.close())
        f
    }

    def countByConceptWithImages(concept: String)(implicit ec: ExecutionContext): Future[Int] = {
        val imDao = daoFactory.newImagedMomentDAO()
        val f     = imDao.runTransaction(d => d.countByConceptWithImages(concept))
        f.onComplete(_ => imDao.close())
        f
    }

    def countModifiedBeforeDate(videoReferenceUuid: UUID, date: Instant)(implicit
        ec: ExecutionContext
    ): Future[Int] = {
        val dao = daoFactory.newImagedMomentDAO()
        val f   = dao.runTransaction(d => d.countModifiedBeforeDate(videoReferenceUuid, date))
        f.onComplete(_ => dao.close())
        f
    }

    def deleteByVideoReferenceUUID(
        videoReferenceUUID: UUID
    )(implicit ec: ExecutionContext): Future[Int] = {
        def fn(dao: IMDAO): Int = {
            val moments = dao.findByVideoReferenceUUID(videoReferenceUUID)
            moments.foreach(dao.delete)
            moments.size
        }
        exec(fn)
    }

    def findByWindowRequest(
        windowRequest: WindowRequest,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    )(implicit ec: ExecutionContext): Future[Iterable[MutableImagedMoment]] =
        exec(d => d.findByWindowRequest(windowRequest, limit, offset))

    def create(
        videoReferenceUUID: UUID,
        timecode: Option[Timecode] = None,
        recordedDate: Option[Instant] = None,
        elapsedTime: Option[Duration] = None
    )(implicit ec: ExecutionContext): Future[MutableImagedMoment] = {

        def fn(d: IMDAO) =
            ImagedMomentController.findOrCreateImagedMoment(
                d,
                videoReferenceUUID,
                timecode,
                recordedDate,
                elapsedTime
            )
        exec(fn)
    }

    /** @param imagedMoments
      *   For your sanity, make sure that they have unique indices BEFORE creating them
      * @param ex
      * @return
      */
    def create(
        imagedMoments: Seq[MutableImagedMoment]
    )(implicit ex: ExecutionContext): Future[Seq[MutableImagedMoment]] = {
        val dao    = daoFactory.newImagedMomentDAO()
        val future = dao.runTransaction(d => imagedMoments.map(im => create(d, im)))
        future.onComplete(_ => dao.close())
        future
    }

    /** This needs to be called inside a transaction. creates a new imaged moment base on the
      * current one
      * @param dao
      * @param sourceImagedMoment
      * @return
      */
    def create(dao: DAO[_], sourceImagedMoment: MutableImagedMoment): MutableImagedMoment = {
        val imDao  = daoFactory.newImagedMomentDAO(dao)
        val irDao  = daoFactory.newImageReferenceDAO(dao)
        val adDao  = daoFactory.newCachedAncillaryDatumDAO(dao)
        val obsDao = daoFactory.newObservationDAO(dao)

        // Reuse existing imagedmoments if it already exists
        val targetImagedMoment =
            ImagedMomentController.findOrCreateImagedMoment(imDao, sourceImagedMoment)

        // Transform source to correct types and remove any existing image references
        val mockImagedMoment        = imDao.newPersistentObject(sourceImagedMoment)
        val existingImageReferences = sourceImagedMoment
            .imageReferences
            .filter(i => irDao.findByURL(i.url).isDefined)
            .toSeq
        existingImageReferences.foreach(ir => mockImagedMoment.removeImageReference(ir))

        Option(mockImagedMoment.ancillaryDatum).foreach(ad => {
            ad.imagedMoment = null
            adDao.create(ad)
            targetImagedMoment.ancillaryDatum = ad
        })

        mockImagedMoment
            .imageReferences
            .toArray
            .foreach(ir => {
                mockImagedMoment.removeImageReference(ir)
                irDao.create(ir)
                targetImagedMoment.addImageReference(ir)
            })

        mockImagedMoment
            .observations
            .toArray
            .foreach(obs => {
                mockImagedMoment.removeObservation(obs)
                obsDao.create(obs)
                targetImagedMoment.addObservation(obs)
            })

        targetImagedMoment
    }

    def update(
        uuid: UUID,
        videoReferenceUUID: Option[UUID] = None,
        timecode: Option[Timecode] = None,
        recordedDate: Option[Instant] = None,
        elapsedTime: Option[Duration] = None
    )(implicit ec: ExecutionContext) = {

        def fn(dao: IMDAO): MutableImagedMoment = {
            dao.findByUUID(uuid) match {
                case None               =>
                    throw new NotFoundInDatastoreException(
                        s"No ImageMoment with UUID of $uuid was found in the datastore"
                    )
                case Some(imagedMoment) =>
                    videoReferenceUUID.foreach(imagedMoment.videoReferenceUUID = _)
                    timecode.foreach(imagedMoment.timecode = _)
                    recordedDate.foreach(imagedMoment.recordedDate = _)
                    elapsedTime.foreach(imagedMoment.elapsedTime = _)
                    // dao.update(imagedMoment)
                    imagedMoment
            }
        }
        exec(fn)
    }

    def updateRecordedTimestampByObservationUuid(observationUuid: UUID, recordedTimestamp: Instant)(
        implicit ec: ExecutionContext
    ): Future[Boolean] = {
        def fn(dao: IMDAO): Boolean =
            dao.updateRecordedTimestampByObservationUuid(observationUuid, recordedTimestamp)
        exec(fn)
    }

    def updateRecordedTimestamps(videoReferenceUuid: UUID, newStartTimestamp: Instant)(implicit
        ec: ExecutionContext
    ): Future[Iterable[MutableImagedMoment]] = {
        def fn(dao: IMDAO): Iterable[MutableImagedMoment] = {
            dao
                .findByVideoReferenceUUID(videoReferenceUuid)
                .map(im => {
                    if (im.elapsedTime != null) {
                        val newRecordedDate = newStartTimestamp.plus(im.elapsedTime)
                        if (newRecordedDate != im.recordedDate) {
                            im.recordedDate = newRecordedDate
                        }
                    }
                    im
                })
        }
        exec(fn)
    }
}

object ImagedMomentController {

    private[this] val log = LoggerFactory.getLogger(getClass)

    /** This method will find or create (if a matching one is not found in the datastore)
      * @param dao
      * @param videoReferenceUUID
      * @param timecode
      * @param recordedDate
      * @param elapsedTime
      * @return
      */
    def findOrCreateImagedMoment(
        dao: ImagedMomentDAO[MutableImagedMoment],
        videoReferenceUUID: UUID,
        timecode: Option[Timecode] = None,
        recordedDate: Option[Instant] = None,
        elapsedTime: Option[Duration] = None
    ): MutableImagedMoment = {
        // -- Return existing or construct a new one if no match is found
        dao.findByVideoReferenceUUIDAndIndex(
            videoReferenceUUID,
            timecode,
            elapsedTime,
            recordedDate
        ) match {
            case Some(imagedMoment) => imagedMoment
            case None               =>
                log.debug(
                    s"Creating new imaged moment at timecode = ${timecode.getOrElse("")}, recordedDate = ${recordedDate
                            .getOrElse("")}, elapsedTime = ${elapsedTime.getOrElse("")}"
                )
                val imagedMoment =
                    dao.newPersistentObject(videoReferenceUUID, timecode, elapsedTime, recordedDate)
                dao.create(imagedMoment)
                imagedMoment
        }
    }

    def findOrCreateImagedMoment(
        dao: ImagedMomentDAO[MutableImagedMoment],
        imagedMoment: MutableImagedMoment
    ): MutableImagedMoment = {
        findOrCreateImagedMoment(
            dao,
            imagedMoment.videoReferenceUUID,
            Option(imagedMoment.timecode),
            Option(imagedMoment.recordedDate),
            Option(imagedMoment.elapsedTime)
        )
    }
}
