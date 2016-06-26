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

  override def newDAO(): ImagedMomentDAO[ImagedMoment] = daoFactory.newImagedMomentDAO()

  def findByVideoReferenceUUID(uuid: UUID)(implicit ec: ExecutionContext): Future[Iterable[ImagedMoment]] =
    exec(d => d.findByVideoReferenceUUID(uuid))

  def findWithImageReferences(videoReferenceUUID: UUID)(implicit ec: ExecutionContext): Future[Iterable[ImagedMoment]] =
    exec(d => d.findWithImageReferences(videoReferenceUUID))

  def create(
    videoReferenceUUID: UUID,
    timecode: Option[Timecode] = None,
    recordedDate: Option[Instant] = None,
    elapsedTime: Option[Duration] = None
  )(implicit ec: ExecutionContext): Future[ImagedMoment] = {

    def fn(dao: IMDAO): ImagedMoment = {
      // -- Return existing or construct a new one if no match is found
      dao.findByVideoReferenceUUIDAndIndex(videoReferenceUUID, timecode, elapsedTime, recordedDate) match {
        case Some(imagedMoment) => imagedMoment
        case None =>
          val imagedMoment = dao.newPersistentObject()
          imagedMoment.videoReferenceUUID = videoReferenceUUID
          timecode.foreach(imagedMoment.timecode = _)
          elapsedTime.foreach(imagedMoment.elapsedTime = _)
          recordedDate.foreach(imagedMoment.recordedDate = _)
          dao.create(imagedMoment)
          imagedMoment
      }
    }
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
          imagedMoment
      }
    }

    exec(fn)
  }
}
