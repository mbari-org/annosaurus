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

import java.time.{Duration, Instant}
import java.util.UUID
import org.mbari.annosaurus.repository.jpa.entity.ObservationEntity
import org.mbari.annosaurus.model.MutableObservation

final case class Observation(
    concept: String,
    durationMillis: Option[Long] = None,
    group: Option[String] = None,
    activity: Option[String] = None,
    observer: Option[String] = None,
    observationTimestamp: Option[Instant] = None,
    associations: Seq[Association] = Nil,
    uuid: Option[UUID] = None,
    lastUpdated: Option[Instant] = None
) extends ToSnakeCase[ObservationSC]
    with ToEntity[ObservationEntity] {
    override def toSnakeCase: ObservationSC =
        ObservationSC(
            concept,
            durationMillis,
            group,
            activity,
            observer,
            observationTimestamp,
            associations.map(_.toSnakeCase),
            uuid,
            lastUpdated
        )

    override def toEntity: ObservationEntity =
        val entity = new ObservationEntity
        entity.concept = concept
        durationMillis.foreach(d => entity.duration = Duration.ofMillis(d))
        group.foreach(entity.group = _)
        activity.foreach(entity.activity = _)
        observer.foreach(entity.observer = _)
        observationTimestamp.foreach(entity.observationDate = _)
        associations.foreach(a => entity.addAssociation(a.toEntity))
        uuid.foreach(entity.uuid = _)
        entity

    lazy val duration: Option[Duration] = durationMillis.map(Duration.ofMillis)
}

object Observation extends FromEntity[MutableObservation, Observation] {
    def from(entity: MutableObservation): Observation = Observation(
        entity.concept,
        Option(entity.duration).map(_.toMillis),
        Option(entity.group),
        Option(entity.activity),
        Option(entity.observer),
        Option(entity.observationDate),
        entity.associations.map(Association.from).toSeq,
        Option(entity.uuid),
        entity.lastUpdated
    )
}

final case class ObservationSC(
    concept: String,
    duration_millis: Option[Long] = None,
    group: Option[String] = None,
    activity: Option[String] = None,
    observer: Option[String] = None,
    observation_timestamp: Option[Instant] = None,
    associations: Seq[AssociationSC] = Nil,
    uuid: Option[UUID] = None,
    last_updated_time: Option[Instant] = None
) extends ToCamelCase[Observation] {
    override def toCamelCase: Observation =
        Observation(
            concept,
            duration_millis,
            group,
            activity,
            observer,
            observation_timestamp,
            associations.map(_.toCamelCase),
            uuid,
            last_updated_time
        )
}
