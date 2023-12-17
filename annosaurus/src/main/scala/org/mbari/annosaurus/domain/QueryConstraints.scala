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

package org.mbari.annosaurus.domain

import java.util.UUID
import java.time.Instant
import org.mbari.annosaurus.model.simple.{QueryConstraints => QueryConstraintsSqlBuilder}
import scala.jdk.CollectionConverters.*

final case class QueryConstraints(
    videoReferenceUuids: List[UUID] = Nil,
    concepts: List[String] = Nil,
    observers: List[String] = Nil,
    groups: List[String] = Nil,
    activities: List[String] = Nil,
    minDepth: Option[Double] = None,
    maxDepth: Option[Double] = None,
    minLat: Option[Double] = None,
    maxLat: Option[Double] = None,
    minLon: Option[Double] = None,
    maxLon: Option[Double] = None,
    minTimestamp: Option[Instant] = None,
    maxTimestamp: Option[Instant] = None,
    linkName: Option[String] = None,
    linkValue: Option[String] = None,
    limit: Option[Int] = Some(5000),
    offset: Option[Int] = Some(0),
    data: Option[Boolean] = Some(false),
    missionContacts: List[String] = Nil,
    platformName: Option[String] = None,
    missionId: Option[String] = None
) extends ToSnakeCase[QueryConstraintsSC] {
    def toSqlBuilder: QueryConstraintsSqlBuilder = {
        QueryConstraintsSqlBuilder(
            concepts,
            videoReferenceUuids,
            observers,
            groups,
            activities,
            minDepth,
            maxDepth,
            minLat,
            maxLat,
            minLon,
            maxLon,
            minTimestamp,
            maxTimestamp,
            linkName,
            linkValue,
            missionContacts,
            platformName,
            missionId,
            limit.getOrElse(5000),
            offset.getOrElse(0)
        )
    }

    def toSnakeCase: QueryConstraintsSC = {
        QueryConstraintsSC(
            videoReferenceUuids,
            concepts,
            observers,
            groups,
            activities,
            minDepth,
            maxDepth,
            minLat,
            maxLat,
            minLon,
            maxLon,
            minTimestamp,
            maxTimestamp,
            linkName,
            linkValue,
            limit,
            offset,
            data,
            missionContacts,
            platformName,
            missionId
        )
    }
}

final case class QueryConstraintsSC(
    video_reference_uuids: List[UUID] = Nil,
    concepts: List[String] = Nil,
    observers: List[String] = Nil,
    groups: List[String] = Nil,
    activities: List[String] = Nil,
    min_depth: Option[Double] = None,
    max_depth: Option[Double] = None,
    min_lat: Option[Double] = None,
    max_lat: Option[Double] = None,
    min_lon: Option[Double] = None,
    max_lon: Option[Double] = None,
    min_timestamp: Option[Instant] = None,
    max_timestamp: Option[Instant] = None,
    link_name: Option[String] = None,
    link_value: Option[String] = None,
    limit: Option[Int] = Some(5000),
    offset: Option[Int] = Some(0),
    data: Option[Boolean] = Some(false),
    mission_contacts: List[String] = Nil,
    platform_name: Option[String] = None,
    mission_id: Option[String] = None
) extends ToCamelCase[QueryConstraints] {
    def toCamelCase: QueryConstraints = {
        QueryConstraints(
            video_reference_uuids,
            concepts,
            observers,
            groups,
            activities,
            min_depth,
            max_depth,
            min_lat,
            max_lat,
            min_lon,
            max_lon,
            min_timestamp,
            max_timestamp,
            link_name,
            link_value,
            limit,
            offset,
            data,
            mission_contacts,
            platform_name,
            mission_id
        )
    }
}
