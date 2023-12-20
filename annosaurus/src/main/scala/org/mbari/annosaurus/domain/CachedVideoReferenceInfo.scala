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

package org.mbari.annosaurus.domain

import java.util.UUID

final case class CachedVideoReferenceInfo(
    uuid: UUID,
    videoReferenceUuid: UUID,
    platformName: Option[String] = None,
    missionId: Option[String] = None,
    missionContact: Option[String] = None,
    lastUpdated: Option[java.time.Instant] = None
) extends ToSnakeCase[CachedVideoReferenceInfoSC] {
    override def toSnakeCase: CachedVideoReferenceInfoSC = CachedVideoReferenceInfoSC(
        uuid,
        videoReferenceUuid,
        platformName,
        missionId,
        missionContact,
        lastUpdated
    )
}

final case class CachedVideoReferenceInfoSC(
    uuid: UUID,
    video_reference_uuid: UUID,
    platform_name: Option[String] = None,
    mission_id: Option[String] = None,
    mission_contact: Option[String] = None,
    last_updated: Option[java.time.Instant] = None
) extends ToCamelCase[CachedVideoReferenceInfo] {
    override def toCamelCase: CachedVideoReferenceInfo = CachedVideoReferenceInfo(
        uuid,
        video_reference_uuid,
        platform_name,
        mission_id,
        mission_contact,
        last_updated
    )
}
