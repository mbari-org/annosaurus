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

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.dao.jpa.ImagedMomentImpl
import org.mbari.vars.annotation.model.{ Annotation, ImagedMoment }
import org.mbari.vcr4j.time.Timecode

import scala.concurrent.Future

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
    recordedDate: Option[Instant] = None): T

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
    offset: Option[Int] = None): Iterable[T]

  def countByConcept(concept: String): Int
  def findByConcept(concept: String, limit: Option[Int], offset: Option[Int]): Iterable[T]

  def countByConceptWithImages(concept: String): Int
  def findByConceptWithImages(concept: String, limit: Option[Int], offset: Option[Int]): Iterable[T]

  def countBetweenUpdatedDates(start: Instant, end: Instant): Int

  def findAllVideoReferenceUUIDs(limit: Option[Int], offset: Option[Int]): Iterable[UUID]
  def findByVideoReferenceUUID(uuid: UUID, limit: Option[Int] = None, offset: Option[Int] = None): Iterable[T]
  def countByVideoReferenceUUID(uUID: UUID): Int

  def findWithImageReferences(videoReferenceUUID: UUID): Iterable[T]
  def findByImageReferenceUUID(imageReferenceUUID: UUID): Option[T]

  def findByVideoReferenceUUIDAndTimecode(uuid: UUID, timecode: Timecode): Option[T]
  def findByVideoReferenceUUIDAndRecordedDate(uuid: UUID, recordedDate: Instant): Option[T]
  def findByVideoReferenceUUIDAndElapsedTime(uuid: UUID, elapsedTime: Duration): Option[T]

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
   * @return
   */
  def findByVideoReferenceUUIDAndIndex(
    uuid: UUID,
    timecode: Option[Timecode] = None,
    elapsedTime: Option[Duration] = None,
    recordedDate: Option[Instant] = None): Option[T] = {
    var imagedMoment = timecode.flatMap(findByVideoReferenceUUIDAndTimecode(uuid, _))
    imagedMoment = if (imagedMoment.isDefined) imagedMoment else elapsedTime.flatMap(findByVideoReferenceUUIDAndElapsedTime(uuid, _))
    if (imagedMoment.isDefined) imagedMoment else recordedDate.flatMap(findByVideoReferenceUUIDAndRecordedDate(uuid, _))
  }

  def findByObservationUUID(uuid: UUID): Option[T]

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
      } else false
    })
  }
}
