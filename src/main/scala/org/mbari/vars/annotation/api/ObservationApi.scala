package org.mbari.vars.annotation.api

import java.util.UUID

import org.mbari.vars.annotation.controllers.{AnnotationController, ObservationController}
import org.scalatra.{BadRequest, NotFound}
import org.scalatra.swagger.Swagger

import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-07-04T21:56:00
 */
class ObservationApi(controller: ObservationController)(implicit val swagger: Swagger, val executor: ExecutionContext)
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

  put("/:uuid") {}

  delete("/:uuid") {}
}
