package org.mbari.vars.annotation.api

import java.util.UUID

import org.mbari.vars.annotation.controllers.AnnotationController
import org.scalatra.swagger.Swagger

import scala.concurrent.ExecutionContext

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-30T10:08:00
 */
class AnnotationV1Api(controller: AnnotationController)(implicit val swagger: Swagger, val executor: ExecutionContext)
    extends APIStack {

  override protected def applicationDescription: String = "Annotation API (v1)"

  override protected val applicationName: Option[String] = Some("AnnotationAPI")

  before() {
    contentType = "application/json"
    response.headers += ("Access-Control-Allow-Origin" -> "*")
  }

}
