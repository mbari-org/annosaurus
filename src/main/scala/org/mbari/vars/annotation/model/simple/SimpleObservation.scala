package org.mbari.vars.annotation.model.simple

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.model.Observation

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-07-11T15:06:00
 */
case class SimpleObservation(uuid: UUID, concept: String, duration: Duration, group: String,
  observer: String, observationDate: Instant,
  assocations: Iterable[SimpleAssociation])

object SimpleObservation {

  def apply(obs: Observation): SimpleObservation =
    new SimpleObservation(obs.uuid, obs.concept, obs.duration, obs.group, obs.observer,
      obs.observationDate, obs.associations.map(SimpleAssociation(_)))
}
