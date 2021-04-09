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

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.persistence.{EntityManager, EntityManagerFactory, Query}
import org.mbari.vars.annotation.model.{Annotation, GeographicRange}
import org.mbari.vars.annotation.model.simple.{ConcurrentRequest, DeleteCount, Image, MultiRequest, QueryConstraints}
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

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

  def deleteByVideoReferenceUuid(videoReferenceUuid: UUID): DeleteCount = {
    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val transaction                           = entityManager.getTransaction
    transaction.begin()
    val queries = List(
      AncillaryDatumSQL.deleteByVideoReferenceUuid,
      ImageReferenceSQL.deleteByVideoReferenceUuid,
      AssociationSQL.deleteByVideoReferenceUuid,
      ObservationSQL.deleteByVideoReferenceUuid,
      ImagedMomentSQL.deleteByVideoReferenceUuid
    ).map(entityManager.createNativeQuery)
    queries.foreach(_.setParameter(1, videoReferenceUuid.toString))
    var deleteCount = DeleteCount(videoReferenceUuid)
    try {
      val counts = queries.map(_.executeUpdate())
      deleteCount =
        DeleteCount(videoReferenceUuid, counts(4), counts(1), counts(3), counts(2), counts(0))
      transaction.commit()
    }
    catch {
      case NonFatal(e) =>
        deleteCount.errorMessage = s"A(n) ${e.getClass} was thrown. It reports: `${e.getMessage}`"
    }
    finally {
      if (transaction.isActive) {
        transaction.rollback()
      }
    }
    entityManager.close()
    deleteCount

  }

  def findByQueryConstraint(constraints: QueryConstraints): Seq[AnnotationExt] = {
    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val query1 = QueryConstraints.toQuery(constraints, entityManager)

    val r1          = query1.getResultList.asScala.toList
    val annotations = AnnotationSQL.resultListToAnnotations(r1)
    executeQueryForAnnotations(annotations, constraints.data)
    entityManager.close()
    annotations
  }

  def countByQueryConstraint(constraints: QueryConstraints): Int = {
    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val query = QueryConstraints.toCountQuery(constraints, entityManager)
    val count = query.getResultList.get(0).asInstanceOf[Int]
    entityManager.close()
    count
  }

  def findGeographicRangeByQueryConstraint(constraints: QueryConstraints): Option[GeographicRange] = {
    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val query = QueryConstraints.toGeographicRangeQuery(constraints, entityManager)
    // Queries return java.util.List[Array[Object]]
    val count = query.getResultList.asScala.toList
    if (count.nonEmpty) {
      val head = count.head.asInstanceOf[Array[_]]
      Some(GeographicRange(head(0).toString.toDouble,
        head(1).toString.toDouble,
        head(2).toString.toDouble,
        head(3).toString.toDouble,
        head(4).toString.toDouble,
        head(5).toString.toDouble))
    }
    else None
  }

  def findAll(
      limit: Option[Int] = Some(1000),
      offset: Option[Int] = None,
      includeAncillaryData: Boolean = false
  ): Seq[AnnotationExt] = {

    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val query1                                = entityManager.createNativeQuery(AnnotationSQL.all)
    limit.foreach(query1.setMaxResults)
    offset.foreach(query1.setFirstResult)

    val r1          = query1.getResultList.asScala.toList
    val annotations = AnnotationSQL.resultListToAnnotations(r1)
    executeQueryForAnnotations(annotations, includeAncillaryData)
//    if (includeAncillaryData) findAncillaryData(annotations)
    entityManager.close()
    annotations

  }

  def countAll(): Long = {
    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val query                                 = entityManager.createNativeQuery(ObservationSQL.countAll)
    val count                                 = query.getSingleResult.asInstanceOf[Int].toLong
    entityManager.close()
    count
  }

  def findByVideoReferenceUuid(
      videoReferenceUuid: UUID,
      limit: Option[Int] = None,
      offset: Option[Int] = None,
      includeAncillaryData: Boolean = false
  ): Seq[AnnotationExt] = {

    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()

    // Fetch annotations
    val queries = List(
      entityManager.createNativeQuery(AnnotationSQL.byVideoReferenceUuid),
      entityManager.createNativeQuery(AssociationSQL.byVideoReferenceUuid),
      entityManager.createNativeQuery(ImageReferenceSQL.byVideoReferenceUuid)
    )

    queries.foreach(q => {
      q.setParameter(1, videoReferenceUuid.toString)
    })

    val annos =
      executeQuery(queries(0), queries(1), queries(2), limit, offset, includeAncillaryData)
    entityManager.close()
    annos
  }

  def findByVideoReferenceUuidAndTimestamps(
      videoReferenceUuid: UUID,
      startTimestamp: Instant,
      endTimestamp: Instant,
      limit: Option[Int] = None,
      offset: Option[Int],
      includeAncillaryData: Boolean = false
  ): Seq[AnnotationExt] = {

    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val queries = List(
      entityManager.createNativeQuery(AnnotationSQL.byVideoReferenceUuidBetweenDates),
      entityManager.createNativeQuery(AssociationSQL.byVideoReferenceUuidBetweenDates),
      entityManager.createNativeQuery(ImageReferenceSQL.byVideoReferenceUuidBetweenDates)
    )

    queries.foreach(q => {
      q.setParameter(1, videoReferenceUuid.toString)
      q.setParameter(2, Timestamp.from(startTimestamp))
      q.setParameter(3, Timestamp.from(endTimestamp))
    })

    val annos =
      executeQuery(queries(0), queries(1), queries(2), limit, offset, includeAncillaryData)
    entityManager.close()
    annos
  }

  def findByConcurrentRequest(
      request: ConcurrentRequest,
      limit: Option[Int] = None,
      offset: Option[Int] = None,
      includeAncillaryData: Boolean = false
  ): Seq[Annotation] = {

    val uuids = request.uuids.map(_.toString)

    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val queries = List(
      AnnotationSQL.byConcurrentRequest,
      AssociationSQL.byConcurrentRequest,
      ImageReferenceSQL.byConcurrentRequest
    ).map(sql => inClause(sql, uuids))
      .map(entityManager.createNativeQuery)

    queries.foreach(q => {
      q.setParameter(1, Timestamp.from(request.startTimestamp))
      q.setParameter(2, Timestamp.from(request.endTimestamp))
    })

    val annos =
      executeQuery(queries(0), queries(1), queries(2), limit, offset, includeAncillaryData)
    entityManager.close()
    annos

  }

  def findByMultiRequest(
      request: MultiRequest,
      limit: Option[Int] = None,
      offset: Option[Int] = None,
      includeAncillaryData: Boolean = false
  ): Seq[AnnotationExt] = {
    val uuids                                 = request.uuids.map(_.toString)
    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val queries = List(
      AnnotationSQL.byMultiRequest,
      AssociationSQL.byMultiRequest,
      ImageReferenceSQL.byMultiRequest
    ).map(sql => inClause(sql, uuids))
      .map(entityManager.createNativeQuery)

    val annos =
      executeQuery(queries(0), queries(1), queries(2), limit, offset, includeAncillaryData)
    entityManager.close()
    annos
  }

  def findByConcept(
      concept: String,
      limit: Option[Int] = None,
      offset: Option[Int] = None,
      includeAncillaryData: Boolean = false
  ): Seq[AnnotationExt] = {
    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val query1                                = entityManager.createNativeQuery(AnnotationSQL.byConcept)
    query1.setParameter(1, concept)
    limit.foreach(query1.setMaxResults)
    offset.foreach(query1.setFirstResult)

    val r1          = query1.getResultList.asScala.toList
    val annotations = AnnotationSQL.resultListToAnnotations(r1)
    executeQueryForAnnotations(annotations, includeAncillaryData)
//    if (includeAncillaryData) findAncillaryData(annotations)
    entityManager.close()
    annotations
  }

  def findByConceptWithImages(
      concept: String,
      limit: Option[Int] = None,
      offset: Option[Int] = None,
      includeAncillaryData: Boolean = false
  ): Seq[AnnotationExt] = {
    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val query1                                = entityManager.createNativeQuery(AnnotationSQL.byConceptWithImages)
    query1.setParameter(1, concept)
    limit.foreach(query1.setMaxResults)
    offset.foreach(query1.setFirstResult)
    val r1          = query1.getResultList.asScala.toList
    val annotations = AnnotationSQL.resultListToAnnotations(r1).distinct
    executeQueryForAnnotations(annotations, includeAncillaryData)
//    if (includeAncillaryData) findAncillaryData(annotations)
    entityManager.close()
    annotations
  }

  def findByToConceptWithImages(
     toConcept: String,
     limit: Option[Int] = None,
     offset: Option[Int] = None,
     includeAncillaryData: Boolean = false): Seq[AnnotationExt] = {
    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val query1                                = entityManager.createNativeQuery(AnnotationSQL.byToConceptWithImages)
    query1.setParameter(1, toConcept)
    limit.foreach(query1.setMaxResults)
    offset.foreach(query1.setFirstResult)
    val r1          = query1.getResultList.asScala.toList
    val annotations = AnnotationSQL.resultListToAnnotations(r1).distinct
    executeQueryForAnnotations(annotations, includeAncillaryData)
//    if (includeAncillaryData) findAncillaryData(annotations)
    entityManager.close()
    annotations
  }

  def findImagedMomentUuidsByConceptWithImages(
      concept: String,
      limit: Option[Int] = None,
      offset: Option[Int] = None
  ): Seq[UUID] = {
    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val query                                 = entityManager.createNativeQuery(ImagedMomentSQL.byConceptWithImages)
    query.setParameter(1, concept)
    limit.foreach(query.setMaxResults)
    offset.foreach(query.setFirstResult)
    val results = query.getResultList.asScala.toList
    val uuids = results
      .map(_.toString)
      .map(UUID.fromString)
    entityManager.close()
    uuids
  }

  def findImagedMomentUuidsByToConceptWithImages(
      toConcept: String,
      limit: Option[Int] = None,
      offset: Option[Int] = None
  ): Seq[UUID] = {
    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val query                                 = entityManager.createNativeQuery(ImagedMomentSQL.byToConceptWithImages)
    query.setParameter(1, toConcept)
    limit.foreach(query.setMaxResults)
    offset.foreach(query.setFirstResult)
    val results = query.getResultList.asScala.toList
    val uuids = results
      .map(_.toString)
      .map(UUID.fromString)
    entityManager.close()
    uuids
  }

  def findImagesByVideoReferenceUuid(
      videoReferenceUuid: UUID,
      limit: Option[Int] = None,
      offset: Option[Int] = None
  ): Seq[Image] = {
    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val query                                 = entityManager.createNativeQuery(ImagedMomentSQL.byVideoReferenceUuid)
    query.setParameter(1, videoReferenceUuid.toString)
    limit.foreach(query.setMaxResults)
    offset.foreach(query.setFirstResult)
    val results = query.getResultList.asScala.toList
    val images  = ImagedMomentSQL.resultListToImages(results)
    entityManager.close()
    images
  }

  def findByLinkNameAndLinkValue(
      linkName: String,
      linkValue: String,
      includeAncillaryData: Boolean = false
  ): Seq[AnnotationExt] = {
    implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
    val query                                 = entityManager.createNativeQuery(AssociationSQL.byLinkNameAndLinkValue)
    query.setParameter(1, linkName)
    query.setParameter(2, linkValue)
    val results           = query.getResultList.asScala.toList
    val associations      = AssociationSQL.resultListToAssociations(results)
    val imagedMomentUuids = associations.map(_.imagedMomentUuid)
    val annotations =
      executeQueryForAnnotationsUsingImagedMomentUuids(imagedMomentUuids, includeAncillaryData)
    entityManager.close()
    annotations
  }

  private def inClause(sql: String, items: Seq[String]): String = {
    val p = items.mkString("('", "','", "')")
    sql.replace("(?)", p)
  }

  private def executeQueryForAnnotationsUsingImagedMomentUuids(
      imagedMomentUuids: Seq[UUID],
      includeAncillaryData: Boolean = false
  )(implicit entityManager: EntityManager): Seq[AnnotationExt] = {
    val imUuids = imagedMomentUuids.map(_.toString).distinct
    val annotations = (for (im <- imUuids.grouped(200)) yield {
      val sql     = inClause(AnnotationSQL.byImagedMomentUuids, im)
      val query   = entityManager.createNativeQuery(sql)
      val results = query.getResultList.asScala.toList
      AnnotationSQL.resultListToAnnotations(results)
    }).flatten.toSeq
    executeQueryForAnnotations(annotations, includeAncillaryData)
  }

  private def executeQueryForAnnotations(
      annotations: Seq[AnnotationExt],
      includeAncillaryData: Boolean = false
  )(implicit entityManager: EntityManager): Seq[AnnotationExt] = {

    // lookup associations
    val observationUuids = annotations.map(_.observationUuid.toString).distinct
    for (obs <- observationUuids.grouped(200)) {
      val sql2         = inClause(AssociationSQL.byObservationUuids, obs)
      val query2       = entityManager.createNativeQuery(sql2)
      val r2           = query2.getResultList.asScala.toList
      val associations = AssociationSQL.resultListToAssociations(r2)
      AssociationSQL.join(annotations, associations)
    }

    // lookup imageMoments
    val imagedMomentUuids = annotations.map(_.imagedMomentUuid.toString).distinct
    for (im <- imagedMomentUuids.grouped(200)) {
      val sql3             = inClause(ImageReferenceSQL.byImagedMomentUuids, im)
      val query3           = entityManager.createNativeQuery(sql3)
      val r3               = query3.getResultList.asScala.toList
      val imagedReferences = ImageReferenceSQL.resultListToImageReferences(r3)
      ImageReferenceSQL.join(annotations, imagedReferences)
    }

    if (includeAncillaryData) findAncillaryData(annotations)
    annotations

  }

  private def executeQuery(
      annotationQuery: Query,
      associationQuery: Query,
      imageReferenceQuery: Query,
      limit: Option[Int] = None,
      offset: Option[Int] = None,
      includeAncillaryData: Boolean = false
  )(implicit entityManager: EntityManager): Seq[AnnotationExt] = {

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

  private def findAncillaryData(
      annotations: Seq[AnnotationExt]
  )(implicit entityManager: EntityManager): Seq[AnnotationExt] = {

    for (annos <- annotations.grouped(200)) {
      val ims   = annos.map(_.imagedMomentUuid.toString).distinct
      val sql   = inClause(AncillaryDatumSQL.byImagedMomentUuid, ims)
      val query = entityManager.createNativeQuery(sql)
      val rows  = query.getResultList.asScala.toList
      val data  = AncillaryDatumSQL.resultListToAnncillaryData(rows)
      AncillaryDatumSQL.join(annos, data)
    }
    annotations
  }

}
