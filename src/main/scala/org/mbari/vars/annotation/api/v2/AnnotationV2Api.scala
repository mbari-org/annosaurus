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

package org.mbari.vars.annotation.api.v2

import java.time.Instant
import java.util.UUID

import org.mbari.vars.annotation.api.APIStack
import org.mbari.vars.annotation.controllers.AnnotationController
import org.mbari.vars.annotation.model.Annotation
import org.mbari.vars.annotation.model.simple.ErrorMsg
import org.mbari.vars.annotation.util.ResponseUtilities
import org.scalatra.BadRequest

import scala.concurrent.ExecutionContext

/**
  * @author Brian Schlining
  * @since 2019-05-08T13:50:00
  */
class AnnotationV2Api(controller: AnnotationController)(implicit val executor: ExecutionContext)
    extends APIStack {

  before() {
    contentType = "application/json"
    response.headers.set("Access-Control-Allow-Origin", "*")
  }

  get("/videoreference/:uuid") {
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "A video reference 'uuid' parameter is required")))))

    // Optional params to filter between dates
    val startTimestamp = params.getAs[Instant]("start")
    val endTimestamp   = params.getAs[Instant]("end")

    val limit  = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")

    val (closeable, stream) = if (startTimestamp.isDefined || endTimestamp.isDefined) {
      val start = startTimestamp.getOrElse(Instant.EPOCH)
      val end   = endTimestamp.getOrElse(Instant.now())
      controller.streamByVideoReferenceUUIDAndTimestamps(uuid, start, end, limit, offset)
    }
    else {
      controller.streamByVideoReferenceUUID(uuid, limit, offset)
    }

    ResponseUtilities.sendStreamedResponse(response, stream, (a: Annotation) => toJson(a))
    closeable.close()
    ()
  }

}
