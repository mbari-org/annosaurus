package org.mbari.vars.annotation.dao.jpa

import java.util.UUID
import javax.persistence.EntityManager

import org.mbari.vars.annotation.dao.CachedVideoReferenceInfoDAO
import org.mbari.vars.annotation.model.CachedVideoReferenceInfo

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T17:15:00
 */
class CachedVideoReferenceInfoDAOImpl(entityManager: EntityManager)
    extends BaseDAO[CachedVideoReferenceInfo](entityManager)
    with CachedVideoReferenceInfoDAO[CachedVideoReferenceInfo] {
  override def findByMissionContact(missionContact: String): Iterable[CachedVideoReferenceInfo] = ???

  override def findByPlatformName(platformName: String): Iterable[CachedVideoReferenceInfo] = ???

  override def findByMissionID(missionID: String): Iterable[CachedVideoReferenceInfo] = ???

  override def findByVideoReferenceUUID(uuid: UUID): Option[CachedVideoReferenceInfo] = ???

  override def findAll(): Iterable[CachedVideoReferenceInfo] = ???

  override def deleteByUUID(primaryKey: UUID): Unit = ???
}
