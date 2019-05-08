package org.mbari.vars.annotation.api.v2

import java.util.UUID

import org.mbari.vars.annotation.api.APIStack
import org.mbari.vars.annotation.controllers.AnnotationController
import org.mbari.vars.annotation.model.Annotation
import org.mbari.vars.annotation.util.ResponseUtilities
import org.scalatra.BadRequest

import scala.concurrent.ExecutionContext

/**
  * @author Brian Schlining
  * @since 2019-05-08T13:50:00
  */
class AnnotationV2Api(controller: AnnotationController)(implicit val executor: ExecutionContext)
  extends APIStack {

  before() {
    contentType = "application/json"
    response.headers += ("Access-Control-Allow-Origin" -> "*")
  }

  get("/videoreference/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "A video reference 'uuid' parameter is required")))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    val (closeable, stream) = controller.streamByVideoReferenceUUID(uuid, limit, offset)
    ResponseUtilities.sendStreamedResponse(response, stream, (a: Annotation) => toJson(a))
    closeable.close()
    Unit
  }

}
