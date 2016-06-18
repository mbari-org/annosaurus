package org.mbari.vars.annotation.dao

import java.time.Instant
import java.util.UUID

import org.mbari.vars.annotation.model.ImagedMoment

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T16:07:00
 */
trait ImagedMomentDAO[T <: ImagedMoment] extends DAO[T] {

  def findByVideoReferenceUUID(uuid: UUID): Iterable[T]

  def findWithImageReferences(videoReferenceUUID: UUID): Iterable[T]

}
