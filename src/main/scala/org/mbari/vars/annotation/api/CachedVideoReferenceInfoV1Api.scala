package org.mbari.vars.annotation.api

import java.util.UUID

import org.mbari.vars.annotation.controllers.CachedVideoReferenceInfoController
import org.mbari.vars.annotation.model.{ StringArray, UUIDArray, ValueArray }
import org.scalatra.{ BadRequest, NoContent, NotFound }
import org.scalatra.swagger.Swagger

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-09-14T10:50:00
 */
class CachedVideoReferenceInfoV1Api(controller: CachedVideoReferenceInfoController)(implicit val swagger: Swagger, val executor: ExecutionContext)
    extends APIStack {

  override protected def applicationDescription: String = "CachedVideoReferenceInfo API (v1)"

  override protected val applicationName: Option[String] = Some("CachedVideoReferenceInfoAPI")

  get("/?") {
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    controller.findAll()
      .map(_.asJava)
      .map(toJson)
  }

  get("/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a UUID")))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    controller.findByUUID(uuid).map({
      case None => halt(NotFound(
        body = "{}",
        reason = s"A CachedVideoReferenceInfo with a UUID of $uuid was not found"
      ))
      case Some(v) => toJson(v)
    })
  }

  get("/videoreferences") {
    controller.findAllVideoReferenceUUIDs
      .map(s => UUIDArray(s.toArray))
      .map(toJson)
  }

  // find a CachedVideoReferenceInfo by it's unique videoreferenceuuid
  get("/videoreference/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a Video Reference UUID")))

    controller.findByVideoReferenceUUID(uuid)
      .map({
        case None => halt(NotFound(reason = s"A CachedVideoReferenceInfo with a videoreference uuid of $uuid was not found"))
        case Some(v) => toJson(v)
      })
  }

  // find all mission ids
  get("/missionids") {
    controller.findAllMissionIDs
      .map(s => StringArray(s.toArray))
      .map(toJson)
  }

  get("/missionid/:id") {
    val id = params.get("id").getOrElse(halt(BadRequest("Please provide a mission id")))
    controller.findByMissionID(id)
      .map(_.asJava)
      .map(toJson)
  }

  get("/missioncontacts") {
    controller.findAllMissionContacts
      .map(s => StringArray(s.toArray))
      .map(toJson)
  }

  get("/missioncontact/:name") {
    val name = params.get("name").getOrElse(halt(BadRequest("Please provide a mission contact")))
    controller.findByMissionContact(name)
      .map(_.asJava)
      .map(toJson)
  }

  post("/") {
    val videoReferenceUUID = params.getAs[UUID]("video_reference_uuid").getOrElse(halt(BadRequest(
      body = "{}",
      reason = "A 'video_reference_uuid' parameter is required"
    )))
    val missionContact = params.get("mission_contact")
    val missionID = params.get("mission_id").getOrElse(halt(BadRequest("A 'misison_id' parameter is required")))
    val platformName = params.get("platform_name").getOrElse(halt(BadRequest("A 'platform_name' parameter is required")))
    controller.create(videoReferenceUUID, platformName, missionID, missionContact)
      .map(toJson)
  }

  put("/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "{}",
      reason = "A 'uuid' parameter is required"
    )))
    val videoReferenceUUID = params.getAs[UUID]("video_reference_uuid")
    val missionContact = params.get("mission_contact")
    val missionID = params.get("mission_id")
    val platformName = params.get("platform_name")
    controller.update(uuid, videoReferenceUUID, platformName, missionID, missionContact)
      .map({
        case None => halt(NotFound(reason = s"Failed. No VideoReferenceInfo with UUID of $uuid was found."))
        case Some(v) => toJson(v)
      })
  }

  delete("/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "{}",
      reason = "A 'uuid' parameter is required"
    )))
    controller.delete(uuid).map({
      case true => halt(NoContent(reason = s"Success! Deleted observation with UUID of $uuid"))
      case false => halt(NotFound(reason = s"Failed. No observation with UUID of $uuid was found."))
    })
  }

}