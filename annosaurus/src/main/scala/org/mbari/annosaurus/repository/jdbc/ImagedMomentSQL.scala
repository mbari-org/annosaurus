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

import org.mbari.annosaurus.domain.Image

object ImagedMomentSQL:

    /** recorded_timestamp is used for sorting, but not returned in the result set */
    val SELECT_UUID: String = "SELECT DISTINCT im.uuid, im.recorded_timestamp "

    val SELECT_IMAGES: String =
        """SELECT DISTINCT
      |  im.uuid AS imaged_moment_uuid,
      |  im.video_reference_uuid,
      |  im.elapsed_time_millis,
      |  im.recorded_timestamp,
      |  im.timecode,
      |  ir.description,
      |  ir.format,
      |  ir.height_pixels,
      |  ir.width_pixels,
      |  ir.url,
      |  ir.uuid as image_reference_uuid
      |""".stripMargin

    val FROM: String =
        """FROM
      | imaged_moments im LEFT JOIN
      | observations obs ON obs.imaged_moment_uuid = im.uuid LEFT JOIN
      | image_references ir ON ir.imaged_moment_uuid = im.uuid
      |""".stripMargin

    val FROM_WITH_IMAGES_AND_ASSOCIATIONS: String =
        """ FROM
      |  imaged_moments im LEFT JOIN
      |  observations obs ON obs.imaged_moment_uuid = im.uuid LEFT JOIN
      |  image_references ir ON ir.imaged_moment_uuid = im.uuid RIGHT JOIN
      |  associations ass ON ass.observation_uuid = obs.uuid""".stripMargin

    val byConceptWithImages: String = SELECT_UUID + FROM +
        " WHERE concept = ? AND ir.url IS NOT NULL ORDER BY im.recorded_timestamp, im.uuid"

    val byToConceptWithImages: String = SELECT_UUID +
        FROM_WITH_IMAGES_AND_ASSOCIATIONS +
        " WHERE ass.to_concept = ? AND ir.url IS NOT NULL ORDER BY im.recorded_timestamp, im.uuid"

    val byVideoReferenceUuid: String =
        SELECT_IMAGES + FROM + " WHERE im.video_reference_uuid = ? AND ir.url IS NOT NULL ORDER BY im.recorded_timestamp, image_reference_uuid"

    val deleteByVideoReferenceUuid: String =
        "DELETE FROM imaged_moments WHERE video_reference_uuid = ?"

    def resultListToImages(rows: List[?]): Seq[Image] =
        for row <- rows
        yield
            val xs = row.asInstanceOf[Array[Object]]
            Image(
                imagedMomentUuid = xs(0).asUUID.orNull,
                videoReferenceUuid = xs(1).asUUID.orNull,
                elapsedTimeMillis = xs(2).asLong,
                recordedTimestamp = xs(3).asInstant,
                timecode = xs(4).asString,
                description = xs(5).asString,
                format = xs(6).asString,
                heightPixels = xs(7).asInt,
                widthPixels = xs(8).asInt,
                url = xs(9).asUrl,
                imageReferenceUuid = xs(10).asUUID.orNull
            )
