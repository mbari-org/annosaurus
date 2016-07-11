package org.mbari.vars.annotation.controllers

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.dao.{ NotFoundInDatastoreException, ObservationDAO }
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

  type ODAO = ObservationDAO[Observation]

  override def newDAO(): ODAO = daoFactory.newObservationDAO()

  def create(
    imagedMomentUUID: UUID,
    concept: String,
    observer: Option[String] = None,
    observationDate: Instant = Instant.now(),
    duration: Option[Duration] = None,
    group: Option[String] = None
  )(implicit ec: ExecutionContext): Future[Observation] = {

    def fn(dao: ODAO): Observation = {
      val imDao = daoFactory.newImagedMomentDAO(dao)
      imDao.findByUUID(imagedMomentUUID) match {
        case None => throw new NotFoundInDatastoreException(s"ImagedMoment with UUID of $imagedMomentUUID not found")
        case Some(imagedMoment) =>
          val observation = dao.newPersistentObject()
          observation.concept = concept
          observer.foreach(observation.observer = _)
          observation.observationDate = observationDate
          duration.foreach(observation.duration = _)
          group.foreach(observation.group = _)
          observation.imagedMoment = imagedMoment
          observation
      }
    }

    exec(fn)
  }

  def update(
    uuid: UUID,
    concept: Option[String] = None,
    observer: Option[String] = None,
    observationDate: Instant = Instant.now(),
    duration: Option[Duration] = None,
    group: Option[String] = None,
    imagedMomentUUID: Option[UUID] = None
  )(implicit ec: ExecutionContext): Future[Option[Observation]] = {

    def fn(dao: ODAO): Option[Observation] = {
      // --- 1. Does uuid exist?
      val observation = dao.findByUUID(uuid)

      observation.map(obs => {
        concept.foreach(obs.concept = _)
        observer.foreach(obs.observer = _)
        obs.observationDate = observationDate
        duration.foreach(obs.duration = _)
        group.foreach(obs.group = _)

        imagedMomentUUID.foreach(imUUID => {
          val imDao = daoFactory.newImagedMomentDAO(dao)
          val newIm = imDao.findByUUID(imUUID)
          newIm match {
            case None =>
              throw new NotFoundInDatastoreException(s"ImagedMoment with UUID of $imUUID no found")
            case Some(imagedMoment) =>
              obs.imagedMoment.removeObservation(obs)
              imagedMoment.addObservation(obs)
          }
        })

        obs
      })
    }

    exec(fn)

  }

  def findAllNames(implicit ec: ExecutionContext): Future[Iterable[String]] = {
    def fn(dao: ODAO): Iterable[String] = dao.findAllNames()
    exec(fn)
  }


}
