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

package org.mbari.vars.annotation.dao.jpa

import java.sql.Timestamp
import java.time.{ Duration, Instant }
import java.util.UUID

import javax.persistence.EntityManager
import org.mbari.vars.annotation.dao.ImagedMomentDAO
import org.mbari.vcr4j.time.Timecode

import scala.collection.JavaConverters._

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T16:34:00
 */
class ImagedMomentDAOImpl(entityManager: EntityManager)
  extends BaseDAO[ImagedMomentImpl](entityManager)
  with ImagedMomentDAO[ImagedMomentImpl] {

  override def newPersistentObject(): ImagedMomentImpl = new ImagedMomentImpl

  def newPersistentObject(
    videoReferenceUUID: UUID,
    timecode: Option[Timecode] = None,
    elapsedTime: Option[Duration] = None,
    recordedDate: Option[Instant] = None): ImagedMomentImpl = {
    val imagedMoment = new ImagedMomentImpl
    imagedMoment.videoReferenceUUID = videoReferenceUUID
    timecode.foreach(imagedMoment.timecode = _)
    elapsedTime.foreach(imagedMoment.elapsedTime = _)
    recordedDate.foreach(imagedMoment.recordedDate = _)
    imagedMoment
  }

  override def findBetweenUpdatedDates(
    start: Instant,
    end: Instant,
    limit: Option[Int] = None,
    offset: Option[Int] = None): Iterable[ImagedMomentImpl] = {

    val startTimestamp = Timestamp.from(start)
    val endTimestamp = Timestamp.from(end)

    findByNamedQuery(
      "ImagedMoment.findBetweenUpdatedDates",
      Map("start" -> startTimestamp, "end" -> endTimestamp),
      limit,
      offset)
  }

  override def countBetweenUpdatedDates(start: Instant, end: Instant): Int = {
    val query = entityManager.createNamedQuery("ImagedMoment.countBetweenUpdatedDates")
    val startTimestamp = Timestamp.from(start)
    val endTimestamp = Timestamp.from(end)
    query.setParameter(1, startTimestamp)
    query.setParameter(2, endTimestamp)
    query.getResultList
      .asScala
      .map(_.asInstanceOf[Int])
      .head
  }

  override def findAllVideoReferenceUUIDs(limit: Option[Int] = None, offset: Option[Int] = None): Iterable[UUID] = {
    val query = entityManager.createNamedQuery("ImagedMoment.findAllVideoReferenceUUIDs")
    limit.foreach(query.setMaxResults)
    offset.foreach(query.setFirstResult)
    query.getResultList
      .asScala
      .map(s => UUID.fromString(s.toString))
  }

  override def countAllByVideoReferenceUuids(): Map[UUID, Int] = {
    val query = entityManager.createNamedQuery("ImagedMoment.countAllByVideoReferenceUUIDs")
    query.getResultList
      .asScala
      .map(_.asInstanceOf[Array[Object]])
      .map(xs => {
        val uuid = UUID.fromString(xs(0).asInstanceOf[String])
        val count = xs(1).asInstanceOf[Number].intValue()
        uuid -> count
      })
      .toMap
  }

  override def countByConcept(concept: String): Int = {
    val query = entityManager.createNamedQuery("ImagedMoment.countByConcept")
    query.setParameter(1, concept)
    query.getResultList
      .asScala
      .map(_.asInstanceOf[Int])
      .head
  }

  override def findByConcept(
    concept: String,
    limit: Option[Int],
    offset: Option[Int]): Iterable[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findByConcept", Map("concept" -> concept), limit, offset)

  override def countByConceptWithImages(concept: String): Int = {
    val query = entityManager.createNamedQuery("ImagedMoment.countByConceptWithImages")
    query.setParameter(1, concept)
    query.getResultList
      .asScala
      .map(_.asInstanceOf[Int])
      .head
  }

  override def findByConceptWithImages(
    concept: String,
    limit: Option[Int],
    offset: Option[Int]): Iterable[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findByConceptWithImages", Map("concept" -> concept), limit, offset)

  override def countByVideoReferenceUUID(uuid: UUID): Int = {
    val query = entityManager.createNamedQuery("ImagedMoment.countBetweenUpdatedDates")
    query.setParameter(1, uuid)
    query.getResultList
      .asScala
      .map(_.asInstanceOf[Int])
      .head
  }

  override def findByVideoReferenceUUID(uuid: UUID, limit: Option[Int], offset: Option[Int]): Iterable[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findByVideoReferenceUUID", Map("uuid" -> uuid), limit, offset)

  override def streamByVideoReferenceUUID(uuid: UUID, limit: Option[Int], offset: Option[Int]): Iterable[ImagedMomentImpl] =
    streamByNamedQuery("ImagedMoment.findByVideoReferenceUUID", Map("uuid" -> uuid), limit, offset)

  override def findWithImageReferences(videoReferenceUUID: UUID): Iterable[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findWithImageReferences", Map("uuid" -> videoReferenceUUID))

  override def findByImageReferenceUUID(imageReferenceUUID: UUID): Option[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findByImageReferenceUUID", Map("uuid" -> imageReferenceUUID)).headOption

  //  override def findByUUID(primaryKey: UUID): Option[ImagedMomentImpl] =
  //    findByNamedQuery("ImagedMoment.findByUUID", Map("uuid" -> primaryKey)).headOption

  override def findAll(): Iterable[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findAll")

  override def findAll(limit: Int, offset: Int): Iterable[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findAll", limit = Some(limit), offset = Some(offset))

  override def findByVideoReferenceUUIDAndElapsedTime(uuid: UUID, elapsedTime: Duration): Option[ImagedMomentImpl] =
    findByNamedQuery(
      "ImagedMoment.findByVideoReferenceUUIDAndElapsedTime",
      Map("elapsedTime" -> elapsedTime, "uuid" -> uuid)).headOption

  override def findByVideoReferenceUUIDAndTimecode(uuid: UUID, timecode: Timecode): Option[ImagedMomentImpl] =
    findByNamedQuery(
      "ImagedMoment.findByVideoReferenceUUIDAndTimecode",
      Map("timecode" -> timecode, "uuid" -> uuid)).headOption

  override def findByVideoReferenceUUIDAndRecordedDate(uuid: UUID, recordedDate: Instant): Option[ImagedMomentImpl] =
    findByNamedQuery(
      "ImagedMoment.findByVideoReferenceUUIDAndRecordedDate",
      Map("recordedDate" -> recordedDate, "uuid" -> uuid)).headOption

  override def findByObservationUUID(uuid: UUID): Option[ImagedMomentImpl] =
    findByNamedQuery("ImagedMoment.findByObservationUUID", Map("uuid" -> uuid)).headOption

  /**
   * A bulk delete operation. This will delete all annotation related data for a single video.
   * (which is identified via its uuid (e.g. videoReferenceUUID)
   *
   * @param uuid The UUID of the VideoReference. WARNING!! All annotation data associated to
   *             this videoReference will be deleted.
   */
  override def deleteByVideoReferenceUUUID(uuid: UUID): Int =
    executeNamedQuery("ImagedMoment.deleteByVideoReferenceUUID", Map("uuid" -> uuid))
}
