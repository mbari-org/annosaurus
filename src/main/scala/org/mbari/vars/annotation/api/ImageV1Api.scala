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

import java.net.{ URL, URLDecoder }
import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.controllers.ImageController
import org.mbari.vcr4j.time.Timecode
import org.scalatra.{ BadRequest, NotFound }
import org.scalatra.swagger.Swagger

import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._

/**
 * Created by brian on 7/14/16.
 */
class ImageV1Api(controller: ImageController)(implicit val swagger: Swagger, val executor: ExecutionContext)
    extends APIStack {

  override protected def applicationDescription: String = "Image API (v1)"
  override protected val applicationName: Option[String] = Some("ImageAPI")

  before() {
    contentType = "application/json"
    response.headers += ("Access-Control-Allow-Origin" -> "*")
  }

  get("/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "{}",
      reason = "A 'uuid' parameter is required"
    )))
    controller.findByUUID(uuid).map({
      case None => halt(NotFound(body = "{}", reason = s"an Image with an image_reference_uuid of $uuid was not found"))
      case Some(v) => toJson(v)
    })
  }

  get("/videoreference/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "{}",
      reason = "A video reference 'uuid' parameter is required"
    )))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    controller.findByVideoReferenceUUID(uuid, limit, offset)
      .map(_.asJava)
      .map(toJson)
  }

  // URL should be encoded e.g. URLEncoder.encode(...)
  get("/url/:url") {
    // val url = params.get("url")
    //   .map(URLDecoder.decode(_, "UTF-8"))
    //   .map(new URL(_))
    //   .getOrElse(halt(BadRequest(
    //     body = "{}",
    //     reason = "A 'url' parameter is required"
    //   )))
    val url = params.getAs[URL]("url").getOrElse(halt(BadRequest("Please provide a URL")))
    controller.findByURL(url).map({
      case None => halt(NotFound(reason = s"an Image with a URL of $url was not found"))
      case Some(i) => toJson(i)
    })
  }

  post("/") {
    validateRequest() // Apply API security
    val videoReferenceUUID = params.getAs[UUID]("video_reference_uuid").getOrElse(halt(BadRequest(
      body = "{}",
      reason = "A 'video_reference_uuid' parameter is required"
    )))
    val url = params.getAs[URL]("url").getOrElse(halt(BadRequest(
      body = "{}",
      reason = "A 'url' parameter is required"
    )))
    val timecode = params.getAs[Timecode]("timecode")
    val elapsedTime = params.getAs[Duration]("elapsed_time_millis")
    val recordedDate = params.getAs[Instant]("recorded_timestamp")

    if (timecode.isEmpty && elapsedTime.isEmpty && recordedDate.isEmpty) {
      halt(BadRequest("An valid index of timecode, elapsed_time_millis, or recorded_timestamp is required"))
    }

    val format = params.get("format")
    val width = params.getAs[Int]("width_pixels")
    val height = params.getAs[Int]("height_pixels")
    val description = params.get("description")
    controller.create(videoReferenceUUID, url, timecode, elapsedTime, recordedDate,
      format, width, height, description)
      .map(toJson)
  }

  put("/:uuid") {
    validateRequest() // Apply API security
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "{}",
      reason = "A image reference 'uuid' parameter is required"
    )))
    val videoReferenceUUID = params.getAs[UUID]("video_reference_uuid")
    val url = params.getAs[URL]("url")
    val timecode = params.getAs[Timecode]("timecode")
    val elapsedTime = params.getAs[Duration]("elapsed_time_millis")
    val recordedDate = params.getAs[Instant]("recorded_timestamp")
    val format = params.get("format")
    val width = params.getAs[Int]("width_pixels")
    val height = params.getAs[Int]("height_pixels")
    val description = params.get("description")
    controller.update(uuid, url, videoReferenceUUID, timecode, elapsedTime, recordedDate,
      format, width, height, description)
      .map({
        case None => halt(NotFound(reason = s"an Image with an image_reference_uuid of $uuid was not found"))
        case Some(v) => toJson(v)
      })
  }

}
