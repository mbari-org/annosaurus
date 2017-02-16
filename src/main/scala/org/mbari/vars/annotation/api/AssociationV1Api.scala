package org.mbari.vars.annotation.api

import java.util.UUID

import org.mbari.vars.annotation.controllers.{ AssociationController, ImagedMomentController }
import org.mbari.vars.annotation.model.Association
import org.scalatra.{ BadRequest, NoContent, NotFound }
import org.scalatra.swagger.Swagger

import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._

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
    controller.findByLinkNameAndVideoReferenceUUID(linkName, videoReferenceUUID)
      .map(as => toJson(as.asJava))
  }

  post("/") {
    validateRequest() // Apply API security
    val uuid = params.getAs[UUID]("observation_uuid").getOrElse(halt(BadRequest("Please provide an 'observation_uuid'")))
    val linkName = params.get("link_name").getOrElse(halt(BadRequest("A 'link_name' parameter is required")))
    val toConcept = params.get("to_concept").getOrElse(Association.TO_CONCEPT_SELF)
    val linkValue = params.get("link_value").getOrElse(Association.LINK_VALUE_NIL)
    val mimeType = params.get("mime_type").getOrElse("text/plain")
    controller.create(uuid, linkName, toConcept, linkValue, mimeType).map(toJson)
  }

  put("/:uuid") {
    validateRequest() // Apply API security
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide the 'uuid' of the association")))
    val observationUUID = params.getAs[UUID]("observation_uuid")
    val linkName = params.get("link_name")
    val toConcept = params.get("to_concept")
    val linkValue = params.get("link_value")
    val mimeType = params.get("mime_type")
    controller.update(uuid, observationUUID, linkName, toConcept, linkValue, mimeType).map({
      case None => halt(NotFound(body = "{}", reason = s"No association with uuid of $uuid was found"))
      case Some(a) => toJson(a)
    })
  }

  delete("/:uuid") {
    validateRequest() // Apply API security
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide the 'uuid' of the association")))
    controller.delete(uuid).map({
      case true => halt(NoContent(reason = s"Success! Deleted association with UUID of $uuid"))
      case false => halt(NotFound(reason = s"Failed. No association with UUID of $uuid was found."))
    })
  }

  get("/toconcept/count/:concept") {
    val concept = params.get("concept").getOrElse(halt(BadRequest("Please provide a concept to search for")))
    controller.countByToConcept(concept)
      .map(n => s"""{"concept":"$concept", "count":"$n"}""")
  }

  put("/toconcept/rename") {
    val oldConcept = params.get("old").getOrElse(halt(BadRequest("Please provide the concept being replaced")))
    val newConcept = params.get("new").getOrElse(halt(BadRequest("Please provide the replacement concept")))
    controller.updateToConcept(oldConcept, newConcept)
      .map(n => s"""{"old_concept":"$oldConcept", "new_concept":"$newConcept", "number_updated":"$n"}""")
  }

}
