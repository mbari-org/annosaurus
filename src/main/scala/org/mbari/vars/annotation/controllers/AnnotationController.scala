package org.mbari.vars.annotation.controllers

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.dao.{ ImagedMomentDAO, ObservationDAO }
import org.mbari.vars.annotation.model.{ ImageReference, ImagedMoment, Observation }
import org.mbari.vars.annotation.model.simple.Annotation
import org.mbari.vcr4j.time.Timecode

import scala.concurrent.{ ExecutionContext, Future }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-25T20:28:00
 */
class AnnotationController(daoFactory: BasicDAOFactory) {

  def findByUUID(uuid: UUID)(implicit ec: ExecutionContext): Future[Option[Annotation]] = {
    val obsDao = daoFactory.newObservationDAO()
    val f = obsDao.runTransaction(d => obsDao.findByUUID(uuid))
    f.onComplete(t => obsDao.close())
    f.map(_.map(Annotation(_)))
  }

  /*
      This searches for the ImagedMoments but returns an Annotation view. Keep in mind
      that each ImagedMoment may contain more than one observations. The limit and
      offset are for the ImagedMoments, and each may contain more than one Observation
      (i.e. Annotation). So this call will appear to return more rows than limit and offest
      specify.
   */
  def findByVideoReferenceUUID(
    videoReferenceUUID: UUID,
    limit: Option[Int] = None,
    offset: Option[Int] = None
  )(implicit ec: ExecutionContext): Future[Seq[Annotation]] = {

    val imDao = daoFactory.newImagedMomentDAO()
    val f = imDao.runTransaction(d => d.findByVideoReferenceUUID(videoReferenceUUID, limit, offset))
    f.onComplete(t => imDao.close())
    f.map(ims => ims.flatMap(_.observations)) // Convert to Iterable[Observation]
      .map(obss => obss.toSeq.map(Annotation(_))) // Convert to Iterable[Annotation]

  }

  def create(
    videoReferenceUUID: UUID,
    concept: String,
    observer: String,
    observationDate: Instant = Instant.now(),
    timecode: Option[Timecode] = None,
    elapsedTime: Option[Duration] = None,
    recordedDate: Option[Instant] = None,
    duration: Option[Duration] = None,
    group: Option[String] = None
  )(implicit ec: ExecutionContext): Future[Annotation] = {

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
      group.foreach(observation.group = _)
      obsDao.create(observation)
      imagedMoment.addObservation(observation)
      observation
    })

    f.onComplete(t => imDao.close())
    f.map(Annotation(_))
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
    duration: Option[Duration] = None,
    group: Option[String] = None
  )(implicit ec: ExecutionContext): Future[Option[Annotation]] = {

    val imDao = daoFactory.newImagedMomentDAO()
    val obsDao = daoFactory.newObservationDAO(imDao)

    // --- 1. Does uuid exist?

    val f = obsDao.runTransaction(d => {
      val observation = obsDao.findByUUID(uuid)

      observation.map(obs => {
        val vrUUID = videoReferenceUUID.getOrElse(obs.imagedMoment.videoReferenceUUID)
        // --- 2. Move annotation
        if (timecode.isDefined || elapsedTime.isDefined || recordedDate.isDefined) {
          // Find existing
          val newIm = ImagedMomentController.findImagedMoment(imDao, vrUUID, timecode, recordedDate, elapsedTime)
          move(imDao, newIm, obs)
        } else if (videoReferenceUUID.isDefined) {
          // move to new video-reference/imaged-moment using the existing images
          val tc = Option(obs.imagedMoment.timecode)
          val rd = Option(obs.imagedMoment.recordedDate)
          val et = Option(obs.imagedMoment.elapsedTime)
          val newIm = ImagedMomentController.findImagedMoment(imDao, vrUUID, tc, rd, et)
          move(imDao, newIm, obs)
        }

        concept.foreach(obs.concept = _)
        observer.foreach(obs.observer = _)
        duration.foreach(obs.duration = _)
        group.foreach(obs.group = _)
        obs.observationDate = observationDate
        obs
      })
    })
    f.onComplete(t => imDao.close())
    f.map(opt => opt.map(Annotation(_)))
  }

  private def move(dao: ImagedMomentDAO[ImagedMoment], newIm: ImagedMoment, observation: Observation): Unit = {
    val oldIm = observation.imagedMoment
    oldIm.removeObservation(observation)
    newIm.addObservation(observation)
    dao.deleteIfEmpty(oldIm)
  }

}
