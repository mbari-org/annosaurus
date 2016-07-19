package org.mbari.vars.annotation.api

import java.net.URL
import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.controllers.ImageController
import org.mbari.vcr4j.time.Timecode
import org.scalatra.BadRequest
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
    controller.findByUUID(uuid).map(toJson)
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

  post("/") {
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
    val format = params.get("format")
    val width = params.getAs[Int]("width_pixels")
    val height = params.getAs[Int]("height_pixels")
    val description = params.get("description")
    controller.create(videoReferenceUUID, url, timecode, elapsedTime, recordedDate,
      format, width, height, description)
      .map(toJson)
  }

  put("/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "{}",
      reason = "A video reference 'uuid' parameter is required"
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
    controller.update(uuid, videoReferenceUUID, timecode, elapsedTime, recordedDate,
      format, width, height, description).map(toJson)
  }

}
