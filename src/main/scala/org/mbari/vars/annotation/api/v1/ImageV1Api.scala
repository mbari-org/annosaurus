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

import java.net.URL
import java.time.{Duration, Instant}
import java.util.UUID

import org.mbari.vars.annotation.controllers.ImageController
import org.mbari.vars.annotation.model.simple.ErrorMsg
import org.mbari.vcr4j.time.Timecode
import org.scalatra.{BadRequest, NotFound}

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

/**
  * Created by brian on 7/14/16.
  */
class ImageV1Api(controller: ImageController)(implicit val executor: ExecutionContext)
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
        case None =>
          halt(
            NotFound(
              toJson(ErrorMsg(404, s"an Image with an image_reference_uuid of $uuid was not found"))
            )
          )
        case Some(v) => toJson(v)
      })
  }

  get("/videoreference/:uuid") {
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(
        halt(BadRequest(toJson(ErrorMsg(400, "A video reference 'uuid' parameter is required"))))
      )
    val limit  = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    controller
      .findByVideoReferenceUUID(uuid, limit, offset)
      .map(_.asJava)
      .map(toJson)
  }

  get("/name/:name") {
    val name = params
      .get("name")
      .getOrElse(
        halt(BadRequest(toJson(ErrorMsg(400, "An image name is required as part of the path"))))
      )
    controller
      .findByImageName(name)
      .map(_.asJava)
      .map(toJson)
  }

  // URL should be encoded e.g. URLEncoder.encode(...)
  get("/url/*") {
//     val url = params.get("url")
//       .map(URLDecoder.decode(_, "UTF-8"))
//       .map(new URL(_))
//       .getOrElse(halt(BadRequest(
//         ErrorMsg(400, "Please provide a URL")
//       )))
    val url = params
      .getAs[URL]("splat")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide a URL")))))
    controller
      .findByURL(url)
      .map({
        case None =>
          halt(NotFound(toJson(ErrorMsg(404, "an Image with a URL of $url was not found"))))
        case Some(i) => toJson(i)
      })
  }

  post("/") {
    validateRequest() // Apply API security
    val videoReferenceUUID = params
      .getAs[UUID]("video_reference_uuid")
      .getOrElse(
        halt(BadRequest(toJson(ErrorMsg(400, "A 'video_reference_uuid' parameter is required"))))
      )
    val url =
      params
        .getAs[URL]("url")
        .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "A 'url' parameter is required")))))
    val timecode     = params.getAs[Timecode]("timecode")
    val elapsedTime  = params.getAs[Duration]("elapsed_time_millis")
    val recordedDate = params.getAs[Instant]("recorded_timestamp")

    if (timecode.isEmpty && elapsedTime.isEmpty && recordedDate.isEmpty) {
      halt(
        BadRequest(
          toJson(
            ErrorMsg(
              400,
              "An valid index of timecode, elapsed_time_millis, or recorded_timestamp is required"
            )
          )
        )
      )
    }

    val format      = params.get("format")
    val width       = params.getAs[Int]("width_pixels")
    val height      = params.getAs[Int]("height_pixels")
    val description = params.get("description")
    controller
      .create(
        videoReferenceUUID,
        url,
        timecode,
        elapsedTime,
        recordedDate,
        format,
        width,
        height,
        description
      )
      .map(toJson)
  }

  put("/:uuid") {
    validateRequest() // Apply API security
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(
        halt(BadRequest(toJson(ErrorMsg(400, "A image reference 'uuid' parameter is required"))))
      )
    val videoReferenceUUID = params.getAs[UUID]("video_reference_uuid")
    val url                = params.getAs[URL]("url")
    val timecode           = params.getAs[Timecode]("timecode")
    val elapsedTime        = params.getAs[Duration]("elapsed_time_millis")
    val recordedDate       = params.getAs[Instant]("recorded_timestamp")
    val format             = params.get("format")
    val width              = params.getAs[Int]("width_pixels")
    val height             = params.getAs[Int]("height_pixels")
    val description        = params.get("description")
    controller
      .update(
        uuid,
        url,
        videoReferenceUUID,
        timecode,
        elapsedTime,
        recordedDate,
        format,
        width,
        height,
        description
      )
      .map({
        case None =>
          halt(
            NotFound(
              toJson(ErrorMsg(404, s"an Image with an image_reference_uuid of $uuid was not found"))
            )
          )
        case Some(v) => toJson(v)
      })
  }

}
