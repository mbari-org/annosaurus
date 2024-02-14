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

import jakarta.persistence.EntityManager
import org.mbari.annosaurus.domain.{ConcurrentRequest, MultiRequest}
import org.mbari.annosaurus.repository.ObservationDAO
import org.mbari.annosaurus.repository.jpa.entity.ObservationEntity

import java.sql.Timestamp
import java.time.{Duration, Instant}
import java.util.{stream, UUID}
import scala.jdk.CollectionConverters._
import java.{util => ju}
import org.mbari.annosaurus.repository.jdbc.*

/** @author
  *   Brian Schlining
  * @since 2016-06-17T17:10:00
  */
class ObservationDAOImpl(entityManager: EntityManager)
    extends BaseDAO[ObservationEntity](entityManager)
    with ObservationDAO[ObservationEntity] {

    override def newPersistentObject(): ObservationEntity = new ObservationEntity

    override def newPersistentObject(
        concept: String,
        observer: String,
        observationDate: Instant = Instant.now(),
        group: Option[String] = None,
        duration: Option[Duration] = None
    ): ObservationEntity = {

        val observation = new ObservationEntity
        observation.setConcept(concept)
        observation.setObserver(observer)
        observation.setObservationTimestamp(observationDate)
        group.foreach(observation.setGroup)
        duration.foreach(observation.setDuration)
        observation
    }

    override def findByVideoReferenceUuid(
        uuid: UUID,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): Iterable[ObservationEntity] =
        findByNamedQuery("Observation.findByVideoReferenceUUID", Map("uuid" -> uuid), limit, offset)

    override def streamByVideoReferenceUUID(
        uuid: UUID,
        limit: Option[Int],
        offset: Option[Int]
    ): stream.Stream[ObservationEntity] =
        streamByNamedQuery(
            "Observation.findByVideoReferenceUUID",
            Map("uuid" -> uuid),
            limit,
            offset
        )

    override def streamByVideoReferenceUUIDAndTimestamps(
        uuid: UUID,
        startTimestamp: Instant,
        endTimestamp: Instant,
        limit: Option[Int],
        offset: Option[Int]
    ): stream.Stream[ObservationEntity] = {

        streamByNamedQuery(
            "Observation.findByVideoReferenceUUIDAndTimestamps",
            Map("uuid" -> uuid, "start" -> startTimestamp, "end" -> endTimestamp),
            limit,
            offset
        )
    }

    override def countByVideoReferenceUUIDAndTimestamps(
        uuid: UUID,
        startTimestamp: Instant,
        endTimestamp: Instant
    ): Int = {
        // val query =
        //     entityManager.createNamedQuery("Observation.countByVideoReferenceUUIDAndTimestamps")
        // // setUuidParameter(query, 1, uuid)
        //     // query.setParameter(1, uuid.toString().toLowerCase())
        // query.setParameter(1, uuid)
        //     .setParameter(2, Timestamp.from(startTimestamp))
        //     .setParameter(3, Timestamp.from(endTimestamp))
        //     .getSingleResult
        //     .asInstanceOf[Number]
        //     .intValue()
        val query = entityManager.createNamedQuery("Observation.countByConcurrentRequest")
        query.setParameter("uuids", ju.List.of(uuid))
        query.setParameter("start", startTimestamp)
        query.setParameter("end", endTimestamp)
        query.getSingleResult.asInstanceOf[Number].intValue()
    }

    override def streamByConcurrentRequest(
        request: ConcurrentRequest,
        limit: Option[Int],
        offset: Option[Int]
    ): stream.Stream[ObservationEntity] = {
        streamByNamedQuery(
            "Observation.findByConcurrentRequest",
            Map(
                "uuids" -> request.videoReferenceUuids,
                "start" -> request.startTimestamp,
                "end"   -> request.endTimestamp
            ),
            limit,
            offset
        )
    }

    override def countByConcurrentRequest(request: ConcurrentRequest): Long = {
        val query = entityManager.createNamedQuery("Observation.countByConcurrentRequest")
        query.setParameter("uuids", request.videoReferenceUuids.asJava)
        query.setParameter("start", request.startTimestamp)
        query.setParameter("end", request.endTimestamp)
        query.getSingleResult.asInstanceOf[Number].longValue()
    }

    override def streamByMultiRequest(
        request: MultiRequest,
        limit: Option[Int],
        offset: Option[Int]
    ): stream.Stream[ObservationEntity] = {
        streamByNamedQuery(
            "Observation.findByMultiRequest",
            Map("uuids" -> request.videoReferenceUuids),
            limit,
            offset
        )
    }

    override def countByMultiRequest(request: MultiRequest): Long = {
        val query = entityManager.createNamedQuery("Observation.countByMultiRequest")
        query.setParameter("uuids", request.videoReferenceUuids.asJava)
        query.getSingleResult.asInstanceOf[Number].longValue()
    }

    /** @return
      *   Order sequence of all concept names used
      */
    override def findAllConcepts(): Seq[String] =
        entityManager
            .createNamedQuery("Observation.findAllNames")
            .getResultList
            .asScala
            .filter(_ != null)
            .map(_.toString)
            .toSeq

    override def findAllGroups(): Seq[String] =
        entityManager
            .createNamedQuery("Observation.findAllGroups")
            .getResultList
            .asScala
            .filter(_ != null)
            .map(_.toString)
            .toSeq

    override def findAllActivities(): Seq[String] =
        entityManager
            .createNamedQuery("Observation.findAllActivities")
            .getResultList
            .asScala
            .filter(_ != null)
            .map(_.toString)
            .toSeq

    override def findAll(
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): Iterable[ObservationEntity] =
        findByNamedQuery("Observation.findAll", limit = limit, offset = offset)

    override def findAllConceptsByVideoReferenceUUID(uuid: UUID): Seq[String] = {
        val query = entityManager.createNamedQuery("Observation.findAllNamesByVideoReferenceUUID")
        query.setParameter(1, uuid)
        query
            .getResultList
            .asScala
            .map(_.toString)
            .toSeq
    }

    override def countByConcept(name: String): Int = {
        val query = entityManager.createNamedQuery("Observation.countByConcept")
        query.setParameter(1, name)
        query
            .getResultList
            .asScala
            .map(_.asInstanceOf[Number].intValue())
            .head
    }

    override def countByConceptWithImages(name: String): Int = {
        val query = entityManager.createNamedQuery("Observation.countByConceptWithImages")
        query.setParameter(1, name)
        query
            .getResultList
            .asScala
            .map(_.asInstanceOf[Number].intValue())
            .head
    }

    override def countByVideoReferenceUUID(uuid: UUID): Int = {
        val query = entityManager.createNamedQuery("Observation.countByVideoReferenceUUID")
        query.setParameter(1, uuid)
        query
            .getResultList
            .asScala
            .map(_.asInstanceOf[Number].intValue())
            .head
    }

    override def countAllByVideoReferenceUuids(): Map[UUID, Int] = {
        val query = entityManager.createNamedQuery("Observation.countAllByVideoReferenceUUIDs")
        query
            .getResultList
            .asScala
            .map(_.asInstanceOf[Array[Object]])
            .map(xs => {
                val uuid  = xs(0).asUUID.getOrElse(throw new RuntimeException("UUID is null"))
                val count = xs(1).asInt.getOrElse(0)
                uuid -> count
            })
            .toMap
    }

    override def updateConcept(oldConcept: String, newConcept: String): Int = {
        val query = entityManager.createNamedQuery("Observation.updateConcept")
        query.setParameter(1, newConcept)
        query.setParameter(2, oldConcept)
        query.executeUpdate()
    }

    override def changeImageMoment(imagedMomentUuid: UUID, observationUuid: UUID): Int = {
        val query = entityManager.createNamedQuery("Observation.updateImagedMomentUUID")
        query
            .setParameter(1, imagedMomentUuid)
            .setParameter(2, observationUuid)
        query.executeUpdate()
    }

}
