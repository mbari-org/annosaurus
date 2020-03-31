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

import java.util.UUID

import org.mbari.vars.annotation.controllers.AssociationController
import org.mbari.vars.annotation.model.Association
import org.mbari.vars.annotation.model.simple.{ConceptAssociationRequest, ErrorMsg}
import org.scalatra.{BadRequest, NoContent, NotFound}

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-07-13T11:21:00
  */
class AssociationV1Api(controller: AssociationController)(implicit val executor: ExecutionContext)
    extends V1APIStack {

  before() {
    contentType = "application/json"
    response.headers.set("Access-Control-Allow-Origin", "*")
  }

  // Find an association
  get("/:uuid") {
    val uuid =
      params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide a 'uuid' parameter")))))
    controller
      .findByUUID(uuid)
      .map({
        case None    => halt(NotFound(toJson(ErrorMsg(400, s"An Association with a UUID of $uuid was not found"))))
        case Some(v) => toJson(v)
      })
  }

  // find associations
  get("/:video_reference_uuid/:link_name") {
    val videoReferenceUUID = params
      .getAs[UUID]("video_reference_uuid")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide a video-reference 'uuid'")))))
    val linkName =
      params.get("link_name")
        .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "A 'link_name' parameter is required")))))
    val concept = params.get("concept")
    controller
      .findByLinkNameAndVideoReferenceUUIDAndConcept(linkName, videoReferenceUUID, concept)
      .map(as => toJson(as.asJava))
  }

  post("/") {
    validateRequest() // Apply API security
    val uuid = params
      .getAs[UUID]("observation_uuid")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide an 'observation_uuid'")))))
    val linkName =
      params.get("link_name")
        .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "A 'link_name' parameter is required")))))
    val toConcept = params.get("to_concept").getOrElse(Association.TO_CONCEPT_SELF)
    val linkValue = params.get("link_value").getOrElse(Association.LINK_VALUE_NIL)
    val mimeType  = params.get("mime_type").getOrElse("text/plain")
    val associationUuid = params.getAs[UUID]("association_uuid")
    controller.create(uuid, linkName, toConcept, linkValue, mimeType, associationUuid).map(toJson)
  }

  put("/:uuid") {
    validateRequest() // Apply API security
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(
        halt(BadRequest(toJson(ErrorMsg(400, "Please provide the 'uuid' of the association"))))
      )
    val observationUUID = params.getAs[UUID]("observation_uuid")
    val linkName        = params.get("link_name")
    val toConcept       = params.get("to_concept")
    val linkValue       = params.get("link_value")
    val mimeType        = params.get("mime_type")
    controller
      .update(uuid, observationUUID, linkName, toConcept, linkValue, mimeType)
      .map({
        case None =>
          halt(NotFound(toJson(ErrorMsg(404, s"No association with uuid of $uuid was found"))))
        case Some(a) => toJson(a)
      })
  }

  put("/bulk") {
    validateRequest()
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val associations = fromJson(request.body, classOf[Array[Association]])
        controller
          .bulkUpdate(associations)
          .map(assos => toJson(assos.asJava))
      case _ =>
        halt(
          BadRequest(
            toJson(
              ErrorMsg(
                412,
                "Puts to /bulk only accept JSON body (i.e. Content-Type: application/json"
              )
            )
          )
        )
    }
  }

  post("/delete") {
    validateRequest()
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val uuids = fromJson(request.body, classOf[Array[UUID]])
        if (uuids == null || uuids.isEmpty)
          halt(BadRequest(toJson(ErrorMsg(404, "No observation UUIDs were provided as JSON"))))
        controller.bulkDelete(uuids)
      case _ =>
        halt(
          BadRequest(
            toJson(ErrorMsg(412, "bulk delete only accepts JSON (Content-Type: application/json)"))
          )
        )
    }
  }

  delete("/:uuid") {
    validateRequest() // Apply API security
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(
        halt(BadRequest(toJson(ErrorMsg(400, "Please provide the 'uuid' of the association"))))
      )
    controller
      .delete(uuid)
      .map({
        case true => halt(NoContent()) // Success!
        case false =>
          halt(
            NotFound(toJson(ErrorMsg(412, s"Failed. No association with UUID of $uuid was found.")))
          )
      })
  }

  get("/toconcept/count/:concept") {
    val concept = params
      .get("concept")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide a concept to search for")))))
    controller
      .countByToConcept(concept)
      .map(n => s"""{"concept":"$concept", "count":"$n"}""")
  }

  put("/toconcept/rename") {
    val oldConcept = params
      .get("old")
      .getOrElse(
        halt(BadRequest(toJson(ErrorMsg(400, "Please provide the concept being replaced"))))
      )
    val newConcept = params
      .get("new")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "Please provide the replacement concept")))))
    controller
      .updateToConcept(oldConcept, newConcept)
      .map(n =>
        s"""{"old_concept":"$oldConcept", "new_concept":"$newConcept", "number_updated":"$n"}"""
      )
  }

  post("/conceptassociations") {
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val b                         = request.body
        val limit                     = params.getAs[Int]("limit")
        val offset                    = params.getAs[Int]("offset")
        val conceptAssociationRequest = fromJson(b, classOf[ConceptAssociationRequest])
        controller
          .findByConceptAssociationRequest(conceptAssociationRequest)
          .map(toJson)
      case _ =>
        halt(
          BadRequest(
            toJson(
              ErrorMsg(
                400,
                "Posts to /conceptassociations only accept a JSON body (i.e. Content-Type: application/json)"
              )
            )
          )
        )
    }
  }

}
