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

import org.mbari.annosaurus.repository.jpa.entity.ImageReferenceEntity

import java.net.URL
import java.time.Instant
import java.util.UUID

case class ImageUpdateSC(
    video_reference_uuid: Option[UUID] = None,
    url: Option[URL] = None,
    timecode: Option[String] = None,
    elapsed_time_millis: Option[Long] = None,
    recorded_timestamp: Option[Instant] = None,
    format: Option[String] = None,
    width_pixels: Option[Int] = None,
    height_pixels: Option[Int] = None,
    description: Option[String] = None
)

object ImageUpdateSC extends FromEntity[ImageReferenceEntity, ImageUpdateSC] {

    override def from(entity: ImageReferenceEntity, extend: Boolean = false): ImageUpdateSC = {
        val im = entity.getImagedMoment
        ImageUpdateSC(
            video_reference_uuid = Option(im.getVideoReferenceUuid),
            url = Option(entity.getUrl),
            timecode = Option(im.getTimecode).map(_.toString),
            elapsed_time_millis = Option(im.getElapsedTime).map(_.toMillis),
            recorded_timestamp = Option(im.getRecordedTimestamp),
            format = Option(entity.getFormat),
            width_pixels = Option(entity.getWidth),
            height_pixels = Option(entity.getHeight),
            description = Option(entity.getDescription)
        )
    }
}
