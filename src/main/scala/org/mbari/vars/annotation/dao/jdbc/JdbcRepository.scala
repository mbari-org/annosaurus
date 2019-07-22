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

import java.sql.{ResultSet, Timestamp}
import java.time.Duration
import java.util.UUID

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.reactivex.{Scheduler, Single}
import javax.persistence.EntityManager
import javax.sql.DataSource
import org.mbari.vars.annotation.dao.jpa.AnnotationImpl
import org.mbari.vars.annotation.model.Annotation
import org.mbari.vcr4j.time.Timecode

import scala.collection.JavaConverters._


class JdbcRepository(entityManager: EntityManager) {

  def findByVideoReferenceUuid(videoReferenceUuid: UUID,
                               limit: Option[Int] = None,
                               offset: Option[Int]): Seq[Annotation] = {

    // Fetch annotations
    val q0 = entityManager.createNativeQuery(AnnotationSQL.byVideoReferenceUuid)
    q0.setParameter(1, videoReferenceUuid)
    limit.foreach(q0.setMaxResults)
    limit.foreach(q0.setFirstResult)
    val r0 = q0.getResultList.asScala.toList
    val annotations = AnnotationSQL.resultListToAnnotations(r0)

    // Fetch association

    annotations

  }



}

object AnnotationSQL {

  def resultListToAnnotations(rows: List[_]): Seq[Annotation] = {
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

//  def resultSetToAnnotations(rs: ResultSet): Seq[Annotation] = {
//    val annotations = new mutable.ArrayBuffer[Annotation]
//    while (rs.next()) {
//      val a = new AnnotationImpl
//      a.imagedMomentUuid = UUID.fromString(rs.getString(1))
//      a.videoReferenceUuid = UUID.fromString(rs.getString(2))
//      Option(rs.getLong(3))
//        .map(Duration.ofMillis)
//        .foreach(v => a.elapsedTime = v)
//      Option(rs.getTimestamp(4))
//        .map(v => v.toInstant)
//        .foreach(v => a.recordedTimestamp = v)
//      Option(rs.getString(5))
//        .map(v => new Timecode(v))
//        .foreach(v => a.timecode = v)
//      a.observationUuid = UUID.fromString(rs.getString(6))
//      a.concept = rs.getString(7)
//      a.activity = rs.getString(8)
//      Option(rs.getLong(9))
//        .map(Duration.ofMillis)
//        .foreach(v => a.duration = v)
//      a.group = rs.getString(10)
//      a.observationTimestamp = rs.getTimestamp(11).toInstant
//      a.observer = rs.getString(12)
//      annotations += a
//    }
//    annotations
//  }

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

  val all: String = SELECT + FROM

  val byVideoReferenceUuid: String = SELECT + FROM + " WHERE im.video_reference_uuid = ?"

  val byConcept: String = SELECT + FROM + " WHERE obs.concept = ?"

  val byConceptWithImages: String = SELECT + FROM_WITH_IMAGES +
    " WHERE ir.url IS NOT NULL AND obs.concept = ?"

  val betweenDates: String = SELECT + FROM +
    " WHERE im.recorded_timestamp BETWEEN ? AND ?"

  val byVideoReferenceUuidBetweenDates: String = SELECT + FROM +
    " WHERE im.video_reference_uuid = ? AND im.recorded_timestamp BETWEEN ? AND ? "

}

object AssociationSQL {
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

  var FROM: String =
    """ FROM
      |  image_references ir RIGHT JOIN
      |  observations obs ON ir.observation_uuid = obs.uuid RIGHT JOIN
      |  imaged_moments im ON obs.imaged_moment_uuid = im.uuid
    """.stripMargin

  val byVideoReferenceUuid: String = SELECT + FROM + " WHERE im.video_reference_uuid = ?"

}

object JdbcRepository {


  val DataSource: DataSource = {
    val config = ConfigFactory.load()
    val environment = config.getString("database.environment")
    val nodeName = if (environment.equalsIgnoreCase("production")) "org.mbari.vars.annotation.database.production"
    else "org.mbari.vars.annotation.database.development"
    val hikariConfig = new HikariConfig
    val url = config.getString(nodeName + ".url")
    val user = config.getString(nodeName + ".user")
    val password = config.getString(nodeName + ".password")
    hikariConfig.setJdbcUrl(url)
    hikariConfig.setUsername(user)
    hikariConfig.setPassword(password)
    hikariConfig.setMaximumPoolSize(Runtime.getRuntime.availableProcessors * 2)

    new HikariDataSource(hikariConfig)
  }
}
