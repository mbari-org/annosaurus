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
    extends BaseDAO[CachedVideoReferenceInfoImpl](entityManager)
    with CachedVideoReferenceInfoDAO[CachedVideoReferenceInfoImpl] {

  override def newPersistentObject(): CachedVideoReferenceInfoImpl = new CachedVideoReferenceInfoImpl

  override def findByMissionContact(missionContact: String): Iterable[CachedVideoReferenceInfoImpl] = ???

  override def findByPlatformName(platformName: String): Iterable[CachedVideoReferenceInfoImpl] = ???

  override def findByMissionID(missionID: String): Iterable[CachedVideoReferenceInfoImpl] = ???

  override def findByVideoReferenceUUID(uuid: UUID): Option[CachedVideoReferenceInfoImpl] = ???

  override def findAll(): Iterable[CachedVideoReferenceInfoImpl] = ???

  override def deleteByUUID(primaryKey: UUID): Unit = ???
}
