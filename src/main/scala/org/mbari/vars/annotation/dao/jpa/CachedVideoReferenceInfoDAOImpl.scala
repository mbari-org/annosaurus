package org.mbari.vars.annotation.dao.jpa

import java.util.UUID
import javax.persistence.EntityManager

import org.mbari.vars.annotation.dao.CachedVideoReferenceInfoDAO
import scala.collection.JavaConverters._

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

  override def findByMissionContact(missionContact: String): Iterable[CachedVideoReferenceInfoImpl] =
    findByNamedQuery("VideoReferenceInfo.findByMissionContact", Map("contact" -> missionContact))

  override def findByPlatformName(platformName: String): Iterable[CachedVideoReferenceInfoImpl] =
    findByNamedQuery("VideoReferenceInfo.findByPlatformName", Map("name" -> platformName))

  override def findByMissionID(missionID: String): Iterable[CachedVideoReferenceInfoImpl] =
    findByNamedQuery("VideoReferenceInfo.findByMissionID", Map("id" -> missionID))

  override def findByVideoReferenceUUID(uuid: UUID): Option[CachedVideoReferenceInfoImpl] =
    findByNamedQuery("VideoReferenceInfo.findByVideoReferenceUUID", Map("uuid" -> uuid)).headOption

  override def findAll(): Iterable[CachedVideoReferenceInfoImpl] =
    findByNamedQuery("VideoReferenceInfo.findAll")

  override def findAll(limit: Int, offset: Int): Iterable[CachedVideoReferenceInfoImpl] =
    findByNamedQuery("VideoReferenceInfo.findAll", limit = Some(limit), offset = Some(offset))

  override def findAllVideoReferenceUUIDs(): Iterable[UUID] =
    fetchUsing("VideoReferenceInfo.findAllVideoReferenceUUIDs")
      .map(s => UUID.fromString(s))

  override def findAllMissionContacts(): Iterable[String] = fetchUsing("VideoReferenceInfo.findAllMissionContacts")

  override def findAllPlatformNames(): Iterable[String] = fetchUsing("VideoReferenceInfo.findAllVideoReferenceUUIDs")

  override def findAllMissionIDs(): Iterable[String] = fetchUsing("VideoReferenceInfo.findAllMissionIDs")

  private def fetchUsing(namedQuery: String): Iterable[String] = entityManager.createNamedQuery(namedQuery)
    .getResultList
    .asScala
    .filter(_ != null)
    .map(_.toString)

}
