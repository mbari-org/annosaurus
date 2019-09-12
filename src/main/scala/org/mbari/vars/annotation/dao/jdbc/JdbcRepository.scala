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

import com.google.gson.annotations.Expose
import javax.persistence.{EntityManager, EntityManagerFactory, Query}
import org.mbari.vars.annotation.dao.jpa.{AnnotationImpl, AssociationImpl, CachedAncillaryDatumImpl, ImageReferenceImpl}
import org.mbari.vars.annotation.model.Annotation
import org.mbari.vars.annotation.model.simple.{ConcurrentRequest, Image, MultiRequest}
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
                               offset: Option[Int],
                               includeAncillaryData: Boolean = false): Seq[AnnotationExt] = {

    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()

    // Fetch annotations
    val queries = List(
      entityManager.createNativeQuery(AnnotationSQL.byVideoReferenceUuid),
      entityManager.createNativeQuery(AssociationSQL.byVideoReferenceUuid),
      entityManager.createNativeQuery(ImageReferenceSQL.byVideoReferenceUuid)
    )

    queries.foreach(q => {
      q.setParameter(1, videoReferenceUuid)
    })

    val annos = executeQuery(queries(0), queries(1), queries(2), limit, offset, includeAncillaryData)
    entityManager.close()
    annos
  }


  def findByVideoReferenceUuidAndTimestamps(videoReferenceUuid: UUID,
                                            startTimestamp: Instant,
                                            endTimestamp: Instant,
                                            limit: Option[Int] = None,
                                            offset: Option[Int],
                                            includeAncillaryData: Boolean = false): Seq[AnnotationExt] = {

    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
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

    val annos = executeQuery(queries(0), queries(1), queries(2), limit, offset, includeAncillaryData)
    entityManager.close()
    annos
  }

  def findByConcurrentRequest(request: ConcurrentRequest,
                               limit: Option[Int] = None,
                               offset: Option[Int] = None,
                              includeAncillaryData: Boolean = false): Seq[Annotation] = {

    val uuids = request.uuids.map(_.toString)

    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val queries = List(AnnotationSQL.byConcurrentRequest,
      AssociationSQL.byConcurrentRequest,
      ImageReferenceSQL.byConcurrentRequest)
      .map(sql => inClause(sql, uuids))
      .map(entityManager.createNativeQuery)

    queries.foreach(q => {
      q.setParameter(1, Timestamp.from(request.startTimestamp))
      q.setParameter(2, Timestamp.from(request.endTimestamp))
    })

    val annos = executeQuery(queries(0), queries(1), queries(2), limit, offset, includeAncillaryData)
    entityManager.close()
    annos

  }

  def findByMultiRequest(request: MultiRequest,
                         limit: Option[Int] = None,
                         offset: Option[Int] = None,
                         includeAncillaryData: Boolean = false): Seq[AnnotationExt] = {
    val uuids = request.uuids.map(_.toString)
    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val queries = List(AnnotationSQL.byMultiRequest,
      AssociationSQL.byMultiRequest,
      ImageReferenceSQL.byMultiRequest)
      .map(sql => inClause(sql, uuids))
      .map(entityManager.createNativeQuery)

    val annos = executeQuery(queries(0), queries(1), queries(2), limit, offset, includeAncillaryData)
    entityManager.close()
    annos
  }

  def findByConcept(concept: String,
                    limit: Option[Int] = None,
                    offset: Option[Int] = None,
                    includeAncillaryData: Boolean = false): Seq[AnnotationExt] = {
    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val query1 = entityManager.createNativeQuery(AnnotationSQL.byConcept)
    query1.setParameter(1, concept)
    limit.foreach(query1.setMaxResults)
    offset.foreach(query1.setFirstResult)

    val r1 = query1.getResultList.asScala.toList
    val annotations = AnnotationSQL.resultListToAnnotations(r1)
    executeQueryForAnnotations(annotations, includeAncillaryData)
    if (includeAncillaryData) findAncillaryData(annotations)
    entityManager.close()
    annotations
  }

  def findByConceptWithImages(concept: String,
                    limit: Option[Int] = None,
                    offset: Option[Int] = None,
                    includeAncillaryData: Boolean = false): Seq[AnnotationExt] = {
    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val query1 = entityManager.createNativeQuery(AnnotationSQL.byConceptWithImages)
    query1.setParameter(1, concept)
    limit.foreach(query1.setMaxResults)
    offset.foreach(query1.setFirstResult)
    val r1 = query1.getResultList.asScala.toList
    val annotations = AnnotationSQL.resultListToAnnotations(r1).distinct
    executeQueryForAnnotations(annotations, includeAncillaryData)
    if (includeAncillaryData) findAncillaryData(annotations)
    entityManager.close()
    annotations
  }

  def findImagedMomentUuidsByConceptWithImages(concept: String,
                                               limit: Option[Int] = None,
                                               offset: Option[Int] = None): Seq[UUID] = {
    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val query = entityManager.createNativeQuery(ImagedMomentSQL.byConceptWithImages)
    query.setParameter(1, concept)
    val results = query.getResultList.asScala.toList
    val uuids = results.map(_.toString)
      .map(UUID.fromString)
    entityManager.close()
    uuids
  }

  def findImagesByVideoReferenceUuid(videoReferenceUuid: UUID,
                                     limit: Option[Int] = None,
                                     offset: Option[Int]): Seq[Image] = {
    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val query = entityManager.createNativeQuery(ImagedMomentSQL.byVideoReferenceUuid)
    query.setParameter(1, videoReferenceUuid.toString)
    limit.foreach(query.setMaxResults)
    offset.foreach(query.setFirstResult)
    val results = query.getResultList.asScala.toList
    val images = ImagedMomentSQL.resultListToImages(results)
    entityManager.close()
    images
  }

  private def inClause(sql: String, items: Seq[String]): String = {
    val p = items.mkString("('", "','", "')")
    sql.replace("(?)", p)
  }

  private def executeQueryForAnnotations(annotations: Seq[AnnotationExt],
                                         includeAncillaryData: Boolean = false)
                                        (implicit entityManager: EntityManager): Seq[AnnotationExt] = {

    val observationUuids = annotations.map(_.observationUuid.toString).distinct
    for (obs <- observationUuids.grouped(200)) {
      val sql2 = inClause(AssociationSQL.byObservationUuids, obs)
      val query2 = entityManager.createNativeQuery(sql2)
      val r2 = query2.getResultList.asScala.toList
      val associations = AssociationSQL.resultListToAssociations(r2)
      AssociationSQL.join(annotations, associations)
    }

    val imagedMomentUuids = annotations.map(_.imagedMomentUuid.toString).distinct
    for (im <- imagedMomentUuids.grouped(200)) {
      val sql3 = inClause(ImageReferenceSQL.byImagedMomentUuids, im)
      val query3 = entityManager.createNativeQuery(sql3)
      val r3 = query3.getResultList.asScala.toList
      val imagedReferences = ImageReferenceSQL.resultListToImageReferences(r3)
      ImageReferenceSQL.join(annotations, imagedReferences)
    }

    if (includeAncillaryData) findAncillaryData(annotations)
    annotations

  }

  private def executeQuery(annotationQuery: Query,
                   associationQuery: Query,
                   imageReferenceQuery: Query,
                   limit: Option[Int] = None,
                   offset: Option[Int] = None,
                   includeAncillaryData: Boolean = false)
                          (implicit entityManager: EntityManager): Seq[AnnotationExt] = {

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

    if (includeAncillaryData) findAncillaryData(annotations)

    annotations
  }

  private def findAncillaryData(annotations: Seq[AnnotationExt])
                               (implicit entityManager: EntityManager): Seq[AnnotationExt] = {

    for (annos <- annotations.grouped(200)) {
      val ims = annos.map(_.imagedMomentUuid.toString).distinct
      val sql = inClause(AncillaryDatumSQL.byImagedMomentUuid, ims)
      val query = entityManager.createNativeQuery(sql)
      val rows = query.getResultList.asScala.toList
      val data = AncillaryDatumSQL.resultListToAnncillaryData(rows)
      AncillaryDatumSQL.join(annos, data)
    }
    annotations
  }

}

class AnnotationExt extends AnnotationImpl {
  @Expose(serialize = true)
  var ancillaryData: AncillaryDatumExt = _

  override def equals(obj: Any): Boolean = {
    if (!obj.isInstanceOf[AnnotationExt]) false
    else {
      val other: AnnotationExt = obj.asInstanceOf[AnnotationExt]
      other.observationUuid != null &&
        this.observationUuid != null &&
        other.observationUuid == this.observationUuid
    }
  }

  override def hashCode(): Int = this.observationUuid.hashCode()

}

/**
* Object that contains the SQL and methods to build annotations
  */
object AnnotationSQL {

  def resultListToAnnotations(rows: List[_]): Seq[AnnotationExt] = {
    for {
      row <- rows
    } yield {
      val xs = row.asInstanceOf[Array[Object]]
      val a = new AnnotationExt
      a.imagedMomentUuid = UUID.fromString(xs(0).toString)
      a.videoReferenceUuid = UUID.fromString(xs(1).toString)
      Option(xs(2))
        .map(v => v.asInstanceOf[java.math.BigDecimal].longValue())
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
      Option(xs(7)).foreach(v => a.activity = v.toString)
      Option(xs(8))
        .map(v => v.asInstanceOf[Long])
        .map(Duration.ofMillis)
        .foreach(v => a.duration = v)
      Option(xs(9)).foreach(v => a.group = v.toString)
      a.observationTimestamp = xs(10).asInstanceOf[Timestamp].toInstant
      a.observer = xs(11).toString
      a
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
      |  observations obs ON obs.imaged_moment_uuid = im.uuid """.stripMargin

  val FROM_WITH_IMAGES: String =
    """ FROM
      |  imaged_moments im RIGHT JOIN
      |  observations obs ON obs.imaged_moment_uuid = im.uuid LEFT JOIN
      |  image_references ir ON ir.imaged_moment_uuid = im.uuid """.stripMargin

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
    """ SELECT DISTINCT
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

  val ORDER: String = " ORDER BY ass.uuid"

  val byVideoReferenceUuid: String = SELECT + FROM + " WHERE im.video_reference_uuid = ?" + ORDER

  val byVideoReferenceUuidBetweenDates: String = SELECT + FROM +
    " WHERE im.video_reference_uuid = ? AND im.recorded_timestamp BETWEEN ? AND ? " + ORDER

  val byConcurrentRequest: String = SELECT + FROM +
    " WHERE im.video_reference_uuid IN (?) AND im.recorded_timestamp BETWEEN ? AND ?" + ORDER

  val byMultiRequest: String = SELECT + FROM + " WHERE im.video_reference_uuid IN (?)" + ORDER

  val byObservationUuids: String = SELECT + FROM + " WHERE obs.uuid IN (?)" + ORDER
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

  val ORDER: String = " ORDER BY ir.uuid"

  val byVideoReferenceUuid: String = SELECT + FROM + " WHERE im.video_reference_uuid = ?" + ORDER

  val byVideoReferenceUuidBetweenDates: String = SELECT + FROM +
    " WHERE im.video_reference_uuid = ? AND im.recorded_timestamp BETWEEN ? AND ? " + ORDER


  val byConcurrentRequest: String = SELECT + FROM +
    " WHERE im.video_reference_uuid IN (?) AND im.recorded_timestamp BETWEEN ? AND ?" + ORDER

  val byMultiRequest: String = SELECT + FROM + " WHERE im.video_reference_uuid IN (?)" + ORDER

  val byImagedMomentUuids: String = SELECT + FROM + " WHERE im.uuid IN (?)" + ORDER

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

class AncillaryDatumExt extends CachedAncillaryDatumImpl {
  var imagedMomentUuid: UUID = _
}

object AncillaryDatumSQL {

  def resultListToAnncillaryData(rows: List[_]): Seq[AncillaryDatumExt] = {
    for {
      row <- rows
    } yield {
      val xs = row.asInstanceOf[Array[Object]]
      val a = new AncillaryDatumExt
      a.uuid = UUID.fromString(xs(0).toString)
      a.altitude = toDouble(xs(1).asInstanceOf[Number])
      Option(xs(2)).foreach(v => a.crs = v.toString)
      a.depthMeters = toDouble(xs(3).asInstanceOf[Number])
      a.latitude = toDouble(xs(4).asInstanceOf[Number])
      a.longitude = toDouble(xs(5).asInstanceOf[Number])
      a.oxygenMlL = toDouble(xs(6).asInstanceOf[Number])
      a.phi = toDouble(xs(7).asInstanceOf[Number])
      Option(xs(8)).foreach(v => a.posePositionUnits = v.toString)
      a.pressureDbar = toDouble(xs(9).asInstanceOf[Number])
      a.psi = toDouble(xs(10).asInstanceOf[Number])
      a.salinity = toDouble(xs(11).asInstanceOf[Number])
      a.temperatureCelsius = toDouble(xs(12).asInstanceOf[Number])
      a.theta = toDouble(xs(13).asInstanceOf[Number])
      a.x = toDouble(xs(14).asInstanceOf[Number])
      a.y = toDouble(xs(15).asInstanceOf[Number])
      a.z = toDouble(xs(16).asInstanceOf[Number])
      a.lightTransmission = toDouble(xs(17).asInstanceOf[Number])
      a.imagedMomentUuid = UUID.fromString(xs(18).toString)
      a
    }
  }

  private def toDouble(obj: Number): Option[Double] = if (obj != null) Some(obj.doubleValue())
    else None

  val SELECT: String =
    """ SELECT
      |  ad.uuid AS ancillary_data_uuid,
      |  ad.altitude,
      |  ad.coordinate_reference_system,
      |  ad.depth_meters,
      |  ad.latitude,
      |  ad.longitude,
      |  ad.oxygen_ml_per_l,
      |  ad.phi,
      |  ad.xyz_position_units,
      |  ad.pressure_dbar,
      |  ad.psi,
      |  ad.salinity,
      |  ad.temperature_celsius,
      |  ad.theta,
      |  ad.x,
      |  ad.y,
      |  ad.z,
      |  ad.light_transmission,
      |  im.uuid as imaged_moment_uuid
    """.stripMargin

  val FROM: String =
    """ FROM
      |  ancillary_data ad LEFT JOIN
      |  imaged_moments im ON ad.imaged_moment_uuid = im.uuid
    """.stripMargin

  val ORDER: String = " ORDER BY ad.uuid"

  val byVideoReferenceUuid: String = SELECT + FROM + " WHERE im.video_reference_uuid = ?" + ORDER

  val byImagedMomentUuid: String = SELECT + FROM + " WHERE im.uuid IN (?)" + ORDER

  val byVideoReferenceUuidBetweenDates: String = SELECT + FROM +
    " WHERE im.video_reference_uuid = ? AND im.recorded_timestamp BETWEEN ? AND ? " + ORDER

  val byConcurrentRequest: String = SELECT + FROM +
    " WHERE im.video_reference_uuid IN (?) AND im.recorded_timestamp BETWEEN ? AND ?" + ORDER

  val byMultiRequest: String = SELECT + FROM + " WHERE im.video_reference_uuid IN (?)" + ORDER

  def join(annotations: Seq[AnnotationExt], data: Seq[AncillaryDatumExt]): Seq[Annotation] = {
    for {
      d <- data
    } {
      annotations.filter(anno => anno.imagedMomentUuid == d.imagedMomentUuid)
        .foreach(anno => anno.ancillaryData = d)
    }
    annotations
  }

}

object ImagedMomentSQL {

  val SELECT_UUID: String = "SELECT DISTINCT im.uuid "

  val SELECT_IMAGES: String =
    """SELECT
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

  val byConceptWithImages: String = SELECT_UUID + FROM + " WHERE concept = ? AND ir.url IS NOT NULL"

  val byVideoReferenceUuid: String = SELECT_IMAGES + FROM + " WHERE im.video_reference_uuid = ? AND ir.url IS NOT NULL"

  def resultListToImages(rows: List[_]): Seq[Image] = {
    for {
      row <- rows
    } yield {
      val xs = row.asInstanceOf[Array[Object]]
      val i = new Image;
      i.imagedMomentUuid = UUID.fromString(xs(0).toString)
      i.videoReferenceUuid = UUID.fromString(xs(1).toString)
      Option(xs(2))
        .map(v => v.asInstanceOf[java.math.BigDecimal].longValue())
        .map(Duration.ofMillis)
        .foreach(v => i.elapsedTime = v)
      Option(xs(3))
        .map(v => v.asInstanceOf[Timestamp])
        .map(v => v.toInstant)
        .foreach(v => i.recordedTimestamp = v)
      Option(xs(4))
        .map(v => v.toString)
        .map(v => new Timecode(v))
        .foreach(v => i.timecode = v)
      Option(xs(5)).foreach(v => i.description = v.toString)
      Option(xs(6)).foreach(v => i.format = v.toString)
      Option(xs(7)).foreach(v => i.height = v.asInstanceOf[Number].intValue())
      Option(xs(8)).foreach(v => i.width = v.asInstanceOf[Number].intValue())
      i.url =  new URL(xs(9).toString)
      i.imageReferenceUuid = UUID.fromString(xs(10).toString)

      i
    }
  }

}


