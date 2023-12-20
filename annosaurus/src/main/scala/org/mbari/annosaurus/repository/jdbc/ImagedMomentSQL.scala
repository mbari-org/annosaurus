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
import java.util.UUID
import java.time.Duration
import java.sql.Timestamp
import org.mbari.vcr4j.time.Timecode
import java.net.URI

object ImagedMomentSQL {

    val SELECT_UUID: String = "SELECT DISTINCT im.uuid "

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
        " WHERE concept = ? AND ir.url IS NOT NULL ORDER BY im.uuid"

    val byToConceptWithImages: String = SELECT_UUID +
        FROM_WITH_IMAGES_AND_ASSOCIATIONS +
        " WHERE ass.to_concept = ? AND ir.url IS NOT NULL ORDER BY im.uuid"

    val byVideoReferenceUuid: String =
        SELECT_IMAGES + FROM + " WHERE im.video_reference_uuid = ? AND ir.url IS NOT NULL ORDER BY im.recorded_timestamp, image_reference_uuid"

    val deleteByVideoReferenceUuid: String =
        "DELETE FROM imaged_moments WHERE video_reference_uuid = ?"

    def resultListToImages(rows: List[_]): Seq[Image] = {
        for {
            row <- rows
        } yield {
            val xs = row.asInstanceOf[Array[Object]]
            Image(
                imagedMomentUuid = UUID.fromString(xs(0).toString()),
                videoReferenceUuid = UUID.fromString(xs(1).toString()),
                elapsedTimeMillis = Option(xs(2))
                    .map(v => v.asInstanceOf[Number].longValue()),
                recordedTimestamp = Option(xs(3))
                    .map(v => v.asInstanceOf[Timestamp])
                    .map(v => v.toInstant),
                timecode = Option(xs(4))
                    .map(v => v.toString),
                description = Option(xs(5)).map(v => v.toString),
                format = Option(xs(6)).map(v => v.toString),
                height = Option(xs(7)).map(v => v.asInstanceOf[Number].intValue()),
                width = Option(xs(8)).map(v => v.asInstanceOf[Number].intValue()),
                url = Option(xs(9)).map(x => URI.create(x.toString()).toURL),
                imageReferenceUuid = UUID.fromString(xs(10).toString())
            )

            // val i  = new Image;
            // i.imagedMomentUuid = UUID.fromString(xs(0).toString)
            // i.videoReferenceUuid = UUID.fromString(xs(1).toString)
            // Option(xs(2))
            //     .map(v => v.asInstanceOf[Number].longValue())
            //     .map(Duration.ofMillis)
            //     .foreach(v => i.elapsedTime = v)
            // Option(xs(3))
            //     .map(v => v.asInstanceOf[Timestamp])
            //     .map(v => v.toInstant)
            //     .foreach(v => i.recordedTimestamp = v)
            // Option(xs(4))
            //     .map(v => v.toString)
            //     .map(v => new Timecode(v))
            //     .foreach(v => i.timecode = v)
            // Option(xs(5)).foreach(v => i.description = v.toString)
            // Option(xs(6)).foreach(v => i.format = v.toString)
            // Option(xs(7)).foreach(v => i.height = v.asInstanceOf[Number].intValue())
            // Option(xs(8)).foreach(v => i.width = v.asInstanceOf[Number].intValue())
            // i.url = URI.create(xs(9).toString).toURL
            // i.imageReferenceUuid = UUID.fromString(xs(10).toString)
            // i
        }
    }

}
