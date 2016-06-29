package org.mbari.vars.annotation.api

import org.scalatra.ScalatraServlet
import org.scalatra.swagger.{ JacksonSwaggerBase, Swagger }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-29T15:37:00
 */
class AnnotationSwagger(implicit val swagger: Swagger)
    extends ScalatraServlet with JacksonSwaggerBase {

}
