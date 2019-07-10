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
import java.time.{Duration, Instant}
import java.util.{UUID, stream}

import javax.persistence.EntityManager
import org.mbari.vars.annotation.dao.ObservationDAO
import org.mbari.vars.annotation.model.simple.{ConcurrentRequest, MultiRequest}

import scala.collection.JavaConverters._

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T17:10:00
 */
class ObservationDAOImpl(entityManager: EntityManager)
  extends BaseDAO[ObservationImpl](entityManager)
  with ObservationDAO[ObservationImpl] {

  override def newPersistentObject(): ObservationImpl = new ObservationImpl

  override def newPersistentObject(
    concept: String,
    observer: String,
    observationDate: Instant = Instant.now(),
    group: Option[String] = None,
    duration: Option[Duration] = None): ObservationImpl = {

    val observation = newPersistentObject()
    observation.concept = concept
    observation.observer = observer
    observation.observationDate = observationDate
    group.foreach(observation.group = _)
    duration.foreach(observation.duration = _)
    observation
  }

  override def findByVideoReferenceUUID(uuid: UUID, limit: Option[Int] = None, offset: Option[Int] = None): Iterable[ObservationImpl] =
    findByNamedQuery("Observation.findByVideoReferenceUUID", Map("uuid" -> uuid), limit, offset)


  override def streamByVideoReferenceUUID(uuid: UUID, limit: Option[Int], offset: Option[Int]): stream.Stream[ObservationImpl] =
    streamByNamedQuery("Observation.findByVideoReferenceUUID", Map("uuid" -> uuid), limit, offset)

  override def streamByVideoReferenceUUIDAndTimestamps(uuid: UUID,
                                                  startTimestamp: Instant,
                                                  endTimestamp: Instant,
                                                  limit: Option[Int],
                                                  offset: Option[Int]): stream.Stream[ObservationImpl] = {

    streamByNamedQuery("Observation.findByVideoReferenceUUIDAndTimestamps",
      Map("uuid" -> uuid,
        "start" -> startTimestamp,
        "end" -> endTimestamp), limit, offset)
  }


  override def countByVideoReferenceUUIDAndTimestamps(uuid: UUID, startTimestamp: Instant, endTimestamp: Instant): Int = {
    val query = entityManager.createNamedQuery("Observation.countByVideoReferenceUUIDAndTimestamps")
    query.setParameter(1, UUIDConverter.uuidToString(uuid))
    query.setParameter(2, Timestamp.from(startTimestamp))
    query.setParameter(3, Timestamp.from(endTimestamp))
    query.getSingleResult.asInstanceOf[Int]
  }


  override def streamByConcurrentRequest(request: ConcurrentRequest, limit: Option[Int], offset: Option[Int]): stream.Stream[ObservationImpl] = {
    streamByNamedQuery("Observation.findByConcurrentRequest",
      Map("uuids" -> request.videoReferenceUuids,
        "start" -> request.startTimestamp,
        "end" -> request.endTimestamp), limit, offset)
  }

  override def countByConcurrentRequest(request: ConcurrentRequest): Long = {
    val query = entityManager.createNamedQuery("Observation.countByConcurrentRequest")
    query.setParameter("uuids", request.videoReferenceUuids)
    query.setParameter("start", request.startTimestamp)
    query.setParameter("end", request.endTimestamp)
    query.getSingleResult.asInstanceOf[Long]
  }

  override def streamByMultiRequest(request: MultiRequest, limit: Option[Int], offset: Option[Int]): stream.Stream[ObservationImpl] = {
    streamByNamedQuery("Observation.findByMultiRequest",
      Map("uuids" -> request.videoReferenceUuids), limit, offset)
  }

  override def countByMultiRequest(request: MultiRequest): Long = {
    val query = entityManager.createNamedQuery("Observation.countByMultiRequest")
    query.setParameter("uuids", request.videoReferenceUuids)
    query.getSingleResult.asInstanceOf[Long]
  }

  /**
   *
   * @return Order sequence of all concept names used
   */
  override def findAllConcepts(): Seq[String] = entityManager.createNamedQuery("Observation.findAllNames")
    .getResultList
    .asScala
    .filter(_ != null)
    .map(_.toString)

  override def findAllGroups(): Seq[String] = entityManager.createNamedQuery("Observation.findAllGroups")
    .getResultList
    .asScala
    .filter(_ != null)
    .map(_.toString)

  override def findAllActivities(): Seq[String] = entityManager.createNamedQuery("Observation.findAllActivities")
    .getResultList
    .asScala
    .filter(_ != null)
    .map(_.toString)


  override def findAll(limit: Option[Int] = None, offset: Option[Int] = None): Iterable[ObservationImpl] =
    findByNamedQuery("Observation.findAll", limit = limit, offset = offset)

  override def findAllConceptsByVideoReferenceUUID(uuid: UUID): Seq[String] = {
    val query = entityManager.createNamedQuery("Observation.findAllNamesByVideoReferenceUUID")
    query.setParameter(1, UUIDConverter.uuidToString(uuid))
    query.getResultList
      .asScala
      .map(_.toString)
  }

  override def countByConcept(name: String): Int = {
    val query = entityManager.createNamedQuery("Observation.countByConcept")
    query.setParameter(1, name)
    query.getResultList
      .asScala
      .map(_.asInstanceOf[Number])
      .map(_.intValue())
      .head
  }

  override def countByVideoReferenceUUID(uuid: UUID): Int = {
    val query = entityManager.createNamedQuery("Observation.countByVideoReferenceUUID")
    query.setParameter(1, UUIDConverter.uuidToString(uuid))
    query.getResultList
      .asScala
      .map(_.asInstanceOf[Number])
      .map(_.intValue())
      .head
  }

  override def countAllByVideoReferenceUuids(): Map[UUID, Int] = {
    val query = entityManager.createNamedQuery("Observation.countAllByVideoReferenceUUIDs")
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

  override def updateConcept(oldConcept: String, newConcept: String): Int = {
    val query = entityManager.createNamedQuery("Observation.updateConcept")
    query.setParameter(1, newConcept)
    query.setParameter(2, oldConcept)
    query.executeUpdate()
  }

  override def changeImageMoment(imagedMomentUuid: UUID, observationUuid: UUID): Int = {
    val query = entityManager.createNamedQuery("Observation.updateImagedMomentUUID")
    query.setParameter(1, imagedMomentUuid.toString)
    query.setParameter(2, observationUuid.toString)
    query.executeUpdate()
  }

}
