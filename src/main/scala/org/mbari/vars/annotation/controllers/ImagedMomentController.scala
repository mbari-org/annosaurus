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

import org.mbari.vars.annotation.dao.{ImagedMomentDAO, NotFoundInDatastoreException}
import org.mbari.vars.annotation.model.ImagedMoment
import org.mbari.vars.annotation.model.simple.WindowRequest
import org.mbari.vcr4j.time.Timecode
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T16:06:00
 */
class ImagedMomentController(val daoFactory: BasicDAOFactory)
  extends BaseController[ImagedMoment, ImagedMomentDAO[ImagedMoment]] {

  protected type IMDAO = ImagedMomentDAO[ImagedMoment]

  override def newDAO(): IMDAO = daoFactory.newImagedMomentDAO()

  override def findAll(limit: Option[Int] = None, offset: Option[Int] = None)(implicit ec: ExecutionContext): Future[Iterable[ImagedMoment]] =
    exec(d => d.findAll(limit, offset))

  def findAllVideoReferenceUUIDs(limit: Option[Int] = None, offset: Option[Int] = None)(implicit ec: ExecutionContext): Future[Iterable[UUID]] =
    exec(d => d.findAllVideoReferenceUUIDs(limit, offset))

  def findByVideoReferenceUUID(uuid: UUID, limit: Option[Int] = None, offset: Option[Int] = None)(implicit ec: ExecutionContext): Future[Iterable[ImagedMoment]] =
    exec(d => d.findByVideoReferenceUUID(uuid, limit, offset))

  /**
    *
    * @param uuid
    * @param limit
    * @param offset
    * @return A butple of a closeable, and a stream. When the stream is done being processed
    *         invoke the closeable
    */
  def streamByVideoReferenceUUID(uuid: UUID, limit: Option[Int] = None, offset: Option[Int] = None): (Closeable, java.util.stream.Stream[ImagedMoment]) = {
    val dao = daoFactory.newImagedMomentDAO()
    (() => dao.close(), dao.streamByVideoReferenceUUID(uuid, limit, offset))
  }

  def findByImageReferenceUUID(uuid: UUID)(implicit ec: ExecutionContext): Future[Option[ImagedMoment]] = {
    def fn(dao: IMDAO): Option[ImagedMoment] = {
      val irDao = daoFactory.newImageReferenceDAO(dao)
      irDao.findByUUID(uuid).map(_.imagedMoment)
    }
    exec(fn)
  }

  def findByObservationUUID(uuid: UUID)(implicit ec: ExecutionContext): Future[Option[ImagedMoment]] = {
    def fn(dao: IMDAO): Option[ImagedMoment] = {
      val obsDao = daoFactory.newObservationDAO(dao)
      obsDao.findByUUID(uuid).map(_.imagedMoment)
    }
    exec(fn)
  }

  def findWithImageReferences(videoReferenceUUID: UUID)(implicit ec: ExecutionContext): Future[Iterable[ImagedMoment]] =
    exec(d => d.findWithImageReferences(videoReferenceUUID))

  def findBetweenUpdatedDates(
    start: Instant,
    end: Instant,
    limit: Option[Int] = None,
    offset: Option[Int] = None)(implicit ec: ExecutionContext): Future[Seq[ImagedMoment]] = {
    val imDao = daoFactory.newImagedMomentDAO()
    val f = imDao.runTransaction(d => d.findBetweenUpdatedDates(start, end, limit, offset))
    f.onComplete(_ => imDao.close())
    f.map(_.toSeq)
  }

  def streamBetweenUpdatedDates(start: Instant,
                                end: Instant,
                                limit: Option[Int] = None,
                                offset: Option[Int] = None): (Closeable, java.util.stream.Stream[ImagedMoment]) = {
    val dao = daoFactory.newImagedMomentDAO()
    (() => dao.close(), dao.streamBetweenUpdatedDates(start, end, limit, offset))
  }



  def streamVideoReferenceUuidsBetweenUpdatedDates(start: Instant,
                                                   end: Instant,
                                                   limit: Option[Int] = None,
                                                   offset: Option[Int] = None):
      (Closeable, java.util.stream.Stream[UUID]) = {
    val dao = daoFactory.newImagedMomentDAO()
    (() => dao.close(), dao.streamVideoReferenceUuidsBetweenUpdatedDates(start, end, limit, offset))
  }


  def countBetweenUpdatedDates(
    start: Instant,
    end: Instant)(implicit ec: ExecutionContext): Future[Int] = {
    val imDao = daoFactory.newImagedMomentDAO()
    val f = imDao.runTransaction(d => d.countBetweenUpdatedDates(start, end))
    f.onComplete(_ => imDao.close())
    f
  }

  def countAllGroupByVideoReferenceUUID()(implicit ec: ExecutionContext): Future[Map[UUID, Int]] =
    exec(dao => dao.countAllByVideoReferenceUuids())

  def countByVideoReferenceUuid(uuid: UUID)(implicit ec: ExecutionContext): Future[Int] =
    exec(dao => dao.countByVideoReferenceUUID(uuid))

  def findByConcept(
    concept: String,
    limit: Option[Int] = None,
    offset: Option[Int] = None)(implicit ec: ExecutionContext): Future[Iterable[ImagedMoment]] = {
    val imDao = daoFactory.newImagedMomentDAO()
    val f = imDao.runTransaction(d => d.findByConcept(concept, limit, offset))
    f.onComplete(_ => imDao.close())
    f
  }

  def streamByConcept(concept: String, limit: Option[Int] = None, offset: Option[Int] = None):
      (Closeable, java.util.stream.Stream[ImagedMoment]) = {
    val dao = daoFactory.newImagedMomentDAO()
    (() => dao.close(), dao.streamByConcept(concept, limit, offset))
  }

  def countByConcept(concept: String)(implicit ec: ExecutionContext): Future[Int] = {
    val imDao = daoFactory.newImagedMomentDAO()
    val f = imDao.runTransaction(d => d.countByConcept(concept))
    f.onComplete(_ => imDao.close())
    f
  }

  def findByConceptWithImages(
    concept: String,
    limit: Option[Int] = None,
    offset: Option[Int] = None)(implicit ec: ExecutionContext): Future[Iterable[ImagedMoment]] = {
    val imDao = daoFactory.newImagedMomentDAO()
    val f = imDao.runTransaction(d => d.findByConceptWithImages(concept, limit, offset))
    f.onComplete(_ => imDao.close())
    f
  }

  def countByConceptWithImages(concept: String)(implicit ec: ExecutionContext): Future[Int] = {
    val imDao = daoFactory.newImagedMomentDAO()
    val f = imDao.runTransaction(d => d.countByConceptWithImages(concept))
    f.onComplete(_ => imDao.close())
    f
  }

  def deleteByVideoReferenceUUID(videoReferenceUUID: UUID)(implicit ec: ExecutionContext): Future[Int] =
    exec(d => d.deleteByVideoReferenceUUUID(videoReferenceUUID))

  def findByWindowRequest(windowRequest: WindowRequest,
                          limit: Option[Int] = None,
                          offset: Option[Int] = None)(implicit ec: ExecutionContext): Future[Iterable[ImagedMoment]] =
    exec(d => d.findByWindowRequest(windowRequest, limit, offset))

  def create(
    videoReferenceUUID: UUID,
    timecode: Option[Timecode] = None,
    recordedDate: Option[Instant] = None,
    elapsedTime: Option[Duration] = None)(implicit ec: ExecutionContext): Future[ImagedMoment] = {

    def fn(d: IMDAO) = ImagedMomentController.findImagedMoment(
      d, videoReferenceUUID, timecode, recordedDate, elapsedTime)
    exec(fn)
  }

  def update(
    uuid: UUID,
    videoReferenceUUID: Option[UUID] = None,
    timecode: Option[Timecode] = None,
    recordedDate: Option[Instant] = None,
    elapsedTime: Option[Duration] = None)(implicit ec: ExecutionContext) = {

    def fn(dao: IMDAO): ImagedMoment = {
      dao.findByUUID(uuid) match {
        case None =>
          throw new NotFoundInDatastoreException(s"No ImageMoment with UUID of $uuid was found in the datastore")
        case Some(imagedMoment) =>
          videoReferenceUUID.foreach(imagedMoment.videoReferenceUUID = _)
          timecode.foreach(imagedMoment.timecode = _)
          recordedDate.foreach(imagedMoment.recordedDate = _)
          elapsedTime.foreach(imagedMoment.elapsedTime = _)
          //dao.update(imagedMoment)
          imagedMoment
      }
    }
    exec(fn)
  }

  def updateRecordedTimestampByObservationUuid(
    observationUuid: UUID,
    recordedTimestamp: Instant)(implicit ec: ExecutionContext): Future[Boolean] = {
    def fn(dao: IMDAO): Boolean =
      dao.updateRecordedTimestampByObservationUuid(observationUuid, recordedTimestamp)
    exec(fn)
  }

  def updateRecordedTimestamps(videoReferenceUuid: UUID, newStartTimestamp: Instant)(implicit ec: ExecutionContext): Future[Iterable[ImagedMoment]] = {
    def fn(dao: IMDAO): Iterable[ImagedMoment] = {
      dao.findByVideoReferenceUUID(videoReferenceUuid)
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

  /**
   * This method will find or create (if a matching one is not found in the datastore)
   * @param dao
   * @param videoReferenceUUID
   * @param timecode
   * @param recordedDate
   * @param elapsedTime
   * @return
   */
  def findImagedMoment(
    dao: ImagedMomentDAO[ImagedMoment],
    videoReferenceUUID: UUID,
    timecode: Option[Timecode] = None,
    recordedDate: Option[Instant] = None,
    elapsedTime: Option[Duration] = None): ImagedMoment = {
    // -- Return existing or construct a new one if no match is found
    dao.findByVideoReferenceUUIDAndIndex(videoReferenceUUID, timecode, elapsedTime, recordedDate) match {
      case Some(imagedMoment) => imagedMoment
      case None =>
        log.info(s"Creating new imaged moment at timecode = ${timecode.getOrElse("")}, recordedDate = ${recordedDate.getOrElse("")}, elapsedTime = ${elapsedTime.getOrElse("")}")
        val imagedMoment = dao.newPersistentObject(videoReferenceUUID, timecode, elapsedTime, recordedDate)
        dao.create(imagedMoment)
        imagedMoment
    }
  }
}
