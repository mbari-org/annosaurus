package org.mbari.vars.annotation.model.simple

import java.net.URL
import java.util.UUID

import org.mbari.vars.annotation.model.ImageReference

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-07-11T14:59:00
 */
case class SimpleImageReference(uuid: UUID, url: URL, description: String,
  format: String, width: Int, height: Int)

object SimpleImageReference {

  def apply(imageReference: ImageReference): SimpleImageReference =
    new SimpleImageReference(imageReference.uuid, imageReference.url, imageReference.description,
      imageReference.format, imageReference.width, imageReference.height)

}