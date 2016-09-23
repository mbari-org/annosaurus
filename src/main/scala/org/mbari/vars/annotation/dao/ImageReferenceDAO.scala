package org.mbari.vars.annotation.dao

import java.net.URL

import org.mbari.vars.annotation.model.{ ImageReference, ImagedMoment }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T16:10:00
 */
trait ImageReferenceDAO[T <: ImageReference] extends DAO[T] {

  def newPersistentObject(
    url: URL,
    description: Option[String] = None,
    heightPixels: Option[Int] = None,
    widthPixels: Option[Int] = None,
    format: Option[String] = None
  ): T

  def findByURL(url: URL): Option[T]

}
