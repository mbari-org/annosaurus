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

import org.mbari.annosaurus.domain.{Image, ImageCreateSC}
import org.mbari.annosaurus.repository.ImagedMomentDAO
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import org.mbari.annosaurus.repository.jpa.entity.{ImageReferenceEntity, ImagedMomentEntity}
import org.mbari.vcr4j.time.Timecode

import java.net.URL
import java.time.{Duration, Instant}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

/**
 * Created by brian on 7/14/16.
 */
class ImageController(daoFactory: JPADAOFactory):

    private val log = System.getLogger(getClass.getName)

    def findByUUID(uuid: UUID)(implicit ec: ExecutionContext): Future[Option[Image]] =
        val irDao = daoFactory.newImageReferenceDAO()
        val f     = irDao.runReadOnlyTransaction(d => irDao.findByUUID(uuid))
        f.onComplete(t => irDao.close())
        f.map(_.map(Image.from(_, true)))

    def findByVideoReferenceUUID(
        videoReferenceUUID: UUID,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    )(implicit ec: ExecutionContext): Future[Seq[Image]] =
        val dao = daoFactory.newImagedMomentDAO()
        val f   =
            dao.runReadOnlyTransaction(d =>
                d.findByVideoReferenceUUID(videoReferenceUUID, limit, offset)
                    .flatMap(_.getImageReferences.asScala)
                    .map(Image.from(_, true))
            ).map(_.toSeq)
        f.onComplete(t => dao.close())
        f

    def findByURL(url: URL)(implicit ec: ExecutionContext): Future[Option[Image]] =
        val dao = daoFactory.newImageReferenceDAO()
        val f   = dao.runReadOnlyTransaction(d =>
            d.findByURL(url)
                .map(Image.from(_, true))
        )
        f.onComplete(_ => dao.close())
        f

    def findByImageName(name: String)(implicit ec: ExecutionContext): Future[Seq[Image]] =
        val dao = daoFactory.newImageReferenceDAO()
        val f   = dao.runReadOnlyTransaction(d =>
            d.findByImageName(name)
                .map(Image.from(_, true))
        )
        f.onComplete(t => dao.close())
        f

    /**
     * @param imageCreates
     *   The image data to create
     * @param ec
     * @return
     *   Only the newly created images. If an image already exists, it is not returned.
     */
    def bulkCreate(imageCreates: Seq[ImageCreateSC])(using
        ec: ExecutionContext
    ): Future[Seq[Image]] =
        val imDao      = daoFactory.newImagedMomentDAO()
        val irDao      = daoFactory.newImageReferenceDAO(imDao)
        val candidates = imageCreates.distinctBy(_.url)
        // prefilter
        if candidates.isEmpty then return Future.successful(Seq.empty)
        val f          = for
            newOnes      <- irDao.runTransaction(d => candidates.filter(c => d.findByURL(c.url).isEmpty))
            newlyCreated <- irDao.runTransaction(d =>
                                for ic <- newOnes
                                yield
                                    val imagedMoment   = ImagedMomentController
                                        .findOrCreateImagedMoment(
                                            imDao,
                                            ic.video_reference_uuid,
                                            ic.timecode.map(new Timecode(_)),
                                            ic.recorded_timestamp,
                                            ic.elapsed_time_millis.map(Duration.ofMillis)
                                        )
                                    val imageReference = irDao.newPersistentObject(
                                        ic.url,
                                        ic.description,
                                        ic.height_pixels,
                                        ic.width_pixels,
                                        ic.format
                                    )
                                    imagedMoment.addImageReference(imageReference)
                                    irDao.flush()
                                    imageReference
                            )
            persisted    <-
                irDao.runTransaction(d => newlyCreated.flatMap(i => d.findByUUID(i.getUuid).map(Image.from(_, true))))
        yield persisted

        f.onComplete(t => irDao.close())
        f

    def create(
        videoReferenceUUID: UUID,
        url: URL,
        timecode: Option[Timecode] = None,
        elapsedTime: Option[Duration] = None,
        recordedDate: Option[Instant] = None,
        format: Option[String] = None,
        width: Option[Int] = None,
        height: Option[Int] = None,
        description: Option[String] = None
    )(implicit ec: ExecutionContext): Future[Image] =

        val imDao = daoFactory.newImagedMomentDAO()
        val irDao = daoFactory.newImageReferenceDAO(imDao)
        val f     = irDao.runTransaction(d =>
            val imagedMoment   = ImagedMomentController
                .findOrCreateImagedMoment(
                    imDao,
                    videoReferenceUUID,
                    timecode,
                    recordedDate,
                    elapsedTime
                )
            val imageReference = irDao.newPersistentObject(url, description, height, width, format)
//            imageReferenceUUID.foreach(imageReference.setUuid)
//            irDao.create(imageReference)
            imagedMoment.addImageReference(imageReference)
            d.flush()
            Image.from(imageReference, true)
        )
        f.onComplete(t => irDao.close())
        f

    /**
     * Update params. Note that if you provide video indices then the image is moved, the indices are not updated in
     * place as this would effect any observations or images associated with the same image moment. If you want to
     * change the indices in place, use the the ImageMomentController instead.
     * @param imageReferenceUuid
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
        imageReferenceUuid: UUID,
        videoReferenceUUID: Option[UUID] = None,
        url: Option[URL] = None,
        timecode: Option[Timecode] = None,
        elapsedTime: Option[Duration] = None,
        recordedDate: Option[Instant] = None,
        format: Option[String] = None,
        width: Option[Int] = None,
        height: Option[Int] = None,
        description: Option[String] = None
    )(implicit ec: ExecutionContext): Future[Option[Image]] =

        val imDao = daoFactory.newImagedMomentDAO()
        val irDao = daoFactory.newImageReferenceDAO(imDao)

        val f = irDao.runTransaction(d =>
            val opt = d.findByUUID(imageReferenceUuid)

            opt.map(ir =>

                url.foreach(ir.setUrl)
                format.foreach(ir.setFormat)
                width.foreach(ir.setWidth(_))
                height.foreach(ir.setHeight(_))
                description.foreach(ir.setDescription)
                d.flush()

                val vrUUID = videoReferenceUUID.getOrElse(ir.getImagedMoment.getVideoReferenceUuid)
                if timecode.isDefined || elapsedTime.isDefined || recordedDate.isDefined then
                    // change indices
                    val newIm = ImagedMomentController
                        .findOrCreateImagedMoment(
                            imDao,
                            vrUUID,
                            timecode,
                            recordedDate,
                            elapsedTime
                        )
                    move(imDao, newIm, ir)
                else if videoReferenceUUID.isDefined then
                    val imagedMoment = ir.getImagedMoment
                    // move to new video-reference/imaged-moment using the existing images
                    val tc           = Option(imagedMoment.getTimecode)
                    val rd           = Option(imagedMoment.getRecordedTimestamp)
                    val et           = Option(imagedMoment.getElapsedTime)
                    val newIm        =
                        ImagedMomentController.findOrCreateImagedMoment(imDao, vrUUID, tc, rd, et)
                    move(imDao, newIm, ir)
            )
        )

        val g = f.flatMap(opt =>
            opt.map(i => irDao.runTransaction(d => d.findByUUID(imageReferenceUuid).map(Image.from(_, true))))
                .getOrElse(Future(None))
        )

        g.onComplete(_ => irDao.close())
        g

    def delete(uuid: UUID)(implicit ec: ExecutionContext): Future[Boolean] =
        val imDao = daoFactory.newImagedMomentDAO()
        val irDao = daoFactory.newImageReferenceDAO(imDao)
        val f     = irDao.runTransaction(d =>
            d.findByUUID(uuid) match
                case None                 => false
                case Some(imageReference) =>
                    val imagedMoment = imageReference.getImagedMoment
                    if imagedMoment.getImageReferences.size == 1 && imagedMoment
                            .getObservations
                            .isEmpty
                    then
                        val imDao = daoFactory.newImagedMomentDAO(d)
                        imDao.delete(imagedMoment)
                    else imagedMoment.removeImageReference(imageReference)
//                        d.delete(imageReference)
                    true
        )
        f.onComplete(t => irDao.close())
        f

    private def move(
        dao: ImagedMomentDAO[ImagedMomentEntity],
        newIm: ImagedMomentEntity,
        imageReference: ImageReferenceEntity
    ): Unit =
        val oldIm = imageReference.getImagedMoment
        if !oldIm.getUuid.equals(newIm.getUuid) then
            val shouldDelete = oldIm.getImageReferences.size == 1 && oldIm.getObservations.isEmpty
            oldIm.removeImageReference(imageReference)
            newIm.addImageReference(imageReference)
            if shouldDelete then dao.delete(oldIm)
