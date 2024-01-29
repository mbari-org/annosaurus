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

import org.mbari.annosaurus.repository.jpa.entity.ImagedMomentEntity

import java.net.URL
import java.time.Instant
import java.util.UUID
import scala.jdk.CollectionConverters.*

case class ImageCreateSC(
    video_reference_uuid: UUID,
    url: URL,
    timecode: Option[String] = None,
    elapsed_time_millis: Option[Long] = None,
    recorded_timestamp: Option[Instant] = None,
    format: Option[String] = None,
    width_pixels: Option[Int] = None,
    height_pixels: Option[Int] = None,
    description: Option[String] = None
)

object ImageCreateSC extends FromEntity[ImagedMomentEntity, Seq[ImageCreateSC]]:
    override def from(entity: ImagedMomentEntity, extend: Boolean = false): Seq[ImageCreateSC] =
        for i <- entity.getImageReferences.asScala.toSeq
        yield ImageCreateSC(
            video_reference_uuid = entity.getVideoReferenceUuid,
            url = i.getUrl,
            timecode = Option(entity.getTimecode).map(_.toString),
            elapsed_time_millis = Option(entity.getElapsedTime).map(_.toMillis),
            recorded_timestamp = Option(entity.getRecordedTimestamp),
            format = Option(i.getFormat),
            width_pixels = Option(i.getWidth),
            height_pixels = Option(i.getHeight),
            description = Option(i.getDescription)
        )

    def fromAnnotation(a: Annotation): Seq[ImageCreateSC] =
        if a.videoReferenceUuid.isEmpty then Seq.empty
        else
            a.imageReferences
                .map(i =>
                    ImageCreateSC(
                        video_reference_uuid = a.videoReferenceUuid.get,
                        url = i.url,
                        timecode = a.timecode,
                        elapsed_time_millis = a.elapsedTimeMillis,
                        recorded_timestamp = a.recordedTimestamp,
                        format = i.format,
                        width_pixels = i.widthPixels,
                        height_pixels = i.heightPixels,
                        description = i.description
                    )
                )
