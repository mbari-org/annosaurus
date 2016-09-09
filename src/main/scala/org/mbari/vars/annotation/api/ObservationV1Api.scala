package org.mbari.vars.annotation.api

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.controllers.{ AnnotationController, ObservationController }
import org.scalatra.{ BadRequest, NoContent, NotFound }
import org.scalatra.swagger.Swagger

import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-07-04T21:56:00
 */
class ObservationV1Api(controller: ObservationController)(implicit val swagger: Swagger, val executor: ExecutionContext)
    extends APIStack {
  override protected def applicationDescription: String = "Observation API (v1)"

  override protected val applicationName: Option[String] = Some("ObservationAPI")

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

  get("/videoreference/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a Video Reference UUID")))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    controller.findByVideoReferenceUUID(uuid)
      .map(_.asJava)
      .map(toJson)
  }

  get("/association/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide an Association UUID")))
    controller.findByAssociationUUID(uuid)
      .map({
        case None => halt(NotFound(s"No observation for association with uuid of ${uuid} was found"))
        case Some(obs) => toJson(obs)
      })
  }

  get("/names") {
    controller.findAllNames
      .map(_.asJava)
      .map(toJson)
  }

  get("/names/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a video-reference 'uuid'")))
    controller.findAllNamesByVideoReferenceUUID(uuid)
      .map(_.asJava)
      .map(toJson)
  }

  put("/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "{}",
      reason = "A 'uuid' parameter is required"
    )))
    val concept = params.get("concept")
    val observer = params.get("observer")
    val observationDate = params.getAs[Instant]("observation_timestamp").getOrElse(Instant.now())
    val duration = params.getAs[Duration]("duration_millis")
    val group = params.get("group")
    val imagedMomentUUID = params.getAs[UUID]("imaged_moment_uuid")
    controller.update(uuid, concept, observer, observationDate, duration, group, imagedMomentUUID)
      .map(toJson)
  }

  delete("/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a UUID")))
    controller.delete(uuid).map({
      case true => halt(NoContent(reason = s"Success! Deleted observation with UUID of $uuid"))
      case false => halt(NotFound(reason = s"Failed. No observation with UUID of $uuid was found."))
    })
  }
}
