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
import org.mbari.annosaurus.repository.jpa.entity.ImageReferenceEntity

case class ImageReference(
    url: URL,
    format: Option[String] = None,
    widthPixels: Option[Int] = None,
    heightPixels: Option[Int] = None,
    description: Option[String] = None,
    uuid: Option[UUID] = None,
    lastUpdated: Option[java.time.Instant] = None,
    imagedMomentUuid: Option[UUID] = None
) extends ToSnakeCase[ImageReferenceSC]
    with ToEntity[ImageReferenceEntity] {
    override def toSnakeCase: ImageReferenceSC =
        ImageReferenceSC(
            url,
            format,
            widthPixels,
            heightPixels,
            description,
            uuid,
            lastUpdated,
            imagedMomentUuid
        )

    override def toEntity: ImageReferenceEntity =
        val entity = new ImageReferenceEntity
        entity.url = url
        format.foreach(entity.format = _)
        widthPixels.foreach(entity.width = _)
        heightPixels.foreach(entity.height = _)
        description.foreach(entity.description = _)
        uuid.foreach(entity.uuid = _)
        entity
}

object ImageReference extends FromEntity[ImageReferenceEntity, ImageReference] {
    override def from(entity: ImageReferenceEntity, extend: Boolean = false): ImageReference =
        val opt = if extend then entity.imagedMoment.primaryKey else None
        ImageReference(
            entity.url,
            Option(entity.format),
            Option(entity.width),
            Option(entity.height),
            Option(entity.description),
            Option(entity.uuid),
            entity.lastUpdated,
            opt
        )
}

case class ImageReferenceSC(
    url: URL,
    format: Option[String] = None,
    width_pixels: Option[Int] = None,
    height_pixels: Option[Int] = None,
    description: Option[String] = None,
    uuid: Option[UUID] = None,
    last_updated_time: Option[java.time.Instant] = None,
    imaged_moment_uuid: Option[UUID] = None
) extends ToCamelCase[ImageReference] {
    override def toCamelCase: ImageReference =
        ImageReference(
            url,
            format,
            width_pixels,
            height_pixels,
            description,
            uuid,
            last_updated_time,
            imaged_moment_uuid
        )
}
