package org.mbari.vars.annotation.api

import java.util.UUID

import org.mbari.vars.annotation.controllers.{AnnotationController, ImagedMomentController}
import org.mbari.vcr4j.time.Timecode
import org.scalatra.{BadRequest, NotFound}
import org.scalatra.swagger.Swagger

import scala.concurrent.ExecutionContext
import scala.util.Try

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

  get("/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a UUID")))
    controller.findByUUID(uuid).map({
      case None => halt(NotFound(
        body = "{}",
        reason = s"An ImagedMoment with a UUID of $uuid was not found"))
      case Some(v) => toJson(v)
    })
  }

  get("/videoreference/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a Video Reference UUID")))
    controller.findByVideoReferenceUUID(uuid).map(toJson)
  }

  get("/videoreference/:uuid/:index") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a Video Reference UUID")))
    val index = params.get("index").getOrElse(halt(BadRequest("Please provide an index into the video reference")))

    val timecode = new Timecode(index)
    if (timecode.isValid) {
      controller.
    }
  }

}
