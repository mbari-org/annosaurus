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

import java.time.Duration
import java.util.UUID
import java.sql.Timestamp
import org.mbari.vcr4j.time.Timecode
import org.mbari.annosaurus.domain.Annotation
import java.time.Instant

/** Object that contains the SQL and methods to build annotations
  */
object AnnotationSQL {

    def instantConverter(obj: Object): Option[Instant] =
        obj match
            case null => None
            case ts: Timestamp => Some(ts.toInstant)
            case m: microsoft.sql.DateTimeOffset => Some(m.getOffsetDateTime().toInstant())
            case _ => None


        


    def resultListToAnnotations(rows: List[_]): Seq[Annotation] = {
        for {
            row <- rows
        } yield {
            val xs = row.asInstanceOf[Array[Object]]
            Annotation(
                imagedMomentUuid = Some(UUID.fromString(xs(0).toString)),
                videoReferenceUuid = Some(UUID.fromString(xs(1).toString)),
                elapsedTimeMillis = Option(xs(2))
                    .map(v => v.asInstanceOf[Number].longValue()),
                recordedTimestamp = instantConverter(xs(3)),
                // recordedTimestamp = Option(xs(3))
                //     .map(v => v.asInstanceOf[Timestamp])
                //     .map(v => v.toInstant),
                timecode = Option(xs(4)).map(_.toString()),
                observationUuid = Some(UUID.fromString(xs(5).toString)),
                concept = Option(xs(6)).map(_.toString),
                activity = Option(xs(7)).map(_.toString()),
                durationMillis = Option(xs(8))
                    .map(v => v.asInstanceOf[Number].longValue()),
                group = Option(xs(9)).map(_.toString()),
                observationTimestamp = instantConverter(xs(10)),
                // observationTimestamp = Option(xs(10))
                //     .map(v => v.asInstanceOf[Timestamp])
                //     .map(v => v.toInstant),
                observer = Option(xs(11)).map(_.toString())
            )

            // ORIGINAL CODE
            // val a  = new MutableAnnotationExt
            // a.imagedMomentUuid = UUID.fromString(xs(0).toString)
            // a.videoReferenceUuid = UUID.fromString(xs(1).toString)
            // Option(xs(2))
            //     .map(v => v.asInstanceOf[Number].longValue())
            //     .map(Duration.ofMillis)
            //     .foreach(v => a.elapsedTime = v)
            // Option(xs(3))
            //     .map(v => v.asInstanceOf[Timestamp])
            //     .map(v => v.toInstant)
            //     .foreach(v => a.recordedTimestamp = v)
            // Option(xs(4))
            //     .map(v => v.toString)
            //     .map(v => new Timecode(v))
            //     .foreach(v => a.timecode = v)
            // a.observationUuid = UUID.fromString(xs(5).toString)
            // a.concept = xs(6).toString
            // Option(xs(7)).foreach(v => a.activity = v.toString)
            // Option(xs(8))
            //     .map(v => v.asInstanceOf[Number].longValue())
            //     .map(Duration.ofMillis)
            //     .foreach(v => a.duration = v)
            // Option(xs(9)).foreach(v => a.group = v.toString)
            // Option(xs(10)).foreach(v =>
            //     a.observationTimestamp = v.asInstanceOf[Timestamp].toInstant
            // )
            // // a.observationTimestamp = xs(10).asInstanceOf[Timestamp].toInstant
            // a.observer = xs(11).toString
            // a
        }
    }

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

}
