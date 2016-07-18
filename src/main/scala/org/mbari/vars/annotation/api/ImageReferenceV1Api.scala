package org.mbari.vars.annotation.api

import java.net.URL
import java.time.{Duration, Instant}
import java.util.UUID

import org.mbari.vars.annotation.controllers.ImageReferenceController
import org.scalatra.{BadRequest, NoContent, NotFound}
import org.scalatra.swagger.Swagger

import scala.concurrent.ExecutionContext

/**
 * Created by brian on 7/14/16.
 */
class ImageReferenceV1Api(controller: ImageReferenceController)(implicit val swagger: Swagger, val executor: ExecutionContext)
    extends APIStack {

  override protected def applicationDescription: String = "ImageReference API (v1)"

  override protected val applicationName: Option[String] = Some("ImageReferenceAPI")

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

  put("/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "{}",
      reason = "A 'uuid' parameter is required"
    )))
    val url = params.getAs[URL]("url")
    val format = params.get("format")
    val width = params.getAs[Int]("width_pixels")
    val height = params.getAs[Int]("height_pixels")
    val description = params.get("description")
    val imagedMomentUUID = params.getAs[UUID]("imaged_moment_uuid")
    controller.update(uuid, url, description, height, width, format, imagedMomentUUID)
          .map(toJson)
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
