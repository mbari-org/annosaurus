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
import java.net.URL
import java.time.Instant
import org.mbari.annosaurus.repository.jpa.entity.ImageReferenceEntity

final case class Image(
    imageReferenceUuid: UUID,
    videoReferenceUuid: UUID,
    imagedMomentUuid: UUID,
    format: Option[String] = None,
    width: Option[Int] = None,
    height: Option[Int] = None,
    url: Option[URL] = None,
    description: Option[String] = None,
    timecode: Option[String] = None,
    elapsedTimeMillis: Option[Long] = None,
    recordedTimestamp: Option[Instant] = None
) extends ToSnakeCase[ImageSC] {
    override def toSnakeCase: ImageSC = ImageSC(
        imageReferenceUuid,
        videoReferenceUuid,
        imagedMomentUuid,
        format,
        width,
        height,
        url,
        description,
        timecode,
        elapsedTimeMillis,
        recordedTimestamp
    )
}

object Image extends FromEntity[ImageReferenceEntity, Image] {
    override def from(entity: ImageReferenceEntity, extend: Boolean = false): Image =
        val im = entity.imagedMoment // TODO: This may be null!!
        val (tc, etm, rt) = if extend then
            (Option(im.timecode).map(_.toString), 
            Option(im.elapsedTime).map(_.toMillis()), Option(im.recordedDate))
        else (None, None, None)

        Image(
            entity.uuid,
            im.videoReferenceUUID,
            im.uuid,
            Option(entity.format),
            Option(entity.width),
            Option(entity.height),
            Option(entity.url),
            Option(entity.description),
            tc,
            etm,
            rt
        )
}


final case class ImageSC(
    image_reference_uuid: UUID,
    video_reference_uuid: UUID,
    imaged_moment_uuid: UUID,
    format: Option[String] = None,
    width: Option[Int] = None,
    height: Option[Int] = None,
    url: Option[URL] = None,
    description: Option[String] = None,
    timecode: Option[String] = None,
    elapsed_time_millis: Option[Long] = None,
    recorded_timestamp: Option[Instant] = None
) extends ToCamelCase[Image] {
    override def toCamelCase: Image = Image(
        image_reference_uuid,
        video_reference_uuid,
        imaged_moment_uuid,
        format,
        width,
        height,
        url,
        description,
        timecode,
        elapsed_time_millis,
        recorded_timestamp
    )
}
