package org.mbari.vars.annotation.model.simple

import java.util.UUID

import org.mbari.vars.annotation.model.Association

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-07-11T14:54:00
 */
case class SimpleAssociation(uuid: UUID, linkName: String, toConcept: String, linkValue: String)

object SimpleAssociation {
  def apply(association: Association): SimpleAssociation =
    new SimpleAssociation(association.uuid, association.linkName, association.toConcept,
      association.linkValue)
}