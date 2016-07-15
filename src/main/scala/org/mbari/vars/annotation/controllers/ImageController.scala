package org.mbari.vars.annotation.controllers

import java.net.URL
import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.dao.ImagedMomentDAO
import org.mbari.vars.annotation.model.{ ImageReference, ImagedMoment }
import org.mbari.vars.annotation.model.simple.Image
import org.mbari.vcr4j.time.Timecode

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Created by brian on 7/14/16.
 */
class ImageController(daoFactory: BasicDAOFactory) {

  def findByUUID(uuid: UUID)(implicit ec: ExecutionContext): Future[Option[Image]] = {
    val irDao = daoFactory.newImageReferenceDAO()
    val f = irDao.runTransaction(d => irDao.findByUUID(uuid))
    f.onComplete(t => irDao.close())
    f.map(_.map(Image(_)))
  }

  def findByVideoReferenceUUID(
    videoReferenceUUID: UUID,
    limit: Option[Int] = None,
    offset: Option[Int] = None
  )(implicit ec: ExecutionContext): Future[Seq[Image]] = {
    val dao = daoFactory.newImagedMomentDAO()
    val f = dao.runTransaction(d => d.findByVideoReferenceUUID(videoReferenceUUID, limit, offset))
    f.onComplete(t => dao.close())
    f.map(ims => ims.flatMap(_.imageReferences))
      .map(irs => irs.toSeq.map(Image(_)))
  }

  def create(
    videoReferenceUUID: UUID,
    url: URL,
    timecode: Option[Timecode] = None,
    elapsedTime: Option[Duration] = None,
    recordedDate: Option[Instant] = None,
    format: Option[String],
    width: Option[Int],
    height: Option[Int],
    description: Option[String]
  )(implicit ec: ExecutionContext): Future[Image] = {

    val imDao = daoFactory.newImagedMomentDAO()
    val irDao = daoFactory.newImageReferenceDAO(imDao)
    val f = irDao.runTransaction(d => {
      val imagedMoment = ImagedMomentController.findImagedMoment(imDao, videoReferenceUUID, timecode,
        recordedDate, elapsedTime)
      val imageReference = irDao.newPersistentObject(url, description, height, width, format)
      irDao.create(imageReference)
      imagedMoment.addImageReference(imageReference)
      imageReference
    })
    f.onComplete(t => irDao.close())
    f.map(Image(_))
  }

  /**
   * Update params. Note that if you provide video indices then the image is moved, the
   * indices are not updated in place as this would effect any observations or images associated
   * with the same image moment. If you want to change the indices in place, use the the ImageMomentController instead.
   * @param uuid
   * @param videoReferenceUUID
   * @param timecode
   * @param elapsedTime
   * @param recordedDate
   * @param format
   * @param width
   * @param height
   * @param description
   * @param ec
   * @return
   */
  def update(
    uuid: UUID,
    videoReferenceUUID: Option[UUID] = None,
    timecode: Option[Timecode] = None,
    elapsedTime: Option[Duration] = None,
    recordedDate: Option[Instant] = None,
    format: Option[String],
    width: Option[Int],
    height: Option[Int],
    description: Option[String]
  )(implicit ec: ExecutionContext): Future[Option[Image]] = {

    val imDao = daoFactory.newImagedMomentDAO()
    val irDao = daoFactory.newImageReferenceDAO(imDao)

    val f = irDao.runTransaction(d => {
      val imageReference = d.findByUUID(uuid)
      imageReference.map(ir => {
        val vrUUID = videoReferenceUUID.getOrElse(ir.imagedMoment.videoReferenceUUID)
        if (timecode.isDefined || elapsedTime.isDefined || recordedDate.isDefined) {
          // change indices
          val newIm = ImagedMomentController.findImagedMoment(imDao, vrUUID, timecode, recordedDate, elapsedTime)
          move(imDao, newIm, ir)
        } else if (videoReferenceUUID.isDefined) {
          // move to new video-reference/imaged-moment using the existing images
          val tc = Option(ir.imagedMoment.timecode)
          val rd = Option(ir.imagedMoment.recordedDate)
          val et = Option(ir.imagedMoment.elapsedTime)
          val newIm = ImagedMomentController.findImagedMoment(imDao, vrUUID, tc, rd, et)
          move(imDao, newIm, ir)
        }

        format.foreach(ir.format = _)
        width.foreach(ir.width = _)
        height.foreach(ir.height = _)
        description.foreach(ir.description = _)
        ir
      })
    })
    f.onComplete(t => irDao.close())
    f.map(_.map(Image(_)))
  }

  def delete(uuid: UUID)(implicit ec: ExecutionContext): Future[Boolean] = {
    val imDao = daoFactory.newImagedMomentDAO()
    val irDao = daoFactory.newImageReferenceDAO(imDao)
    val f = irDao.runTransaction(d => {
      d.findByUUID(uuid) match {
        case None => false
        case Some(v) =>
          val imagedMoment = v.imagedMoment
          imagedMoment.removeImageReference(v)
          d.delete(v)
          imDao.deleteIfEmpty(imagedMoment)
      }
    })
    f.onComplete(t => irDao.close())
    f
  }

  private def move(dao: ImagedMomentDAO[ImagedMoment], newIm: ImagedMoment, imageReference: ImageReference): Unit = {
    val oldIm = imageReference.imagedMoment
    oldIm.removeImageReference(imageReference)
    newIm.addImageReference(imageReference)
    dao.deleteIfEmpty(oldIm)
  }

}
