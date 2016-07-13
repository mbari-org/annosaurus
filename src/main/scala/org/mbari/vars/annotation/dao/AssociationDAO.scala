package org.mbari.vars.annotation.dao

import java.util.UUID

import org.mbari.vars.annotation.model.Association

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T16:08:00
 */
trait AssociationDAO[T <: Association] extends DAO[T] {

  def findByLinkName(linkName: String): Iterable[T]

  def findByLinkNameAndVideoReferenceUUID(linkName: String, videoReferenceUUID: UUID): Iterable[T]

}
