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

package org.mbari.vars.annotation.dao.jdbc

import java.util.UUID

import org.mbari.vars.annotation.dao.jpa.AnnotationImpl
import org.mbari.vars.annotation.model.Annotation


object AssociationSQL {

  def resultListToAssociations(rows: List[_]): Seq[AssociationExt] = {
    for {
      row <- rows
    } yield {
      val xs = row.asInstanceOf[Array[Object]]
      val a = new AssociationExt
      a.uuid = UUID.fromString(xs(0).toString)
      a.observationUuid = UUID.fromString(xs(1).toString)
      a.linkName = xs(2).toString
      a.toConcept = xs(3).toString
      a.linkValue = xs(4).toString
      a.mimeType = xs(5).toString
      a.imagedMomentUuid = UUID.fromString(xs(6).toString)
      a
    }
  }

  def join(annotations: Seq[AnnotationImpl], associations: Seq[AssociationExt]): Seq[Annotation] = {
    for {
      a <- associations
    } {
      annotations.find(anno => anno.observationUuid == a.observationUuid) match {
        case None =>
        // TODO warn of missing match?
        case Some(anno) =>
          anno.javaAssociations.add(a)
      }
    }
    annotations
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

  val FROM: String =
    """ FROM
      |  associations ass LEFT JOIN
      |  observations obs ON ass.observation_uuid = obs.uuid LEFT JOIN
      |  imaged_moments im ON obs.imaged_moment_uuid = im.uuid
    """.stripMargin

  val ORDER: String = " ORDER BY ass.uuid"

  val byVideoReferenceUuid: String = SELECT + FROM + " WHERE im.video_reference_uuid = ?" + ORDER

  val byVideoReferenceUuidBetweenDates: String = SELECT + FROM +
    " WHERE im.video_reference_uuid = ? AND im.recorded_timestamp BETWEEN ? AND ? " + ORDER

  val byConcurrentRequest: String = SELECT + FROM +
    " WHERE im.video_reference_uuid IN (?) AND im.recorded_timestamp BETWEEN ? AND ?" + ORDER

  val byMultiRequest: String = SELECT + FROM + " WHERE im.video_reference_uuid IN (?)" + ORDER

  val byObservationUuids: String = SELECT + FROM + " WHERE obs.uuid IN (?)" + ORDER

  val byLinkNameAndLinkValue: String = SELECT + FROM + " WHERE link_name = ? AND link_value = ?" + ORDER

}