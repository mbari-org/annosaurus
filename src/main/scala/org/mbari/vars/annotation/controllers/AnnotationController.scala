package org.mbari.vars.annotation.controllers

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.dao.{ ImagedMomentDAO, ObservationDAO }
import org.mbari.vars.annotation.model.{ ImagedMoment, Observation }
import org.mbari.vcr4j.time.Timecode

import scala.concurrent.{ ExecutionContext, Future }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-25T20:28:00
 */
class AnnotationController(
    imagedMomentController: ImagedMomentController,
    observationController: ObservationController
) {

  def newAnnotation(
    videoReferenceUUID: UUID,
    concept: String,
    observer: String,
    observationDate: Instant = Instant.now(),
    timecode: Option[Timecode] = None,
    elapsedTime: Option[Duration] = None,
    recordedDate: Option[Instant] = None,
    duration: Option[Duration] = None
  )(implicit ec: ExecutionContext): Future[Observation] = {

    // TODO make this ACID. Right now it's done in 2 transactions
    imagedMomentController.create(videoReferenceUUID, timecode, recordedDate, elapsedTime)
      .map(imagedMoment => {
        val obsDAO = observationController.daoFactory.newObservationDAO()
        val observation = obsDAO.newPersistentObject()
        observation.concept = concept
        observation.observer = observer
        observation.observationDate = observationDate
        duration.foreach(observation.duration = _)
        obsDAO.create(observation)
        imagedMoment.addObservation(observation)
        obsDAO.close()
        observation
      })

  }

}
