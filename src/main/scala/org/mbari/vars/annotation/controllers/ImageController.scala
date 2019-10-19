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
    offset: Option[Int] = None)(implicit ec: ExecutionContext): Future[Seq[Image]] = {
    val dao = daoFactory.newImagedMomentDAO()
    val f = dao.runTransaction(d => d.findByVideoReferenceUUID(videoReferenceUUID, limit, offset))
    f.onComplete(t => dao.close())
    f.map(ims => ims.flatMap(_.imageReferences))
      .map(irs => irs.toSeq.map(Image(_)))
  }

  def findByURL(url: URL)(implicit ec: ExecutionContext): Future[Option[Image]] = {
    val dao = daoFactory.newImageReferenceDAO()
    val f = dao.runTransaction(d => d.findByURL(url))
    f.onComplete(t => dao.close())
    f.map(_.map(Image(_)))
  }

  def findByImageName(name: String)(implicit ec: ExecutionContext): Future[Seq[Image]] = {
    val dao = daoFactory.newImageReferenceDAO()
    val f = dao.runTransaction(d => d.findByImageName(name))
    f.onComplete(t => dao.close())
    f.map(_.map(Image(_)))
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
    description: Option[String])(implicit ec: ExecutionContext): Future[Image] = {

    val imDao = daoFactory.newImagedMomentDAO()
    val irDao = daoFactory.newImageReferenceDAO(imDao)
    val f = irDao.runTransaction(d => {
      val imagedMoment = ImagedMomentController.findOrCreateImagedMoment(imDao, videoReferenceUUID, timecode,
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
    url: Option[URL] = None,
    videoReferenceUUID: Option[UUID] = None,
    timecode: Option[Timecode] = None,
    elapsedTime: Option[Duration] = None,
    recordedDate: Option[Instant] = None,
    format: Option[String],
    width: Option[Int],
    height: Option[Int],
    description: Option[String])(implicit ec: ExecutionContext): Future[Option[Image]] = {

    val imDao = daoFactory.newImagedMomentDAO()
    val irDao = daoFactory.newImageReferenceDAO(imDao)

    val f = irDao.runTransaction(d => {
      val imageReference = d.findByUUID(uuid)
      imageReference.map(ir => {
        val vrUUID = videoReferenceUUID.getOrElse(ir.imagedMoment.videoReferenceUUID)
        if (timecode.isDefined || elapsedTime.isDefined || recordedDate.isDefined) {
          // change indices
          val newIm = ImagedMomentController.findOrCreateImagedMoment(imDao, vrUUID, timecode, recordedDate, elapsedTime)
          move(imDao, newIm, ir)
        } else if (videoReferenceUUID.isDefined) {
          // move to new video-reference/imaged-moment using the existing images
          val tc = Option(ir.imagedMoment.timecode)
          val rd = Option(ir.imagedMoment.recordedDate)
          val et = Option(ir.imagedMoment.elapsedTime)
          val newIm = ImagedMomentController.findOrCreateImagedMoment(imDao, vrUUID, tc, rd, et)
          move(imDao, newIm, ir)
        }

        url.foreach(ir.url = _)
        format.foreach(ir.format = _)
        width.foreach(ir.width = _)
        height.foreach(ir.height = _)
        description.foreach(ir.description = _)
        ir
      })
    })
    f.onComplete(_ => irDao.close())
    f.map(_.map(Image(_)))
  }

  def delete(uuid: UUID)(implicit ec: ExecutionContext): Future[Boolean] = {
    val imDao = daoFactory.newImagedMomentDAO()
    val irDao = daoFactory.newImageReferenceDAO(imDao)
    val f = irDao.runTransaction(d => {
      d.findByUUID(uuid) match {
        case None => false
        case Some(imageReference) =>
          val imagedMoment = imageReference.imagedMoment
          if (imagedMoment.imageReferences.size == 1 && imagedMoment.observations.isEmpty) {
            val imDao = daoFactory.newImagedMomentDAO(d)
            imDao.delete(imagedMoment)
          } else {
            d.delete(imageReference)
          }
          true
      }
    })
    f.onComplete(t => irDao.close())
    f
  }

  private def move(dao: ImagedMomentDAO[ImagedMoment], newIm: ImagedMoment, imageReference: ImageReference): Unit = {
    val oldIm = imageReference.imagedMoment
    if (!oldIm.uuid.equals(newIm.uuid)) {
      val shouldDelete = oldIm.imageReferences.size == 1 && oldIm.observations.isEmpty
      oldIm.removeImageReference(imageReference)
      newIm.addImageReference(imageReference)
      if (shouldDelete) {
        dao.delete(oldIm)
      }
    }
  }

}
