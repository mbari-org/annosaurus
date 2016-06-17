package org.mbari.vars.annotation.model

import java.time.Instant
import java.util.UUID

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-15T16:54:00
 */
trait Association {

  var uuid: UUID
  var observation: Observation
  var linkName: String
  var toConcept: String
  var linkValue: String
  def lastUpdated: Option[Instant]

}

object Association {

  val LINK_VALUE_NIL = "nil"

  val TO_CONCEPT_SELF = "self"

  val SEPARATOR = " | "

  def asString(association: Association): String = {
    association.linkName + SEPARATOR +
      Option(association.toConcept).getOrElse(TO_CONCEPT_SELF) + SEPARATOR +
      Option(association.linkValue).getOrElse(LINK_VALUE_NIL)
  }

}
