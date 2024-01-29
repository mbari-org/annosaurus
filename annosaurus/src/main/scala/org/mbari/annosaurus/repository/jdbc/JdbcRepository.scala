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

import java.time.Instant
import java.util.UUID
import jakarta.persistence.{EntityManager, EntityManagerFactory, Query}

import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal
import org.mbari.annosaurus.domain.{ConcurrentRequest, DeleteCount, MultiRequest}
import org.mbari.annosaurus.domain.QueryConstraints
import org.mbari.annosaurus.domain.Annotation
import org.mbari.annosaurus.domain.GeographicRange
import org.mbari.annosaurus.domain.Image
import org.mbari.annosaurus.etc.jdk.Logging.given

/** Database access (read-only) provider that uses SQL for fast lookups. WHY? JPA makes about 1 +
  * (rows * 4) database requests when looking up annotations. For 1000 rows that 4001 database calls
  * which is very SLOW!!. With SQL we can fetch annotations for a video using 3 database queries
  * which is so amazingly fast compared to JPA.
  * @param entityManagerFactory
  */
class JdbcRepository(entityManagerFactory: EntityManagerFactory) {

    private[this] val log = System.getLogger(getClass.getName)

    def deleteByVideoReferenceUuid(videoReferenceUuid: UUID): DeleteCount = {
        implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
        val transaction                           = entityManager.getTransaction
        transaction.begin()
        val queries                               = List(
            AncillaryDatumSQL.deleteByVideoReferenceUuid,
            ImageReferenceSQL.deleteByVideoReferenceUuid,
            AssociationSQL.deleteByVideoReferenceUuid,
            ObservationSQL.deleteByVideoReferenceUuid,
            ImagedMomentSQL.deleteByVideoReferenceUuid
        ).map(entityManager.createNativeQuery)
        queries.foreach(q => {
            // if (DatabaseProductName.isPostgreSQL()) {
            //     q.setParameter(1, videoReferenceUuid)
            // }
            // else {
            q.setParameter(1, videoReferenceUuid.toString)
            // }
        })
        var deleteCount                           = DeleteCount(videoReferenceUuid)
        try {
            val counts = queries.map(_.executeUpdate())
            deleteCount = DeleteCount(
                videoReferenceUuid,
                counts(4),
                counts(1),
                counts(3),
                counts(2),
                counts(0)
            )
            transaction.commit()
        }
        catch {
            case NonFatal(e) =>
                val errorMessage = s"A(n) ${e.getClass} was thrown. It reports: `${e.getMessage}`"
                deleteCount = deleteCount.copy(errorMessage = Some(errorMessage))

        }
        finally {
            if (transaction.isActive) {
                transaction.rollback()
            }
        }
        entityManager.close()
        deleteCount

    }

    def findByQueryConstraint(constraints: QueryConstraints): Seq[Annotation] = {
        given entityManager: EntityManager = entityManagerFactory.createEntityManager()
        val query1                         = QueryConstraintsSqlBuilder.toQuery(constraints, entityManager)
        val r1                             = query1.getResultList.asScala.toList
        val annotations                    = AnnotationSQL.resultListToAnnotations(r1)
        val resolvedAnnotations            = executeQueryForAnnotations(annotations, constraints.includeData)
        entityManager.close()
        resolvedAnnotations.map(_.removeForeignKeys())
    }

    def countByQueryConstraint(constraints: QueryConstraints): Int = {
        given entityManager: EntityManager = entityManagerFactory.createEntityManager()
        val query                          = QueryConstraintsSqlBuilder.toCountQuery(constraints, entityManager)
        // Postgresql returns a Long, Everything else returns an Int
        val count                          = query.getResultList.get(0).toString().toInt
        entityManager.close()
        count
    }

    def findGeographicRangeByQueryConstraint(
        constraints: QueryConstraints
    ): Option[GeographicRange] = {
        given entityManager: EntityManager = entityManagerFactory.createEntityManager()
        val query                          = QueryConstraintsSqlBuilder.toGeographicRangeQuery(constraints, entityManager)
        // Queries return java.util.List[Array[Object]]
        val count                          = query.getResultList.asScala.toList
        if (count.nonEmpty) {
            val head = count.head.asInstanceOf[Array[_]]
            Some(
                GeographicRange(
                    head(0).toString.toDouble,
                    head(1).toString.toDouble,
                    head(2).toString.toDouble,
                    head(3).toString.toDouble,
                    head(4).toString.toDouble,
                    head(5).toString.toDouble
                )
            )
        }
        else None
    }

    def findAll(
        limit: Option[Int] = Some(1000),
        offset: Option[Int] = None,
        includeAncillaryData: Boolean = false
    ): Seq[Annotation] = {

        given entityManager: EntityManager = entityManagerFactory.createEntityManager()
        val query1                         = entityManager.createNativeQuery(AnnotationSQL.all)
        limit.foreach(query1.setMaxResults)
        offset.foreach(query1.setFirstResult)

        val r1 = query1.getResultList.asScala.toList
        val a1 = AnnotationSQL.resultListToAnnotations(r1)
        val a2 = executeQueryForAnnotations(a1, includeAncillaryData)
        entityManager.close()
        a2

    }

    def countAll(): Long = {
        given entityManager: EntityManager = entityManagerFactory.createEntityManager()
        val query                          = entityManager.createNativeQuery(ObservationSQL.countAll)
        // This will throw and exception if nothing is returned. That's ok.
        val count                          = query.getSingleResult.asLong.get
        entityManager.close()
        count
    }

    def countImagesByVideoReferenceUuid(videoReferenceUuid: UUID): Long = {
        given entityManager: EntityManager = entityManagerFactory.createEntityManager()
        val query                          = entityManager.createNativeQuery(ImageReferenceSQL.countByVideoReferenceUuid)
        query.setParameter(1, videoReferenceUuid.toString())
        // This will throw and exception if nothing is returned. That's ok.
        // SQL Server returns Int, Postgresql returns Long
        val count                          = query.getSingleResult.asLong.get
        entityManager.close()
        count
    }

    def findByVideoReferenceUuid(
        videoReferenceUuid: UUID,
        limit: Option[Int] = None,
        offset: Option[Int] = None,
        includeAncillaryData: Boolean = false
    ): Seq[Annotation] = {

        given entityManager: EntityManager = entityManagerFactory.createEntityManager()

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
        offset: Option[Int] = None,
        includeAncillaryData: Boolean = false
    ): Seq[Annotation] = {

        given entityManager: EntityManager = entityManagerFactory.createEntityManager()
        val queries                        = List(
            entityManager.createNativeQuery(AnnotationSQL.byVideoReferenceUuidBetweenDates),
            entityManager.createNativeQuery(AssociationSQL.byVideoReferenceUuidBetweenDates),
            entityManager.createNativeQuery(ImageReferenceSQL.byVideoReferenceUuidBetweenDates)
        )

        queries.foreach(q => {
            q.setParameter(1, videoReferenceUuid.toString)
            q.setParameter(2, startTimestamp)
            q.setParameter(3, endTimestamp)
            // q.setParameter(2, Timestamp.from(startTimestamp))
            // q.setParameter(3, Timestamp.from(endTimestamp))
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

        val videoReferenceUuids = request.videoReferenceUuids.map(_.toString)

        given entityManager: EntityManager = entityManagerFactory.createEntityManager()
        val queries                        = List(
            AnnotationSQL.byConcurrentRequest,
            AssociationSQL.byConcurrentRequest,
            ImageReferenceSQL.byConcurrentRequest
        ).map(sql => inClause(sql, videoReferenceUuids))
            .map(entityManager.createNativeQuery)

        queries.foreach(q => {
            q.setParameter(1, request.startTimestamp)
            q.setParameter(2, request.endTimestamp)
            // q.setParameter(1, Timestamp.from(request.startTimestamp))
            // q.setParameter(2, Timestamp.from(request.endTimestamp))
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
    ): Seq[Annotation] = {
        val uuids                          = request.videoReferenceUuids.map(_.toString)
        given entityManager: EntityManager = entityManagerFactory.createEntityManager()
        val queries                        = List(
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
    ): Seq[Annotation] = {
        given entityManager: EntityManager = entityManagerFactory.createEntityManager()
        val query1                         = entityManager.createNativeQuery(AnnotationSQL.byConcept)
        query1.setParameter(1, concept)
        limit.foreach(query1.setMaxResults)
        offset.foreach(query1.setFirstResult)

        val r1 = query1.getResultList.asScala.toList
        val a1 = AnnotationSQL.resultListToAnnotations(r1)
        val a2 = executeQueryForAnnotations(a1, includeAncillaryData)
        entityManager.close()
        a2
    }

    def findByConceptWithImages(
        concept: String,
        limit: Option[Int] = None,
        offset: Option[Int] = None,
        includeAncillaryData: Boolean = false
    ): Seq[Annotation] = {
        given entityManager: EntityManager = entityManagerFactory.createEntityManager()
        val query1                         = entityManager.createNativeQuery(AnnotationSQL.byConceptWithImages)
        query1.setParameter(1, concept)
        limit.foreach(query1.setMaxResults)
        offset.foreach(query1.setFirstResult)
        val r1                             = query1.getResultList.asScala.toList
        val a1                             = AnnotationSQL.resultListToAnnotations(r1).distinct
        val a2                             = executeQueryForAnnotations(a1, includeAncillaryData)
        entityManager.close()
        a2
    }

    def findByToConceptWithImages(
        toConcept: String,
        limit: Option[Int] = None,
        offset: Option[Int] = None,
        includeAncillaryData: Boolean = false
    ): Seq[Annotation] = {
        given entityManager: EntityManager = entityManagerFactory.createEntityManager()
        val query1                         = entityManager.createNativeQuery(AnnotationSQL.byToConceptWithImages)
        query1.setParameter(1, toConcept)
        limit.foreach(query1.setMaxResults)
        offset.foreach(query1.setFirstResult)
        val r1                             = query1.getResultList.asScala.toList
        val a1                             = AnnotationSQL.resultListToAnnotations(r1).distinct
        val a2                             = executeQueryForAnnotations(a1, includeAncillaryData)
        entityManager.close()
        a2
    }

    def findImagedMomentUuidsByConceptWithImages(
        concept: String,
        limit: Option[Int] = None,
        offset: Option[Int] = None
    ): Seq[UUID] = {
        given entityManager: EntityManager = entityManagerFactory.createEntityManager()
        val query                          = entityManager.createNativeQuery(ImagedMomentSQL.byConceptWithImages)
        query.setParameter(1, concept)
        limit.foreach(query.setMaxResults)
        offset.foreach(query.setFirstResult)
        val uuids                          = query
            .getResultList()
            .asScala
            .flatMap(_.asUUID)
            .toSeq
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
        val uuids                                 = query
            .getResultList()
            .asScala
            .flatMap(_.asUUID)
            .toSeq
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
        val results                               = query.getResultList.asScala.toList
        val images                                = ImagedMomentSQL.resultListToImages(results)
        entityManager.close()
        images
    }

    def findByLinkNameAndLinkValue(
        linkName: String,
        linkValue: String,
        includeAncillaryData: Boolean = false
    ): Seq[Annotation] = {
        implicit val entityManager: EntityManager = entityManagerFactory.createEntityManager()
        val query                                 = entityManager.createNativeQuery(AssociationSQL.byLinkNameAndLinkValue)
        query.setParameter(1, linkName)
        query.setParameter(2, linkValue)
        val results                               = query.getResultList.asScala.toList
        val associations                          = AssociationSQL.resultListToAssociations(results)
        val imagedMomentUuids                     = associations.flatMap(_.imagedMomentUuid)
        val annotations                           =
            executeQueryForAnnotationsUsingImagedMomentUuids(
                imagedMomentUuids,
                includeAncillaryData
            )
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
    )(implicit entityManager: EntityManager): Seq[Annotation] = {
        val imUuids     = imagedMomentUuids.map(_.toString).distinct
        val annotations = (for (im <- imUuids.grouped(200)) yield {
            val sql     = inClause(AnnotationSQL.byImagedMomentUuids, im)
            val query   = entityManager.createNativeQuery(sql)
            val results = query.getResultList.asScala.toList
            AnnotationSQL.resultListToAnnotations(results)
        }).flatten.toSeq
        executeQueryForAnnotations(annotations, includeAncillaryData)
    }

    private def executeQueryForAnnotations(
        annotations: Seq[Annotation],
        includeAncillaryData: Boolean = false
    )(implicit entityManager: EntityManager): Seq[Annotation] = {

        // lookup associations
        val observationUuids = annotations.flatMap(_.observationUuid.map(_.toString())).distinct
        val assocTemp        = for (obs <- observationUuids.grouped(200)) yield {
            val sql2   = inClause(AssociationSQL.byObservationUuids, obs)
            val query2 = entityManager.createNativeQuery(sql2)
            val r2     = query2.getResultList.asScala.toList

            // Remove the observationUuid and imagedMomentUuid from the association
            AssociationSQL.resultListToAssociations(r2)
        }
        val associations     = assocTemp.flatten.toSeq

        // lookup imageMoments
        val imagedMomentUuids = annotations.flatMap(_.imagedMomentUuid.map(_.toString())).distinct
        val irTemp            = for (im <- imagedMomentUuids.grouped(200)) yield {
            val sql3   = inClause(ImageReferenceSQL.byImagedMomentUuids, im)
            val query3 = entityManager.createNativeQuery(sql3)
            val r3     = query3.getResultList.asScala.toList
            ImageReferenceSQL.resultListToImageReferences(r3)
        }
        val imageReferences   = irTemp.flatten.toSeq

        // join associations and imageReferences to annotations
        val a2 =
            for a <- annotations
            yield
                val assocs = associations.filter(_.observationUuid == a.observationUuid)
                val irs    = imageReferences.filter(_.imagedMomentUuid == a.imagedMomentUuid)
                a.copy(associations = assocs, imageReferences = irs)

        val xs =
            if (includeAncillaryData) findAncillaryData(a2)
            else a2

        // IMPORTANT remove all join keys from output
        xs.map(_.removeForeignKeys())
    }

    private def executeQuery(
        annotationQuery: Query,
        associationQuery: Query,
        imageReferenceQuery: Query,
        limit: Option[Int] = None,
        offset: Option[Int] = None,
        includeAncillaryData: Boolean = false
    )(implicit entityManager: EntityManager): Seq[Annotation] = {

        limit.foreach(annotationQuery.setMaxResults)
        offset.foreach(annotationQuery.setFirstResult)

        log.atDebug.log("Running annotation query")
        val r0 = annotationQuery.getResultList.asScala.toList
        log.atDebug.log(s"Transforming ${r0.size} annotations")
        val a1 = AnnotationSQL.resultListToAnnotations(r0)

        log.atDebug.log("Running association query")
        val r1           = associationQuery.getResultList.asScala.toList
        log.atDebug.log(s"Transforming ${r1.size} associations")
        val associations = AssociationSQL.resultListToAssociations(r1)
        log.atDebug.log("Joining annotations to associations")
        val a2           = AssociationSQL.join(a1, associations)

        log.atDebug.log("Running imageReference query")
        val r2              = imageReferenceQuery.getResultList.asScala.toList
        log.atDebug.log(s"Transforming ${r2.size} imageReferences")
        val imageReferences = ImageReferenceSQL.resultListToImageReferences(r2)
        log.atDebug.log("Joining annotations to imageReferences")
        val a3              = ImageReferenceSQL.join(a2, imageReferences)

        val xs =
            if (includeAncillaryData)
                findAncillaryData(a3)
            else
                a3

        // IMPORTANT remove all join keys from output
        xs.map(_.removeForeignKeys())

    }

    private def findAncillaryData(
        annotations: Seq[Annotation]
    )(implicit entityManager: EntityManager): Seq[Annotation] = {

        log.atDebug.log(() => s"Running ancillaryData queries for ${annotations.size} annotations")
        val groups = for (annos <- annotations.grouped(200)) yield {
            val ims   = annos.flatMap(_.imagedMomentUuid.map(_.toString())).distinct
            val sql   = inClause(AncillaryDatumSQL.byImagedMomentUuid, ims)
            val query = entityManager.createNativeQuery(sql)
            val rows  = query.getResultList.asScala.toList
            val data  = AncillaryDatumSQL.resultListToAnncillaryData(rows)
            AncillaryDatumSQL.join(annos, data)
        }
        groups.flatten.toSeq
    }

}
