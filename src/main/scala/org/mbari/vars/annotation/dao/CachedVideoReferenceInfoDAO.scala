package org.mbari.vars.annotation.dao

import java.util.UUID

import org.mbari.vars.annotation.model.CachedVideoReferenceInfo

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T16:10:00
 */
trait CachedVideoReferenceInfoDAO[T <: CachedVideoReferenceInfo] extends DAO[T] {

  def findByVideoReferenceUUID(uuid: UUID): Option[CachedVideoReferenceInfo]
  def findByPlatformName(platformName: String): Iterable[CachedVideoReferenceInfo]
  def findByMissionID(missionID: String): Iterable[CachedVideoReferenceInfo]
  def findByMissionContact(missionContact: String): Iterable[CachedVideoReferenceInfo]

}
