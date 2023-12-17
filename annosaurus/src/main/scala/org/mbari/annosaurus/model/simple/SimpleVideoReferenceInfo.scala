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

package org.mbari.annosaurus.model.simple

import org.mbari.annosaurus.model.MutableCachedVideoReferenceInfo

import java.util.UUID

/** @author
  *   Brian Schlining
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

    def apply(info: MutableCachedVideoReferenceInfo): SimpleVideoReferenceInfo =
        new SimpleVideoReferenceInfo(
            info.uuid,
            info.videoReferenceUUID,
            info.platformName,
            info.missionId,
            info.missionContact
        )
}
