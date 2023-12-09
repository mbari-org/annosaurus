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

package org.mbari.annosaurus.api.v1

import java.util.UUID

import org.mbari.annosaurus.controllers.IndexController
import org.mbari.annosaurus.model.simple.ErrorMsg
import org.mbari.vars.annotation.repository.jpa.entity.IndexEntity
import org.scalatra.BadRequest

import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext

/**
  * @author Brian Schlining
  * @since 2019-02-08T11:00:00
  */
class IndexV1Api(controller: IndexController)(implicit val executor: ExecutionContext)
    extends V1APIStack {

  before() {
    contentType = "application/json"
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

  put("/tapetime") {
    validateRequest()
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val indices = fromJson(request.body, classOf[Array[IndexEntity]])
        controller
          .bulkUpdateRecordedTimestamps(indices)
          .map(_.asJava)
          .map(toJson)
      case _ =>
        val m = Map(
          "error" -> "Puts to tapetime only accept JSON body (i.e. Content-Type: application/json)"
        ).asJava
        halt(BadRequest(toJson(m)))
    }
  }

}
