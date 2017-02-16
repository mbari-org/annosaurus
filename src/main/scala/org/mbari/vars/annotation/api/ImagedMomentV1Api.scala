package org.mbari.vars.annotation.api

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.controllers.{ AnnotationController, ImagedMomentController }
import org.mbari.vars.annotation.model.{ UUIDArray, ValueArray }
import org.mbari.vcr4j.time.Timecode
import org.scalatra.{ BadRequest, NoContent, NotFound }
import org.scalatra.swagger.Swagger

import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._
/**
 *
 *
 * @author Brian Schlining
 * @since 2016-07-11T16:58:00
 */
class ImagedMomentV1Api(controller: ImagedMomentController)(implicit val swagger: Swagger, val executor: ExecutionContext)
    extends APIStack {

  override protected def applicationDescription: String = "ImagedMoment API (v1)"

  override protected val applicationName: Option[String] = Some("ImagedMomentAPI")

  before() {
    contentType = "application/json"
    response.headers += ("Access-Control-Allow-Origin" -> "*")
  }

  get("/") {
    val limit = params.getAs[Int]("limit").getOrElse(1000)
    val offset = params.getAs[Int]("offset").getOrElse(0)
    controller.findAll(limit, offset)
      .map(_.asJava)
      .map(toJson)
  }

  get("/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a UUID")))
    controller.findByUUID(uuid).map({
      case None => halt(NotFound(
        body = "{}",
        reason = s"An ImagedMoment with a UUID of $uuid was not found"
      ))
      case Some(v) => toJson(v)
    })
  }

  get("/videoreference") {
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    controller.findAllVideoReferenceUUIDs(limit, offset)
      .map(_.toArray)
      .map(a => ValueArray(a))
      .map(toJson)
  }

  get("/videoreference/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a Video Reference UUID")))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    controller.findByVideoReferenceUUID(uuid, limit, offset)
      .map(_.asJava)
      .map(toJson)
  }

  get("/imagereference/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide an ImageReference UUID")))
    controller.findByImageReferenceUUID(uuid)
      .map({
        case None => halt(NotFound(reason = s"No imagereference with a uuid of $uuid was found"))
        case Some(im) => toJson(im)
      })
  }

  get("/observation/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide an Observation UUID")))
    controller.findByObservationUUID(uuid)
      .map({
        case None => halt(NotFound(reason = s"No observation with a uuid of $uuid was found"))
        case Some(im) => toJson(im)
      })
  }

  //  post("/") {
  //    val videoReferenceUUID = params.getAs[UUID]("video_reference_uuid")
  //        .getOrElse(halt(BadRequest("A video_reference_uuid is required")))
  //    val timecode = params.getAs[Timecode]("timecode")
  //    val elapsedTime = params.getAs[Duration]("elapsed_time_millis")
  //    val recordedDate = params.getAs[Instant]("recorded_timestamp")
  //    if (timecode.isEmpty && elapsedTime.isEmpty && recordedDate.isEmpty) {
  //      halt(BadRequest("At least one of: timecode, elapsed_time_millis, or recorded_timestamp is required"))
  //    }
  //
  //  }

  put("/:uuid") {
    validateRequest() // Apply API security
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a UUID")))
    val timecode = params.getAs[Timecode]("timecode")
    val elapsedTime = params.getAs[Duration]("elapsed_time_millis")
    val recordedDate = params.getAs[Instant]("recorded_timestamp")
    val videoReferenceUUID = params.getAs[UUID]("video_reference_uuid")
    controller.update(uuid, videoReferenceUUID, timecode, recordedDate, elapsedTime).map(toJson)
  }

  delete("/:uuid") {
    validateRequest() // Apply API security
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide the 'uuid' of the association")))
    controller.delete(uuid).map({
      case true => halt(NoContent(reason = s"Success! Deleted ImagedMoment with UUID of $uuid"))
      case false => halt(NotFound(reason = s"Failed. No ImagedMoment with UUID of $uuid was found."))
    })
  }

}
