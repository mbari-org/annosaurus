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

import java.time.Instant
import java.util.UUID
import scala.jdk.CollectionConverters.*

final case class QueryConstraints(
    videoReferenceUuids: Seq[UUID] = Nil,
    concepts: Seq[String] = Nil,
    observers: Seq[String] = Nil,
    groups: Seq[String] = Nil,
    activities: Seq[String] = Nil,
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
    missionContacts: Seq[String] = Nil,
    platformName: Option[String] = None,
    missionId: Option[String] = None
) extends ToSnakeCase[QueryConstraintsSC]:

    // Used by Circe reify. If serializing fails, the circe codec will fall back to snake_case
    require(
        videoReferenceUuids.nonEmpty || concepts.nonEmpty || observers.nonEmpty || groups.nonEmpty || activities.nonEmpty || minDepth.isDefined || maxDepth.isDefined || minLat.isDefined || maxLat.isDefined || minLon.isDefined || maxLon.isDefined || minTimestamp.isDefined || maxTimestamp.isDefined || linkName.isDefined || linkValue.isDefined || missionContacts.nonEmpty || platformName.isDefined || missionId.isDefined,
        "At least one constraint must be defined"
    )

    val definedLimit: Int    = limit.getOrElse(5000)
    val definedOffset: Int   = offset.getOrElse(0)
    val includeData: Boolean = data.getOrElse(false)

    def toSnakeCase: QueryConstraintsSC =
        QueryConstraintsSC(
            Option(videoReferenceUuids),
            Option(concepts),
            Option(observers),
            Option(groups),
            Option(activities),
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
            Option(missionContacts),
            platformName,
            missionId
        )

final case class QueryConstraintsSC(
    video_reference_uuids: Option[Seq[UUID]] = None,
    concepts: Option[Seq[String]] = None,
    observers: Option[Seq[String]] = None,
    groups: Option[Seq[String]] = None,
    activities: Option[Seq[String]] = None,
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
    mission_contacts: Option[Seq[String]] = None,
    platform_name: Option[String] = None,
    mission_id: Option[String] = None
) extends ToCamelCase[QueryConstraints]:
    def toCamelCase: QueryConstraints =
        QueryConstraints(
            video_reference_uuids.getOrElse(Nil),
            concepts.getOrElse(Nil),
            observers.getOrElse(Nil),
            groups.getOrElse(Nil),
            activities.getOrElse(Nil),
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
            mission_contacts.getOrElse(Nil),
            platform_name,
            mission_id
        )
