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

import java.io.Closeable
import java.time.{Duration, Instant}
import java.util.UUID
import java.util.concurrent.Executors

import io.reactivex.rxjava3.subjects.Subject
import org.mbari.vars.annotation.dao.jpa.{AnnotationImpl, ImagedMomentImpl}
import org.mbari.vars.annotation.dao.DAO
import org.mbari.vars.annotation.messaging.{AnnotationPublisher, MessageBus}
import org.mbari.vars.annotation.model.simple.{ConcurrentRequest, MultiRequest}
import org.mbari.vars.annotation.model.{Annotation, Observation}
import org.mbari.vcr4j.time.Timecode

import scala.concurrent.{ExecutionContext, Future}
import org.mbari.vars.annotation.dao.jpa.UUIDSequence

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-06-25T20:28:00
  */
class AnnotationController(daoFactory: BasicDAOFactory, bus: Subject[Any] = MessageBus.RxSubject) {

  // HACK Assumes where using JDADAPFactory!
//  private[this] val repository: JdbcRepository = {
//    val entityManagerFactory = daoFactory.asInstanceOf[JPADAOFactory].entityManagerFactory
//    new JdbcRepository(entityManagerFactory)
//  }

  private[this] val imagedMomentController = new ImagedMomentController(daoFactory)
  private[this] val annotationPublisher    = new AnnotationPublisher(bus)

  // Executor to throttle bulk inserts
  //private[this] val bulkExecutionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))

  def findByUUID(uuid: UUID)(implicit ec: ExecutionContext): Future[Option[Annotation]] = {
    val obsDao = daoFactory.newObservationDAO()
    val f      = obsDao.runTransaction(d => obsDao.findByUUID(uuid))
    f.onComplete(_ => obsDao.close())
    f.map(_.map(AnnotationImpl(_)))
  }

  def countByVideoReferenceUuid(uuid: UUID)(implicit ec: ExecutionContext): Future[Int] = {
    val dao = daoFactory.newObservationDAO()
    val f   = dao.runTransaction(d => d.countByVideoReferenceUUID(uuid))
    f.onComplete(_ => dao.close())
    f
  }

  /*
      This searches for the ImagedMoments but returns an Annotation view. Keep in mind
      that each ImagedMoment may contain more than one observations. The limit and
      offset are for the ImagedMoments, and each may contain more than one Observation
      (i.e. Annotation). So this call will appear to return more rows than limit and offest
      specify.
   */
  def findByVideoReferenceUUID(
      videoReferenceUUID: UUID,
      limit: Option[Int] = None,
      offset: Option[Int] = None,
      includedAncillaryData: Boolean = false
  )(implicit ec: ExecutionContext): Future[Seq[Annotation]] = {

    val dao = daoFactory.newObservationDAO()
    val f   = dao.runTransaction(d => d.findByVideoReferenceUUID(videoReferenceUUID, limit, offset))
    f.onComplete(_ => dao.close())
    f.map(_.map(AnnotationImpl(_)).toSeq)
  }

  /**
    *
    * @param videoReferenceUuid
    * @param limit
    * @param offset
    * @return A butple of a closeable, and a stream. When the stream is done being processed
    *         invoke the closeable
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
        .map(obs => AnnotationImpl(obs))
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
//        .flatMap(im => im.javaObservations.stream().map(obs => AnnotationImpl(obs))))
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
        .map(obs => AnnotationImpl(obs))
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
      dao.streamByConcurrentRequest(request, limit, offset).map(obs => AnnotationImpl(obs))
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
      dao.streamByMultiRequest(request, limit, offset).map(obs => AnnotationImpl(obs))
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
    val f     = imDao.runTransaction(d => d.findByImageReferenceUUID(imageReferenceUUID))
    f.onComplete(t => imDao.close())
    f.map({
        case None     => Nil
        case Some(im) => im.observations
      })
      .map(obs => obs.map(AnnotationImpl(_)))
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
    val annotation = AnnotationImpl(
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

    // We need to assign a UUID first so that we can find the correct
    //observation. This is only needed if more than one observation
    // exists at the same timestamp
    val observationUuid = UUIDSequence.newUuid()
    annotation.observationUuid = observationUuid

    bulkCreate(Seq(annotation))
      .map(xs => xs.find(_.observationUuid == observationUuid).get)

//    val f = obsDao.runTransaction(d => create(d, annotation))
//    f.onComplete(_ => obsDao.close())
//    val rf = f.map(AnnotationImpl(_))
//    rf.foreach(annotationPublisher.publish) // publish new annotations
//    rf
  }

  /**
    * Bulk create annotations
    * @param annotations THe annotations to create
    * @return The newly created annotations along with any existing ones that share
    *         the same imagedMoment
    */
  def bulkCreate(annotations: Iterable[Annotation]): Future[Seq[AnnotationImpl]] = {

    // We're inserting the annotations using a fixed thread executor
    // grouping them into 50 annotation chunks (ie. 50 annotations per
    // database transactions

    implicit val ec: ExecutionContext =
      ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

    val obsDao = daoFactory.newObservationDAO()

    val imagedMoments = ImagedMomentImpl(annotations.toSeq)
    val futures = imagedMoments
      .grouped(50)
      .map(imagedMoments => {
        val f =
          obsDao.runTransaction(d => imagedMoments.map(i => imagedMomentController.create(d, i)))
        f.map(imagedMomentList => imagedMomentList.flatMap(i => AnnotationImpl(i)))
      })
    val future = Future.sequence(futures)
    future.onComplete(_ => obsDao.close())
    val rf = future.map(_.flatten.toSeq)
    rf.foreach(annotationPublisher.publish) // publish new annotations
    rf

  }

//  @deprecated(message = "Use ImagedMomentController.create instead", since = "2019-10-21")
//  private def create(dao: DAO[_], annotation: Annotation): Observation = {
//    val imDao  = daoFactory.newImagedMomentDAO(dao)
//    val obsDao = daoFactory.newObservationDAO(dao)
//    val assDao = daoFactory.newAssociationDAO(dao)
//    val irDao  = daoFactory.newImageReferenceDAO(dao)
//    val imagedMoment = ImagedMomentController.findOrCreateImagedMoment(
//      imDao,
//      annotation.videoReferenceUuid,
//      Option(annotation.timecode),
//      Option(annotation.recordedTimestamp),
//      Option(annotation.elapsedTime)
//    )
//    val observation = obsDao.newPersistentObject()
//    observation.concept = annotation.concept
//    observation.observer = annotation.observer
//    observation.observationDate = annotation.observationTimestamp
//    Option(annotation.duration).foreach(observation.duration = _)
//    Option(annotation.group).foreach(observation.group = _)
//    Option(annotation.activity).foreach(observation.activity = _)
//    obsDao.create(observation)
//    imagedMoment.addObservation(observation)
//
//    // Add associations
//    annotation
//      .associations
//      .foreach(a => {
//        val newA = assDao.newPersistentObject(
//          a.linkName,
//          Option(a.toConcept),
//          Option(a.linkValue),
//          Option(a.mimeType)
//        )
//        newA.uuid = a.uuid
//        observation.addAssociation(newA)
//      })
//
//    // Add imagereferences
//    annotation
//      .imageReferences
//      .foreach(i => {
//        irDao.findByURL(i.url) match {
//          case Some(v) => // Do nothing. It should already be attached to the imagedmoment
//          case None =>
//            val newI = irDao.newPersistentObject(
//              i.url,
//              Option(i.description),
//              Option(i.height),
//              Option(i.width),
//              Option(i.format)
//            )
//            newI.uuid = i.uuid
//            imagedMoment.addImageReference(newI)
//        }
//      })
//
//    annotationPublisher.publish(observation)
//    observation
//  }

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
    val f = dao.runTransaction(d =>
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
      val ff   = dao1.runTransaction(d => d.findByUUID(uuid).map(AnnotationImpl(_)))
      ff.onComplete(_ => dao1.close())
      ff
    })

    g.foreach(annotationPublisher.publish)

    g
  }

  def bulkUpdate(
      annotations: Iterable[Annotation]
  )(implicit ec: ExecutionContext): Future[Iterable[AnnotationImpl]] = {
    val dao = daoFactory.newObservationDAO()
    // We have to do this in 2 transactions. The first makes all the changes. The second to
    // retrieve them. We have to do this because we may make a SQL call to move an observaton
    // to a new imagedmoment. The enitymanage doesn't see this change and so returns the cached
    // value which may have the wrong time index or videoreference.
    val f = dao.runTransaction(d => {
      annotations.flatMap(a => {
        _update(
          d,
          a.observationUuid,
          Some(a.videoReferenceUuid),
          Some(a.concept),
          Some(a.observer),
          a.observationTimestamp,
          Option(a.timecode),
          Option(a.elapsedTime),
          Option(a.recordedTimestamp),
          Option(a.duration),
          Option(a.group),
          Option(a.activity)
        )
      })
    })
    f.onComplete(_ => dao.close())
    // --- After update find all the changes
    val g = f.flatMap(obs => {
      val dao1 = daoFactory.newObservationDAO()
      val ff =
        dao1.runTransaction(d => obs.flatMap(o => d.findByUUID(o.uuid).map(AnnotationImpl(_))))
      ff.onComplete(_ => dao1.close())
      ff.foreach(annotationPublisher.publish)
      ff
    })
    g
  }

  def bulkUpdateRecordedTimestampOnly(
      annotations: Iterable[Annotation]
  )(implicit ec: ExecutionContext): Future[Iterable[AnnotationImpl]] = {
    val dao = daoFactory.newObservationDAO()
    val f = dao.runTransaction(d => {
      annotations.flatMap(a => {
        _updateRecordedTimestamp(d, a.observationUuid, Option(a.recordedTimestamp))
      })
    })
    f.onComplete(_ => dao.close())
    // --- After update find all the changes
    val g = f.flatMap(obs => {
      val dao1 = daoFactory.newObservationDAO()
      val ff =
        dao1.runTransaction(d => obs.flatMap(o => d.findByUUID(o.uuid).map(AnnotationImpl(_))))
      ff.onComplete(_ => dao1.close())
      ff
    })
    g
  }

  /**
    * This is a special method to handle the case where an
    * ImagedMoment's recordedTimestamp needs to be explicity
    * changed and NOT moved. It is meant for tape annotations where
    * the timecode is the correct index and the recordedTimestamp
    * may need to be adjusted in-place
    * @param dao
    * @param uuid
    * @param recordedTimestampOpt
    * @return Observations that belong to the imagedmoment that was modified
    */
  private def _updateRecordedTimestamp(
      dao: DAO[_],
      uuid: UUID,
      recordedTimestampOpt: Option[Instant]
  ): Seq[Observation] = {

    val obsDao = daoFactory.newObservationDAO(dao)
    obsDao
      .findByUUID(uuid)
      .map(observation => {
        val imagedMoment        = observation.imagedMoment
        val timecodeOpt         = Option(imagedMoment.timecode)
        val currentTimestampOpt = Option(imagedMoment.recordedDate)
        // MUST have a timecode!! This method is for tape annotations
        if (timecodeOpt.isEmpty) Nil
        else if (recordedTimestampOpt.isEmpty && currentTimestampOpt.isDefined) {
          imagedMoment.recordedDate = null
          imagedMoment.observations.toSeq
        }
        else if (recordedTimestampOpt.isDefined
                 && (currentTimestampOpt.isEmpty || currentTimestampOpt.get != recordedTimestampOpt.get)) {
          imagedMoment.recordedDate = recordedTimestampOpt.get
          imagedMoment.observations.toSeq
        }
        else Nil
      })
      .getOrElse(Nil)

  }

  /**
    * This private method is meant to be wrapped in a transaction, either for a
    * single update or for bulk updates
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
  ): Option[Observation] = {
    val obsDao      = daoFactory.newObservationDAO(dao)
    val imDao       = daoFactory.newImagedMomentDAO(dao)
    val observation = obsDao.findByUUID(uuid)
    observation.map(obs => {

      val vrChanged = videoReferenceUUID.isDefined &&
        videoReferenceUUID.get != obs.imagedMoment.videoReferenceUUID

      val tcChanged = timecode.isDefined &&
        timecode.get != obs.imagedMoment.timecode

      val etChanged = elapsedTime.isDefined &&
        elapsedTime.get != obs.imagedMoment.elapsedTime

      val rdChanged = recordedDate.isDefined &&
        recordedDate.get != obs.imagedMoment.recordedDate

      if (vrChanged || tcChanged || etChanged || rdChanged) {
        val vrUUID = videoReferenceUUID.getOrElse(obs.imagedMoment.videoReferenceUUID)
        val tc     = Option(timecode.getOrElse(obs.imagedMoment.timecode))
        val rd     = Option(recordedDate.getOrElse(obs.imagedMoment.recordedDate))
        val et     = Option(elapsedTime.getOrElse(obs.imagedMoment.elapsedTime))
        val newIm  = ImagedMomentController.findOrCreateImagedMoment(imDao, vrUUID, tc, rd, et)
        obsDao.changeImageMoment(newIm.uuid, obs.uuid)
      }

      concept.foreach(obs.concept = _)
      observer.foreach(obs.observer = _)
      duration.foreach(obs.duration = _)
      group.foreach(obs.group = _)
      activity.foreach(obs.activity = _)
      obs.observationDate = observationDate
      obs
    })
  }

  def delete(uuid: UUID)(implicit ec: ExecutionContext): Future[Boolean] = {
    val imDao  = daoFactory.newImagedMomentDAO()
    val obsDao = daoFactory.newObservationDAO(imDao)
    val f = obsDao.runTransaction(d => {
      d.findByUUID(uuid) match {
        case None => false
        case Some(v) =>
          val imagedMoment = v.imagedMoment
          if (imagedMoment.observations.size == 1 && imagedMoment.imageReferences.isEmpty) {
            imDao.delete(imagedMoment)
          }
          else {
            d.delete(v)
          }
          true
      }
    })
    f.onComplete(_ => obsDao.close())
    f
  }

}
