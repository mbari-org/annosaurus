package org.mbari.vars.annotation.controllers

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.dao.ObservationDAO
import org.mbari.vars.annotation.model.Observation

import scala.concurrent.{ ExecutionContext, Future }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-25T20:33:00
 */
class ObservationController(val daoFactory: BasicDAOFactory)
    extends BaseController[Observation, ObservationDAO[Observation]] {

  override def newDAO(): ObservationDAO[Observation] = daoFactory.newObservationDAO()

  def update(
    uuid: UUID,
    concept: Option[String] = None,
    observer: Option[String] = None,
    observationDate: Instant = Instant.now(),
    duration: Option[Duration] = None
  )(implicit ec: ExecutionContext): Future[Option[Observation]] = {

    def fn(dao: ObservationDAO[Observation]): Option[Observation] = {
      // --- 1. Does uuid exist?
      val observation = dao.findByUUID(uuid)

      observation.map(obs => {
        concept.foreach(obs.concept = _)
        observer.foreach(obs.observer = _)
        obs.observationDate = observationDate
        duration.foreach(obs.duration = _)
        obs
      })
    }

    exec(fn)

  }

}
