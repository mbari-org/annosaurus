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
import org.mbari.annosaurus.repository.jpa.entity.ImagedMomentEntity
import org.mbari.vcr4j.time.Timecode
import java.time.Duration
import java.time.Instant
import org.mbari.annosaurus.repository.jpa.entity.ObservationEntity
import scala.collection.mutable

final case class Annotation(
    activity: Option[String] = None,
    ancillaryData: Option[CachedAncillaryDatum] = None,
    associations: Seq[Association] = Nil,
    concept: Option[String] = None,
    durationMillis: Option[Long] = None,
    elapsedTimeMillis: Option[Long] = None,
    group: Option[String] = None,
    imagedMomentUuid: Option[UUID] = None,
    imageReferences: Seq[ImageReference] = Nil,
    observationTimestamp: Option[Instant] = None,
    observationUuid: Option[UUID] = None,
    observer: Option[String] = None,
    recordedTimestamp: Option[Instant] = None,
    timecode: Option[String] = None,
    videoReferenceUuid: Option[UUID] = None
) extends ToSnakeCase[AnnotationSC] {

    lazy val elapsedTime: Option[Duration]   = elapsedTimeMillis.map(Duration.ofMillis(_))
    lazy val duration: Option[Duration]      = durationMillis.map(Duration.ofMillis(_))
    lazy val validTimecode: Option[Timecode] = timecode.map(Timecode(_))

    override def toSnakeCase: AnnotationSC =
        AnnotationSC(
            activity,
            ancillaryData.map(_.toSnakeCase),
            associations.map(_.toSnakeCase),
            concept,
            durationMillis,
            elapsedTimeMillis,
            group,
            imagedMomentUuid,
            imageReferences.map(_.toSnakeCase),
            observationTimestamp,
            observationUuid,
            observer,
            recordedTimestamp,
            timecode,
            videoReferenceUuid
        )

}

object Annotation extends FromEntity[ObservationEntity, Annotation] {

    override def from(entity: ObservationEntity, extend: Boolean = false): Annotation =
        val ad =
            if extend && entity.imagedMoment != null then
                Option(entity.imagedMoment.ancillaryDatum).map(x =>
                    CachedAncillaryDatum.from(x, false)
                )
            else None

        val imOpt = Option(entity.imagedMoment)

        val irs = imOpt
            .map(_.imageReferences)
            .getOrElse(Nil)
            .map(x => ImageReference.from(x, false))
            .toSeq

        Annotation(
            Option(entity.activity),
            ad,
            entity.associations.map(x => Association.from(x, false)).toSeq,
            Option(entity.concept),
            Option(entity.duration).map(_.toMillis),
            imOpt.flatMap(x => Option(x.elapsedTime)).map(_.toMillis),
            Option(entity.group),
            imOpt.map(_.uuid),
            irs,
            Option(entity.observationDate),
            Option(entity.uuid),
            Option(entity.observer),
            imOpt.map(_.recordedDate),
            imOpt.flatMap(x => Option(x.timecode).map(_.toString)),
            imOpt.map(_.videoReferenceUUID)
        )

    def fromImagedMoment(entity: ImagedMomentEntity, extend: Boolean = false): Seq[Annotation] = {
        entity.observations.map(x => from(x, extend)).toSeq
    }

    def toEntities(annotations: Seq[Annotation]): Seq[ImagedMomentEntity] = {

        val imagedMoments =
            for (imagedMomentUuid, annos) <- annotations
                                                 .filter(x =>
                                                     x.imagedMomentUuid.isDefined && x
                                                         .videoReferenceUuid
                                                         .isDefined && x.concept.isDefined
                                                 )
                                                 .groupBy(_.imagedMomentUuid.get)
            yield
                val a  = annos.head
                val tc = a.timecode.map(Timecode(_))
                val et = a.elapsedTimeMillis.map(Duration.ofMillis(_))
                val im = ImagedMomentEntity(a.videoReferenceUuid, a.recordedTimestamp, tc, et)
                im.uuid = imagedMomentUuid

                for a <- annos
                do
                    val d   = a.durationMillis.map(Duration.ofMillis(_))
                    val obs = ObservationEntity(
                        a.concept.get,
                        d,
                        a.observationTimestamp,
                        a.observer,
                        a.group,
                        a.activity
                    )
                    a.observationUuid.foreach(obs.uuid = _)
                    im.addObservation(obs)

                    a.associations.foreach(x => obs.addAssociation(x.toEntity))
                    a.imageReferences
                        .foreach(x =>
                            im.imageReferences.find(i => i.url == x.url) match
                                case Some(link_value) => // Do nothing. Image already exists
                                case None             => im.addImageReference(x.toEntity)
                        )
                im

        imagedMoments.toSeq

    }

}

final case class AnnotationSC(
    activity: Option[String] = None,
    ancillary_data: Option[CachedAncillaryDatumSC] = None,
    associations: Seq[AssociationSC] = Nil,
    concept: Option[String] = None,
    duration_millis: Option[Long] = None,
    elapsed_time_millis: Option[Long] = None,
    group: Option[String] = None,
    imaged_moment_uuid: Option[UUID] = None,
    image_references: Seq[ImageReferenceSC] = Nil,
    observation_timestamp: Option[Instant] = None,
    observation_uuid: Option[UUID] = None,
    observer: Option[String] = None,
    recorded_timestamp: Option[Instant] = None,
    timecode: Option[String] = None,
    video_reference_uuid: Option[UUID] = None
) extends ToCamelCase[Annotation] {

    override def toCamelCase: Annotation =
        Annotation(
            activity,
            ancillary_data.map(_.toCamelCase),
            associations.map(_.toCamelCase),
            concept,
            duration_millis,
            elapsed_time_millis,
            group,
            imaged_moment_uuid,
            image_references.map(_.toCamelCase),
            observation_timestamp,
            observation_uuid,
            observer,
            recorded_timestamp,
            timecode,
            video_reference_uuid
        )
}
