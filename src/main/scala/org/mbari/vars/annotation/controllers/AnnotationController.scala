package org.mbari.vars.annotation.controllers

import java.time.{ Duration, Instant }
import java.util.UUID

import org.mbari.vars.annotation.Constants
import org.mbari.vars.annotation.dao.jpa.AnnotationImpl
import org.mbari.vars.annotation.dao.{ DAO, ImagedMomentDAO, ObservationDAO }
import org.mbari.vars.annotation.model.{ Annotation, ImageReference, ImagedMoment, Observation }
import org.mbari.vcr4j.time.Timecode

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

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
    f.onComplete(_ => obsDao.close())
    f.map(_.map(AnnotationImpl(_)))
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
      .map(obss => obss.toSeq.map(AnnotationImpl(_))) // Convert to Iterable[Annotation]

  }

  def findByImageReferenceUUID(imageReferenceUUID: UUID)(implicit ec: ExecutionContext): Future[Iterable[Annotation]] = {

    val imDao = daoFactory.newImagedMomentDAO()
    val f = imDao.runTransaction(d => d.findByImageReferenceUUID(imageReferenceUUID))
    f.onComplete(t => imDao.close())
    f.map({
      case None => Nil
      case Some(im) => im.observations
    })
      .map(obs => obs.map(AnnotationImpl(_)))
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
    group: Option[String] = None,
    activity: Option[String] = None
  )(implicit ec: ExecutionContext): Future[Annotation] = {
    val obsDao = daoFactory.newObservationDAO()
    val annotation = AnnotationImpl(videoReferenceUUID, concept, observer,
      observationDate, timecode, elapsedTime, recordedDate, duration, group, activity)
    val f = obsDao.runTransaction(d => create(d, annotation))
    f.onComplete(_ => obsDao.close())
    f.map(AnnotationImpl(_))
  }

  def bulkCreate(annotations: Iterable[Annotation])(implicit ec: ExecutionContext): Future[Seq[AnnotationImpl]] = {
    val obsDao = daoFactory.newObservationDAO()
    val f = obsDao.runTransaction(d => annotations.map(a => create(d, a)))
    f.onComplete(_ => obsDao.close())
    f.map(obsList => obsList.map(AnnotationImpl(_)).toSeq)
  }

  private def create(dao: DAO[_], annotation: Annotation): Observation = {
    val imDao = daoFactory.newImagedMomentDAO(dao)
    val obsDao = daoFactory.newObservationDAO(dao)
    val assDao = daoFactory.newAssociationDAO(dao)
    val irDao = daoFactory.newImageReferenceDAO(dao)
    val imagedMoment = ImagedMomentController.findImagedMoment(
      imDao,
      annotation.videoReferenceUuid,
      Option(annotation.timecode),
      Option(annotation.recordedTimestamp),
      Option(annotation.elapsedTime)
    )
    val observation = obsDao.newPersistentObject()
    observation.concept = annotation.concept
    observation.observer = annotation.observer
    observation.observationDate = annotation.observationTimestamp
    Option(annotation.duration).foreach(observation.duration = _)
    Option(annotation.group).foreach(observation.group = _)
    Option(annotation.activity).foreach(observation.activity = _)
    obsDao.create(observation)
    imagedMoment.addObservation(observation)

    // Add associations
    annotation.associations.foreach(a => {
      val newA = assDao.newPersistentObject(
        a.linkName,
        Option(a.toConcept), Option(a.linkValue), Option(a.mimeType)
      )
      newA.uuid = a.uuid
      observation.addAssociation(newA)
    })

    // TODO add imagereferences
    annotation.imageReferences.foreach(i => {
      irDao.findByURL(i.url) match {
        case Some(v) => // Do nothing. It should already be attached to the imagedmoment
        case None =>
          val newI = irDao.newPersistentObject(i.url, Option(i.description),
            Option(i.height), Option(i.width), Option(i.format))
          newI.uuid = i.uuid
          imagedMoment.addImageReference(newI)
      }

    })

    observation
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
    group: Option[String] = None,
    activity: Option[String] = None
  )(implicit ec: ExecutionContext): Future[Option[Annotation]] = {

    // We have to do this in 2 transactions. The first makes all the changes. The second to
    // retrieve them. We have to do this because we may make a SQL call to move an observaton
    // to a new imagedmoment. The enitymanage doesn't see this change and so returns the cached
    // value which may have the wrong time index or videoreference.
    val dao = daoFactory.newObservationDAO()
    val f = dao.runTransaction(d => _update(d, uuid, videoReferenceUUID, concept,
      observer, observationDate, timecode, elapsedTime, recordedDate, duration,
      group, activity))
    f.onComplete(_ => dao.close())

    val g = f.flatMap(opt => {
      val dao1 = daoFactory.newObservationDAO()
      val ff = dao1.runTransaction(d => d.findByUUID(uuid).map(AnnotationImpl(_)))
      ff.onComplete(_ => dao1.close())
      ff
    })

    g
  }

  def bulkUpdate(annotations: Iterable[Annotation])(implicit ec: ExecutionContext): Future[Iterable[AnnotationImpl]] = {
    val dao = daoFactory.newObservationDAO()
    // We have to do this in 2 transactions. The first makes all the changes. The second to
    // retrieve them. We have to do this because we may make a SQL call to move an observaton
    // to a new imagedmoment. The enitymanage doesn't see this change and so returns the cached
    // value which may have the wrong time index or videoreference.
    val f = dao.runTransaction(d => {
      annotations.flatMap(a => {
        _update(
          d,
          a.observationUuid,
          Some(a.videoReferenceUuid),
          Some(a.concept),
          Some(a.observer),
          a.observationTimestamp,
          Option(a.timecode),
          Option(a.elapsedTime),
          Option(a.recordedTimestamp),
          Option(a.duration),
          Option(a.group),
          Option(a.activity)
        )
      })
    })
    f.onComplete(_ => dao.close())
    // --- After update find all the changes
    val g = f.flatMap(obs => {
      val dao1 = daoFactory.newObservationDAO()
      val ff = dao1.runTransaction(d => obs.flatMap(o => d.findByUUID(o.uuid).map(AnnotationImpl(_))))
      ff.onComplete(_ => dao1.close())
      ff
    })
    g
  }

  /**
   * This private method is meant to be wrapped in a transaction, either for a
   * single update or for bulk updates
   * @param dao
   * @param uuid
   * @param videoReferenceUUID
   * @param concept
   * @param observer
   * @param observationDate
   * @param timecode
   * @param elapsedTime
   * @param recordedDate
   * @param duration
   * @param group
   * @param activity
   * @return
   */
  private def _update(
    dao: DAO[_],
    uuid: UUID,
    videoReferenceUUID: Option[UUID] = None,
    concept: Option[String] = None,
    observer: Option[String] = None,
    observationDate: Instant = Instant.now(),
    timecode: Option[Timecode] = None,
    elapsedTime: Option[Duration] = None,
    recordedDate: Option[Instant] = None,
    duration: Option[Duration] = None,
    group: Option[String] = None,
    activity: Option[String] = None
  ): Option[Observation] = {
    val obsDao = daoFactory.newObservationDAO(dao)
    val imDao = daoFactory.newImagedMomentDAO(dao)
    val observation = obsDao.findByUUID(uuid)
    observation.map(obs => {

      val vrChanged = videoReferenceUUID.isDefined &&
        videoReferenceUUID.get != obs.imagedMoment.videoReferenceUUID

      val tcChanged = timecode.isDefined &&
        timecode.get != obs.imagedMoment.timecode

      val etChanged = elapsedTime.isDefined &&
        elapsedTime.get != obs.imagedMoment.elapsedTime

      val rdChanged = recordedDate.isDefined &&
        recordedDate.get != obs.imagedMoment.recordedDate

      if (vrChanged || tcChanged || etChanged || rdChanged) {
        val vrUUID = videoReferenceUUID.getOrElse(obs.imagedMoment.videoReferenceUUID)
        val tc = Option(timecode.getOrElse(obs.imagedMoment.timecode))
        val rd = Option(recordedDate.getOrElse(obs.imagedMoment.recordedDate))
        val et = Option(elapsedTime.getOrElse(obs.imagedMoment.elapsedTime))
        val newIm = ImagedMomentController.findImagedMoment(imDao, vrUUID, tc, rd, et)
        obsDao.changeImageMoment(newIm.uuid, obs.uuid)
      }

      concept.foreach(obs.concept = _)
      observer.foreach(obs.observer = _)
      duration.foreach(obs.duration = _)
      group.foreach(obs.group = _)
      activity.foreach(obs.activity = _)
      obs.observationDate = observationDate
      obs
    })
  }

  def delete(uuid: UUID)(implicit ec: ExecutionContext): Future[Boolean] = {
    val imDao = daoFactory.newImagedMomentDAO()
    val obsDao = daoFactory.newObservationDAO(imDao)
    val f = obsDao.runTransaction(d => {
      d.findByUUID(uuid) match {
        case None => false
        case Some(v) =>
          val imagedMoment = v.imagedMoment
          if (imagedMoment.observations.size == 1 && imagedMoment.imageReferences.isEmpty) {
            imDao.delete(imagedMoment)
          } else {
            d.delete(v)
          }
          true
      }
    })
    f.onComplete(_ => obsDao.close())
    f
  }

}
