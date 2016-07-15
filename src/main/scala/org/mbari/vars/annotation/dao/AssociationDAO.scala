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

  def newPersistentObject(
    linkName: String,
    toConcept: Option[String] = Some(Association.TO_CONCEPT_SELF),
    linkValue: Option[String] = Some(Association.LINK_VALUE_NIL)
  ): T

  def findByLinkName(linkName: String): Iterable[T]

  def findByLinkNameAndVideoReferenceUUID(linkName: String, videoReferenceUUID: UUID): Iterable[T]

}
