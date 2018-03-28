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

import java.util.UUID

import org.mbari.vars.annotation.controllers.CachedAncillaryDatumController
import org.mbari.vars.annotation.model.simple.CachedAncillaryDatumBean
import org.scalatra.{ BadRequest, NotFound }

import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._

/**
 * @author Brian Schlining
 * @since 2017-05-01T13:24:00
 */
class CachedAncillaryDatumV1Api(controller: CachedAncillaryDatumController)(implicit val executor: ExecutionContext)
    extends APIStack {

  before() {
    contentType = "application/json"
    response.headers += ("Access-Control-Allow-Origin" -> "*")
  }

  get("/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a UUID")))
    controller.findByUUID(uuid).map({
      case None => halt(NotFound(
        body = s"An ImagedMoment with a UUID of $uuid was not found"
      ))
      case Some(v) => toJson(v)
    })
  }

  get("/imagedmoment/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide an ImageReference UUID")))
    controller.findByImagedMomentUUID(uuid)
      .map({
        case None => halt(NotFound(body = s"No imagereference with a uuid of $uuid was found"))
        case Some(im) => toJson(im)
      })
  }

  get("/observation/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide an Observation UUID")))
    controller.findByObservationUUID(uuid)
      .map({
        case None => halt(NotFound(body = s"No observation with a uuid of $uuid was found"))
        case Some(im) => toJson(im)
      })
  }

  post("/") {
    validateRequest() // Apply API security
    val imagedMomentUuid = params.getAs[UUID]("imaged_moment_uuid")
      .getOrElse(halt(BadRequest("An imaged_moment_uuid is required")))
    val latitude = params.getAs[Double]("latitude")
      .getOrElse(halt(BadRequest("A latitude is required")))
    val longitude = params.getAs[Double]("longitude")
      .getOrElse(halt(BadRequest("A longitude is required")))
    val depthMeters = params.getAs[Float]("depth_meters")
      .getOrElse(halt(BadRequest("A depth_meters is required")))
    val altitude = params.getAs[Float]("altitude_meters")
    val crs = params.get("crs")
    val salinity = params.getAs[Float]("salinity")
    val oxygenMlL = params.getAs[Float]("oxygen")
    val temperature = params.getAs[Float]("temperature_celsius")
    val pressureDbar = params.getAs[Float]("pressure_dbar")
    val lightTransmission = params.getAs[Float]("light_transmission")
    val x = params.getAs[Double]("x")
    val y = params.getAs[Double]("y")
    val z = params.getAs[Double]("z")
    val posePositionUnits = params.get("pose_position_units")
    val phi = params.getAs[Double]("phi")
    val theta = params.getAs[Double]("theta")
    val psi = params.getAs[Double]("psi")

    controller.create(imagedMomentUuid, latitude, longitude, depthMeters, altitude, crs, salinity,
      oxygenMlL, temperature, pressureDbar, lightTransmission, x, y, z, posePositionUnits, phi, theta, psi)
      .map(toJson)

  }

  post("/bulk") {
    validateRequest()
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val data = fromJson(request.body, classOf[Array[CachedAncillaryDatumBean]])
        controller.bulkCreateOrUpdate(data)
          .map(ds => toJson(ds.asJava))
      case _ =>
        halt(BadRequest("Posts to /bulk only accept a JSON body (i.e. Content-Type: application/json)"))
    }
  }

  put("/:uuid") {
    validateRequest() // Apply API security
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "A video reference 'uuid' parameter is required"
    )))
    val latitude = params.getAs[Double]("latitude")
    val longitude = params.getAs[Double]("longitude")
    val depthMeters = params.getAs[Float]("depth_meters")
    val altitudeMeters = params.getAs[Float]("altitude_meters")
    val crs = params.get("crs")
    val salinity = params.getAs[Float]("salinity")
    val oxygenMlL = params.getAs[Float]("oxygen")
    val temperature = params.getAs[Float]("temperature_celsius")
    val pressureDbar = params.getAs[Float]("pressure_dbar")
    val lightTransmission = params.getAs[Float]("light_transmission")
    val x = params.getAs[Double]("x")
    val y = params.getAs[Double]("y")
    val z = params.getAs[Double]("z")
    val posePositionUnits = params.get("pose_position_units")
    val phi = params.getAs[Double]("phi")
    val theta = params.getAs[Double]("theta")
    val psi = params.getAs[Double]("psi")

    controller.update(uuid, latitude, longitude, depthMeters, altitudeMeters, crs, salinity,
      oxygenMlL, temperature, pressureDbar, lightTransmission, x, y, z, posePositionUnits, phi, theta, psi)
      .map({
        case None => halt(NotFound(body = s"A CachedAncillaryDatm with uuid of $uuid was not found"))
        case Some(v) => toJson(v)
      })
  }

}
