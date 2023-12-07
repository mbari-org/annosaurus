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

package org.mbari.vars.annotation.model.simple

import com.google.gson.annotations.Expose
import org.mbari.vars.annotation.Constants
import org.mbari.vars.annotation.dao.jdbc.AnnotationSQL

import java.sql.Timestamp
import java.time.Instant
import java.util
import java.util.{UUID, List => JList}
import jakarta.persistence.{EntityManager, Query}
import scala.jdk.CollectionConverters._

class QueryConstraints {

  /**
    * If present, limits returns to annotations that belong to these videoReferenceUuids
    */
  @Expose(serialize = true)
  var videoReferenceUuids: JList[UUID]    = new util.ArrayList[UUID]()
  def videoReferenceUuidSeq(): List[UUID] = videoReferenceUuids.asScala.toList

  /**
    * If present, only returns that match one of these concepts is returned
    */
  @Expose(serialize = true)
  var concepts: JList[String]    = new util.ArrayList[String]()
  def conceptSeq(): List[String] = concepts.asScala.toList

  @Expose(serialize = true)
  var observers: JList[String]    = new util.ArrayList[String]()
  def observerSeq(): List[String] = observers.asScala.toList

  @Expose(serialize = true)
  var groups: JList[String]    = new util.ArrayList[String]()
  def groupSeq(): List[String] = groups.asScala.toList

  @Expose(serialize = true)
  var activities: JList[String]   = new util.ArrayList[String]()
  def activitySeq(): List[String] = activities.asScala.toList

  @Expose(serialize = true)
  var minDepth: Option[Double] = Option.empty

  @Expose(serialize = true)
  var maxDepth: Option[Double] = Option.empty

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
  var linkName: Option[String] = Option.empty

  @Expose(serialize = true)
  var linkValue: Option[String] = Option.empty

  @Expose(serialize = true)
  var limit: Int = 5000

  @Expose(serialize = true)
  var offset: Int = 0

  /**
    * When set to `true` ancillary data is returned
    */
  @Expose(serialize = true)
  var data: Boolean = false

  @Expose(serialize = true)
  var missionContacts: JList[String] = new util.ArrayList[String]()
  def missionContactSeq(): List[String] = missionContacts.asScala.toList

  @Expose(serialize = true)
  var platformName: Option[String] = Option.empty

  @Expose(serialize = true)
  var missionId: Option[String] = Option.empty

}

object QueryConstraints {

  /**
    *
    * @param concepts
    * @param videoReferenceUuids
    * @param observers
    * @param groups
    * @param activities
    * @param minDepth
    * @param maxDepth
    * @param minLat
    * @param maxLat
    * @param minLon
    * @param maxLon
    * @param minTimestamp
    * @param maxTimestamp
    * @param linkName
    * @param linkValue
    * @param missionContact
    * @param limit
    * @param offset
    * @return
    */

  def apply(concepts: List[String] = Nil,
            videoReferenceUuids: List[UUID] = Nil,
            observers: List[String] = Nil,
            groups: List[String] = Nil,
            activities: List[String] = Nil,
            // toConcepts: List[String] = Nil,
            minDepth: Option[Double] = Option.empty,
            maxDepth: Option[Double] = Option.empty,
            minLat: Option[Double] = Option.empty,
            maxLat: Option[Double] = Option.empty,
            minLon: Option[Double] = Option.empty,
            maxLon: Option[Double] = Option.empty,
            minTimestamp: Option[Instant] = Option.empty,
            maxTimestamp: Option[Instant] = Option.empty,
            linkName: Option[String] = Option.empty,
            linkValue: Option[String] = Option.empty,
            missionContacts: List[String] = Nil,
            platformName: Option[String] = Option.empty,
            missionId: Option[String] = Option.empty,
            limit: Int = 5000,
            offset: Int = 0) = {

    val qc = new QueryConstraints
    qc.concepts = concepts.asJava
    qc.videoReferenceUuids = videoReferenceUuids.asJava
    qc.observers = observers.asJava
    qc.groups = groups.asJava
    qc.activities = activities.asJava
    qc.minDepth = minDepth
    qc.maxDepth = maxDepth
    qc.minLat = minLat
    qc.maxLat = maxLat
    qc.minLon = minLon
    qc.maxLon = maxLon
    qc.minTimestamp = minTimestamp
    qc.maxTimestamp = maxTimestamp
    qc.linkName = linkName
    qc.linkValue = linkValue
    qc.missionContacts = missionContacts.asJava
    qc.platformName = platformName
    qc.missionId = missionId
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
  def toFromWhereSql(qc: QueryConstraints): String = {
    import org.mbari.vars.annotation.dao.jdbc.AnnotationSQL._

    val sqlConstraints = List(
      if (qc.conceptSeq().nonEmpty) Some("(obs.concept IN (A?) OR ass.to_concept IN (A?))")
      else None,
      if (qc.videoReferenceUuidSeq().nonEmpty) Some("im.video_reference_uuid IN (B?)") else None,
      if (qc.observerSeq().nonEmpty) Some("obs.observer IN (C?)") else None,
      if (qc.groupSeq().nonEmpty) Some("obs.observation_group IN (D?)") else None,
      if (qc.activitySeq().nonEmpty) Some("obs.activity IN (E?)") else None,
      if (qc.missionContactSeq().nonEmpty) Some("vri.mission_contact IN (F?)") else None,
      qc.minDepth.map(_ => "ad.depth_meters >= ?"),
      qc.maxDepth.map(_ => "ad.depth_meters < ?"),
      qc.minLon.map(_ => "ad.longitude >= ?"),
      qc.maxLon.map(_ => "ad.longitude < ?"),
      qc.minLat.map(_ => "ad.latitude >= ?"),
      qc.maxLat.map(_ => "ad.latitude < ?"),
      qc.minTimestamp.map(_ => "im.recorded_timestamp >= ?"),
      qc.maxTimestamp.map(_ => "im.recorded_timestamp < ?"),
      qc.linkName.map(_ => "ass.link_name = ?"),
      qc.linkValue.map(_ => "ass.link_value = ?"),
      qc.platformName.map(_ => "vri.platform_name = ?"),
      qc.missionId.map(_ => "vri.mission_id = ?")
    ).flatten

    FROM_WITH_ANCILLARY_DATA + " WHERE " + sqlConstraints.mkString(" AND ")

  }


  private def toSql(qc: QueryConstraints, 
      selectStatement: String, 
      orderStatement: String = AnnotationSQL.ORDER): String = {
    // import org.mbari.vars.annotation.dao.jdbc.AnnotationSQL._
    val fromWhere = toFromWhereSql(qc)
    selectStatement + fromWhere + orderStatement
  }

  private def toCountSql(qc: QueryConstraints): String = {
    val fromWhere = toFromWhereSql(qc)
    val select    = "SELECT COUNT(DISTINCT obs.uuid)"
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
  def toQuery(
      qc: QueryConstraints,
      entityManager: EntityManager,
      selectStatement: String = AnnotationSQL.SELECT,
      orderStatment: String = AnnotationSQL.ORDER
  ): Query = {
    val sql = toSql(qc, selectStatement, orderStatment)
    buildQuery(qc, entityManager, sql)
  }

  private def toGeographicRangeSql(qc: QueryConstraints): String = {
    val fromWhere = toFromWhereSql(qc) +
      " AND ad.longitude IS NOT NULL AND ad.latitude IS NOT NULL AND ad.depth_meters IS NOT NULL"
    val select =
      "SELECT min(ad.latitude), max(ad.latitude), min(ad.longitude), max(ad.longitude), min(ad.depth_meters), max(ad.depth_meters) "
    select + " " + fromWhere
  }

  def toGeographicRangeQuery(qc: QueryConstraints, entityManager: EntityManager): Query = {
    val sql = toGeographicRangeSql(qc)
    buildQuery(qc, entityManager, sql)
  }

  private def buildQuery(
      qc: QueryConstraints,
      entityManager: EntityManager,
      base: String
  ): Query = {

    // JPA doesn't handle in clauses well. So this is a cludge
    def replaceInClause[A](xs: Iterable[A], sql: String, target: String): String =
      if (xs.nonEmpty) {
        val s = xs.mkString("('", "','", "')")
        sql.replace(target, s)
      }
      else sql

    val a = replaceInClause(qc.conceptSeq(), base, "(A?)")
    val b = replaceInClause(qc.videoReferenceUuidSeq(), a, "(B?)")
    val c = replaceInClause(qc.observerSeq(), b, "(C?)")
    val d = replaceInClause(qc.groupSeq(), c, "(D?)")
    val e = replaceInClause(qc.activitySeq(), d, "(E?)")
    val sql = replaceInClause(qc.missionContactSeq(), e, "(F?)")

    // Bind the params in the correct order. This is the same order they are found in the SQL.
    // The flattened list excludes empty Options
    def params = List(
      qc.minDepth,
      qc.maxDepth,
      qc.minLon,
      qc.maxLon,
      qc.minLat,
      qc.maxLat,
      qc.minTimestamp.map(Timestamp.from),
      qc.maxTimestamp.map(Timestamp.from),
      qc.linkName,
      qc.linkValue,
      qc.platformName,
      qc.missionId).flatten
    val query = entityManager.createNativeQuery(sql)
    for {
      i <- params.indices
    } {
      query.setParameter(i + 1, params(i))
    }
    query.setMaxResults(qc.limit)
    query.setFirstResult(qc.offset)

    query

  }

}
