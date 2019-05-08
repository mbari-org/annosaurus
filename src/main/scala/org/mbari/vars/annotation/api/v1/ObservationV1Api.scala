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

package org.mbari.vars.annotation.api.v1

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.controllers.ObservationController
import org.mbari.vars.annotation.model.simple.ObservationCount
import org.scalatra.{ BadRequest, NoContent, NotFound }

import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-07-04T21:56:00
 */
class ObservationV1Api(controller: ObservationController)(implicit val executor: ExecutionContext)
  extends V1APIStack {

  get("/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a UUID")))
    controller.findByUUID(uuid).map({
      case None => halt(NotFound(
        body = s"{reason: 'An ImagedMoment with a UUID of $uuid was not found'}"))
      case Some(v) => toJson(v)
    })
  }

  get("/videoreference/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a Video Reference UUID")))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    controller.findByVideoReferenceUUID(uuid, limit, offset)
      .map(_.asJava)
      .map(toJson)
  }

  get("/activities") {
    controller.findAllActivities
      .map(_.toArray)
      .map(toJson)
  }

  get("/association/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide an Association UUID")))
    controller.findByAssociationUUID(uuid)
      .map({
        case None => halt(NotFound(s"No observation for association with uuid of ${uuid} was found"))
        case Some(obs) => toJson(obs)
      })
  }

  get("/concepts") {
    controller.findAllConcepts
      .map(_.toArray)
      .map(toJson)
  }

  get("/concepts/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a video-reference 'uuid'")))
    controller.findAllConceptsByVideoReferenceUUID(uuid)
      .map(_.toArray)
      .map(toJson)
  }

  get("/concept/count/:concept") {
    val concept = params.get("concept").getOrElse(halt(BadRequest("Please provide a concept to search for")))
    controller.countByConcept(concept)
      .map(n => s"""{"concept":"$concept", "count":"$n"}""")
  }

  // TODO add method that returns all concepts and their count

  get("/groups") {
    controller.findAllGroups
      .map(_.toArray)
      .map(toJson)
  }

  get("/videoreference/count/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a video-reference 'uuid'")))
    controller.countByVideoReferenceUUID(uuid)
      .map(n => ObservationCount(uuid, n))
      .map(toJson)
    //.map(n => s"""{"video_reference_uuid": "${uuid}", "count":"$n"}""")
  }

  get("/counts") {
    controller.countAllGroupByVideoReferenceUUID()
      .map(_.map({ case (uuid, count) => ObservationCount(uuid, count) }))
      .map(_.asJava)
      .map(toJson)
  }

  put("/concept/rename") {
    val oldConcept = params.get("old").getOrElse(halt(BadRequest("Please provide the concept being replaced")))
    val newConcept = params.get("new").getOrElse(halt(BadRequest("Please provide the replacement concept")))
    controller.updateConcept(oldConcept, newConcept)
      .map(n => s"""{"old_concept":"$oldConcept", "new_concept":"$newConcept", "number_updated":"$n"}""")
  }

  put("/:uuid") {
    validateRequest() // Apply API security
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "{reason: 'A uuid parameter is required'}")))
    val concept = params.get("concept")
    val observer = params.get("observer")
    val observationDate = params.getAs[Instant]("observation_timestamp").getOrElse(Instant.now())
    val duration = params.getAs[Duration]("duration_millis")
    val group = params.get("group")
    val activity = params.get("activity")
    val imagedMomentUUID = params.getAs[UUID]("imaged_moment_uuid")
    controller.update(uuid, concept, observer, observationDate, duration, group, activity, imagedMomentUUID)
      .map({
        case None => halt(NotFound(s"Failed. No observation with UUID of $uuid was found."))
        case Some(obs) => toJson(obs)
      })
  }

  delete("/:uuid") {
    validateRequest() // Apply API security
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a UUID")))
    controller.delete(uuid).map({
      case true => halt(NoContent()) // Success!!
      case false => halt(NotFound(s"Failed. No observation with UUID of $uuid was found."))
    })
  }

  /*
     DELETE spec says ignore the request body. So to where the body contains an
     array of UUID's, we'll use a post methods instead.
   */
  post("/delete") {
    validateRequest() // Apply API security
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val uuids = fromJson(request.body, classOf[Array[UUID]])
        if (uuids == null || uuids.isEmpty) halt(BadRequest("No observation UUIDs were provided as JSON"))
        controller.bulkDelete(uuids)
      case _ =>
        halt(BadRequest("bulk delete only accepts JSON (Content-Type: application/json)"))
    }
  }
}
