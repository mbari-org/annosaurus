package org.mbari.vars.annotation.controllers

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.model.Observation
import org.mbari.vcr4j.time.Timecode

import scala.concurrent.{ ExecutionContext, Future }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-25T20:28:00
 */
class AnnotationController(daoFactory: BasicDAOFactory) {

  def create(
    videoReferenceUUID: UUID,
    concept: String,
    observer: String,
    observationDate: Instant = Instant.now(),
    timecode: Option[Timecode] = None,
    elapsedTime: Option[Duration] = None,
    recordedDate: Option[Instant] = None,
    duration: Option[Duration] = None
  )(implicit ec: ExecutionContext): Future[Observation] = {

    val imDao = daoFactory.newImagedMomentDAO()
    val obsDao = daoFactory.newObservationDAO(imDao)

    val f = obsDao.runTransaction(d => {
      val imagedMoment = ImagedMomentController.findImagedMoment(imDao, videoReferenceUUID, timecode,
        recordedDate, elapsedTime)
      val observation = obsDao.newPersistentObject()
      observation.concept = concept
      observation.observer = observer
      observation.observationDate = observationDate
      duration.foreach(observation.duration = _)
      obsDao.create(observation)
      imagedMoment.addObservation(observation)
      observation
    })

    f.onComplete(t => imDao.close())
    f
  }

  def update(
    uuid: UUID,
    videoReferenceUUID: Option[UUID] = None,
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

    // --- 1. Does uuid exist?

    val f = obsDao.runTransaction(d => {
      val observation = obsDao.findByUUID(uuid)

      observation.map(obs => {

        // --- 2. Move annotation
        if (timecode.isDefined || elapsedTime.isDefined || recordedDate.isDefined) {
          // Find existing
          for {
            vrUUID <- videoReferenceUUID
            im <- imDao.findByVideoReferenceUUIDAndIndex(vrUUID, timecode, elapsedTime, recordedDate)
          } {
            val newIm = imDao.newPersistentObject()
            newIm.videoReferenceUUID = vrUUID
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
    })
    f.onComplete(t => imDao.close())
    f
  }

}
