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
    observationController: ObservationController,
    daoFactory: BasicDAOFactory
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

    // TODO make this ACID. Right now it's done in 2 transactions.
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

  def updateObservation(
    uuid: UUID,
    concept: Option[String] = None,
    observer: Option[String] = None,
    observationDate: Instant = Instant.now(),
    duration: Option[Duration] = None
  )(implicit ec: ExecutionContext): Future[Option[Observation]] = {

    val obsDao = daoFactory.newObservationDAO()

    val f = Future {
      // --- 1. Does uuid exist?
      val observation = obsDao.findByUUID(uuid)

      observation.map(obs => {
        concept.foreach(obs.concept = _)
        observer.foreach(obs.observer = _)
        obs.observationDate = observationDate
        duration.foreach(obs.duration = _)
        obs
      })
    }

    f.onComplete(t => obsDao.close())
    f

  }

  def updateAnnotation(
    uuid: UUID,
    videoReferenceUUID: UUID,
    concept: Option[String] = None,
    observer: Option[String] = None,
    observationDate: Instant = Instant.now(),
    timecode: Option[Timecode] = None,
    elapsedTime: Option[Duration] = None,
    recordedDate: Option[Instant] = None,
    duration: Option[Duration] = None
  )(implicit ec: ExecutionContext): Future[Option[Observation]] = {

    val imDao = daoFactory.newImagedMomentDAO()
    val obsDao = daoFactory.newObservationDAO(imDao)

    val f = Future {

      // --- 1. Does uuid exist?
      val observation = obsDao.findByUUID(uuid)

      observation.map(obs => {

        // --- 2. Move annotation
        if (timecode.isDefined || elapsedTime.isDefined || recordedDate.isDefined) {
          // Find existing
          imDao.findByVideoReferenceUUIDAndIndex(videoReferenceUUID, timecode, elapsedTime, recordedDate) match {
            case Some(im) => // Do nothing
            case None =>
              val newIm = imDao.newPersistentObject()
              newIm.videoReferenceUUID = videoReferenceUUID
              timecode.foreach(newIm.timecode = _)
              elapsedTime.foreach(newIm.elapsedTime = _)
              recordedDate.foreach(newIm.recordedDate = _)
              val oldIm = obs.imagedMoment
              oldIm.removeObservation(obs)
              newIm.addObservation(obs)

              // Delete imagedMoment if no observations or imageReferences are attached
              if (oldIm.imageReferences.isEmpty && oldIm.observations.isEmpty) {
                imDao.delete(oldIm)
              }
          }
        }

        concept.foreach(obs.concept = _)
        observer.foreach(obs.observer = _)
        duration.foreach(obs.duration = _)
        obs.observationDate = observationDate
        obs
      })
    }

    f.onComplete(t => imDao.close())
    f
  }

}
