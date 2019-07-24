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
import java.sql.Timestamp
import java.time.{Duration, Instant}
import java.util.UUID

import javax.persistence.{EntityManagerFactory, Query}
import org.mbari.vars.annotation.dao.jpa.{AnnotationImpl, AssociationImpl, ImageReferenceImpl}
import org.mbari.vars.annotation.model.Annotation
import org.mbari.vars.annotation.model.simple.{ConcurrentRequest, MultiRequest}
import org.mbari.vcr4j.time.Timecode
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
 * Database access (read-only) provider that uses SQL for fast lookups. WHY?
  * JPA makes about 1 + (rows * 4) database requests when looking up annotations.
  * For 1000 rows that 4001 database calls which is very SLOW!!. With SQL we can
  * fetch annotations for a video using 3 database queries which is so amazingly
  * fast compared to JPA.
  * @param entityManagerFactory
  */
class JdbcRepository(entityManagerFactory: EntityManagerFactory) {


  private[this] val log = LoggerFactory.getLogger(getClass)

  def findByVideoReferenceUuid(videoReferenceUuid: UUID,
                               limit: Option[Int] = None,
                               offset: Option[Int]): Seq[AnnotationImpl] = {

    val entityManager = entityManagerFactory.createEntityManager()

    // Fetch annotations
    val queries = List(
      entityManager.createNativeQuery(AnnotationSQL.byVideoReferenceUuid),
      entityManager.createNativeQuery(AssociationSQL.byVideoReferenceUuid),
      entityManager.createNativeQuery(ImageReferenceSQL.byVideoReferenceUuid)
    )

    queries.foreach(q => {
      q.setParameter(1, videoReferenceUuid)
    })

    val annos = executeQuery(queries(0), queries(1), queries(2), limit, offset)
    entityManager.close()
    annos
  }

  def findByVideoReferenceUuidAndTimestamps(videoReferenceUuid: UUID,
                                            startTimestamp: Instant,
                                            endTimestamp: Instant,
                                            limit: Option[Int] = None,
                                            offset: Option[Int]): Seq[AnnotationImpl] = {

    val entityManager = entityManagerFactory.createEntityManager()
    val queries = List(
      entityManager.createNativeQuery(AnnotationSQL.byVideoReferenceUuidBetweenDates),
      entityManager.createNativeQuery(AssociationSQL.byVideoReferenceUuidBetweenDates),
      entityManager.createNativeQuery(ImageReferenceSQL.byVideoReferenceUuidBetweenDates)
    )

    queries.foreach(q => {
      q.setParameter(1, videoReferenceUuid)
      q.setParameter(2, Timestamp.from(startTimestamp))
      q.setParameter(3, Timestamp.from(endTimestamp))
    })

    val annos = executeQuery(queries(0), queries(1), queries(2), limit, offset)
    entityManager.close()
    annos
  }

  def findByConcurrentRequest(request: ConcurrentRequest,
                               limit: Option[Int] = None,
                               offset: Option[Int] = None): Seq[Annotation] = {

    val uuids = request.uuids.mkString("('", "','", "')")

    val entityManager = entityManagerFactory.createEntityManager()
    val queries = List(AnnotationSQL.byConcurrentRequest,
      AssociationSQL.byConcurrentRequest,
      ImageReferenceSQL.byConcurrentRequest)
      .map(_.replace("(?)", uuids))
      .map(entityManager.createNativeQuery)

    queries.foreach(q => {
      q.setParameter(1, Timestamp.from(request.startTimestamp))
      q.setParameter(2, Timestamp.from(request.endTimestamp))
    })

    val annos = executeQuery(queries(0), queries(1), queries(2), limit, offset)
    entityManager.close()
    annos

  }

  def findByMultiRequest(request: MultiRequest,
                         limit: Option[Int] = None,
                         offset: Option[Int] = None): Seq[AnnotationImpl] = {
    val uuids = request.uuids.mkString("('", "','", "')")
    val entityManager = entityManagerFactory.createEntityManager()
    val queries = List(AnnotationSQL.byMultiRequest,
      AssociationSQL.byMultiRequest,
      ImageReferenceSQL.byMultiRequest)
      .map(_.replace("(?)", uuids))
      .map(entityManager.createNativeQuery)

    val annos = executeQuery(queries(0), queries(1), queries(2), limit, offset)
    entityManager.close()
    annos
  }

  def findByConcept(concept: String,
                    limit: Option[Int] = None,
                    offset: Option[Int] = None): Seq[AnnotationImpl] = ???

  def findByConcepts(concepts: Seq[String],
                     limit: Option[Int] = None,
                     offset: Option[Int] = None): Seq[AnnotationImpl] = ???

  private def executeQuery(annotationQuery: Query,
                   associationQuery: Query,
                   imageReferenceQuery: Query,
                   limit: Option[Int] = None,
                   offset: Option[Int] = None): Seq[AnnotationImpl] = {

    limit.foreach(annotationQuery.setMaxResults)
    offset.foreach(annotationQuery.setFirstResult)

    log.debug("Running annotation query")
    val r0 = annotationQuery.getResultList.asScala.toList
    log.debug(s"Transforming ${r0.size} annotations")
    val annotations = AnnotationSQL.resultListToAnnotations(r0)

    log.debug("Running association query")
    val r1 = associationQuery.getResultList.asScala.toList
    log.debug(s"Transforming ${r1.size} associations")
    val associations = AssociationSQL.resultListToAssociations(r1)
    log.debug("Joining annotations to associations")
    AssociationSQL.join(annotations, associations)

    log.debug("Running imageReference query")
    val r2 = imageReferenceQuery.getResultList.asScala.toList
    log.debug(s"Transforming ${r2.size} imageReferences")
    val imageReferences = ImageReferenceSQL.resultListToImageReferences(r2)
    log.debug("Joining annotations to imageReferences")
    ImageReferenceSQL.join(annotations, imageReferences)

    annotations
  }


}

/**
* Object that contains the SQL and methods to build annotations
  */
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

  val ORDER: String = " ORDER BY obs.uuid"

  val all: String = SELECT + FROM + ORDER

  val byVideoReferenceUuid: String = SELECT + FROM + " WHERE im.video_reference_uuid = ?" + ORDER

  val byConcept: String = SELECT + FROM + " WHERE obs.concept = ?" + ORDER

  val byConceptWithImages: String = SELECT + FROM_WITH_IMAGES +
    " WHERE ir.url IS NOT NULL AND obs.concept = ?" + ORDER

  val betweenDates: String = SELECT + FROM +
    " WHERE im.recorded_timestamp BETWEEN ? AND ?" + ORDER

  val byVideoReferenceUuidBetweenDates: String = SELECT + FROM +
    " WHERE im.video_reference_uuid = ? AND im.recorded_timestamp BETWEEN ? AND ? " + ORDER

  val byConcurrentRequest: String = SELECT + FROM +
    " WHERE im.video_reference_uuid IN (?) AND im.recorded_timestamp BETWEEN ? AND ? " + ORDER

  val byMultiRequest: String = SELECT + FROM + " WHERE im.video_reference_uuid IN (?) " + ORDER


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
      |  associations ass LEFT JOIN
      |  observations obs ON ass.observation_uuid = obs.uuid LEFT JOIN
      |  imaged_moments im ON obs.imaged_moment_uuid = im.uuid
    """.stripMargin

  val byVideoReferenceUuid: String = SELECT + FROM + " WHERE im.video_reference_uuid = ?"

  val byVideoReferenceUuidBetweenDates: String = SELECT + FROM +
    " WHERE im.video_reference_uuid = ? AND im.recorded_timestamp BETWEEN ? AND ? "

  val byConcurrentRequest: String = SELECT + FROM +
    " WHERE im.video_reference_uuid IN (?) AND im.recorded_timestamp BETWEEN ? AND ?"

  val byMultiRequest: String = SELECT + FROM + " WHERE im.video_reference_uuid IN (?)"
}

class ImageReferenceExt extends ImageReferenceImpl {
  var imagedMomentUuid: UUID = _
}

object ImageReferenceSQL {
  val SELECT: String =
    """ SELECT
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

  val byVideoReferenceUuid: String = SELECT + FROM + " WHERE im.video_reference_uuid = ?"

  val byVideoReferenceUuidBetweenDates: String = SELECT + FROM +
    " WHERE im.video_reference_uuid = ? AND im.recorded_timestamp BETWEEN ? AND ? "

  val byConcurrentRequest: String = SELECT + FROM +
    " WHERE im.video_reference_uuid IN (?) AND im.recorded_timestamp BETWEEN ? AND ?"

  val byMultiRequest: String = SELECT + FROM + " WHERE im.video_reference_uuid IN (?)"

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
      annotations.filter(anno => anno.imagedMomentUuid == i.imagedMomentUuid)
        .foreach(anno => anno.javaImageReferences.add(i))
    }
    annotations
  }

}

