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

import java.net.URL
import java.util.UUID

import org.mbari.vars.annotation.controllers.ImageReferenceController
import org.scalatra.{ BadRequest, NoContent, NotFound }

import scala.concurrent.ExecutionContext

/**
 * Created by brian on 7/14/16.
 */
class ImageReferenceV1Api(controller: ImageReferenceController)(implicit val executor: ExecutionContext)
  extends APIStack {

  get("/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a UUID")))
    controller.findByUUID(uuid).map({
      case None => halt(NotFound(
        body = s"An ImagedMoment with a UUID of $uuid was not found"))
      case Some(v) => toJson(v)
    })
  }

  put("/:uuid") {
    validateRequest() // Apply API security
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "A 'uuid' parameter is required")))
    val url = params.getAs[URL]("url")
    val format = params.get("format")
    val width = params.getAs[Int]("width_pixels")
    val height = params.getAs[Int]("height_pixels")
    val description = params.get("description")
    val imagedMomentUUID = params.getAs[UUID]("imaged_moment_uuid")
    controller.update(uuid, url, description, height, width, format, imagedMomentUUID)
      .map({
        case None => halt(NotFound(s"An ImageReference with uuid of $uuid was not found"))
        case Some(ir) => toJson(ir)
      })
  }

  delete("/:uuid") {
    validateRequest() // Apply API security
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "A 'uuid' parameter is required")))
    controller.delete(uuid).map({
      case true => halt(NoContent()) // Success
      case false => halt(NotFound(s"Failed. No observation with UUID of $uuid was found."))
    })
  }
}
