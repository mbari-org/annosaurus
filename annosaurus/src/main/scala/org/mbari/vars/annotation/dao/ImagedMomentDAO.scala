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

package org.mbari.vars.annotation.dao

import java.time.{Duration, Instant}
import java.util.UUID

import org.mbari.vars.annotation.dao.jpa.ImagedMomentEntity
import org.mbari.vars.annotation.model.ImagedMoment
import org.mbari.vars.annotation.model.simple.WindowRequest
import org.mbari.vcr4j.time.Timecode

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-06-17T16:07:00
  */
trait ImagedMomentDAO[T <: ImagedMoment] extends DAO[T] {

  def newPersistentObject(
      videoReferenceUUID: UUID,
      timecode: Option[Timecode] = None,
      elapsedTime: Option[Duration] = None,
      recordedDate: Option[Instant] = None
  ): T

  def newPersistentObject(imagedMoment: ImagedMoment): T

  /**
    * Find ImagedMoments where the imagedmoment OR observation has been updated
    * between the requested dates.
    * @param start The starting date
    * @param end The ending date
    * @param limit The number of results to return. Default is all of them
    * @param offset The starting index of the results to return. Default is 0.
    * @return ImagedMoments between the given dates
    */
  def findBetweenUpdatedDates(
      start: Instant,
      end: Instant,
      limit: Option[Int] = None,
      offset: Option[Int] = None
  ): Iterable[T]

  def streamBetweenUpdatedDates(
      start: Instant,
      end: Instant,
      limit: Option[Int] = None,
      offset: Option[Int] = None
  ): java.util.stream.Stream[T]

  def streamByVideoReferenceUUIDAndTimestamps(
      uuid: UUID,
      startTimestamp: Instant,
      endTimestamp: Instant,
      limit: Option[Int],
      offset: Option[Int]
  ): java.util.stream.Stream[ImagedMomentEntity]

  def streamVideoReferenceUuidsBetweenUpdatedDates(
      start: Instant,
      end: Instant,
      limit: Option[Int] = None,
      offset: Option[Int] = None
  ): java.util.stream.Stream[UUID]

  def countAll(): Int

  def countWithImages(): Int
  def findWithImages(limit: Option[Int], offset: Option[Int]): Iterable[T]

  def countByLinkName(linkName: String): Int
  def findByLinkName(linkName: String, limit: Option[Int], offset: Option[Int]): Iterable[T]

  def countByConcept(concept: String): Int
  def findByConcept(concept: String, limit: Option[Int], offset: Option[Int]): Iterable[T]
  def streamByConcept(
      concept: String,
      limit: Option[Int],
      offset: Option[Int]
  ): java.util.stream.Stream[T]

  def countByConceptWithImages(concept: String): Int
  def countModifiedBeforeDate(videoReferenceUuid: UUID, date: Instant): Int
  def findByConceptWithImages(concept: String, limit: Option[Int], offset: Option[Int]): Iterable[T]

  def countBetweenUpdatedDates(start: Instant, end: Instant): Int
  def countAllByVideoReferenceUuids(): Map[UUID, Int]

  def findAllVideoReferenceUUIDs(limit: Option[Int], offset: Option[Int]): Iterable[UUID]
  def findByVideoReferenceUUID(
      uuid: UUID,
      limit: Option[Int] = None,
      offset: Option[Int] = None
  ): Iterable[T]
  def streamByVideoReferenceUUID(
      uuid: UUID,
      limit: Option[Int] = None,
      offset: Option[Int] = None
  ): java.util.stream.Stream[T]
  def countByVideoReferenceUUID(uUID: UUID): Int
  def countByVideoReferenceUUIDWithImages(uUID: UUID): Int

  def findWithImageReferences(videoReferenceUUID: UUID): Iterable[T]
  def findByImageReferenceUUID(imageReferenceUUID: UUID): Option[T]

  def findByVideoReferenceUUIDAndTimecode(uuid: UUID, timecode: Timecode): Option[T]
  def findByVideoReferenceUUIDAndRecordedDate(uuid: UUID, recordedDate: Instant): Option[T]
  def findByVideoReferenceUUIDAndElapsedTime(uuid: UUID, elapsedTime: Duration): Option[T]

  def findByWindowRequest(
      windowRequest: WindowRequest,
      limit: Option[Int] = None,
      offset: Option[Int] = None
  ): Iterable[T]

  /**
    * Look up an imaged moment based on the videoReferenceUUID and one of the indices into a video.
    * The order of search is
    * 1. Timecode
    * 2. ElapsedTime
    * 3. RecordedDate
    *
    * @param uuid The videoReferenceUUID that the imagedMoment is attached to
    * @param timecode The timecode index
    * @param elapsedTime The elapsedTime index (This is the index of runtime into the video)
    * @param recordedDate The recordedDate index
    * @return None if no match is found. Some if a match exists
    */
  def findByVideoReferenceUUIDAndIndex(
      uuid: UUID,
      timecode: Option[Timecode] = None,
      elapsedTime: Option[Duration] = None,
      recordedDate: Option[Instant] = None
  ): Option[T] = {

    // If timecode is supplied and no existing match is found return None.
    timecode match {
      case Some(t) => findByVideoReferenceUUIDAndTimecode(uuid, t)
      case None =>
        None
        val im0 = elapsedTime.flatMap(findByVideoReferenceUUIDAndElapsedTime(uuid, _))
        if (im0.isEmpty) recordedDate.flatMap(findByVideoReferenceUUIDAndRecordedDate(uuid, _))
        else im0
    }
    // This code has bug when resolving timecodes. See M3-15
//    val im0 = timecode.flatMap(findByVideoReferenceUUIDAndTimecode(uuid, _))
//    val im1 = if (im0.isEmpty) elapsedTime.flatMap(findByVideoReferenceUUIDAndElapsedTime(uuid, _)) else im0
//    if (im1.isEmpty) recordedDate.flatMap(findByVideoReferenceUUIDAndRecordedDate(uuid, _)) else im1

  }

  def findByObservationUUID(uuid: UUID): Option[T]

  def updateRecordedTimestampByObservationUuid(
      observationUuid: UUID,
      recordedTimestamp: Instant
  ): Boolean

  /**
    * A bulk delete operation. This will delete all annotation related data for a single video.
    * (which is identified via its uuid (e.g. videoReferenceUUID))
    *
    * @param uuid The UUID of the VideoReference. WARNING!! All annotation data associated to
    *             this videoReference will be deleted.
    * @return The number of records deleted
    */
  def deleteByVideoReferenceUUUID(uuid: UUID): Int

  /**
    * Deletes an imagedMoment if it does not contain any observations or imageReferences
    * @param imagedMoment The object to delete
    * @return true if deleted, false if not deleted.
    */
  def deleteIfEmpty(imagedMoment: T): Boolean = deleteIfEmptyByUUID(imagedMoment.uuid)

  def deleteIfEmptyByUUID(uuid: UUID): Boolean = {
    findByUUID(uuid).exists(imagedMoment => {

      if (imagedMoment.imageReferences.isEmpty && imagedMoment.observations.isEmpty) {
        delete(imagedMoment)
        true
      }
      else false
    })
  }
}
