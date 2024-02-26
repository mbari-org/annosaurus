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
import java.time.{Duration, Instant}
import org.mbari.annosaurus.repository.jpa.entity.ImageReferenceEntity
import extensions.*

final case class Image(
                          imageReferenceUuid: UUID,
                          videoReferenceUuid: UUID,
                          imagedMomentUuid: UUID,
                          format: Option[String] = None,
                          widthPixels: Option[Int] = None,
                          heightPixels: Option[Int] = None,
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
        widthPixels,
        heightPixels,
        url,
        description,
        timecode,
        elapsedTimeMillis,
        recordedTimestamp
    )

    lazy val elapsedTime: Option[Duration] = elapsedTimeMillis.map(Duration.ofMillis)
}

object Image extends FromEntity[ImageReferenceEntity, Image] {
    override def from(entity: ImageReferenceEntity, extend: Boolean = false): Image =
        val im            = entity.getImagedMoment // TODO: This may be null!!
        val (tc, etm, rt) =
            if extend then
                (
                    Option(im.getTimecode).map(_.toString),
                    Option(im.getElapsedTime).map(_.toMillis()),
                    Option(im.getRecordedTimestamp)
                )
            else (None, None, None)

        Image(
            entity.getUuid,
            im.getVideoReferenceUuid,
            im.getUuid,
            Option(entity.getFormat),
            entity.getWidth.toOption,
            entity.getHeight.toOption,
            Option(entity.getUrl),
            Option(entity.getDescription),
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
                            width_pixels: Option[Int] = None,
                            height_pixels: Option[Int] = None,
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
        width_pixels,
        height_pixels,
        url,
        description,
        timecode,
        elapsed_time_millis,
        recorded_timestamp
    )
}
