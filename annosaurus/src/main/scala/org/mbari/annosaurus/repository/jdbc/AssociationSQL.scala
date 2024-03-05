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
import org.mbari.annosaurus.domain.Association
import org.mbari.annosaurus.domain.Annotation

// @deprecated("Use Association's NamedQueries instead", "2023-12-18")
object AssociationSQL {

    def resultListToAssociations(rows: List[?]): Seq[Association] = {
        for {
            row <- rows
        } yield {
            val xs               = row.asInstanceOf[Array[Object]]
            val uuid             = xs(0).asUUID
            val observationUuid  = xs(1).asUUID
            val linkName         = xs(2).asString
            val toConcept        = xs(3).asString
            val linkValue        = xs(4).asString
            val mimeType         = xs(5).asString
            val imagedMomentUuid = xs(6).asUUID
            Association(
                linkName.getOrElse(""),
                toConcept.getOrElse(""),
                linkValue.getOrElse(""),
                mimeType,
                uuid,
                None,
                observationUuid,
                imagedMomentUuid
            )
            // val a  = new MutableAssociationExt
            // a.uuid = UUID.fromString(xs(0).toString)
            // a.observationUuid = UUID.fromString(xs(1).toString)
            // Option(xs(2)).foreach(v => a.linkName = v.toString)
            // Option(xs(3)).foreach(v => a.toConcept = v.toString)
            // Option(xs(4)).foreach(v => a.linkValue = v.toString)
            // Option(xs(5)).foreach(v => a.mimeType = v.toString)
            // // a.linkName = xs(2).toString
            // // a.toConcept = xs(3).toString
            // // a.linkValue = xs(4).toString
            // // a.mimeType = xs(5).toString
            // a.imagedMomentUuid = UUID.fromString(xs(6).toString)
            // a
        }
    }

    def join(
        annotations: Seq[Annotation],
        associations: Seq[Association]
    ): Seq[Annotation] =
        for a <- annotations
        yield {
            val matches = associations.filter(anno => anno.observationUuid == a.observationUuid)
            if (matches.isEmpty) a
            else a.copy(associations = a.associations.appendedAll(matches))
        }

    val SELECT: String =
        """ SELECT DISTINCT
      |  ass.uuid AS association_uuid,
      |  ass.observation_uuid,
      |  ass.link_name,
      |  ass.to_concept,
      |  ass.link_value,
      |  ass.mime_type,
      |  im.uuid AS imaged_moment_uuid
    """.stripMargin

    // LEFT JOIN Performance is TERRIBLE  on Derby
    // val FROM: String =
    //   """ FROM
    //     |  associations ass LEFT JOIN
    //     |  observations obs ON ass.observation_uuid = obs.uuid LEFT JOIN
    //     |  imaged_moments im ON obs.imaged_moment_uuid = im.uuid
    //   """.stripMargin

    val FROM: String =
        """FROM 
      |  imaged_moments im RIGHT JOIN
      |  observations obs ON obs.imaged_moment_uuid = im.uuid RIGHT JOIN
      |  associations ass ON ass.observation_uuid = obs.uuid""".stripMargin

    val ORDER: String = " ORDER BY ass.uuid"

    val byVideoReferenceUuid: String = SELECT + FROM + " WHERE im.video_reference_uuid = ?" + ORDER

    val byVideoReferenceUuidBetweenDates: String = SELECT + FROM +
        " WHERE im.video_reference_uuid = ? AND im.recorded_timestamp BETWEEN ? AND ? " + ORDER

    val byConcurrentRequest: String = SELECT + FROM +
        " WHERE im.video_reference_uuid IN (?) AND im.recorded_timestamp BETWEEN ? AND ?" + ORDER

    val byMultiRequest: String = SELECT + FROM + " WHERE im.video_reference_uuid IN (?)" + ORDER

    val byObservationUuids: String = SELECT + FROM + " WHERE obs.uuid IN (?)" + ORDER

    val byLinkNameAndLinkValue: String =
        SELECT + FROM + " WHERE link_name = ? AND link_value = ?" + ORDER

    val deleteByVideoReferenceUuid: String =
        """ DELETE FROM associations WHERE EXISTS (
      |   SELECT
      |     *
      |   FROM
      |     imaged_moments im RIGHT JOIN
      |     observations obs ON obs.imaged_moment_uuid = im.uuid
      |   WHERE
      |     im.video_reference_uuid = ? AND
      |     obs.uuid = associations.observation_uuid
      | )
      |""".stripMargin

}
