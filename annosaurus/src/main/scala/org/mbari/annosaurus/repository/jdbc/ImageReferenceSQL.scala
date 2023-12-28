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

package org.mbari.annosaurus.repository.jdbc

import java.util.UUID
import java.net.URI
import org.mbari.annosaurus.domain.ImageReference
import org.mbari.annosaurus.domain.Annotation

object ImageReferenceSQL {
    val SELECT: String =
        """ SELECT DISTINCT
      |  ir.uuid AS image_reference_uuid,
      |  ir.description,
      |  ir.format,
      |  ir.height_pixels,
      |  ir.url,
      |  ir.width_pixels,
      |  ir.imaged_moment_uuid
    """.stripMargin

    val FROM: String =
        """ FROM
      |  image_references ir LEFT JOIN
      |  imaged_moments im ON ir.imaged_moment_uuid = im.uuid
    """.stripMargin

    val ORDER: String = " ORDER BY ir.uuid"

    val byVideoReferenceUuid: String = SELECT + FROM + " WHERE im.video_reference_uuid = ?" + ORDER

    val countByVideoReferenceUuid: String =
        "SELECT COUNT(DISTINCT ir.uuid) " + FROM + " WHERE im.video_reference_uuid = ?"

    val byVideoReferenceUuidBetweenDates: String = SELECT + FROM +
        " WHERE im.video_reference_uuid = ? AND im.recorded_timestamp BETWEEN ? AND ? " + ORDER

    val byConcurrentRequest: String = SELECT + FROM +
        " WHERE im.video_reference_uuid IN (?) AND im.recorded_timestamp BETWEEN ? AND ?" + ORDER

    val byMultiRequest: String = SELECT + FROM + " WHERE im.video_reference_uuid IN (?)" + ORDER

    val byImagedMomentUuids: String = SELECT + FROM + " WHERE im.uuid IN (?)" + ORDER

    val deleteByVideoReferenceUuid: String =
        """ DELETE FROM image_references WHERE EXISTS (
      |   SELECT
      |     *
      |   FROM
      |     imaged_moments im
      |   WHERE
      |     im.video_reference_uuid = ? AND
      |     im.uuid = image_references.imaged_moment_uuid
      | )
      |""".stripMargin

    def resultListToImageReferences(rows: List[_]): Seq[ImageReference] = {
        for {
            row <- rows
        } yield {
            val xs = row.asInstanceOf[Array[Object]]
            ImageReference(
                url = xs(4).asUrl.orNull,
                format = xs(2).asString,
                widthPixels = xs(5).asInt,
                heightPixels = xs(3).asInt,
                description = xs(1).asString,
                uuid = xs(0).asUUID,
                imagedMomentUuid = xs(6).asUUID
            )

        }
    }

    def join(
        annotations: Seq[Annotation],
        images: Seq[ImageReference]
    ): Seq[Annotation] = {
        val mergedAnnos = for {
            i <- images
        } yield {
            annotations
                .filter(anno => anno.imagedMomentUuid == i.imagedMomentUuid)
                .map(anno => anno.copy(imageReferences = anno.imageReferences :+ i))

        }
        mergedAnnos.flatten
    }

}
