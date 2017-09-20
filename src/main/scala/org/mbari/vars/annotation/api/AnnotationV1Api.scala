package org.mbari.vars.annotation.api

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.controllers.AnnotationController
import org.mbari.vars.annotation.dao.jpa.AnnotationImpl
import org.mbari.vcr4j.time.Timecode
import org.scalatra.{ BadRequest, NotFound }
import org.scalatra.swagger.Swagger

import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-30T10:08:00
 */
class AnnotationV1Api(controller: AnnotationController)(implicit val swagger: Swagger, val executor: ExecutionContext)
    extends APIStack {

  override protected def applicationDescription: String = "Annotation API (v1)"
  override protected val applicationName: Option[String] = Some.apply("AnnotationAPI")

  before() {
    contentType = "application/json"
    response.headers += ("Access-Control-Allow-Origin" -> "*")
  }

  get("/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "{}",
      reason = "A 'uuid' parameter is required"
    )))
    controller.findByUUID(uuid)
      .map({
        case None => halt(NotFound(body = "{}", reason = s"An observation with uuid of $uuid was not found"))
        case Some(obs) => toJson(obs)
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

  get("/imagereference/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "{}",
      reason = "A image reference 'uuid' parameter is required"
    )))
    controller.findByImageReferenceUUID(uuid)
      .map(_.asJava)
      .map(toJson)

  }

  post("/") {
    validateRequest() // Apply API security
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
    val duration = params.getAs[Duration]("duration_millis")
    val group = params.get("group")
    val activity = params.get("activity")

    if (timecode.isEmpty && elapsedTime.isEmpty && recordedDate.isEmpty) {
      halt(BadRequest.apply("One or more of the following indices into the video are required: timecode, elapsed_time_millis, recorded_date"))
    }

    controller.create(videoReferenceUUID, concept, observer, observationDate, timecode,
      elapsedTime, recordedDate, duration, group, activity)
      .map(toJson) // Convert to JSON

  }

  post("/bulk") {
    validateRequest()
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val annotations = fromJson(request.body, classOf[Array[AnnotationImpl]])
        controller.bulkCreate(annotations)
          .map(annos => toJson(annos.asJava))
      case _ =>
        halt(BadRequest("Posts to /bulk only accept JSON body (i.e. Content-Type: application/json)"))
    }
  }

  put("/:uuid") {
    validateRequest() // Apply API security
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "{}",
      reason = "An observation 'uuid' parameter is required"
    )))
    val videoReferenceUUID = params.getAs[UUID]("video_reference_uuid")
    val concept = params.get("concept")
    val observer = params.get("observer")
    val observationDate = params.getAs[Instant]("observation_timestamp").getOrElse(Instant.now())
    val timecode = params.getAs[Timecode]("timecode")
    val elapsedTime = params.getAs[Duration]("elapsed_time_millis")
    val recordedDate = params.getAs[Instant]("recorded_timestamp")
    val duration = params.getAs[Duration]("duration_millis")
    val group = params.get("group")
    val activity = params.get("activity")

    controller.update(uuid, videoReferenceUUID, concept, observer, observationDate,
      timecode, elapsedTime, recordedDate, duration, group, activity)
      .map({
        case None =>
          halt(NotFound(body = "{}", reason = s"An annotation with observation_uuid of $uuid was not found"))
        case Some(ann) => toJson(ann)
      }) // Convert to JSON

  }

  put("/bulk") {
    validateRequest()
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val annotations = fromJson(request.body, classOf[Array[AnnotationImpl]])
        controller.bulkUpdate(annotations)
          .map(annos => toJson(annos.asJava))
      case _ =>
        halt(BadRequest("Puts to /bulk only accept JSON body (i.e. Content-Type: application/json)"))
    }
  }

}
