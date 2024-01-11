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
import java.util.concurrent.Executors

import io.reactivex.rxjava3.subjects.Subject
import org.mbari.annosaurus.messaging.{AnnotationPublisher, MessageBus}
import org.mbari.annosaurus.domain.{ConcurrentRequest, MultiRequest}

import org.mbari.annosaurus.repository.DAO
import org.mbari.annosaurus.repository.jpa.entity.{ImagedMomentEntity, ObservationEntity}
import org.mbari.vcr4j.time.Timecode
import org.mbari.annosaurus.repository.jpa.Implicits._

import scala.concurrent.{ExecutionContext, Future}
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import org.mbari.annosaurus.repository.jpa.entity.ObservationEntity
import org.mbari.annosaurus.domain.Annotation
import org.mbari.annosaurus.repository.ObservationDAO
import org.mbari.annosaurus.etc.jdk.Logging.given
import scala.jdk.CollectionConverters.*
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}

import scala.util.chaining.*

/** @author
  *   Brian Schlining
  * @since 2016-06-25T20:28:00
  */
class AnnotationController(
    val daoFactory: JPADAOFactory,
    bus: Subject[Any] = MessageBus.RxSubject
) {

    private[this] val imagedMomentController = new ImagedMomentController(daoFactory)
    private[this] val annotationPublisher    = new AnnotationPublisher(bus)
    private val log                          = System.getLogger(getClass.getName())

    protected def exec[T](
        fn: ObservationDAO[ObservationEntity] => T
    )(implicit ec: ExecutionContext): Future[T] = {
        val dao = daoFactory.newObservationDAO()
        val f   = dao.runTransaction(fn)
        f.onComplete(_ => dao.close())
        f
    }

    def findByUUID(uuid: UUID)(implicit ec: ExecutionContext): Future[Option[Annotation]] = {
        // val obsDao = daoFactory.newObservationDAO()
        // val f      = obsDao.runTransaction(d => obsDao.findByUUID(uuid))
        // f.onComplete(_ => obsDao.close())
        // f.map(_.map(obs => Annotation.from(obs, true)))
        exec(d => d.findByUUID(uuid).map(Annotation.from(_, true)))

    }

    def countByVideoReferenceUuid(uuid: UUID)(implicit ec: ExecutionContext): Future[Int] = {
        val dao = daoFactory.newObservationDAO()
        val f   = dao.runTransaction(d => d.countByVideoReferenceUUID(uuid))
        f.onComplete(_ => dao.close())
        f
    }

    /*
      This searches for the ImagedMoments but returns an MutableAnnotation view. Keep in mind
      that each ImagedMoment may contain more than one observations. The limit and
      offset are for the ImagedMoments, and each may contain more than one MutableObservation
      (i.e. MutableAnnotation). So this call will appear to return more rows than limit and offest
      specify.
     */
    def findByVideoReferenceUuid(
        videoReferenceUUID: UUID,
        limit: Option[Int] = None,
        offset: Option[Int] = None,
        includedAncillaryData: Boolean = false
    )(implicit ec: ExecutionContext): Future[Seq[Annotation]] = {

        val dao = daoFactory.newObservationDAO()
        val f   =
            dao.runTransaction(d =>
                d.findByVideoReferenceUuid(videoReferenceUUID, limit, offset)
                    .map(obs => Annotation.from(obs, includedAncillaryData))
                    .toSeq
            )
        f.onComplete(_ => dao.close())
        f
        // f.map(_.map(obs => Annotation.from(obs, true)).toSeq)
    }

    /** @param videoReferenceUuid
      * @param limit
      * @param offset
      * @return
      *   A butple of a closeable, and a stream. When the stream is done being processed invoke the
      *   closeable
      */
    def streamByVideoReferenceUUID(
        videoReferenceUuid: UUID,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): (Closeable, java.util.stream.Stream[Annotation]) = {
        val dao = daoFactory.newObservationDAO()
        (
            () => dao.close(),
            dao
                .streamByVideoReferenceUUID(videoReferenceUuid, limit, offset)
                .map(obs => Annotation.from(obs, true))
        )
    }

    def streamByVideoReferenceUUIDAndTimestamps(
        videoReferenceUuid: UUID,
        startTimestamp: Instant,
        endTimestamp: Instant,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): (Closeable, java.util.stream.Stream[Annotation]) = {
//    val dao = daoFactory.newImagedMomentDAO()
//    (() => dao.close(),
//      dao.streamByVideoReferenceUUIDAndTimestamps(videoReferenceUuid, startTimestamp, endTimestamp, limit, offset)
//        .flatMap(im => im.javaObservations.stream().map(obs => MutableAnnotationImpl(obs))))
        val dao = daoFactory.newObservationDAO()
        (
            () => dao.close(),
            dao
                .streamByVideoReferenceUUIDAndTimestamps(
                    videoReferenceUuid,
                    startTimestamp,
                    endTimestamp,
                    limit,
                    offset
                )
                .map(obs => Annotation.from(obs, true))
        )
    }

    def streamByConcurrentRequest(
        request: ConcurrentRequest,
        limit: Option[Int],
        offset: Option[Int]
    ): (Closeable, java.util.stream.Stream[Annotation]) = {
        val dao = daoFactory.newObservationDAO()
        (
            () => dao.close(),
            dao.streamByConcurrentRequest(request, limit, offset)
                .map(obs => Annotation.from(obs, true))
        )
    }

    def countByConcurrentRequest(
        request: ConcurrentRequest
    )(implicit ec: ExecutionContext): Future[Long] = {
        def dao = daoFactory.newObservationDAO()
        val f   = dao.runTransaction(d => d.countByConcurrentRequest(request))
        f.onComplete(t => dao.close())
        f
    }

    def streamByMultiRequest(
        request: MultiRequest,
        limit: Option[Int],
        offset: Option[Int]
    ): (Closeable, java.util.stream.Stream[Annotation]) = {
        val dao = daoFactory.newObservationDAO()
        (
            () => dao.close(),
            dao.streamByMultiRequest(request, limit, offset).map(obs => Annotation.from(obs))
        )
    }

    def countByMultiRequest(request: MultiRequest)(implicit ec: ExecutionContext): Future[Long] = {
        def dao = daoFactory.newObservationDAO()
        val f   = dao.runTransaction(d => d.countByMultiRequest(request))
        f.onComplete(t => dao.close())
        f
    }

    def findByImageReferenceUUID(
        imageReferenceUUID: UUID
    )(implicit ec: ExecutionContext): Future[Iterable[Annotation]] = {

        val imDao = daoFactory.newImagedMomentDAO()
        val f     = imDao.runTransaction(d =>
            d.findByImageReferenceUUID(imageReferenceUUID) match
                case None     => Nil
                case Some(im) => im.getObservations.asScala.map(Annotation.from(_))
        )
        f.onComplete(t => imDao.close())
        f
    }

    def create(
        videoReferenceUUID: UUID,
        concept: String,
        observer: String,
        observationDate: Instant = Instant.now(),
        timecode: Option[Timecode] = None,
        elapsedTime: Option[Duration] = None,
        recordedDate: Option[Instant] = None,
        duration: Option[Duration] = None,
        group: Option[String] = None,
        activity: Option[String] = None
    )(implicit ec: ExecutionContext): Future[Annotation] = {
        // We need to assign a UUID first so that we can find the correct
        // observation. This is only needed if more than one observation
        // exists at the same timestamp
        val annotation = Annotation(
            videoReferenceUuid = Option(videoReferenceUUID),
            concept = Some(concept),
            observer = Some(observer),
            observationTimestamp = Some(observationDate),
            timecode = timecode.map(_.toString),
            elapsedTimeMillis = elapsedTime.map(_.toMillis),
            recordedTimestamp = recordedDate,
            durationMillis = duration.map(_.toMillis),
            group = group,
            activity = activity
        )

        bulkCreate(Seq(annotation))
            .map(xs =>
                xs.find(a =>
                    a.concept.orNull == concept
                        && a.videoReferenceUuid.orNull == videoReferenceUUID
                        && a.observer.orNull == observer
                        && a.observationTimestamp.orNull == observationDate
                ).orNull
            )
    }

    /** Bulk create annotations
      * @param annotations
      *   THe annotations to create
      * @return
      *   The newly created annotations along with any existing ones that share the same
      *   imagedMoment
      */
    def bulkCreate(
        annotations: Iterable[Annotation]
    )(using ec: ExecutionContext): Future[Seq[Annotation]] = {

        // TODO add prefilter to remove duplicate image references

        // TODO need to stress test this to find the maximum number oa annotations that can be inserted at once
        log.atTrace.log(() => s"Creating ${annotations.size} Annotations: ${annotations.stringify}")
        val imagedMoments = Annotation.toEntities(annotations.toSeq, true)

        val obsDao = daoFactory.newObservationDAO()
        val imDao  = daoFactory.newImagedMomentDAO(obsDao)

        val future = obsDao
            .runTransaction(d => {
                for im <- imagedMoments
                yield imagedMomentController.create(d, im)
            })
            .map(entities =>
                // We map to annotations outside of a transaction. They should already be fully loaded
                // so no lazy loading issues. The primary keys aren't set until the transaction is
                // completed and we need those!!
                entities.flatMap(im => Annotation.fromImagedMoment(im, true))
            )

        future.onComplete(_ => obsDao.close())
        future.foreach(annotationPublisher.publish) // publish new annotations
        future
    }

    def update(
        uuid: UUID,
        videoReferenceUUID: Option[UUID] = None,
        concept: Option[String] = None,
        observer: Option[String] = None,
        observationDate: Instant = Instant.now(),
        timecode: Option[Timecode] = None,
        elapsedTime: Option[Duration] = None,
        recordedDate: Option[Instant] = None,
        duration: Option[Duration] = None,
        group: Option[String] = None,
        activity: Option[String] = None
    )(implicit ec: ExecutionContext): Future[Option[Annotation]] = {

        // We have to do this in 2 transactions. The first makes all the changes. The second to
        // retrieve them. We have to do this because we may make a SQL call to move an observaton
        // to a new imagedmoment. The enitymanage doesn't see this change and so returns the cached
        // value which may have the wrong time index or videoreference.
        val dao = daoFactory.newObservationDAO()
        val f   = dao.runTransaction(d =>
            _update(
                d,
                uuid,
                videoReferenceUUID,
                concept,
                observer,
                observationDate,
                timecode,
                elapsedTime,
                recordedDate,
                duration,
                group,
                activity
            )
        )
        f.onComplete(_ => dao.close())

        val g = f.flatMap(opt => {
            val dao1 = daoFactory.newObservationDAO()
            val ff   = dao1.runTransaction(d => d.findByUUID(uuid).map(Annotation.from(_, true)))
            ff.onComplete(_ => dao1.close())
            ff
        })

        g.foreach(annotationPublisher.publish)

        g
    }

    def bulkUpdate(
        annotations: Iterable[Annotation]
    )(implicit ec: ExecutionContext): Future[Iterable[Annotation]] = {
        val dao       = daoFactory.newObservationDAO()
        // We have to do this in 2 transactions. The first makes all the changes. The second to
        // retrieve them. We have to do this because we may make a SQL call to move an observaton
        // to a new imagedmoment. The enitymanage doesn't see this change and so returns the cached
        // value which may have the wrong time index or videoreference.
        val goodAnnos = annotations.filter(x =>
            x.observationUuid.isDefined
                && x.concept.isDefined
                && x.videoReferenceUuid.isDefined
                && x.observer.isDefined
                && x.observationTimestamp.isDefined
        )
        val f         = dao.runTransaction(d => {
            goodAnnos.flatMap(a => {
                _update(
                    d,
                    a.observationUuid.get,
                    a.videoReferenceUuid,
                    a.concept,
                    a.observer,
                    a.observationTimestamp.get,
                    a.validTimecode,
                    a.elapsedTime,
                    a.recordedTimestamp,
                    a.duration,
                    a.group,
                    a.activity
                )
            })
        })
        f.onComplete(_ => dao.close())
        // --- After update find all the changes
        val g         = f.flatMap(obs => {
            val dao1 = daoFactory.newObservationDAO()
            val ff   =
                dao1.runTransaction(d =>
                    obs.flatMap(o => d.findByUUID(o.getUuid).map(Annotation.from(_, true)))
                )
            ff.onComplete(_ => dao1.close())
            ff.foreach(annotationPublisher.publish)
            ff
        })
        g
    }

    def bulkUpdateRecordedTimestampOnly(
        annotations: Iterable[Annotation]
    )(implicit ec: ExecutionContext): Future[Iterable[Annotation]] = {
        if (annotations.isEmpty) Future.successful(Nil)
        else
            val goodAnnos = annotations.filter(x => x.observationUuid.isDefined)
            val dao       = daoFactory.newObservationDAO()
            val f         = dao.runTransaction(d => {
                goodAnnos.flatMap(a => {
                    _updateRecordedTimestamp(d, a.observationUuid.get, a.recordedTimestamp)
                })
            })
            f.onComplete(_ => dao.close())
            // --- After update find all the changes
            val g         = f.flatMap(obs => {
                val dao1 = daoFactory.newObservationDAO()
                val ff   =
                    dao1.runTransaction(d =>
                        obs.flatMap(o => d.findByUUID(o.getUuid).map(Annotation.from(_, true)))
                    )
                ff.onComplete(_ => dao1.close())
                ff
            })
            g
    }

    /** This is a special method to handle the case where an ImagedMoment's recordedTimestamp needs
      * to be explicity changed and NOT moved. It is meant for tape annotations where the timecode
      * is the correct index and the recordedTimestamp may need to be adjusted in-place
      * @param dao
      * @param uuid
      * @param recordedTimestampOpt
      * @return
      *   Observations that belong to the imagedmoment that was modified
      */
    private def _updateRecordedTimestamp(
        dao: DAO[_],
        uuid: UUID,
        recordedTimestampOpt: Option[Instant]
    ): Seq[ObservationEntity] = {

        val obsDao = daoFactory.newObservationDAO(dao)
        obsDao
            .findByUUID(uuid)
            .map(observation => {
                val imagedMoment        = observation.getImagedMoment
                val timecodeOpt         = Option(imagedMoment.getTimecode)
                val currentTimestampOpt = Option(imagedMoment.getRecordedTimestamp)
                // MUST have a timecode!! This method is for tape annotations
                if (timecodeOpt.isEmpty) Nil
                else if (recordedTimestampOpt.isEmpty && currentTimestampOpt.isDefined) {
                    imagedMoment.setRecordedTimestamp(null)
                    imagedMoment.getObservations.asScala.toSeq
                }
                else if (
                    recordedTimestampOpt.isDefined
                    && (currentTimestampOpt.isEmpty || currentTimestampOpt.get != recordedTimestampOpt.get)
                ) {
                    recordedTimestampOpt.foreach(imagedMoment.setRecordedTimestamp)
                    imagedMoment.getObservations.asScala.toSeq
                }
                else Nil
            })
            .getOrElse(Nil)

    }

    /** This private method is meant to be wrapped in a transaction, either for a single update or
      * for bulk updates
      * @param dao
      * @param uuid
      * @param videoReferenceUUID
      * @param concept
      * @param observer
      * @param observationDate
      * @param timecode
      * @param elapsedTime
      * @param recordedDate
      * @param duration
      * @param group
      * @param activity
      * @return
      */
    private def _update(
        dao: DAO[_],
        uuid: UUID,
        videoReferenceUUID: Option[UUID] = None,
        concept: Option[String] = None,
        observer: Option[String] = None,
        observationDate: Instant = Instant.now(),
        timecode: Option[Timecode] = None,
        elapsedTime: Option[Duration] = None,
        recordedDate: Option[Instant] = None,
        duration: Option[Duration] = None,
        group: Option[String] = None,
        activity: Option[String] = None
    ): Option[ObservationEntity] = {
        val obsDao      = daoFactory.newObservationDAO(dao)
        val imDao       = daoFactory.newImagedMomentDAO(dao)
        val observation = obsDao.findByUUID(uuid)
        observation.map(obs => {

            val imagedMoment = obs.getImagedMoment

            val vrSame = videoReferenceUUID.contains(imagedMoment.getVideoReferenceUuid)
            val tcSame = timecode.map(_.toString)
                .contains(Option(imagedMoment.getTimecode).map(_.toString).getOrElse(""))
            val etSame = elapsedTime.contains(imagedMoment.getElapsedTime)
            val rtSame = recordedDate.contains(imagedMoment.getRecordedTimestamp)

            if (!vrSame || !tcSame || !etSame || !rtSame) {
                val vrUUID = videoReferenceUUID.getOrElse(imagedMoment.getVideoReferenceUuid)
                val tc     = Option(timecode.getOrElse(imagedMoment.getTimecode))
                val rd     = Option(recordedDate.getOrElse(imagedMoment.getRecordedTimestamp))
                val et     = Option(elapsedTime.getOrElse(imagedMoment.getElapsedTime))
                val newIm  =
                    ImagedMomentController.findOrCreateImagedMoment(imDao, vrUUID, tc, rd, et)
                log.atDebug.log(() => s"Moving observation ${obs.getUuid} to imagedMoment ${newIm.getUuid}")
                imDao.update(imagedMoment)
//                imagedMoment.removeObservation(obs) // This causes a delete which messes up the transaction as the observation becomes detached
                newIm.addObservation(obs)
            }

            concept.foreach(obs.setConcept)
            observer.foreach(obs.setObserver)
            duration.foreach(obs.setDuration)
            group.foreach(obs.setGroup)
            activity.foreach(obs.setActivity)
            obs.setObservationTimestamp(observationDate)
//            obsDao.update(obs)
            dao.flush()
            obs
        })
    }

    def delete(uuid: UUID)(implicit ec: ExecutionContext): Future[Boolean] = {
        val imDao  = daoFactory.newImagedMomentDAO()
        val obsDao = daoFactory.newObservationDAO(imDao)
        val f      = obsDao.runTransaction(d => {
            d.findByUUID(uuid) match {
                case None    => false
                case Some(v) =>
                    val imagedMoment = v.getImagedMoment
                    if (
                        imagedMoment.getObservations.size == 1 && imagedMoment
                            .getImageReferences
                            .isEmpty
                    ) {
                        imDao.delete(imagedMoment)
                    }
                    else {
                        imagedMoment.removeObservation(v)
//                        d.delete(v)
                    }
                    true
            }
        })
        f.onComplete(_ => obsDao.close())
        f
    }

}
