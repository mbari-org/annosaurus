package org.mbari.vars.annotation.controllers

import java.util.UUID

import org.mbari.vars.annotation.dao.CachedVideoReferenceInfoDAO
import org.mbari.vars.annotation.model.{ CachedVideoReferenceInfo, ImagedMoment }

import scala.concurrent.{ ExecutionContext, Future }

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-09-14T10:50:00
 */
class CachedVideoReferenceInfoController(val daoFactory: BasicDAOFactory)
    extends BaseController[CachedVideoReferenceInfo, CachedVideoReferenceInfoDAO[CachedVideoReferenceInfo]] {

  protected type VRDAO = CachedVideoReferenceInfoDAO[CachedVideoReferenceInfo]

  override def newDAO(): CachedVideoReferenceInfoDAO[CachedVideoReferenceInfo] = daoFactory.newCachedVideoReferenceInfoDAO()

  //  def findAll(limit: Int, offset: Int)(implicit ec: ExecutionContext): Future[Iterable[CachedVideoReferenceInfo]] =
  //    exec(d => d.findAll(limit, offset))

  def findByVideoReferenceUUID(uuid: UUID)(implicit ec: ExecutionContext): Future[Option[CachedVideoReferenceInfo]] = {
    def fn(dao: VRDAO): Option[CachedVideoReferenceInfo] = dao.findByVideoReferenceUUID(uuid)
    exec(fn)
  }

  def findByPlatformName(name: String)(implicit ec: ExecutionContext): Future[Iterable[CachedVideoReferenceInfo]] = {
    def fn(dao: VRDAO): Iterable[CachedVideoReferenceInfo] = dao.findByPlatformName(name)
    exec(fn)
  }

  def findByMissionID(id: String)(implicit ec: ExecutionContext): Future[Iterable[CachedVideoReferenceInfo]] = {
    def fn(dao: VRDAO): Iterable[CachedVideoReferenceInfo] = dao.findByMissionID(id)
    exec(fn)
  }

  def findByMissionContact(contact: String)(implicit ec: ExecutionContext): Future[Iterable[CachedVideoReferenceInfo]] = {
    def fn(dao: VRDAO): Iterable[CachedVideoReferenceInfo] = dao.findByMissionContact(contact)
    exec(fn)
  }

  def findAllMissionContacts(implicit ec: ExecutionContext): Future[Iterable[String]] = {
    def fn(dao: VRDAO): Iterable[String] = dao.findAllMissionContacts()
    exec(fn)
  }

  def findAllPlatformNames(implicit ec: ExecutionContext): Future[Iterable[String]] = {
    def fn(dao: VRDAO): Iterable[String] = dao.findAllPlatformNames()
    exec(fn)
  }

  def findAllMissionIDs(implicit ec: ExecutionContext): Future[Iterable[String]] = {
    def fn(dao: VRDAO): Iterable[String] = dao.findAllMissionIDs()
    exec(fn)
  }

  def findAllVideoReferenceUUIDs(implicit ec: ExecutionContext): Future[Iterable[UUID]] = {
    def fn(dao: VRDAO): Iterable[UUID] = dao.findAllVideoReferenceUUIDs()
    exec(fn)
  }

  def create(
    videoReferenceUUID: UUID,
    platformName: String,
    missionID: String,
    missionContact: Option[String] = None
  )(implicit ec: ExecutionContext): Future[CachedVideoReferenceInfo] = {

    def fn(dao: VRDAO): CachedVideoReferenceInfo = {
      val v = dao.newPersistentObject()
      v.videoReferenceUUID = videoReferenceUUID
      v.platformName = platformName
      v.missionID = missionID
      missionContact.foreach(v.missionContact = _)
      dao.create(v)
      v
    }
    exec(fn)
  }

  def update(
    uuid: UUID,
    videoReferenceUUID: Option[UUID] = None,
    platformName: Option[String] = None,
    missionID: Option[String] = None,
    missionContact: Option[String] = None
  )(implicit ec: ExecutionContext): Future[Option[CachedVideoReferenceInfo]] = {

    def fn(dao: VRDAO): Option[CachedVideoReferenceInfo] = dao.findByUUID(uuid) match {
      case None => None
      case Some(v) =>
        videoReferenceUUID.foreach(v.videoReferenceUUID = _)
        platformName.foreach(v.platformName = _)
        missionID.foreach(v.missionID = _)
        missionContact.foreach(v.missionContact = _)
        Some(v)
    }
    exec(fn)
  }

}