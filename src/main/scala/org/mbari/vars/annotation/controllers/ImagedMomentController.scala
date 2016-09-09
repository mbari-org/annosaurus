package org.mbari.vars.annotation.controllers

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.dao.{ ImagedMomentDAO, NotFoundInDatastoreException }
import org.mbari.vars.annotation.model.ImagedMoment
import org.mbari.vcr4j.time.Timecode

import scala.concurrent.{ ExecutionContext, Future }

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

  def findAll(limit: Int, offset: Int)(implicit ec: ExecutionContext): Future[Iterable[ImagedMoment]] =
    exec(d => d.findAll(limit, offset))

  def findAllVideoReferenceUUIDs(limit: Option[Int] = None, offset: Option[Int] = None)(implicit ec: ExecutionContext): Future[Iterable[UUID]] =
    exec(d => d.findAllVideoReferenceUUIDs(limit, offset))

  def findByVideoReferenceUUID(uuid: UUID, limit: Option[Int] = None, offset: Option[Int] = None)(implicit ec: ExecutionContext): Future[Iterable[ImagedMoment]] =
    exec(d => d.findByVideoReferenceUUID(uuid, limit, offset))

  def findWithImageReferences(videoReferenceUUID: UUID)(implicit ec: ExecutionContext): Future[Iterable[ImagedMoment]] =
    exec(d => d.findWithImageReferences(videoReferenceUUID))

  def create(
    videoReferenceUUID: UUID,
    timecode: Option[Timecode] = None,
    recordedDate: Option[Instant] = None,
    elapsedTime: Option[Duration] = None
  )(implicit ec: ExecutionContext): Future[ImagedMoment] = {

    def fn(d: IMDAO) = ImagedMomentController.findImagedMoment(
      d, videoReferenceUUID, timecode, recordedDate, elapsedTime
    )
    exec(fn)
  }

  def update(
    uuid: UUID,
    videoReferenceUUID: Option[UUID] = None,
    timecode: Option[Timecode] = None,
    recordedDate: Option[Instant] = None,
    elapsedTime: Option[Duration] = None
  )(implicit ec: ExecutionContext) = {

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
}

object ImagedMomentController {

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
    elapsedTime: Option[Duration] = None
  ): ImagedMoment = {
    // -- Return existing or construct a new one if no match is found
    dao.findByVideoReferenceUUIDAndIndex(videoReferenceUUID, timecode, elapsedTime, recordedDate) match {
      case Some(imagedMoment) => imagedMoment
      case None =>
        val imagedMoment = dao.newPersistentObject(videoReferenceUUID, timecode, elapsedTime, recordedDate)
        dao.create(imagedMoment)
        imagedMoment
    }
  }
}
