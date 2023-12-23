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

package org.mbari.annosaurus.repository.jpa

import java.sql.Timestamp
import java.time.{Duration, Instant}
import java.util.function.Function
import java.util.{stream, UUID}
import jakarta.persistence.EntityManager

import org.mbari.annosaurus.domain.{ImagedMoment, WindowRequest}
import org.mbari.annosaurus.repository.ImagedMomentDAO
import org.mbari.annosaurus.repository.jpa.entity.ImagedMomentEntity
import org.mbari.vcr4j.time.Timecode

import scala.jdk.CollectionConverters._

/** @author
  *   Brian Schlining
  * @since 2016-06-17T16:34:00
  */
class ImagedMomentDAOImpl(entityManager: EntityManager)
    extends BaseDAO[ImagedMomentEntity](entityManager)
    with ImagedMomentDAO[ImagedMomentEntity] {

    override def newPersistentObject(): ImagedMomentEntity = new ImagedMomentEntity

    override def newPersistentObject(
        videoReferenceUUID: UUID,
        timecode: Option[Timecode] = None,
        elapsedTime: Option[Duration] = None,
        recordedDate: Option[Instant] = None
    ): ImagedMomentEntity = {
        val imagedMoment = new ImagedMomentEntity
        imagedMoment.setVideoReferenceUuid(videoReferenceUUID)
        timecode.foreach(imagedMoment.setTimecode)
        elapsedTime.foreach(imagedMoment.setElapsedTime)
        recordedDate.foreach(imagedMoment.setRecordedTimestamp)
        imagedMoment
    }

    override def newPersistentObject(imagedMoment: ImagedMomentEntity): ImagedMomentEntity =
        ImagedMoment.from(imagedMoment).toEntity

    def deleteIfEmptyByUUID(uuid: UUID): Boolean = {
        findByUUID(uuid).exists(imagedMoment => {

            if (imagedMoment.getImageReferences.isEmpty && imagedMoment.getObservations.isEmpty) {
                delete(imagedMoment)
                true
            }
            else false
        })
    }

    override def findBetweenUpdatedDates(
        start: Instant,
        end: Instant,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): Iterable[ImagedMomentEntity] = {

        val startTimestamp = Timestamp.from(start)
        val endTimestamp   = Timestamp.from(end)

        findByNamedQuery(
            "ImagedMoment.findBetweenUpdatedDates",
            Map("start" -> startTimestamp, "end" -> endTimestamp),
            limit,
            offset
        )
    }

    override def streamBetweenUpdatedDates(
        start: Instant,
        end: Instant,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): java.util.stream.Stream[ImagedMomentEntity] = {

        val startTimestamp = Timestamp.from(start)
        val endTimestamp   = Timestamp.from(end)

        streamByNamedQuery(
            "ImagedMoment.findBetweenUpdatedDates",
            Map("start" -> startTimestamp, "end" -> endTimestamp),
            limit,
            offset
        )
    }

    override def streamByVideoReferenceUUIDAndTimestamps(
        uuid: UUID,
        startTimestamp: Instant,
        endTimestamp: Instant,
        limit: Option[Int],
        offset: Option[Int]
    ): java.util.stream.Stream[ImagedMomentEntity] = {

        streamByNamedQuery(
            "ImagedMoment.findByVideoReferenceUUIDAndTimestamps",
            Map("uuid" -> uuid, "start" -> startTimestamp, "end" -> endTimestamp),
            limit,
            offset
        )
    }

    override def streamVideoReferenceUuidsBetweenUpdatedDates(
        start: Instant,
        end: Instant,
        limit: Option[Int],
        offset: Option[Int]
    ): java.util.stream.Stream[UUID] = {
        val query          =
            entityManager.createNamedQuery(
                "ImagedMoment.findVideoReferenceUUIDsModifiedBetweenDates"
            )
        val startTimestamp = Timestamp.from(start)
        val endTimestamp   = Timestamp.from(end)
        query.setParameter(1, startTimestamp)
        query.setParameter(2, endTimestamp)
        query
            .getResultStream
            .map(new Function[Any, UUID] {
                override def apply(t: Any): UUID = UUID.fromString(t.toString)
            })
    }

    override def countBetweenUpdatedDates(start: Instant, end: Instant): Int = {
        val query          = entityManager.createNamedQuery("ImagedMoment.countBetweenUpdatedDates")
        val startTimestamp = Timestamp.from(start)
        val endTimestamp   = Timestamp.from(end)
        query.setParameter(1, startTimestamp)
        query.setParameter(2, endTimestamp)
        query
            .getResultList
            .asScala
            .map(_.toString().toInt)
            .head
    }

    override def findAllVideoReferenceUUIDs(
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): Iterable[UUID] = {
        val query = entityManager.createNamedQuery("ImagedMoment.findAllVideoReferenceUUIDs")
        limit.foreach(query.setMaxResults)
        offset.foreach(query.setFirstResult)
        query
            .getResultList
            .asScala
            .map(s => UUID.fromString(s.toString))
    }

    override def countAllByVideoReferenceUuids(): Map[UUID, Int] = {
        val query = entityManager.createNamedQuery("ImagedMoment.countAllByVideoReferenceUUIDs")
        query
            .getResultList
            .asScala
            .map(_.asInstanceOf[Array[Object]])
            .map(xs => {
                val uuid  = UUID.fromString(xs(0).toString())
                val count = xs(1).toString().toInt
                uuid -> count
            })
            .toMap
    }

    override def countByConcept(concept: String): Int = {
        val query = entityManager.createNamedQuery("ImagedMoment.countByConcept")
        query.setParameter(1, concept)
        query
            .getResultList
            .asScala
            .map(_.toString().toInt)
            .head
    }

    override def findByConcept(
        concept: String,
        limit: Option[Int],
        offset: Option[Int]
    ): Iterable[ImagedMomentEntity] =
        findByNamedQuery("ImagedMoment.findByConcept", Map("concept" -> concept), limit, offset)

    override def streamByConcept(
        concept: String,
        limit: Option[Int],
        offset: Option[Int]
    ): stream.Stream[ImagedMomentEntity] =
        streamByNamedQuery("ImagedMoment.findByConcept", Map("concept" -> concept), limit, offset)

    override def countByConceptWithImages(concept: String): Int = {
        val query = entityManager.createNamedQuery("ImagedMoment.countByConceptWithImages")
        query.setParameter(1, concept)
        query
            .getResultList
            .asScala
            .map(_.toString().toInt)
            .head
    }

    override def countModifiedBeforeDate(videoReferenceUuid: UUID, date: Instant): Int = {
        val query = entityManager.createNamedQuery("ImagedMoment.countModifiedBeforeDate")
        query.setParameter(1, videoReferenceUuid)
        query.setParameter(2, Timestamp.from(date))
        query
            .getResultList
            .asScala
            .map(_.toString().toInt)
            .head
    }

    override def findByConceptWithImages(
        concept: String,
        limit: Option[Int],
        offset: Option[Int]
    ): Iterable[ImagedMomentEntity] =
        findByNamedQuery(
            "ImagedMoment.findByConceptWithImages",
            Map("concept" -> concept),
            limit,
            offset
        )

    override def countByVideoReferenceUUID(uuid: UUID): Int = {
        val query = entityManager.createNamedQuery("ImagedMoment.countByVideoReferenceUUID")
//    if (DatabaseProductName.isPostgreSQL()) {
//      query.setParameter(1, uuid)
//    }
//    else {
//      query.setParameter(1, uuid.toString)
//    }
        setUuidParameter(query, 1, uuid)
        query
            .getResultList
            .asScala
            .map(_.toString().toInt)
            .head
    }

    override def countByVideoReferenceUUIDWithImages(uuid: UUID): Int = {
        val query =
            entityManager.createNamedQuery("ImagedMoment.countByVideoReferenceUUIDWithImages")
        setUuidParameter(query, 1, uuid)
        query
            .getResultList
            .asScala
            .map(_.toString().toInt)
            .head
    }

    override def findByVideoReferenceUUID(
        uuid: UUID,
        limit: Option[Int],
        offset: Option[Int]
    ): Iterable[ImagedMomentEntity] =
        findByNamedQuery(
            "ImagedMoment.findByVideoReferenceUUID",
            Map("uuid" -> uuid),
            limit,
            offset
        )

    override def streamByVideoReferenceUUID(
        uuid: UUID,
        limit: Option[Int],
        offset: Option[Int]
    ): java.util.stream.Stream[ImagedMomentEntity] =
        streamByNamedQuery(
            "ImagedMoment.findByVideoReferenceUUID",
            Map("uuid" -> uuid),
            limit,
            offset
        )

    override def findWithImageReferences(videoReferenceUUID: UUID): Iterable[ImagedMomentEntity] =
        findByNamedQuery("ImagedMoment.findWithImageReferences", Map("uuid" -> videoReferenceUUID))

    override def findByImageReferenceUUID(imageReferenceUUID: UUID): Option[ImagedMomentEntity] =
        findByNamedQuery(
            "ImagedMoment.findByImageReferenceUUID",
            Map("uuid" -> imageReferenceUUID)
        ).headOption

    //  override def findByUUID(primaryKey: UUID): Option[ImagedMomentImpl] =
    //    findByNamedQuery("ImagedMoment.findByUUID", Map("uuid" -> primaryKey)).headOption

    override def findByWindowRequest(
        windowRequest: WindowRequest,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): Iterable[ImagedMomentEntity] = {

        findByUUID(windowRequest.imagedMomentUuid) match {
            case None     => Nil
            case Some(im) =>
                Option(im.getRecordedTimestamp) match {
                    case None               => Nil
                    case Some(recordedDate) =>
                        val start = recordedDate.minus(windowRequest.window)
                        val end   = recordedDate.plus(windowRequest.window)
                        findByNamedQuery(
                            "ImagedMoment.findByWindowRequest",
                            Map(
                                "uuids" -> windowRequest.videoReferenceUuids.asJava,
                                "start" -> start,
                                "end"   -> end
                            ),
                            limit,
                            offset
                        )
                }
        }

    }

    override def findAll(
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): Iterable[ImagedMomentEntity] =
        findByNamedQuery("ImagedMoment.findAll", limit = limit, offset = offset)

    override def countAll(): Int =
        entityManager
            .createNamedQuery("ImagedMoment.countAll")
            .getResultList
            .asScala
            .map(_.toString().toInt)
            .head

    override def findWithImages(
        limit: Option[Int],
        offset: Option[Int]
    ): Iterable[ImagedMomentEntity] =
        findByNamedQuery("ImagedMoment.findWithImages", limit = limit, offset = offset)

    override def countWithImages(): Int =
        entityManager
            .createNamedQuery("ImagedMoment.countWithImages")
            .getResultList
            .asScala
            .map(_.toString().toInt)
            .head

    override def findByLinkName(
        linkName: String,
        limit: Option[Int],
        offset: Option[Int]
    ): Iterable[ImagedMomentEntity] =
        findByNamedQuery(
            "ImagedMoment.findByLinkName",
            Map("linkName" -> linkName),
            limit = limit,
            offset = offset
        )

    override def countByLinkName(
        linkName: String
    ): Int = {
        val query = entityManager.createNamedQuery("ImagedMoment.countByLinkName")

        query.setParameter(1, linkName)

        query
            .getResultList
            .asScala
            .map(_.toString().toInt)
            .head
    }

    override def findByVideoReferenceUUIDAndElapsedTime(
        uuid: UUID,
        elapsedTime: Duration
    ): Option[ImagedMomentEntity] =
        findByNamedQuery(
            "ImagedMoment.findByVideoReferenceUUIDAndElapsedTime",
            Map("elapsedTime" -> elapsedTime, "uuid" -> uuid)
        ).headOption

    override def findByVideoReferenceUUIDAndTimecode(
        uuid: UUID,
        timecode: Timecode
    ): Option[ImagedMomentEntity] =
        findByNamedQuery(
            "ImagedMoment.findByVideoReferenceUUIDAndTimecode",
            Map("timecode" -> timecode, "uuid" -> uuid)
        ).headOption

    override def findByVideoReferenceUUIDAndRecordedDate(
        uuid: UUID,
        recordedDate: Instant
    ): Option[ImagedMomentEntity] =
        findByNamedQuery(
            "ImagedMoment.findByVideoReferenceUUIDAndRecordedDate",
            Map("recordedDate" -> recordedDate, "uuid" -> uuid)
        ).headOption

    override def findByObservationUUID(uuid: UUID): Option[ImagedMomentEntity] =
        findByNamedQuery("ImagedMoment.findByObservationUUID", Map("uuid" -> uuid)).headOption

    override def updateRecordedTimestampByObservationUuid(
        observationUuid: UUID,
        recordedTimestamp: Instant
    ): Boolean = {
        val query =
            entityManager.createNamedQuery("ImagedMoment.updateRecordedTimestampByObservationUuid")
        query.setParameter(1, recordedTimestamp)
        query.setParameter(2, observationUuid)
        query.executeUpdate() > 0
    }

    /** A bulk delete operation. This will delete all annotation related data for a single video.
      * (which is identified via its uuid (e.g. videoReferenceUUID)
      *
      * @param uuid
      *   The UUID of the VideoReference. WARNING!! All annotation data associated to this
      *   videoReference will be deleted.
      */
    @deprecated(message = "JPQL doesn't cascade. Don't use this", since = "2019-10-22")
    override def deleteByVideoReferenceUUUID(uuid: UUID): Int =
        executeNamedQuery("ImagedMoment.deleteByVideoReferenceUUID", Map("uuid" -> uuid))

//  override def delete(entity: ImagedMomentImpl): Unit = {
//    Option(entity.ancillaryDatum).foreach(entityManager.remove)
//    entity.observations.flatMap(_.associations).foreach(entityManager.remove)
//    entity.observations.foreach(entityManager.remove)
//    entity.imageReferences.foreach(entityManager.remove)
//    entityManager.remove(entity)
//  }
}
