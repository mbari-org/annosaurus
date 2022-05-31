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

import java.time.{Duration, Instant}
import java.util.UUID

import org.mbari.vars.annotation.controllers.ImagedMomentController
import org.mbari.vars.annotation.dao.jpa.AnnotationImpl
import org.mbari.vars.annotation.model.ImagedMoment
import org.mbari.vars.annotation.model.simple.{ErrorMsg, ObservationCount, WindowRequest}
import org.mbari.vcr4j.time.Timecode
import org.scalatra.{BadRequest, NoContent, NotFound}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.CollectionConverters._

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-07-11T16:58:00
  */
class ImagedMomentV1Api(controller: ImagedMomentController)(implicit val executor: ExecutionContext)
    extends V1APIStack {

  before() {
    contentType = "application/json"
  }

  get("/") {
    val limit  = params.getAs[Int]("limit").orElse(Some(100))
    val offset = params.getAs[Int]("offset").orElse(Some(0))
    controller
      .findAll(limit, offset)
      .map(_.asJava)
      .map(toJson)
  }

  get("/count/all") {
    controller
      .countAll()
      .map(toJson)
  }

  get("/find/images") {
    val limit  = params.getAs[Int]("limit").orElse(Some(100))
    val offset = params.getAs[Int]("offset").orElse(Some(0))
    controller
      .findWithImages(limit, offset)
      .map(_.asJava)
      .map(toJson)
  }

  get("/find/boundingboxes") {
    val limit  = params.getAs[Int]("limit").orElse(Some(100))
    val offset = params.getAs[Int]("offset").orElse(Some(0))
    controller
      .findWithBoundingBoxes(limit, offset)
      .map(_.asJava)
      .map(toJson)
  }

  get("/:uuid") {
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide a UUID")))))
    controller
      .findByUUID(uuid)
      .map({
        case None =>
          halt(
            NotFound(toJson(ErrorMsg(404, s"An ImagedMoment with a UUID of $uuid was not found")))
          )
        case Some(v) => toJson(v)
      })
  }

  get("/concept/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide a concept name")))))
    val limit  = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    controller
      .findByConcept(name, limit, offset)
      .map(_.asJava)
      .map(toJson)
  }

  get("/concept/images/:name") {
    // FIXME: This returns an imagedmoment for EACH image. If there are
    // two images for a moment, you'll get the image moment twice
    // This needs a distinct modifier
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide a concept name")))))
    val limit  = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    controller
      .findByConceptWithImages(name, limit, offset)
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
  //
  // }

  get("/videoreference/chunked/:uuid") {
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide a Video Reference UUID")))))
    val limit          = params.getAs[Int]("limit")
    val offset         = params.getAs[Int]("offset")
    val pageSize       = params.getAs[Int]("pagesize").getOrElse(50)
    val timeoutSeconds = params.getAs[Int]("timeout").getOrElse[Int](20)
    val timeout        = Duration.ofSeconds(timeoutSeconds)

    val start = offset.getOrElse(0)
    val end = limit match {
      case Some(i) => start + i
      case None    => Await.result(controller.countByVideoReferenceUuid(uuid), timeout)
    }

    def fn(limit: Int, offset: Int): Future[Iterable[ImagedMoment]] =
      controller.findByVideoReferenceUUID(uuid, Some(limit), Some(offset))

    autoPage(response, start, end, pageSize, fn, timeout)

  }

  get("/concept/count/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide a concept name")))))
    controller
      .countByConcept(name)
      .map(c => s"""{"concept": "$name", "count": $c}""")
  }

  get("/concept/images/count/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide a concept name")))))
    controller
      .countByConceptWithImages(name)
      .map(c => s"""{"concept": "$name", "count": $c}""")
  }

  get("/modified/:start/:end") {
    val start = params
      .getAs[Instant]("start")
      .getOrElse(
        halt(
          BadRequest(toJson(ErrorMsg(400, "Please provide a start date (yyyy-mm-ddThh:mm:ssZ)")))
        )
      )
    val end = params
      .getAs[Instant]("end")
      .getOrElse(
        halt(BadRequest(toJson(ErrorMsg(400, "Please provide an end date (yyyy-mm-ddThh:mm:ssZ)"))))
      )
    val limit  = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    controller
      .findBetweenUpdatedDates(start, end, limit, offset)
      .map(_.asJava)
      .map(toJson)
  }

  get("/modified/count/:start/:end") {
    val start = params
      .getAs[Instant]("start")
      .getOrElse(
        halt(
          BadRequest(toJson(ErrorMsg(400, "Please provide a start date (yyyy-mm-ddThh:mm:ssZ)")))
        )
      )
    val end = params
      .getAs[Instant]("end")
      .getOrElse(
        halt(BadRequest(toJson(ErrorMsg(400, "Please provide an end date (yyyy-mm-ddThh:mm:ssZ)"))))
      )
    controller
      .countBetweenUpdatedDates(start, end)
      .map(n => s"""{"start_timestamp":"$start", "end_timestamp":"$end", "count": "$n"}""")
  }

  get("/counts") {
    controller
      .countAllGroupByVideoReferenceUUID()
      .map(_.map({ case (uuid, count) => ObservationCount(uuid, count) }))
      .map(_.asJava)
      .map(toJson)
  }

  get("/videoreference") {
    val limit  = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    controller
      .findAllVideoReferenceUUIDs(limit, offset)
      .map(_.toArray)
      .map(toJson)
  }

  get("/videoreference/:uuid") {
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide a Video Reference UUID")))))
    val limit  = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    controller
      .findByVideoReferenceUUID(uuid, limit, offset)
      .map(_.asJava)
      .map(toJson)
  }

  get("/videoreference/modified/:uuid/:date") {
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide a Video Reference UUID")))))
    val date = params
      .getAs[Instant]("date")
      .getOrElse(
        halt(
          BadRequest(toJson(ErrorMsg(400, "Please provide a start date (yyyy-mm-ddThh:mm:ssZ)")))
        )
      )
    controller
      .countModifiedBeforeDate(uuid, date)
      .map(i => ObservationCount(uuid, i))
      .map(toJson)
  }

  post("/windowrequest") {
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val b             = request.body
        val limit         = params.getAs[Int]("limit")
        val offset        = params.getAs[Int]("offset")
        val windowRequest = fromJson(b, classOf[WindowRequest])
        controller
          .findByWindowRequest(windowRequest, limit, offset)
          .map(_.asJava)
          .map(toJson)
      case _ =>
        halt(
          BadRequest(
            toJson(
              ErrorMsg(
                400,
                "Posts to /windowrequest only accept a JSON body (i.e. Content-Type: application/json)"
              )
            )
          )
        )
    }
  }

  delete("/videoreference/:uuid") {
    validateRequest() // Apply API security
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide a Video Reference UUID")))))
    controller
      .deleteByVideoReferenceUUID(uuid)
      .map(ObservationCount(uuid, _))
      .map(toJson)
  }

  get("/imagereference/:uuid") {
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide an ImageReference UUID")))))
    controller
      .findByImageReferenceUUID(uuid)
      .map({
        case None =>
          halt(NotFound(toJson(ErrorMsg(404, s"No imagereference with a uuid of $uuid was found"))))
        case Some(im) => toJson(im)
      })
  }

  get("/observation/:uuid") {
    val uuid =
      params
        .getAs[UUID]("uuid")
        .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide an Observation UUID")))))
    controller
      .findByObservationUUID(uuid)
      .map({
        case None =>
          halt(NotFound(toJson(ErrorMsg(404, s"No observation with a uuid of $uuid was found"))))
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
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide a UUID")))))
    val timecode           = params.getAs[Timecode]("timecode")
    val elapsedTime        = params.getAs[Duration]("elapsed_time_millis")
    val recordedDate       = params.getAs[Instant]("recorded_timestamp")
    val videoReferenceUUID = params.getAs[UUID]("video_reference_uuid")
    controller
      .update(uuid, videoReferenceUUID, timecode, recordedDate, elapsedTime)
      .map(toJson)
  }

  put("/newtime/:uuid/:time") {
    validateRequest()
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(
        halt(BadRequest(toJson(ErrorMsg(400, "Please provide a video reference UUID parameter"))))
      )
    val time = params
      .getAs[Instant]("time")
      .getOrElse(
        halt(BadRequest(toJson(ErrorMsg(400, "Please provide a new start 'time' parameter"))))
      )
    controller
      .updateRecordedTimestamps(uuid, time)
      .map(_.asJava)
      .map(toJson)
  }

  /*
     This endpoint takes annotations as a json body. Annotations should have an
     observation_uuid and a recoreded_timestamp.
   */
  put("/tapetime") {
    validateRequest()
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val annotations = fromJson(request.body, classOf[Array[AnnotationImpl]])
        var n           = 0
        for { a <- annotations } {
          if (a.observationUuid != null && a.recordedTimestamp != null) {
            controller.updateRecordedTimestampByObservationUuid(
              a.observationUuid,
              a.recordedTimestamp
            )
            n = n + 1
          }
        }
        val count = Map("annotation_count" -> annotations.size, "timestamps_updated" -> n).asJava
        toJson(count)
      case _ =>
        halt(
          BadRequest(
            toJson(
              ErrorMsg(
                400,
                "Puts to tapetime only accept JSON body (i.e. Content-Type: application/json)"
              )
            )
          )
        )
    }
  }

  delete("/:uuid") {
    validateRequest() // Apply API security
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(
        halt(BadRequest(toJson(ErrorMsg(400, "Please provide the 'uuid' of the association"))))
      )
    controller
      .delete(uuid)
      .map({
        case true => halt(NoContent()) // Success
        case false =>
          halt(
            NotFound(
              toJson(ErrorMsg(404, s"Failed. No ImagedMoment with UUID of $uuid was found."))
            )
          )
      })
  }

}
