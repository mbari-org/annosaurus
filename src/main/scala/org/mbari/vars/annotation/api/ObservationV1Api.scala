package org.mbari.vars.annotation.api

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.controllers.ObservationController
import org.mbari.vars.annotation.model.ValueArray
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
    controller.findByVideoReferenceUUID(uuid, limit, offset)
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

  get("/concepts") {
    controller.findAllConcepts
      .map(_.toArray)
      .map(toJson)
  }

  get("/concepts/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a video-reference 'uuid'")))
    controller.findAllConceptsByVideoReferenceUUID(uuid)
      .map(_.toArray)
      .map(toJson)
  }

  get("/concept/count/:concept") {
    val concept = params.get("concept").getOrElse(halt(BadRequest("Please provide a concept to search for")))
    controller.countByConcept(concept)
      .map(n => s"""{"concept":"$concept", "count":"$n"}""")
  }

  get("/videoreference/count/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a video-reference 'uuid'")))
    controller.countByVideoReferenceUUID(uuid)
      .map(n => s"""{"video_reference_uuid": "count":"$n"}""")
  }

  put("/concept/rename") {
    val oldConcept = params.get("old").getOrElse(halt(BadRequest("Please provide the concept being replaced")))
    val newConcept = params.get("new").getOrElse(halt(BadRequest("Please provide the replacement concept")))
    controller.updateConcept(oldConcept, newConcept)
      .map(n => s"""{"old_concept":"$oldConcept", "new_concept":"$newConcept", "number_updated":"$n"}""")
  }

  put("/:uuid") {
    validateRequest() // Apply API security
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "{}",
      reason = "A 'uuid' parameter is required"
    )))
    val concept = params.get("concept")
    val observer = params.get("observer")
    val observationDate = params.getAs[Instant]("observation_timestamp").getOrElse(Instant.now())
    val duration = params.getAs[Duration]("duration_millis")
    val group = params.get("group")
    val activity = params.get("activity")
    val imagedMomentUUID = params.getAs[UUID]("imaged_moment_uuid")
    controller.update(uuid, concept, observer, observationDate, duration, group, activity, imagedMomentUUID)
      .map({
        case None => halt(NotFound(reason = s"Failed. No observation with UUID of $uuid was found."))
        case Some(obs) => toJson(obs)
      })
  }

  delete("/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a UUID")))
    controller.delete(uuid).map({
      case true => halt(NoContent(reason = s"Success! Deleted observation with UUID of $uuid"))
      case false => halt(NotFound(reason = s"Failed. No observation with UUID of $uuid was found."))
    })
  }
}
