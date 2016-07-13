package org.mbari.vars.annotation.api

import java.util.UUID

import org.mbari.vars.annotation.controllers.{ AssociationController, ImagedMomentController }
import org.mbari.vars.annotation.model.Association
import org.scalatra.{ BadRequest, NotFound }
import org.scalatra.swagger.Swagger

import scala.concurrent.ExecutionContext

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-07-13T11:21:00
 */
class AssociationV1Api(controller: AssociationController)(implicit val swagger: Swagger, val executor: ExecutionContext)
    extends APIStack {

  override protected def applicationDescription: String = "Association API (v1)"

  override protected val applicationName: Option[String] = Some("AssociationAPI")

  before() {
    contentType = "application/json"
    response.headers += ("Access-Control-Allow-Origin" -> "*")
  }

  // Find an association
  get("/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a 'uuid' parameter")))
    controller.findByUUID(uuid).map({
      case None => halt(NotFound(
        body = "{}",
        reason = s"An Association with a UUID of $uuid was not found"
      ))
      case Some(v) => toJson(v)
    })
  }

  // find associations
  get("/:video_reference_uuid/:link_name") {
    val videoReferenceUUID = params.getAs[UUID]("video_reference_uuid").getOrElse(halt(BadRequest("Please provide a video-reference 'uuid'")))
    val linkName = params.get("link_name").getOrElse(halt(BadRequest("A 'link_name' parameter is required")))
    controller.findByLinkNameAndVideoReferenceUUID(linkName, videoReferenceUUID).map(toJson)
  }

  post("/") {
    val uuid = params.getAs[UUID]("observation_uuid").getOrElse(halt(BadRequest("Please provide an 'observation_uuid'")))
    val linkName = params.get("link_name").getOrElse(halt(BadRequest("A 'link_name' parameter is required")))
    val toConcept = params.get("to_concept").getOrElse(Association.TO_CONCEPT_SELF)
    val linkValue = params.get("link_value").getOrElse(Association.LINK_VALUE_NIL)
    controller.create(uuid, linkName, toConcept, linkValue).map(toJson)
  }

  put("/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide the 'uuid' of the association")))
    val observationUUID = params.getAs[UUID]("observation_uuid")
    val linkName = params.get("link_name")
    val toConcept = params.get("to_concept")
    val linkValue = params.get("link_value")
    controller.update(uuid, observationUUID, linkName, toConcept, linkValue).map(toJson)
  }

  delete("/:uuid") {}

}
