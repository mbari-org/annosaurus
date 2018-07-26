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

package org.mbari.vars.annotation.api

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.controllers.ImagedMomentController
import org.mbari.vars.annotation.model.simple.ObservationCount
import org.mbari.vcr4j.time.Timecode
import org.scalatra.{ BadRequest, NoContent, NotFound }

import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._
/**
 *
 *
 * @author Brian Schlining
 * @since 2016-07-11T16:58:00
 */
class ImagedMomentV1Api(controller: ImagedMomentController)(implicit val executor: ExecutionContext)
  extends APIStack {

  before() {
    contentType = "application/json"
    response.headers += ("Access-Control-Allow-Origin" -> "*")
  }

  get("/") {
    val limit = params.getAs[Int]("limit").getOrElse(1000)
    val offset = params.getAs[Int]("offset").getOrElse(0)
    controller.findAll(limit, offset)
      .map(_.asJava)
      .map(toJson)
  }

  get("/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a UUID")))
    controller.findByUUID(uuid).map({
      case None => halt(NotFound(
        body = s"An ImagedMoment with a UUID of $uuid was not found"))
      case Some(v) => toJson(v)
    })
  }

  get("/concept/:name") {
    val name = params.get("name")
      .getOrElse(halt(BadRequest("""{"reason": "Please provide a concept name"}""")))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    controller.findByConcept(name, limit, offset)
      .map(_.asJava)
      .map(toJson)
  }

  get("/concept/images/:name") {
    val name = params.get("name")
      .getOrElse(halt(BadRequest("""{"reason": "Please provide a concept name"}""")))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    controller.findByConceptWithImages(name, limit, offset)
      .map(_.asJava)
      .map(toJson)
  }

  // get("/concept/:name") {
  //   val name = params.get("name")
  //     .getOrElse(halt(BadRequest("""{"reason": "Please provide a concept name"}""")))
  //   val imDao = controller.daoFactory.newImagedMomentDAO()
  //   imDao match {
  //     case dao: ObservationDAOImpl =>
  //       // HACK: Working around controller!!
  //       response.setHeader("Transfer-Encoding", "chunked")
  //       response.setStatus(200)
  //       val out = response.getWriter
  //       out.write("[")
  //       val moments = dao.findByConcept(name, None, None).toList
  //       val n = moments.size - 1
  //       for (i <- moments.indices) {
  //         out.write(toJson(moments(i)))
  //         if (i < n) out.write(",")
  //       }
  //       out.write("]")
  //       out.flush()
  //     case _ =>
  //       val limit = params.getAs[Int]("limit")
  //       val offset = params.getAs[Int]("offset")
  //       controller.findByConcept(name, limit, offset)
  //         .map(_.asJava)
  //         .map(toJson)
  //   }
  // }

  get("/concept/count/:name") {
    val name = params.get("name")
      .getOrElse(halt(BadRequest("""{"reason": "Please provide a concept name"}""")))
    controller.countByConcept(name)
      .map(c => s"""{"concept": "$name", "count": $c}""")
  }

  get("/concept/images/count/:name") {
    val name = params.get("name")
      .getOrElse(halt(BadRequest("""{"reason": "Please provide a concept name"}""")))
    controller.countByConceptWithImages(name)
      .map(c => s"""{"concept": "$name", "count": $c}""")
  }

  get("/modified/:start/:end") {
    val start = params.getAs[Instant]("start").getOrElse(halt(BadRequest("Please provide a start date (yyyy-mm-ddThh:mm:ssZ)")))
    val end = params.getAs[Instant]("end").getOrElse(halt(BadRequest("Please provide an end date (yyyy-mm-ddThh:mm:ssZ)")))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    controller.findBetweenUpdatedDates(start, end, limit, offset)
      .map(_.asJava)
      .map(toJson)
  }

  get("/modified/count/:start/:end") {
    val start = params.getAs[Instant]("start").getOrElse(halt(BadRequest("Please provide a start date (yyyy-mm-ddThh:mm:ssZ)")))
    val end = params.getAs[Instant]("end").getOrElse(halt(BadRequest("Please provide an end date (yyyy-mm-ddThh:mm:ssZ)")))
    controller.countBetweenUpdatedDates(start, end)
      .map(n => s"""{"start_timestamp":"$start", "end_timestamp":"$end", "count": "$n"}""")
  }

  get("/counts") {
    controller.countAllGroupByVideoReferenceUUID()
      .map(_.map({ case (uuid, count) => ObservationCount(uuid, count) }))
      .map(_.asJava)
      .map(toJson)
  }

  get("/videoreference") {
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    controller.findAllVideoReferenceUUIDs(limit, offset)
      .map(_.toArray)
      .map(toJson)
  }

  get("/videoreference/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a Video Reference UUID")))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    controller.findByVideoReferenceUUID(uuid, limit, offset)
      .map(_.asJava)
      .map(toJson)
  }

  delete("/videoreference/:uuid") {
    validateRequest() // Apply API security
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a Video Reference UUID")))
    controller.deleteByVideoReferenceUUID(uuid)
      .map(toJson)
  }

  get("/imagereference/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide an ImageReference UUID")))
    controller.findByImageReferenceUUID(uuid)
      .map({
        case None => halt(NotFound(s"No imagereference with a uuid of $uuid was found"))
        case Some(im) => toJson(im)
      })
  }

  get("/observation/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide an Observation UUID")))
    controller.findByObservationUUID(uuid)
      .map({
        case None => halt(NotFound(s"No observation with a uuid of $uuid was found"))
        case Some(im) => toJson(im)
      })
  }

  //  post("/") {
  //    val videoReferenceUUID = params.getAs[UUID]("video_reference_uuid")
  //        .getOrElse(halt(BadRequest("A video_reference_uuid is required")))
  //    val timecode = params.getAs[Timecode]("timecode")
  //    val elapsedTime = params.getAs[Duration]("elapsed_time_millis")
  //    val recordedDate = params.getAs[Instant]("recorded_timestamp")
  //    if (timecode.isEmpty && elapsedTime.isEmpty && recordedDate.isEmpty) {
  //      halt(BadRequest("At least one of: timecode, elapsed_time_millis, or recorded_timestamp is required"))
  //    }
  //
  //  }

  put("/:uuid") {
    validateRequest() // Apply API security
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a UUID")))
    val timecode = params.getAs[Timecode]("timecode")
    val elapsedTime = params.getAs[Duration]("elapsed_time_millis")
    val recordedDate = params.getAs[Instant]("recorded_timestamp")
    val videoReferenceUUID = params.getAs[UUID]("video_reference_uuid")
    controller.update(uuid, videoReferenceUUID, timecode, recordedDate, elapsedTime)
      .map(toJson)
  }

  put("/newtime/:uuid/:time") {
    validateRequest()
    val uuid = params.getAs[UUID]("uuid")
      .getOrElse(halt(BadRequest("{\"error\": \"Please provide a video reference UUID parameter\"}")))
    val time = params.getAs[Instant]("time")
      .getOrElse(halt(BadRequest("{\"error\": \"Please provide a new start 'time' parameter\"}")))
    controller.updateRecordedTimestamps(uuid, time)
      .map(_.asJava)
      .map(toJson)
  }

  delete("/:uuid") {
    validateRequest() // Apply API security
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide the 'uuid' of the association")))
    controller.delete(uuid).map({
      case true => halt(NoContent()) // Success
      case false => halt(NotFound(s"Failed. No ImagedMoment with UUID of $uuid was found."))
    })
  }

}
