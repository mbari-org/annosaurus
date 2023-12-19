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

import org.mbari.annosaurus.repository.{ImageReferenceDAO, NotFoundInDatastoreException}

import java.net.URL
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}
import org.mbari.annosaurus.repository.jpa.entity.ImageReferenceEntity
import org.mbari.annosaurus.repository.jpa.JPADAOFactory

/** @author
  *   Brian Schlining
  * @since 2016-07-04T22:15:00
  */
class ImageReferenceController(val daoFactory: JPADAOFactory)
    extends BaseController[ImageReferenceEntity, ImageReferenceDAO[ImageReferenceEntity]] {

    type IRDAO = ImageReferenceDAO[ImageReferenceEntity]

    override def newDAO(): IRDAO = daoFactory.newImageReferenceDAO()

    def create(
        imagedMomentUUID: UUID,
        url: URL,
        description: Option[String],
        heightPixels: Option[Int],
        widthPixels: Option[Int],
        format: Option[String]
    )(implicit ec: ExecutionContext): Future[ImageReferenceEntity] = {

        def fn(dao: IRDAO): ImageReferenceEntity = {
            val imDao = daoFactory.newImagedMomentDAO()
            imDao.findByUUID(imagedMomentUUID) match {
                case None               =>
                    throw new NotFoundInDatastoreException(
                        s"No ImagedMoment with UUID of $imagedMomentUUID was found"
                    )
                case Some(imagedMoment) =>
                    val imageReference =
                        dao.newPersistentObject(url, description, heightPixels, widthPixels, format)
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
    )(implicit ec: ExecutionContext): Future[Option[ImageReferenceEntity]] = {

        def fn(dao: IRDAO): Option[ImageReferenceEntity] = {
            dao
                .findByUUID(uuid)
                .map(imageReference => {
                    url.foreach(imageReference.url = _)
                    description.foreach(imageReference.description = _)
                    heightPixels.foreach(imageReference.height = _)
                    widthPixels.foreach(imageReference.width = _)
                    format.foreach(imageReference.format = _)
                    imagedMomentUUID.foreach(imUUID => {
                        val imDao = daoFactory.newImagedMomentDAO(dao)
                        val newIm = imDao.findByUUID(imUUID)
                        newIm match {
                            case None               =>
                                throw new NotFoundInDatastoreException(
                                    s"ImagedMoment with UUID of $imUUID no found"
                                )
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

    /** This controller will also delete the [[MutableImagedMoment]] if it is empty (i.e. no
      * observations or other imageReferences)
      *
      * @param uuid
      * @param ec
      * @return
      */
    override def delete(uuid: UUID)(implicit ec: ExecutionContext): Future[Boolean] = {
        def fn(dao: IRDAO): Boolean = {
            dao.findByUUID(uuid) match {
                case None                 => false
                case Some(imageReference) =>
                    val imagedMoment = imageReference.imagedMoment
                    // If this is the only imageref and there are no observations, delete the imagemoment
                    if (
                        imagedMoment.imageReferences.size == 1 && imagedMoment.observations.isEmpty
                    ) {
                        val imDao = daoFactory.newImagedMomentDAO(dao)
                        imDao.delete(imagedMoment)
                    }
                    else {
                        dao.delete(imageReference)
                    }
                    true
            }
        }
        exec(fn)
    }

}
