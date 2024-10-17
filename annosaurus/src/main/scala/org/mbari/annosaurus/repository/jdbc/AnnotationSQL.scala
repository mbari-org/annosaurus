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

import org.mbari.annosaurus.domain.Annotation

import java.time.Instant
import java.util.UUID

/**
 * Object that contains the SQL and methods to build annotations
 */
object AnnotationSQL:

    def resultListToAnnotations(rows: List[?]): Seq[Annotation] =
        for row <- rows
        yield
            val xs = row.asInstanceOf[Array[Object]]
            Annotation(
                imagedMomentUuid = xs(0).asUUID,
                videoReferenceUuid = xs(1).asUUID,
                elapsedTimeMillis = xs(2).asLong,
                recordedTimestamp = xs(3).asInstant,
                timecode = xs(4).asString,
                observationUuid = xs(5).asUUID,
                concept = xs(6).asString,
                activity = xs(7).asString,
                durationMillis = xs(8).asLong,
                group = xs(9).asString,
                observationTimestamp = xs(10).asInstant,
                observer = xs(11).asString
            )

    val SELECT: String =
        """ SELECT DISTINCT
      |  im.uuid AS imaged_moment_uuid,
      |  im.video_reference_uuid,
      |  im.elapsed_time_millis,
      |  im.recorded_timestamp,
      |  im.timecode,
      |  obs.uuid AS observation_uuid,
      |  obs.concept,
      |  obs.activity,
      |  obs.duration_millis,
      |  obs.observation_group,
      |  obs.observation_timestamp,
      |  obs.observer """.stripMargin

    val FROM: String =
        """ FROM
      |  imaged_moments im RIGHT JOIN
      |  observations obs ON obs.imaged_moment_uuid = im.uuid LEFT JOIN
      |  video_reference_information vri ON vri.video_reference_uuid = im.video_reference_uuid""".stripMargin

    val FROM_WITH_IMAGES: String =
        """ FROM
      |  imaged_moments im RIGHT JOIN
      |  observations obs ON obs.imaged_moment_uuid = im.uuid LEFT JOIN
      |  image_references ir ON ir.imaged_moment_uuid = im.uuid LEFT JOIN
      |  video_reference_information vri ON vri.video_reference_uuid = im.video_reference_uuid""".stripMargin

    val FROM_WITH_IMAGES_AND_ASSOCIATIONS: String =
        """ FROM
      |  imaged_moments im RIGHT JOIN
      |  observations obs ON obs.imaged_moment_uuid = im.uuid LEFT JOIN
      |  image_references ir ON ir.imaged_moment_uuid = im.uuid RIGHT JOIN
      |  associations ass ON ass.observation_uuid = obs.uuid LEFT JOIN
      |  video_reference_information vri ON vri.video_reference_uuid = im.video_reference_uuid""".stripMargin

    val FROM_WITH_ANCILLARY_DATA: String =
        """ FROM
      |  imaged_moments im RIGHT JOIN
      |  observations obs ON obs.imaged_moment_uuid = im.uuid LEFT JOIN
      |  ancillary_data ad ON ad.imaged_moment_uuid = im.uuid LEFT JOIN
      |  associations ass ON ass.observation_uuid = obs.uuid LEFT JOIN
      |  video_reference_information vri ON vri.video_reference_uuid = im.video_reference_uuid
      |""".stripMargin

    val ORDER: String = " ORDER BY obs.uuid"

    val all: String = SELECT + FROM + ORDER

    val byVideoReferenceUuid: String = SELECT + FROM + " WHERE im.video_reference_uuid = ?" + ORDER

    val byConcept: String = SELECT + FROM + " WHERE obs.concept = ?" + ORDER

    val byConcepts: String = SELECT + FROM + " WHERE obs.concept IN (?)" + ORDER

    val byConceptWithImages: String = SELECT + FROM_WITH_IMAGES +
        " WHERE ir.url IS NOT NULL AND obs.concept = ?" + ORDER

    val betweenDates: String = SELECT + FROM +
        " WHERE im.recorded_timestamp BETWEEN ? AND ?" + ORDER

    val byVideoReferenceUuidBetweenDates: String = SELECT + FROM +
        " WHERE im.video_reference_uuid = ? AND im.recorded_timestamp BETWEEN ? AND ? " + ORDER

    val byConcurrentRequest: String = SELECT + FROM +
        " WHERE im.video_reference_uuid IN (?) AND im.recorded_timestamp BETWEEN ? AND ? " + ORDER

    val byMultiRequest: String = SELECT + FROM + " WHERE im.video_reference_uuid IN (?) " + ORDER

    val byImagedMomentUuids: String = SELECT + FROM + " WHERE im.uuid IN (?) " + ORDER

    val byToConceptWithImages: String =
        SELECT + FROM_WITH_IMAGES_AND_ASSOCIATIONS + " WHERE ass.to_concept = ?" + ORDER
