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
    observer: String,
    observationDate: Instant = Instant.now(),
    duration: Option[Duration] = None,
    group: Option[String] = None
  )(implicit ec: ExecutionContext): Future[Observation] = {

    def fn(dao: ODAO): Observation = {
      val imDao = daoFactory.newImagedMomentDAO(dao)
      imDao.findByUUID(imagedMomentUUID) match {
        case None => throw new NotFoundInDatastoreException(s"ImagedMoment with UUID of $imagedMomentUUID not found")
        case Some(imagedMoment) =>
          val observation = dao.newPersistentObject(concept, observer, observationDate, group, duration)
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
    activity: Option[String] = None,
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
        activity.foreach(obs.activity = _)
        for {
          imUUID <- imagedMomentUUID
          imDao = daoFactory.newImagedMomentDAO(dao)
          newIm <- imDao.findByUUID(imUUID)
        } {
          obs.imagedMoment.removeObservation(obs)
          newIm.addObservation(obs)
        }

        obs
      })
    }

    exec(fn)

  }

  def findAllConcepts(implicit ec: ExecutionContext): Future[Iterable[String]] = {
    def fn(dao: ODAO): Iterable[String] = dao.findAllConcepts()
    exec(fn)
  }

  def findAllConceptsByVideoReferenceUUID(uuid: UUID)(implicit ec: ExecutionContext): Future[Iterable[String]] = {
    def fn(dao: ODAO): Iterable[String] = dao.findAllConceptsByVideoReferenceUUID(uuid)
    exec(fn)
  }

  def findByVideoReferenceUUID(uuid: UUID, limit: Option[Int] = None, offset: Option[Int] = None)(implicit ec: ExecutionContext): Future[Iterable[Observation]] = {
    def fn(dao: ODAO): Iterable[Observation] = dao.findByVideoReferenceUUID(uuid)
    exec(fn)
  }

  def findByAssociationUUID(uuid: UUID)(implicit ec: ExecutionContext): Future[Option[Observation]] = {
    def fn(dao: ODAO): Option[Observation] = {
      val adao = daoFactory.newAssociationDAO(dao)
      adao.findByUUID(uuid).map(_.observation)
    }
    exec(fn)
  }

  /**
   * This controller will also delete the [[org.mbari.vars.annotation.model.ImagedMoment]] if
   * it is empty (i.e. no observations or other imageReferences)
   * @param uuid
   * @param ec
   * @return
   */
  override def delete(uuid: UUID)(implicit ec: ExecutionContext): Future[Boolean] = {
    def fn(dao: ODAO): Boolean = {
      dao.findByUUID(uuid) match {
        case None => false
        case Some(observation) =>
          val imagedMoment = observation.imagedMoment
          // If this is the only observation and there are no imagerefs, delete the imagemoment
          if (imagedMoment.observations.size == 1 && imagedMoment.imageReferences.isEmpty) {
            val imDao = daoFactory.newImagedMomentDAO(dao)
            imDao.delete(imagedMoment)
          } else {
            dao.delete(observation)
          }
          true
      }
    }
    exec(fn)
  }

  def countByConcept(concept: String): Future[Int] = {
    def fn(dao: ODAO): Int = dao.countByConcept(concept)
    exec(fn)
  }

  def updateConcept(oldConcept: String, newConcept: String): Future[Int] = {
    def fn(dao: ODAO): Int = dao.updateConcept(oldConcept, newConcept)
    exec(fn)
  }

}
