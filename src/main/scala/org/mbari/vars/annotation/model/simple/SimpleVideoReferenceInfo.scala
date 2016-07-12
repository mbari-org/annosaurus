package org.mbari.vars.annotation.model.simple

import java.util.UUID

import org.mbari.vars.annotation.model.CachedVideoReferenceInfo

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-07-11T14:57:00
 */
case class SimpleVideoReferenceInfo(
  uuid: UUID,
  videoReferenceUuid: UUID,
  platformName: String,
  missionId: String,
  missionContact: String
)

object SimpleVideoReferenceInfo {

  def apply(info: CachedVideoReferenceInfo): SimpleVideoReferenceInfo =
    new SimpleVideoReferenceInfo(info.uuid, info.videoReferenceUUID, info.platformName,
      info.missionID, info.missionContact)
}
