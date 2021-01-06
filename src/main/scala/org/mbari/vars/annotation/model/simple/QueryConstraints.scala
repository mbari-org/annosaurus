package org.mbari.vars.annotation.model.simple

import com.google.gson.annotations.Expose
import org.mbari.vars.annotation.Constants

import java.time.Instant
import java.util
import java.util.{UUID, List => JList}
import javax.persistence.{EntityManager, Query}
import scala.jdk.CollectionConverters._

class QueryConstraints {

  /**
    * If present, limits returns to annotations that belong to these videoReferenceUuids
    */
  @Expose(serialize = true)
  var videoReferenceUuids: JList[UUID] = new util.ArrayList[UUID]()
  def videoReferenceUuidSeq(): List[UUID] = videoReferenceUuids.asScala.toList

  /**
    * If present, only returns that match one of these concepts is returned
    */
  @Expose(serialize = true)
  var concepts: JList[String] = new util.ArrayList[String]()
  def conceptSeq(): List[String] = concepts.asScala.toList

  @Expose(serialize = true)
  var minLat: Option[Double] = Option.empty

  @Expose(serialize = true)
  var maxLat: Option[Double] = Option.empty

  @Expose(serialize = true)
  var minLon: Option[Double] = Option.empty

  @Expose(serialize = true)
  var maxLon: Option[Double] = Option.empty

  @Expose(serialize = true)
  var minTimestamp: Option[Instant] = Option.empty

  @Expose(serialize = true)
  var maxTimestamp: Option[Instant] = Option.empty

  @Expose(serialize = true)
  var limit: Int = 5000

  @Expose(serialize = true)
  var offset: Int = 0

  /**
    * When set to `true` ancillary data is returned
    */
  @Expose(serialize = true)
  var data: Boolean = false

}

object QueryConstraints {

  /**
    * Factory method for building QueryConstraints
    * @param concepts
    * @param videoReferenceUuids
    * @param minLat
    * @param maxLat
    * @param minLon
    * @param maxLon
    * @param minTimestamp
    * @param maxTimestamp
    * @param limit
    * @param offset
    * @return
    */
  def apply(concepts: List[String] = Nil,
            videoReferenceUuids: List[UUID] = Nil,
            minLat: Option[Double] = Option.empty,
            maxLat: Option[Double] = Option.empty,
            minLon: Option[Double] = Option.empty,
            maxLon: Option[Double] = Option.empty,
            minTimestamp: Option[Instant] = Option.empty,
            maxTimestamp: Option[Instant] = Option.empty,
            limit: Int = 5000,
            offset: Int = 0) = {
    val qc = new QueryConstraints
    qc.concepts = concepts.asJava
    qc.videoReferenceUuids = videoReferenceUuids.asJava
    qc.minLat = minLat
    qc.maxLat = maxLat
    qc.minLon = minLon
    qc.maxLon = maxLon
    qc.minTimestamp = minTimestamp
    qc.maxTimestamp = maxTimestamp
    qc.limit = limit
    qc.offset = offset
    qc
  }

  /**
    * Allow constraints to be passed as camel case or snake case
    * @param json
    * @return
    */
  def fromJson(json: String): QueryConstraints = {
    val gson = if (json.contains("_")) Constants.GSON else Constants.GSON_CAMEL_CASE
    gson.fromJson(json, classOf[QueryConstraints])
  }

  /**
    * Generates a SQL template for use to build a query. It's not executable SQL though!
    * @param qc
    * @return
    */
  private def toSql(qc: QueryConstraints): String = {
    import org.mbari.vars.annotation.dao.jdbc.AnnotationSQL._

    val sqlConstraints = List(
      if (qc.conceptSeq().nonEmpty) Some("obs.concept IN (A?)") else None,
      if (qc.videoReferenceUuidSeq().nonEmpty) Some("im.video_reference_uuid IN (B?)") else None,
      qc.minLon.map(_ => "ad.longitude >= ?"),
      qc.maxLon.map(_ => "ad.longitude <= ?"),
      qc.minLat.map(_ => "ad.latitude >= ?"),
      qc.maxLat.map(_ => "ad.latitude <= ?"),
      qc.minTimestamp.map(_ => "im.recorded_timestamp >= ?"),
      qc.maxTimestamp.map(_ => "im.recorded_timestamp <= ?")
    ).flatten

    SELECT + FROM_WITH_ANCILLARY_DATA + " WHERE " + sqlConstraints.mkString(" AND ") + ORDER
  }

  private def toCountSql(qc: QueryConstraints): String = {
    val sql = toSql(qc)
    val select = "SELECT COUNT(DISTINCT obs.uuid)"
    val fromWhere = sql.substring(sql.indexOf("FROM"))
    select + " " + fromWhere
  }

  def toCountQuery(qc: QueryConstraints, entityManager: EntityManager): Query = {
    val sql = toCountSql(qc)
    buildQuery(qc, entityManager, sql)
  }

  /**
    *
    * @param qc
    * @param entityManager
    * @return
    */
  def toQuery(qc: QueryConstraints, entityManager: EntityManager): Query = {
    val sql = toSql(qc)
    buildQuery(qc, entityManager, sql)
  }

  private def buildQuery(qc: QueryConstraints, entityManager: EntityManager, base: String): Query = {

    // JPA doesn't handle in clauses well. So this is a cludge
    val sql = {

      val a = if (!qc.concepts.isEmpty) {
        val xs = qc.conceptSeq().mkString("('", "','", "')")
        base.replace("(A?)", xs)
      }
      else base

      if (!qc.videoReferenceUuids.isEmpty) {
        val xs = qc.videoReferenceUuidSeq().mkString("('", "','", "')")
        a.replace("(B?)", xs)
      }
      else a

    }

    // Bind the params in the correct order. This is the same order they are found in the SQL.
    // The flattened list excludes empty Options
    def params = List(qc.minLon, qc.maxLon, qc.minLat, qc.maxLat, qc.minTimestamp, qc.maxTimestamp).flatten
    val query = entityManager.createNamedQuery(sql)
    for {
      i <- params.indices
    } {
      query.setParameter(i, params(i))
    }
    query.setMaxResults(qc.limit)
    query.setFirstResult(qc.offset)

    query

  }

}
