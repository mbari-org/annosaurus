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

import java.time.Duration
import java.util.UUID

import org.mbari.vars.annotation.controllers.CachedAncillaryDatumController
import org.mbari.vars.annotation.model.simple.{CachedAncillaryDatumBean, ErrorMsg}
import org.scalatra.{BadRequest, NotFound}

import scala.collection.immutable.ArraySeq
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

/**
  * @author Brian Schlining
  * @since 2017-05-01T13:24:00
  */
class CachedAncillaryDatumV1Api(controller: CachedAncillaryDatumController)(
    implicit val executor: ExecutionContext
) extends V1APIStack {

  before() {
    contentType = "application/json"
  }

  get("/:uuid") {
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide a ancillary data UUID")))))
    controller
      .findByUUID(uuid)
      .map({
        case None =>
          halt(
            NotFound(toJson(ErrorMsg(404, s"An AncillaryDatum with a UUID of $uuid was not found")))
          )
        case Some(v) => toJson(v)
      })
  }

  get("/videoreference/:uuid") {
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide a video reference UUID")))))
    controller
      .findByVideoReferenceUUID(uuid)
      .map(_.asJava)
      .map(toJson)
  }

  get("/imagedmoment/:uuid") {
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide an image reference UUID")))))
    controller
      .findByImagedMomentUUID(uuid)
      .map({
        case None =>
          halt(NotFound(toJson(ErrorMsg(404, s"No imagereference with a uuid of $uuid was found"))))
        case Some(im) => toJson(im)
      })
  }

  get("/observation/:uuid") {
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide an observation UUID")))))
    controller
      .findByObservationUUID(uuid)
      .map({
        case None =>
          halt(NotFound(toJson(ErrorMsg(404, s"No observation with a uuid of $uuid was found"))))
        case Some(im) => toJson(im)
      })
  }

  post("/") {
    validateRequest() // Apply API security
    val imagedMomentUuid = params
      .getAs[UUID]("imaged_moment_uuid")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "An imaged_moment_uuid is required")))))
    val latitude = params
      .getAs[Double]("latitude")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "A latitude is required")))))
    val longitude = params
      .getAs[Double]("longitude")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "A longitude is required")))))
    val depthMeters = params
      .getAs[Double]("depth_meters")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "A depth_meters is required")))))
    val altitude          = params.getAs[Double]("altitude_meters")
    val crs               = params.get("crs")
    val salinity          = params.getAs[Double]("salinity")
    val oxygenMlL         = params.getAs[Double]("oxygen")
    val temperature       = params.getAs[Double]("temperature_celsius")
    val pressureDbar      = params.getAs[Double]("pressure_dbar")
    val lightTransmission = params.getAs[Double]("light_transmission")
    val x                 = params.getAs[Double]("x")
    val y                 = params.getAs[Double]("y")
    val z                 = params.getAs[Double]("z")
    val posePositionUnits = params.get("pose_position_units")
    val phi               = params.getAs[Double]("phi")
    val theta             = params.getAs[Double]("theta")
    val psi               = params.getAs[Double]("psi")

    controller
      .create(
        imagedMomentUuid,
        latitude,
        longitude,
        depthMeters,
        altitude,
        crs,
        salinity,
        temperature,
        oxygenMlL,
        pressureDbar,
        lightTransmission,
        x,
        y,
        z,
        posePositionUnits,
        phi,
        theta,
        psi
      )
      .map(toJson)

  }

  post("/bulk") {
    validateRequest()
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val b    = request.body
        val data = fromJson(b, classOf[Array[CachedAncillaryDatumBean]])
        //log.info("Recieved >>> " + b)
        //log.info("Parse    <<< " + toJson(data))
        controller
          .bulkCreateOrUpdate(ArraySeq.unsafeWrapArray(data))
          .map(ds => toJson(ds.asJava))
      case _ =>
        halt(
          BadRequest(
            toJson(
              ErrorMsg(
                400,
                "Posts to /bulk only accept a JSON body (i.e. Content-Type: application/json)"
              )
            )
          )
        )
    }
  }

  put("/merge/:uuid") {
    validateRequest()
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(
        halt(BadRequest(toJson(ErrorMsg(400, "A video reference 'uuid' parameter is required"))))
      )
    val windowMillis = params.getAs[Long]("window").getOrElse(7500L)
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val data = fromJson(request.body, classOf[Array[CachedAncillaryDatumBean]])
        controller
          .merge(data, uuid, Duration.ofMillis(windowMillis))
          .map(ds => toJson(ds.asJava))
      case _ =>
        halt(
          BadRequest(
            toJson(
              ErrorMsg(
                400,
                "Posts to /merge only accept a JSON body (i.e. Content-Type: application/json)"
              )
            )
          )
        )
    }
  }

  put("/:uuid") {
    validateRequest() // Apply API security
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(
        halt(BadRequest(toJson(ErrorMsg(400, "A video reference 'uuid' parameter is required"))))
      )
    val latitude          = params.getAs[Double]("latitude")
    val longitude         = params.getAs[Double]("longitude")
    val depthMeters       = params.getAs[Double]("depth_meters")
    val altitudeMeters    = params.getAs[Double]("altitude_meters")
    val crs               = params.get("crs")
    val salinity          = params.getAs[Double]("salinity")
    val oxygenMlL         = params.getAs[Double]("oxygen")
    val temperature       = params.getAs[Double]("temperature_celsius")
    val pressureDbar      = params.getAs[Double]("pressure_dbar")
    val lightTransmission = params.getAs[Double]("light_transmission")
    val x                 = params.getAs[Double]("x")
    val y                 = params.getAs[Double]("y")
    val z                 = params.getAs[Double]("z")
    val posePositionUnits = params.get("pose_position_units")
    val phi               = params.getAs[Double]("phi")
    val theta             = params.getAs[Double]("theta")
    val psi               = params.getAs[Double]("psi")

    controller
      .update(
        uuid,
        latitude,
        longitude,
        depthMeters,
        altitudeMeters,
        crs,
        salinity,
        oxygenMlL,
        temperature,
        pressureDbar,
        lightTransmission,
        x,
        y,
        z,
        posePositionUnits,
        phi,
        theta,
        psi
      )
      .map({
        case None =>
          halt(
            NotFound(
              toJson(ErrorMsg(404, s"A CachedAncillaryDatum with uuid of $uuid was not found"))
            )
          )
        case Some(v) => toJson(v)
      })
  }

  delete("/videoreference/:uuid") {
    validateRequest()
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(
        halt(BadRequest(toJson(ErrorMsg(400, "A video reference 'uuid' parameter is required"))))
      )
    controller
      .deleteByVideoReferenceUuid(uuid)
      .map(n => s"""{"video_reference_uuid": "$uuid", "count": $n}""")
  }

}
