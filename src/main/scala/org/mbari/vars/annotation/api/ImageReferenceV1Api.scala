package org.mbari.vars.annotation.api

import org.mbari.vars.annotation.controllers.ImageReferenceController
import org.scalatra.swagger.Swagger

import scala.concurrent.ExecutionContext

/**
 * Created by brian on 7/14/16.
 */
class ImageReferenceV1Api(imageReferenceController: ImageReferenceController)(implicit val swagger: Swagger, val executor: ExecutionContext)
    extends APIStack {

  override protected def applicationDescription: String = "ImageReference API (v1)"

  override protected val applicationName: Option[String] = Some("ImageReferenceAPI")

}
