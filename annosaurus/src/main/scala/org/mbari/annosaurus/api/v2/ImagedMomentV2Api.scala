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

package org.mbari.annosaurus.api.v2

import org.mbari.annosaurus.model.MutableImagedMoment
import java.time.Instant
import java.util.UUID

import org.mbari.vars.annotation.api.APIStack
import org.mbari.annosaurus.controllers.ImagedMomentController
import org.mbari.annosaurus.model.simple.ErrorMsg
import org.mbari.annosaurus.util.ResponseUtilities
import org.scalatra.BadRequest

import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext

/** @author
  *   Brian Schlining
  * @since 2019-05-08T10:02:00
  */
class ImagedMomentV2Api(controller: ImagedMomentController)(implicit val executor: ExecutionContext)
    extends APIStack {

    before() {
        contentType = "application/json"
        response.headers.set("Access-Control-Allow-Origin", "*")
    }

    get("/videoreference/:uuid") {
        val uuid   = params
            .getAs[UUID]("uuid")
            .getOrElse(
                halt(BadRequest(toJson(ErrorMsg(400, "Please provide a Video Reference UUID"))))
            )
        val limit  = params.getAs[Int]("limit").orElse(Some(defaultLimit))
        val offset = params.getAs[Int]("offset")

        val (closeable, stream) = controller.streamByVideoReferenceUUID(uuid, limit, offset)
        ResponseUtilities.sendStreamedResponse(
            response,
            stream,
            (im: MutableImagedMoment) => toJson(im)
        )
        closeable.close()
        ()
    }

    get("/videoreferences/modified/:start") {
        val start               = params
            .getAs[Instant]("start")
            .getOrElse(
                halt(
                    BadRequest(
                        toJson(ErrorMsg(400, "Please provide a start date (yyyy-mm-ddThh:mm:ssZ)"))
                    )
                )
            )
        val end                 = Instant.now()
        val limit               = params.getAs[Int]("limit")
        val offset              = params.getAs[Int]("offset")
        val (closeable, stream) =
            controller.streamVideoReferenceUuidsBetweenUpdatedDates(start, end, limit, offset)
        val json                = toJson(
            stream
                .iterator()
                .asScala
                .toSeq
                .map(_.toString)
                .asJava
        )
        closeable.close()
        json
    }

    get("/videoreferences/modified/:start/:end") {
        val start               = params
            .getAs[Instant]("start")
            .getOrElse(
                halt(
                    BadRequest(
                        toJson(ErrorMsg(400, "Please provide a start date (yyyy-mm-ddThh:mm:ssZ)"))
                    )
                )
            )
        val end                 = params
            .getAs[Instant]("end")
            .getOrElse(
                halt(
                    BadRequest(
                        toJson(ErrorMsg(400, "Please provide an end date (yyyy-mm-ddThh:mm:ssZ)"))
                    )
                )
            )
        val limit               = params.getAs[Int]("limit")
        val offset              = params.getAs[Int]("offset")
        val (closeable, stream) =
            controller.streamVideoReferenceUuidsBetweenUpdatedDates(start, end, limit, offset)
        val json                = toJson(
            stream
                .iterator()
                .asScala
                .toSeq
                .map(_.toString)
                .asJava
        )
        closeable.close()
        json
    }

    get("/modified/:start") {
        val start  = params
            .getAs[Instant]("start")
            .getOrElse(
                halt(
                    BadRequest(
                        toJson(ErrorMsg(400, "Please provide a start date (yyyy-mm-ddThh:mm:ssZ)"))
                    )
                )
            )
        val end    = Instant.now()
        val limit  = params.getAs[Int]("limit")
        val offset = params.getAs[Int]("offset")

        val (closeable, stream) = controller.streamBetweenUpdatedDates(start, end, limit, offset)
        ResponseUtilities.sendStreamedResponse(
            response,
            stream,
            (im: MutableImagedMoment) => toJson(im)
        )
        closeable.close()
        ()
    }

    get("/modified/:start/:end") {
        val start  = params
            .getAs[Instant]("start")
            .getOrElse(
                halt(
                    BadRequest(
                        toJson(ErrorMsg(400, "Please provide a start date (yyyy-mm-ddThh:mm:ssZ)"))
                    )
                )
            )
        val end    = params
            .getAs[Instant]("end")
            .getOrElse(
                halt(
                    BadRequest(
                        toJson(ErrorMsg(400, "Please provide an end date (yyyy-mm-ddThh:mm:ssZ)"))
                    )
                )
            )
        val limit  = params.getAs[Int]("limit").orElse(Some(defaultLimit))
        val offset = params.getAs[Int]("offset")

        val (closeable, stream) = controller.streamBetweenUpdatedDates(start, end, limit, offset)
        ResponseUtilities.sendStreamedResponse(
            response,
            stream,
            (im: MutableImagedMoment) => toJson(im)
        )
        closeable.close()
        ()
    }

    get("/concept/:name") {
        val name                = params
            .get("name")
            .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide a concept name")))))
        val limit               = params.getAs[Int]("limit")
        val offset              = params.getAs[Int]("offset")
        val (closeable, stream) = controller.streamByConcept(name, limit, offset)
        ResponseUtilities.sendStreamedResponse(
            response,
            stream,
            (im: MutableImagedMoment) => toJson(im)
        )
        closeable.close()
        ()
    }

}
