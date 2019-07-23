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

import java.net.URL
import java.sql.{ResultSet, Timestamp}
import java.time.Duration
import java.util.UUID

import com.google.gson.annotations.Expose
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.reactivex.{Scheduler, Single}
import javax.persistence.EntityManager
import javax.sql.DataSource
import org.mbari.vars.annotation.dao.jpa.{AnnotationImpl, AssociationImpl, ImageReferenceImpl}
import org.mbari.vars.annotation.model.{Annotation, Association}
import org.mbari.vcr4j.time.Timecode
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._


class JdbcRepository(entityManager: EntityManager) {

  private[this] val log = LoggerFactory.getLogger(getClass)

  def findByVideoReferenceUuid(videoReferenceUuid: UUID,
                               limit: Option[Int] = None,
                               offset: Option[Int]): Seq[AnnotationImpl] = {

    // Fetch annotations
    log.debug(s"Running:\n${AnnotationSQL.byVideoReferenceUuid}")
    val q0 = entityManager.createNativeQuery(AnnotationSQL.byVideoReferenceUuid)
    q0.setParameter(1, videoReferenceUuid)
    limit.foreach(q0.setMaxResults)
    limit.foreach(q0.setFirstResult)
    val r0 = q0.getResultList.asScala.toList
    val annotations = AnnotationSQL.resultListToAnnotations(r0)

    // Fetch association
    log.debug(s"Running:\n${AssociationSQL.byVideoReferenceUuid}")
    val q1 = entityManager.createNativeQuery(AssociationSQL.byVideoReferenceUuid)
    q1.setParameter(1, videoReferenceUuid)
    val r1 = q1.getResultList.asScala.toList
    val associations = AssociationSQL.resultListToAssociations(r1)
    AssociationSQL.join(annotations, associations)

    // Fetch Image References
    log.debug(s"Running:\n${ImageReferenceSQL.byVideoReferenceUuid}")
    val q2 = entityManager.createNativeQuery(ImageReferenceSQL.byVideoReferenceUuid)
    q2.setParameter(1, videoReferenceUuid)
    val r2 = q2.getResultList.asScala.toList
    val imageReferences = ImageReferenceSQL.resultListToImageReferences(r2)
    ImageReferenceSQL.join(annotations, imageReferences)

    annotations

  }



}

object AnnotationSQL {

  def resultListToAnnotations(rows: List[_]): Seq[AnnotationImpl] = {
    for {
      row <- rows
    } yield {
      val xs = row.asInstanceOf[Array[Object]]
      val a = new AnnotationImpl
      a.imagedMomentUuid = UUID.fromString(xs(0).toString)
      a.videoReferenceUuid = UUID.fromString(xs(1).toString)
      Option(xs(2))
        .map(v => v.asInstanceOf[Long])
        .map(Duration.ofMillis)
        .foreach(v => a.elapsedTime = v)
      Option(xs(3))
        .map(v => v.asInstanceOf[Timestamp])
        .map(v => v.toInstant)
        .foreach(v => a.recordedTimestamp = v)
      Option(xs(4))
        .map(v => v.toString)
        .map(v => new Timecode(v))
        .foreach(v => a.timecode = v)
      a.observationUuid = UUID.fromString(xs(5).toString)
      a.concept = xs(6).toString
      a.activity = xs(7).toString
      Option(xs(8))
        .map(v => v.asInstanceOf[Long])
        .map(Duration.ofMillis)
        .foreach(v => a.duration = v)
      a.group = xs(9).toString
      a.observationTimestamp = xs(10).asInstanceOf[Timestamp].toInstant
      a.observer = xs(11).toString
      a
    }
  }

  val SELECT: String =
    """ SELECT
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
      |  imaged_moments im LEFT JOIN
      |  observations obs ON obs.imaged_moment_uuid = im.uuid """.stripMargin

  val FROM_WITH_IMAGES: String =
    """ FROM
      |  imaged_moments im LEFT JOIN
      |  observations obs ON obs.imaged_moment_uuid = im.uuid LEFT JOIN
      |  image_references ir ON ir.imaged_moment_uuid = im.uuid """.stripMargin

  val ORDER: String = " ORDER BY im.uuid"

  val all: String = SELECT + FROM + ORDER

  val byVideoReferenceUuid: String = SELECT + FROM + " WHERE im.video_reference_uuid = ?" + ORDER

  val byConcept: String = SELECT + FROM + " WHERE obs.concept = ?" + ORDER

  val byConceptWithImages: String = SELECT + FROM_WITH_IMAGES +
    " WHERE ir.url IS NOT NULL AND obs.concept = ?" + ORDER

  val betweenDates: String = SELECT + FROM +
    " WHERE im.recorded_timestamp BETWEEN ? AND ?" + ORDER

  val byVideoReferenceUuidBetweenDates: String = SELECT + FROM +
    " WHERE im.video_reference_uuid = ? AND im.recorded_timestamp BETWEEN ? AND ? " + ORDER

}

class AssociationExt extends AssociationImpl {
//  @Expose(serialize = true)
  var observationUuid: UUID = _
}

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
    """ SELECT
      |  ass.uuid AS association_uuid,
      |  ass.observation_uuid,
      |  ass.link_name,
      |  ass.to_concept,
      |  ass.link_value,
      |  ass.mime_type
    """.stripMargin

  val FROM: String =
    """ FROM
      |  associations ass RIGHT JOIN
      |  observations obs ON ass.observation_uuid = obs.uuid RIGHT JOIN
      |  imaged_moments im ON obs.imaged_moment_uuid = im.uuid
    """.stripMargin

  val byVideoReferenceUuid: String = SELECT + FROM + " WHERE im.video_reference_uuid = ?"
}

class ImageReferenceExt extends ImageReferenceImpl {
  var imagedMomentUuid: UUID = _
}

object ImageReferenceSQL {
  val SELECT: String =
    """ SELECT
      |  ir.uuid AS image_reference_uuid
      |  ir.description,
      |  ir.format,
      |  ir.height_pixels,
      |  ir.url,
      |  ir.width_pixels,
      |  ir.imaged_moment_uuid
    """.stripMargin

  val FROM: String =
    """ FROM
      |  image_references ir RIGHT JOIN
      |  observations obs ON ir.observation_uuid = obs.uuid RIGHT JOIN
      |  imaged_moments im ON obs.imaged_moment_uuid = im.uuid
    """.stripMargin

  val byVideoReferenceUuid: String = SELECT + FROM + " WHERE im.video_reference_uuid = ?"

  def resultListToImageReferences(rows: List[_]): Seq[ImageReferenceExt] = {
    for {
      row <- rows
    } yield {
      val xs = row.asInstanceOf[Array[Object]]
      val i = new ImageReferenceExt
      i.uuid = UUID.fromString(xs(0).toString)
      Option(xs(1)).map(_.toString).foreach(v => i.description = v)
      Option(xs(2)).map(_.toString).foreach(v => i.format = v)
      Option(xs(3)).map(_.asInstanceOf[Int]).foreach(v => i.height = v)
      i.url =  new URL(xs(4).toString)
      Option(xs(5)).map(_.asInstanceOf[Int]).foreach(v => i.width = v)
      i.imagedMomentUuid = UUID.fromString(xs(6).toString)
      i
    }
  }

  def join(annotations: Seq[AnnotationImpl], images: Seq[ImageReferenceExt]): Seq[Annotation] = {
    for {
      i <- images
    } {

      annotations.find(anno => anno.imagedMomentUuid == i.imagedMomentUuid) match {
        case None =>
        // TODO warn of missing match?
        case Some(anno) =>
          anno.javaImageReferences.add(i)
      }
    }
    annotations
  }

}

//object JdbcRepository {
//
//
//  val DataSource: DataSource = {
//    val config = ConfigFactory.load()
//    val environment = config.getString("database.environment")
//    val nodeName = if (environment.equalsIgnoreCase("production")) "org.mbari.vars.annotation.database.production"
//    else "org.mbari.vars.annotation.database.development"
//    val hikariConfig = new HikariConfig
//    val url = config.getString(nodeName + ".url")
//    val user = config.getString(nodeName + ".user")
//    val password = config.getString(nodeName + ".password")
//    hikariConfig.setJdbcUrl(url)
//    hikariConfig.setUsername(user)
//    hikariConfig.setPassword(password)
//    hikariConfig.setMaximumPoolSize(Runtime.getRuntime.availableProcessors * 2)
//
//    new HikariDataSource(hikariConfig)
//  }
//}
