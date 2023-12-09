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

package org.mbari.annosaurus.repository

import org.mbari.annosaurus.model.Observation
import org.mbari.annosaurus.model.simple.{ConcurrentRequest, MultiRequest}
import org.mbari.annosaurus.repository.jpa.entity.ObservationEntity

import java.time.{Duration, Instant}
import java.util.UUID

/** @author
  *   Brian Schlining
  * @since 2016-06-17T16:10:00
  */
trait ObservationDAO[T <: Observation] extends DAO[T] {

    def newPersistentObject(
        concept: String,
        observer: String,
        observationDate: Instant = Instant.now(),
        group: Option[String] = None,
        duration: Option[Duration] = None
    ): T

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

    def streamByVideoReferenceUUIDAndTimestamps(
        uuid: UUID,
        startTimestamp: Instant,
        endTimestamp: Instant,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): java.util.stream.Stream[T]

    def countByVideoReferenceUUIDAndTimestamps(
        uuid: UUID,
        startTimestamp: Instant,
        endTimestamp: Instant
    ): Int

    def streamByConcurrentRequest(
        request: ConcurrentRequest,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): java.util.stream.Stream[T]

    def countByConcurrentRequest(request: ConcurrentRequest): Long

    def streamByMultiRequest(
        request: MultiRequest,
        limit: Option[Int],
        offset: Option[Int]
    ): java.util.stream.Stream[ObservationEntity]

    def countByMultiRequest(request: MultiRequest): Long

    /** @return
      *   Order sequence of all concept names used
      */
    def findAllConcepts(): Seq[String]

    /** @return
      *   Ordered sequence of all activities used.
      */
    def findAllActivities(): Seq[String]

    /** @return
      *   Ordered sequence of all groups used.
      */
    def findAllGroups(): Seq[String]

    def findAllConceptsByVideoReferenceUUID(uuid: UUID): Seq[String]

    def countByConcept(name: String): Int

    def countByConceptWithImages(name: String): Int

    def countByVideoReferenceUUID(uuid: UUID): Int

    def countAllByVideoReferenceUuids(): Map[UUID, Int]

    def updateConcept(oldName: String, newName: String): Int

    /** Move an observation to a different imaged moment efficeintly
      * @param imagedMomentUuid
      *   The image moment we want to move to
      * @param observationUuid
      *   The observation to move
      * @return
      *   The number of records affected. Should be 1
      */
    def changeImageMoment(imagedMomentUuid: UUID, observationUuid: UUID): Int

}
