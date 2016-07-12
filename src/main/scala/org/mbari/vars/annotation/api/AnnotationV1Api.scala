package org.mbari.vars.annotation.api

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.controllers.AnnotationController
import org.mbari.vcr4j.time.Timecode
import org.scalatra.BadRequest
import org.scalatra.swagger.Swagger
import org.mbari.vars.annotation.model.simple.Implicits._

import scala.concurrent.ExecutionContext

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-30T10:08:00
 */
class AnnotationV1Api(controller: AnnotationController)(implicit val swagger: Swagger, val executor: ExecutionContext)
    extends APIStack {

  override protected def applicationDescription: String = "Annotation API (v1)"

  override protected val applicationName: Option[String] = Some("AnnotationAPI")

  before() {
    contentType = "application/json"
    response.headers += ("Access-Control-Allow-Origin" -> "*")
  }

  get("/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "{}",
      reason = "A Video Reference UUID parameter is required")))
    
  }

  post("/") {
    val videoReferenceUUID = params.getAs[UUID]("video_reference_uuid").getOrElse(halt(BadRequest(
      body = "{}",
      reason = "A 'video_reference_uuid' parameter is required"
    )))
    val concept = params.get("concept").getOrElse(halt(BadRequest(
      "A 'concept' parameter is required"
    )))
    val observer = params.get("observer").getOrElse(halt(BadRequest(
      "An 'observer' parameter is required"
    )))
    val observationDate = params.getAs[Instant]("observation_timestamp").getOrElse(Instant.now())
    val timecode = params.getAs[Timecode]("timecode")
    val elapsedTime = params.getAs[Duration]("elapsed_time_millis")
    val recordedDate = params.getAs[Instant]("recorded_timestamp")

    if (timecode.isEmpty && elapsedTime.isEmpty && recordedDate.isEmpty) {
      halt(BadRequest("One or more of the following indicies into the video are required: timecode, elapsed_time_millis, recorded_date"))
    }

    controller.create(videoReferenceUUID, concept, observer, observationDate, timecode,
      elapsedTime, recordedDate).map(toJson)

  }

  put("/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "{}",
      reason = "A 'uuid' parameter is required")))
    val videoReferenceUUID = params.getAs[UUID]("video_reference_uuid")
    val concept = params.get("concept")
    val observer = params.get("observer")
    val observationDate = params.getAs[Instant]("observation_timestamp").getOrElse(Instant.now())
    val timecode = params.getAs[Timecode]("timecode")
    val elapsedTime = params.getAs[Duration]("elapsed_time_millis")
    val recordedDate = params.getAs[Instant]("recorded_timestamp")
    val duration = params.getAs[Duration]("duration_millis")

    controller.update(uuid, videoReferenceUUID, concept, observer, observationDate,
      timecode, elapsedTime, recordedDate, duration).map(toJson)

  }

}
