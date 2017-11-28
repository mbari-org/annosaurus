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

package org.mbari.vars.annotation.dao.jpa

import java.util.UUID
import javax.persistence._

import com.google.gson.annotations.{ Expose, SerializedName }
import org.mbari.vars.annotation.model.CachedVideoReferenceInfo

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-06-17T15:33:00
 */
@Entity(name = "CachedVideoReferenceInfo")
@Table(name = "video_reference_information")
@EntityListeners(value = Array(classOf[TransactionLogger]))
@NamedNativeQueries(Array(
  new NamedNativeQuery(
    name = "VideoReferenceInfo.findAllVideoReferenceUUIDs",
    query = "SELECT DISTINCT video_reference_uuid FROM video_reference_information ORDER BY video_reference_uuid ASC"
  ),
  new NamedNativeQuery(
    name = "VideoReferenceInfo.findAllMissionContacts",
    query = "SELECT DISTINCT mission_contact FROM video_reference_information ORDER BY mission_contact ASC"
  ),
  new NamedNativeQuery(
    name = "VideoReferenceInfo.findAllPlatformNames",
    query = "SELECT DISTINCT platform_name FROM video_reference_information ORDER BY platform_name ASC"
  ),
  new NamedNativeQuery(
    name = "VideoReferenceInfo.findAllMissionIDs",
    query = "SELECT DISTINCT mission_id FROM video_reference_information ORDER BY mission_id ASC"
  )
))
@NamedQueries(Array(
  new NamedQuery(
    name = "VideoReferenceInfo.findAll",
    query = "SELECT v FROM CachedVideoReferenceInfo v"
  ),
  new NamedQuery(
    name = "VideoReferenceInfo.findByVideoReferenceUUID",
    query = "SELECT v FROM CachedVideoReferenceInfo v WHERE v.videoReferenceUUID = :uuid"
  ),
  new NamedQuery(
    name = "VideoReferenceInfo.findByPlatformName",
    query = "SELECT v FROM CachedVideoReferenceInfo v WHERE v.platformName = :name"
  ),
  new NamedQuery(
    name = "VideoReferenceInfo.findByMissionID",
    query = "SELECT v FROM CachedVideoReferenceInfo v WHERE v.missionID = :id"
  ),
  new NamedQuery(
    name = "VideoReferenceInfo.findByMissionContact",
    query = "SELECT v FROM CachedVideoReferenceInfo v WHERE v.missionContact = :contact "
  )
))
class CachedVideoReferenceInfoImpl extends CachedVideoReferenceInfo with JPAPersistentObject {

  /**
   * typically this will be the chief scientist
   */
  @Expose(serialize = true)
  @Column(
    name = "mission_contact",
    nullable = true,
    length = 64
  )
  override var missionContact: String = _

  @Expose(serialize = true)
  @Column(
    name = "platform_name",
    nullable = false,
    length = 64
  )
  override var platformName: String = _

  @Expose(serialize = true)
  @SerializedName(value = "video_reference_uuid")
  @Column(
    name = "video_reference_uuid",
    nullable = false,
    unique = true
  )
  @Convert(converter = classOf[UUIDConverter])
  override var videoReferenceUUID: UUID = _

  @Expose(serialize = true)
  @Column(
    name = "mission_id",
    nullable = false,
    length = 256
  )
  override var missionID: String = _

}

object CachedVideoReferenceInfoImpl {
  def apply(
    videoReferenceUUID: UUID,
    missionID: String,
    platformName: String,
    missionContact: Option[String] = None
  ): CachedVideoReferenceInfoImpl = {

    val d = new CachedVideoReferenceInfoImpl
    d.videoReferenceUUID = videoReferenceUUID
    d.missionID = missionID
    d.platformName = platformName
    missionContact.foreach(d.missionContact = _)
    d
  }
}
