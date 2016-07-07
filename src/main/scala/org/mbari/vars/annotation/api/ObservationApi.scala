package org.mbari.vars.annotation.api

import org.mbari.vars.annotation.controllers.{ AnnotationController, ObservationController }
import org.scalatra.swagger.Swagger

import scala.concurrent.ExecutionContext

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-07-04T21:56:00
 */
class ObservationApi(controller: ObservationController)(implicit val swagger: Swagger, val executor: ExecutionContext)
    extends APIStack {
  override protected def applicationDescription: String = ???
}
