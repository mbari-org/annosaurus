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

  /*
  def create(
    imagedMomentUUID: UUID,
    url: URL,
    description: Option[String],
    heightPixels: Option[Int],
    widthPixels: Option[Int],
    format: Option[String]
  )(implicit ec: ExecutionContext): Future[ImageReference] = {

    def fn(dao: IRDAO): Option[ImageReference] = {
      val imDao = daoFactory.newImagedMomentDAO()
      imDao.findByUUID(imagedMomentUUID) match {
        case None => throw new NotFoundInDatastoreException(s"No ImagedMoment with UUID of $imagedMomentUUID was found")
        case Some(imagedMoment) => ???
      }

    }

    exec(fn)
  }
  */
}
