/*
 * Copyright 2017 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.vars.annotation.repository.jpa.entity

import com.google.gson.annotations.{Expose, SerializedName}
import jakarta.persistence._
import org.mbari.vars.annotation.model.CachedVideoReferenceInfo
import org.mbari.vars.annotation.repository.jpa.{JPAPersistentObject, TransactionLogger, UUIDConverter}

import java.util.UUID

/**
  * idx_video_reference_uuid_vri
  *
  * @author Brian Schlining
  * @since 2016-06-17T15:33:00
  */
@Entity(name = "CachedVideoReferenceInfo")
@Table(
  name = "video_reference_information",
  indexes = Array(
    new Index(
      name = "idx_video_reference_information__video_reference_uuid",
      columnList = "video_reference_uuid"
    )
  )
)
@EntityListeners(value = Array(classOf[TransactionLogger]))
@NamedNativeQueries(
  Array(
    new NamedNativeQuery(
      name = "VideoReferenceInfo.findAllVideoReferenceUUIDs",
      query =
        "SELECT DISTINCT video_reference_uuid FROM video_reference_information ORDER BY video_reference_uuid ASC"
    ),
    new NamedNativeQuery(
      name = "VideoReferenceInfo.findAllMissionContacts",
      query =
        "SELECT DISTINCT mission_contact FROM video_reference_information ORDER BY mission_contact ASC"
    ),
    new NamedNativeQuery(
      name = "VideoReferenceInfo.findAllPlatformNames",
      query =
        "SELECT DISTINCT platform_name FROM video_reference_information ORDER BY platform_name ASC"
    ),
    new NamedNativeQuery(
      name = "VideoReferenceInfo.findAllMissionIDs",
      query = "SELECT DISTINCT mission_id FROM video_reference_information ORDER BY mission_id ASC"
    )
  )
)
@NamedQueries(
  Array(
    new NamedQuery(
      name = "VideoReferenceInfo.findAll",
      query = "SELECT v FROM CachedVideoReferenceInfo v ORDER BY v.uuid"
    ),
    new NamedQuery(
      name = "VideoReferenceInfo.findByVideoReferenceUUID",
      query =
        "SELECT v FROM CachedVideoReferenceInfo v WHERE v.videoReferenceUUID = :uuid ORDER BY v.uuid"
    ),
    new NamedQuery(
      name = "VideoReferenceInfo.findByPlatformName",
      query =
        "SELECT v FROM CachedVideoReferenceInfo v WHERE v.platformName = :name ORDER BY v.uuid"
    ),
    new NamedQuery(
      name = "VideoReferenceInfo.findByMissionID",
      query = "SELECT v FROM CachedVideoReferenceInfo v WHERE v.missionId = :id ORDER BY v.uuid"
    ),
    new NamedQuery(
      name = "VideoReferenceInfo.findByMissionContact",
      query =
        "SELECT v FROM CachedVideoReferenceInfo v WHERE v.missionContact = :contact ORDER BY v.uuid"
    )
  )
)
class CachedVideoReferenceInfoEntity extends CachedVideoReferenceInfo with JPAPersistentObject {

  /**
    * typically this will be the chief scientist
    */
  @Expose(serialize = true)
  @Column(name = "mission_contact", nullable = true, length = 64)
  var missionContact: String = _

  @Expose(serialize = true)
  @Column(name = "platform_name", nullable = false, length = 64)
  var platformName: String = _

  @Expose(serialize = true)
  @SerializedName(value = "video_reference_uuid")
  @Column(
    name = "video_reference_uuid",
    nullable = false,
    unique = true,
    columnDefinition = "CHAR(36)"
  )
  @Convert(converter = classOf[UUIDConverter])
  var videoReferenceUUID: UUID = _

  @Expose(serialize = true)
  @Column(name = "mission_id", nullable = false, length = 256)
  var missionId: String = _

}

object CachedVideoReferenceInfoEntity {
  def apply(
      videoReferenceUUID: UUID,
      missionID: String,
      platformName: String,
      missionContact: Option[String] = None
  ): CachedVideoReferenceInfoEntity = {

    val d = new CachedVideoReferenceInfoEntity
    d.videoReferenceUUID = videoReferenceUUID
    d.missionId = missionID
    d.platformName = platformName
    missionContact.foreach(d.missionContact = _)
    d
  }
}
