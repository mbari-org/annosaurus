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

import org.mbari.vars.annotation.controllers.AnnotationController
import org.mbari.vars.annotation.dao.jpa.AnnotationImpl
import org.mbari.vars.annotation.model.Annotation
import org.mbari.vars.annotation.model.simple.{
  ConcurrentRequest,
  ConcurrentRequestCount,
  ErrorMsg,
  MultiRequest,
  MultiRequestCount
}
import org.mbari.vars.annotation.util.ResponseUtilities
import org.mbari.vcr4j.time.Timecode
import org.scalatra.{BadRequest, NotFound}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.CollectionConverters._

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-06-30T10:08:00
  */
class AnnotationV1Api(controller: AnnotationController)(implicit val executor: ExecutionContext)
    extends V1APIStack {

  before() {
    contentType = "application/json"
    response.headers.set("Access-Control-Allow-Origin", "*")
  }

  get("/:uuid") {
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "A 'uuid' parameter is required")))))
    controller
      .findByUUID(uuid)
      .map({
        case None      => halt(NotFound(body = s"An observation with uuid of $uuid was not found"))
        case Some(obs) => toJson(obs)
      })
  }

  get("/videoreference/:uuid") {
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(halt(BadRequest(body = "A video reference 'uuid' parameter is required")))
    val limit  = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    controller
      .findByVideoReferenceUUID(uuid, limit, offset)
      .map(_.asJava)
      .map(toJson)
  }

  post("/concurrent/count") {
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val b                 = request.body
        val concurrentRequest = fromJson(b, classOf[ConcurrentRequest])
        controller
          .countByConcurrentRequest(concurrentRequest)
          .map(c => ConcurrentRequestCount(concurrentRequest, c))
          .map(toJson)
      case _ =>
        halt(
          BadRequest(
            toJson(
              ErrorMsg(
                500,
                "Posts to /concurrent/count only accept a JSON body (i.e. Content-Type: application/json)"
              )
            )
          )
        )
    }
  }
  post("/concurrent") {
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val b                 = request.body
        val limit             = params.getAs[Int]("limit")
        val offset            = params.getAs[Int]("offset")
        val concurrentRequest = fromJson(b, classOf[ConcurrentRequest])
        val (closeable, stream) =
          controller.streamByConcurrentRequest(concurrentRequest, limit, offset)
        ResponseUtilities.sendStreamedResponse(response, stream, (a: Annotation) => toJson(a))
        closeable.close()
        ()
      case _ =>
        halt(
          BadRequest(
            toJson(
              ErrorMsg(
                400,
                "Posts to /concurrent only accept a JSON body (i.e. Content-Type: application/json)"
              )
            )
          )
        )
    }
  }

  post("/multi/count") {
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val b            = request.body
        val multiRequest = fromJson(b, classOf[MultiRequest])
        controller
          .countByMultiRequest(multiRequest)
          .map(c => MultiRequestCount(multiRequest, c))
          .map(toJson)
      case _ =>
        halt(
          BadRequest(
            toJson(
              ErrorMsg(
                500,
                "Posts to /multi/count only accept a JSON body (i.e. Content-Type: application/json)"
              )
            )
          )
        )
    }
  }

  post("/multi") {
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val b                   = request.body
        val limit               = params.getAs[Int]("limit")
        val offset              = params.getAs[Int]("offset")
        val multiRequest        = fromJson(b, classOf[MultiRequest])
        val (closeable, stream) = controller.streamByMultiRequest(multiRequest, limit, offset)
        ResponseUtilities.sendStreamedResponse(response, stream, (a: Annotation) => toJson(a))
        closeable.close()
        ()
      case _ =>
        halt(
          BadRequest(
            toJson(
              ErrorMsg(
                400,
                "Posts to /multi only accept a JSON body (i.e. Content-Type: application/json)"
              )
            )
          )
        )
    }
  }

  get("/videoreference/chunked/:uuid") {
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(halt(BadRequest(body = "A video reference 'uuid' parameter is required")))
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

    val msg =
      s""" Running chunk lookup for $uuid
         |  limit     = ${limit.map(_.toString).getOrElse("undefined")}
         |  offset    = ${offset.map(_.toString).getOrElse("undefined")}
         |  pagesize  = $pageSize
         |  timeout   = $timeoutSeconds seconds
         |  start idx = $start
         |  end idx   = $end
       """.stripMargin
    log.debug(msg)

    def fn(limit: Int, offset: Int): Future[Iterable[Annotation]] =
      controller.findByVideoReferenceUUID(uuid, Some(limit), Some(offset))

    autoPage(response, start, end, pageSize, fn, timeout)
  }

  get("/imagereference/:uuid") {
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "A image reference 'uuid' parameter is required")))))
    controller
      .findByImageReferenceUUID(uuid)
      .map(_.asJava)
      .map(toJson)

  }

  post("/") {
    validateRequest() // Apply API security
    val videoReferenceUUID = params
      .getAs[UUID]("video_reference_uuid")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "A 'video_reference_uuid' parameter is required")))))
    val concept =
      params.get("concept").getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "A 'concept' parameter is required")))))
    val observer =
      params.get("observer").getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "An 'observer' parameter is required")))))
    val observationDate = params.getAs[Instant]("observation_timestamp").getOrElse(Instant.now())
    val timecode        = params.getAs[Timecode]("timecode")
    val elapsedTime     = params.getAs[Duration]("elapsed_time_millis")
    val recordedDate    = params.getAs[Instant]("recorded_timestamp")
    val duration        = params.getAs[Duration]("duration_millis")
    val group           = params.get("group")
    val activity        = params.get("activity")

    if (timecode.isEmpty && elapsedTime.isEmpty && recordedDate.isEmpty) {
      halt(
        BadRequest(toJson(ErrorMsg(400,
          "One or more of the following indices into the video are required: timecode, elapsed_time_millis, recorded_date"
        )))
      )
    }

    controller
      .create(
        videoReferenceUUID,
        concept,
        observer,
        observationDate,
        timecode,
        elapsedTime,
        recordedDate,
        duration,
        group,
        activity
      )
      .map(toJson) // Convert to JSON

  }

  post("/bulk") {
    validateRequest()
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val annotations = fromJson(request.body, classOf[Array[AnnotationImpl]])
        controller
          .bulkCreate(annotations)
          .map(annos => toJson(annos.asJava))
      case _ =>
        halt(
          BadRequest(toJson(ErrorMsg(400, "Posts to /bulk only accept JSON body (i.e. Content-Type: application/json)")))
        )
    }
  }

  put("/:uuid") {
    validateRequest() // Apply API security
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "An observation 'uuid' parameter is required")))))
    val videoReferenceUUID = params.getAs[UUID]("video_reference_uuid")
    val concept            = params.get("concept")
    val observer           = params.get("observer")
    val observationDate    = params.getAs[Instant]("observation_timestamp").getOrElse(Instant.now())
    val timecode           = params.getAs[Timecode]("timecode")
    val elapsedTime        = params.getAs[Duration]("elapsed_time_millis")
    val recordedDate       = params.getAs[Instant]("recorded_timestamp")
    val duration           = params.getAs[Duration]("duration_millis")
    val group              = params.get("group")
    val activity           = params.get("activity")

    controller
      .update(
        uuid,
        videoReferenceUUID,
        concept,
        observer,
        observationDate,
        timecode,
        elapsedTime,
        recordedDate,
        duration,
        group,
        activity
      )
      .map({
        case None =>
          halt(NotFound(toJson(ErrorMsg(404, s"An annotation with observation_uuid of $uuid was not found"))))
        case Some(ann) => toJson(ann)
      }) // Convert to JSON

  }

  put("/bulk") {
    validateRequest()
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val annotations = fromJson(request.body, classOf[Array[AnnotationImpl]])
        controller
          .bulkUpdate(annotations)
          .map(annos => toJson(annos.asJava))
      case _ =>
        halt(
          BadRequest(toJson(ErrorMsg(400, "Puts to /bulk only accept JSON body (i.e. Content-Type: application/json)")))
        )
    }
  }

  // TODO when there are over about 164 annotations to return this method times-out
  // Maybe just return an ack.
  put("/tapetime") {
    validateRequest()
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val annotations = fromJson(request.body, classOf[Array[AnnotationImpl]])
        controller
          .bulkUpdateRecordedTimestampOnly(annotations)
          .map(annos => toJson(annos.asJava))
      case _ =>
        halt(
          BadRequest(toJson(ErrorMsg(400, "Puts to tapetime only accept JSON body (i.e. Content-Type: application/json)")))
        )
    }
  }

}
