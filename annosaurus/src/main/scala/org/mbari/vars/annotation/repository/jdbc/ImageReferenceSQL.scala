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

package org.mbari.vars.annotation.repository.jdbc

import java.net.URL
import java.util.UUID

import org.mbari.vars.annotation.repository.jpa.AnnotationImpl
import org.mbari.vars.annotation.model.Annotation
import java.net.URI

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

  val countByVideoReferenceUuid: String = "SELECT COUNT(DISTINCT ir.uuid) " + FROM + " WHERE im.video_reference_uuid = ?"

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

  def resultListToImageReferences(rows: List[_]): Seq[ImageReferenceExt] = {
    for {
      row <- rows
    } yield {
      val xs = row.asInstanceOf[Array[Object]]
      val i  = new ImageReferenceExt
      i.uuid = UUID.fromString(xs(0).toString)
      Option(xs(1))
        .map(_.toString)
        .foreach(v => i.description = v)
      Option(xs(2))
        .map(_.toString)
        .foreach(v => i.format = v)
      Option(xs(3))
        .map(_.asInstanceOf[Number].intValue())
        .foreach(v => i.height = v)
      i.url = URI.create(xs(4).toString).toURL
      Option(xs(5))
        .map(_.asInstanceOf[Number].intValue())
        .foreach(v => i.width = v)
      i.imagedMomentUuid = UUID.fromString(xs(6).toString)
      i
    }
  }

  def join(annotations: Seq[AnnotationImpl], images: Seq[ImageReferenceExt]): Seq[Annotation] = {
    for {
      i <- images
    } {
      annotations
        .filter(anno => anno.imagedMomentUuid == i.imagedMomentUuid)
        .foreach(anno => anno.javaImageReferences.add(i))
    }
    annotations
  }

}
