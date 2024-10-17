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

import org.mbari.annosaurus.domain.{ImagedMoment, WindowRequest}
import org.mbari.annosaurus.etc.jdk.Logging.given
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import org.mbari.annosaurus.repository.jpa.entity.{AssociationEntity, ImagedMomentEntity}
import org.mbari.annosaurus.repository.{DAO, ImagedMomentDAO, NotFoundInDatastoreException}
import org.mbari.vcr4j.time.Timecode

import java.io.Closeable
import java.time.{Duration, Instant}
import java.util
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

/**
 * @author
 *   Brian Schlining
 * @since 2016-06-17T16:06:00
 */
class ImagedMomentController(val daoFactory: JPADAOFactory)
    extends BaseController[ImagedMomentEntity, ImagedMomentDAO[ImagedMomentEntity], ImagedMoment]:

    protected type IMDAO = ImagedMomentDAO[ImagedMomentEntity]

    private val log = System.getLogger(getClass.getName)

//  // HACK. Assumes daoFactory is JPA
//  private[this] val jdbcRepository = new JdbcRepository(
//    daoFactory.asInstanceOf[JPADAOFactory].entityManagerFactory
//  )

    override def newDAO(): IMDAO = daoFactory.newImagedMomentDAO()

    override def transform(a: ImagedMomentEntity): ImagedMoment = ImagedMoment.from(a, true)

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
    ): Future[Iterable[ImagedMoment]] =
        exec(d => d.findWithImages(limit, offset).map(transform))

    def countWithImages()(implicit
        ec: ExecutionContext
    ): Future[Int] =
        exec(d => d.countWithImages())

    def findByLinkName(linkName: String, limit: Option[Int] = None, offset: Option[Int] = None)(implicit
        ec: ExecutionContext
    ): Future[Iterable[ImagedMoment]] =
        exec(d => d.findByLinkName(linkName, limit, offset).map(transform))

    def countByLinkName(linkName: String)(implicit
        ec: ExecutionContext
    ): Future[Int] =
        exec(d => d.countByLinkName(linkName))

    def findAllVideoReferenceUUIDs(limit: Option[Int] = None, offset: Option[Int] = None)(implicit
        ec: ExecutionContext
    ): Future[Iterable[UUID]] =
        exec(d => d.findAllVideoReferenceUUIDs(limit, offset))

    def findByVideoReferenceUUID(uuid: UUID, limit: Option[Int] = None, offset: Option[Int] = None)(implicit
        ec: ExecutionContext
    ): Future[Iterable[ImagedMoment]] =
        exec(d => d.findByVideoReferenceUUID(uuid, limit, offset).map(transform))

    /**
     * @param uuid
     * @param limit
     * @param offset
     * @return
     *   A butple of a closeable, and a stream. When the stream is done being processed invoke the closeable
     */
    def streamByVideoReferenceUUID(
        uuid: UUID,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): (Closeable, java.util.stream.Stream[ImagedMomentEntity]) =
        val dao = daoFactory.newImagedMomentDAO()
        (() => dao.close(), dao.streamByVideoReferenceUUID(uuid, limit, offset))

    def findByImageReferenceUUID(
        uuid: UUID
    )(implicit ec: ExecutionContext): Future[Option[ImagedMoment]] =
        def fn(dao: IMDAO): Option[ImagedMoment] =
            val irDao = daoFactory.newImageReferenceDAO(dao)
            irDao.findByUUID(uuid).map(_.getImagedMoment).map(transform)
        exec(fn)

    def findByObservationUUID(
        uuid: UUID
    )(implicit ec: ExecutionContext): Future[Option[ImagedMoment]] =
        def fn(dao: IMDAO): Option[ImagedMoment] =
            val obsDao = daoFactory.newObservationDAO(dao)
            obsDao.findByUUID(uuid).map(_.getImagedMoment).map(transform)
        exec(fn)

    def findWithImageReferences(
        videoReferenceUUID: UUID
    )(implicit ec: ExecutionContext): Future[Iterable[ImagedMoment]] =
        exec(d => d.findWithImageReferences(videoReferenceUUID).map(transform))

    def findBetweenUpdatedDates(
        start: Instant,
        end: Instant,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    )(implicit ec: ExecutionContext): Future[Seq[ImagedMoment]] =
        val imDao = daoFactory.newImagedMomentDAO()
        val f     = imDao.runTransaction(d => d.findBetweenUpdatedDates(start, end, limit, offset).map(transform))
        f.onComplete(_ => imDao.close())
        f.map(_.toSeq)

    def streamBetweenUpdatedDates(
        start: Instant,
        end: Instant,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): (Closeable, java.util.stream.Stream[ImagedMomentEntity]) =
        val dao = daoFactory.newImagedMomentDAO()
        (() => dao.close(), dao.streamBetweenUpdatedDates(start, end, limit, offset))

    def streamVideoReferenceUuidsBetweenUpdatedDates(
        start: Instant,
        end: Instant,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): (Closeable, java.util.stream.Stream[UUID]) =
        val dao = daoFactory.newImagedMomentDAO()
        (
            () => dao.close(),
            dao.streamVideoReferenceUuidsBetweenUpdatedDates(start, end, limit, offset)
        )

    def countBetweenUpdatedDates(start: Instant, end: Instant)(implicit
        ec: ExecutionContext
    ): Future[Int] =
        val imDao = daoFactory.newImagedMomentDAO()
        val f     = imDao.runTransaction(d => d.countBetweenUpdatedDates(start, end))
        f.onComplete(_ => imDao.close())
        f

    def countAllGroupByVideoReferenceUUID()(implicit ec: ExecutionContext): Future[Map[UUID, Int]] =
        exec(dao => dao.countAllByVideoReferenceUuids())

    def countByVideoReferenceUuid(uuid: UUID)(implicit ec: ExecutionContext): Future[Int] =
        exec(dao => dao.countByVideoReferenceUUID(uuid))

    def findByConcept(concept: String, limit: Option[Int] = None, offset: Option[Int] = None)(implicit
        ec: ExecutionContext
    ): Future[Iterable[ImagedMoment]] =
        val imDao = daoFactory.newImagedMomentDAO()
        val f     = imDao.runTransaction(d => d.findByConcept(concept, limit, offset).map(transform))
        f.onComplete(_ => imDao.close())
        f

    def streamByConcept(
        concept: String,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): (Closeable, java.util.stream.Stream[ImagedMomentEntity]) =
        val dao = daoFactory.newImagedMomentDAO()
        (() => dao.close(), dao.streamByConcept(concept, limit, offset))

    def countByConcept(concept: String)(implicit ec: ExecutionContext): Future[Int] =
        val imDao = daoFactory.newImagedMomentDAO()
        val f     = imDao.runTransaction(d => d.countByConcept(concept))
        f.onComplete(_ => imDao.close())
        f

    def findByConceptWithImages(
        concept: String,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    )(implicit ec: ExecutionContext): Future[Iterable[ImagedMoment]] =
        val imDao = daoFactory.newImagedMomentDAO()
        val f     = imDao.runTransaction(d => d.findByConceptWithImages(concept, limit, offset).map(transform))
        f.onComplete(_ => imDao.close())
        f

    def countByConceptWithImages(concept: String)(implicit ec: ExecutionContext): Future[Int] =
        val imDao = daoFactory.newImagedMomentDAO()
        val f     = imDao.runTransaction(d => d.countByConceptWithImages(concept))
        f.onComplete(_ => imDao.close())
        f

    def countModifiedBeforeDate(videoReferenceUuid: UUID, date: Instant)(implicit
        ec: ExecutionContext
    ): Future[Int] =
        val dao = daoFactory.newImagedMomentDAO()
        val f   = dao.runTransaction(d => d.countModifiedBeforeDate(videoReferenceUuid, date))
        f.onComplete(_ => dao.close())
        f

    def deleteByVideoReferenceUUID(
        videoReferenceUUID: UUID
    )(implicit ec: ExecutionContext): Future[Int] =
        def fn(dao: IMDAO): Int =
            val moments = dao.findByVideoReferenceUUID(videoReferenceUUID)
            moments.foreach(dao.delete)
            moments.size
        exec(fn)

    def findByWindowRequest(
        windowRequest: WindowRequest,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    )(implicit ec: ExecutionContext): Future[Iterable[ImagedMoment]] =
        exec(d => d.findByWindowRequest(windowRequest, limit, offset).map(transform))

    def create(
        videoReferenceUUID: UUID,
        timecode: Option[Timecode] = None,
        recordedDate: Option[Instant] = None,
        elapsedTime: Option[Duration] = None
    )(implicit ec: ExecutionContext): Future[ImagedMoment] =

        def fn(d: IMDAO) =
            val im = ImagedMomentController.findOrCreateImagedMoment(
                d,
                videoReferenceUUID,
                timecode,
                recordedDate,
                elapsedTime
            )
            transform(im)
        exec(fn)

    /**
     * @param imagedMoments
     *   For your sanity, make sure that they have unique indices BEFORE creating them
     * @param ex
     * @return
     */
    def create(
        imagedMoments: Seq[ImagedMomentEntity]
    )(implicit ex: ExecutionContext): Future[Seq[ImagedMoment]] =
        val dao     = daoFactory.newImagedMomentDAO()
        val future  = dao.runTransaction(d => imagedMoments.map(im => create(d, im)))
        val future1 = future.flatMap(xs =>
            dao.runTransaction(d => xs.flatMap(x => Option(x.getUuid).flatMap(d.findByUUID).map(transform)))
        )
        future1.onComplete(_ => dao.close())
        future1

    /**
     * This needs to be called inside a transaction. creates a new imaged moment base on the current one
     * @param dao
     * @param sourceImagedMoment
     * @return
     */
    def create(dao: DAO[?], sourceImagedMoment: ImagedMomentEntity): ImagedMomentEntity =

        val imDao = daoFactory.newImagedMomentDAO(dao)

        // Reuse existing imagedmoments if it already exists
        val targetImagedMoment =
            ImagedMomentController.findOrCreateImagedMoment(imDao, sourceImagedMoment)

        // Filter out imagereferences taht already exist
        val existingUrls = targetImagedMoment
            .getImageReferences
            .stream() // using java stream
            .map(_.getUrl)
            .toList()

        val duplicateImageReferences = sourceImagedMoment
            .getImageReferences
            .asScala
            .filter(ir => existingUrls.contains(ir.getUrl))
            .toSeq

        for ir <- duplicateImageReferences
        do sourceImagedMoment.removeImageReference(ir)

        // Create new image references
        sourceImagedMoment
            .getImageReferences
            .forEach(imageReference =>
                if imageReference.getUuid != null then
                    log.atDebug
                        .log(
                            s"An imageReference uuid was found. Setting to null as they need to be generated in the database: ${imageReference.getUuid}"
                        )
                    imageReference.setUuid(null)
                targetImagedMoment.addImageReference(imageReference)
//                irDao.create(imageReference)
            )

        // Create new observations
        sourceImagedMoment
            .getObservations
            .forEach(observation =>
                if observation.getUuid != null then
                    log.atDebug
                        .log(
                            s"An observation uuid was found. Setting to null as they need to be generated in the database: ${observation.getUuid}"
                        )
                    observation.setUuid(null)
                // We ALWAYS set the observation timestamp to now if it's null
                if observation.getObservationTimestamp == null then observation.setObservationTimestamp(Instant.now())
                targetImagedMoment.addObservation(observation)
                val associations = observation.getAssociations.asScala
                observation.setAssociations(new util.HashSet[AssociationEntity]())
                associations.foreach(a =>
                    if a.getUuid != null then
                        log.atDebug
                            .log(
                                s"An association uuid was found. Setting to null as they need to be generated in the database: ${a.getUuid}"
                            )
                        a.setUuid(null)
                    observation.addAssociation(a)
                )
//                obsDao.create(observation)
            )

        if sourceImagedMoment.getAncillaryDatum != null then
            val ad = sourceImagedMoment.getAncillaryDatum
            ad.setUuid(null)
            targetImagedMoment.setAncillaryDatum(ad)
//            adDao.create(ad)

        dao.flush()
        log.atTrace
            .log(() => "Created " + sourceImagedMoment.getObservations.size() + " observations")

        targetImagedMoment

    def bulkMove(newVideoReferenceUuid: UUID, uuids: Seq[UUID])(implicit
        ec: ExecutionContext
    ): Future[Int] =
        def fn(dao: IMDAO): Int =
            dao.moveToVideoReference(newVideoReferenceUuid, uuids)
        exec(fn)

    def update(
        uuid: UUID,
        videoReferenceUUID: Option[UUID] = None,
        timecode: Option[Timecode] = None,
        recordedDate: Option[Instant] = None,
        elapsedTime: Option[Duration] = None
    )(implicit ec: ExecutionContext) =

        def fn(dao: IMDAO): ImagedMoment =
            dao.findByUUID(uuid) match
                case None               =>
                    throw new NotFoundInDatastoreException(
                        s"No ImageMoment with UUID of $uuid was found in the datastore"
                    )
                case Some(imagedMoment) =>
                    videoReferenceUUID.foreach(imagedMoment.setVideoReferenceUuid)
                    timecode.foreach(imagedMoment.setTimecode)
                    recordedDate.foreach(imagedMoment.setRecordedTimestamp)
                    elapsedTime.foreach(imagedMoment.setElapsedTime)
                    // dao.update(imagedMoment)
                    transform(imagedMoment)
        exec(fn)

    def updateRecordedTimestampByObservationUuid(observationUuid: UUID, recordedTimestamp: Instant)(implicit
        ec: ExecutionContext
    ): Future[Boolean] =
        def fn(dao: IMDAO): Boolean =
            dao.updateRecordedTimestampByObservationUuid(observationUuid, recordedTimestamp)
        exec(fn)

    def updateRecordedTimestamps(videoReferenceUuid: UUID, newStartTimestamp: Instant)(implicit
        ec: ExecutionContext
    ): Future[Iterable[ImagedMoment]] =
        def fn(dao: IMDAO): Iterable[ImagedMoment] =
            dao
                .findByVideoReferenceUUID(videoReferenceUuid)
                .map(im =>
                    if im.getElapsedTime != null then
                        val newRecordedDate = newStartTimestamp.plus(im.getElapsedTime)
                        if newRecordedDate != im.getRecordedTimestamp then im.setRecordedTimestamp(newRecordedDate)
                    transform(im)
                )
        exec(fn)

object ImagedMomentController:

    private val log = System.getLogger(getClass.getName)

    /**
     * This method will find or create (if a matching one is not found in the datastore)
     * @param dao
     * @param videoReferenceUUID
     * @param timecode
     * @param recordedDate
     * @param elapsedTime
     * @return
     */
    def findOrCreateImagedMoment(
        dao: ImagedMomentDAO[ImagedMomentEntity],
        videoReferenceUUID: UUID,
        timecode: Option[Timecode] = None,
        recordedDate: Option[Instant] = None,
        elapsedTime: Option[Duration] = None
    ): ImagedMomentEntity =
        // -- Return existing or construct a new one if no match is found
        dao.findByVideoReferenceUUIDAndIndex(
            videoReferenceUUID,
            timecode,
            elapsedTime,
            recordedDate
        ) match
            case Some(imagedMoment) => imagedMoment
            case None               =>
                log.atDebug
                    .log(() =>
                        s"Creating new imaged moment at timecode = ${timecode.getOrElse("")}, recordedDate = ${recordedDate
                                .getOrElse("")}, elapsedTime = ${elapsedTime.getOrElse("")}"
                    )
                val imagedMoment = new ImagedMomentEntity(
                    videoReferenceUUID,
                    recordedDate.orNull,
                    timecode.orNull,
                    elapsedTime.orNull
                )
                dao.create(imagedMoment)
                dao.flush()
                imagedMoment

    def findOrCreateImagedMoment(
        dao: ImagedMomentDAO[ImagedMomentEntity],
        imagedMoment: ImagedMomentEntity
    ): ImagedMomentEntity =
        findOrCreateImagedMoment(
            dao,
            imagedMoment.getVideoReferenceUuid,
            Option(imagedMoment.getTimecode),
            Option(imagedMoment.getRecordedTimestamp),
            Option(imagedMoment.getElapsedTime)
        )
