package org.mbari.vars.annotation.controllers

import java.net.URL
import java.util.UUID

import org.mbari.vars.annotation.dao.{ ImageReferenceDAO, NotFoundInDatastoreException }
import org.mbari.vars.annotation.model.ImageReference

import scala.concurrent.{ ExecutionContext, Future }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-07-04T22:15:00
 */
class ImageReferenceController(val daoFactory: BasicDAOFactory) extends BaseController[ImageReference, ImageReferenceDAO[ImageReference]] {

  type IRDAO = ImageReferenceDAO[ImageReference]

  override def newDAO(): IRDAO = daoFactory.newImageReferenceDAO()

  def create(
    imagedMomentUUID: UUID,
    url: URL,
    description: Option[String],
    heightPixels: Option[Int],
    widthPixels: Option[Int],
    format: Option[String]
  )(implicit ec: ExecutionContext): Future[ImageReference] = {

    def fn(dao: IRDAO): ImageReference = {
      val imDao = daoFactory.newImagedMomentDAO()
      imDao.findByUUID(imagedMomentUUID) match {
        case None => throw new NotFoundInDatastoreException(s"No ImagedMoment with UUID of $imagedMomentUUID was found")
        case Some(imagedMoment) =>
          val imageReference = dao.newPersistentObject()
          imageReference.url = url
          description.foreach(imageReference.description = _)
          heightPixels.foreach(imageReference.height = _)
          widthPixels.foreach(imageReference.width = _)
          format.foreach(imageReference.format = _)
          imagedMoment.addImageReference(imageReference)
          imageReference
      }
    }

    exec(fn)
  }

  def update(
    uuid: UUID,
    url: Option[URL] = None,
    description: Option[String] = None,
    heightPixels: Option[Int] = None,
    widthPixels: Option[Int] = None,
    format: Option[String] = None,
    imagedMomentUUID: Option[UUID] = None
  )(implicit ec: ExecutionContext): Future[Option[ImageReference]] = {

    def fn(dao: IRDAO): Option[ImageReference] = {
      dao.findByUUID(uuid).map(imageReference => {
        url.foreach(imageReference.url = _)
        description.foreach(imageReference.description = _)
        heightPixels.foreach(imageReference.height = _)
        widthPixels.foreach(imageReference.width = _)
        format.foreach(imageReference.format = _)
        imagedMomentUUID.foreach(imUUID => {
          val imDao = daoFactory.newImagedMomentDAO(dao)
          val newIm = imDao.findByUUID(imUUID)
          newIm match {
            case None =>
              throw new NotFoundInDatastoreException(s"ImagedMoment with UUID of $imUUID no found")
            case Some(imagedMoment) =>
              imageReference.imagedMoment.removeImageReference(imageReference)
              imagedMoment.addImageReference(imageReference)
          }
        })
        imageReference
      })
    }

    exec(fn)
  }

}
