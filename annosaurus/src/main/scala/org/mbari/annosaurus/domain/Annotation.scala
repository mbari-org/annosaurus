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
import scala.jdk.CollectionConverters.*
import org.mbari.annosaurus.repository.jpa.entity.ImageReferenceEntity

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
    videoReferenceUuid: Option[UUID] = None,
    lastUpdated: Option[java.time.Instant] = None
) extends ToSnakeCase[AnnotationSC]
    with ToEntity[ImagedMomentEntity]:

    lazy val elapsedTime: Option[Duration]   = elapsedTimeMillis.map(Duration.ofMillis(_))
    lazy val duration: Option[Duration]      = durationMillis.map(Duration.ofMillis(_))
    lazy val validTimecode: Option[Timecode] = timecode.map(Timecode(_))

    def removeForeignKeys(): Annotation =
        copy(
            associations = associations.map(_.removeForeignKeys()),
            imageReferences = imageReferences.map(_.removeForeignKeys()),
            ancillaryData = ancillaryData.map(_.removeForeignKeys())
        )

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
            videoReferenceUuid,
            lastUpdated
        )

    override def toEntity: ImagedMomentEntity =
        val im  = ImagedMomentEntity(
            videoReferenceUuid.orNull,
            recordedTimestamp.orNull,
            validTimecode.orNull,
            elapsedTime.orNull
        )
        im.setUuid(imagedMomentUuid.orNull)
        val obs = ObservationEntity(
            concept.orNull,
            duration.orNull,
            observationTimestamp.orNull,
            observer.orNull,
            group.orNull,
            activity.orNull
        )
        obs.setUuid(observationUuid.orNull)
        associations.foreach(x => obs.addAssociation(x.toEntity))
        im.addObservation(obs)
        imageReferences.foreach(x => im.addImageReference(x.toEntity))
        ancillaryData.foreach(x => im.setAncillaryDatum(x.toEntity))
        im

object Annotation extends FromEntity[ObservationEntity, Annotation]:

    override def from(entity: ObservationEntity, extend: Boolean = false): Annotation =

        val imagedMomentOpt = Option(entity.getImagedMoment)

        // Do not extend data here. As that would include redundant information
        val data =
            if extend then
                imagedMomentOpt
                    .flatMap(x => Option(x.getAncillaryDatum))
                    .map(x => CachedAncillaryDatum.from(x, false))
            else None

        // NOTE: An annotaiton ALWAYS includes imageReferences and associations
        // Do not extend image references here. As that would include redundant information
        val imageReferences =
            imagedMomentOpt
                .map(_.getImageReferences.asScala)
                .getOrElse(Nil)
                .map(x => ImageReference.from(x, false))
                .toSeq

        // We always include associations
        // Do not extend associations here. As that would include redundant information
        val associations = entity.getAssociations.asScala.map(Association.from(_, false)).toSeq

        Annotation(
            Option(entity.getActivity),
            data,
            associations,
            Option(entity.getConcept),
            Option(entity.getDuration).map(_.toMillis),
            imagedMomentOpt.flatMap(x => Option(x.getElapsedTime)).map(_.toMillis),
            Option(entity.getGroup),
            imagedMomentOpt.flatMap(im => Option(im.getUuid)),
            imageReferences,
            Option(entity.getObservationTimestamp),
            Option(entity.getUuid),
            Option(entity.getObserver),
            imagedMomentOpt.flatMap(im => Option(im.getRecordedTimestamp)),
            imagedMomentOpt.flatMap(im => Option(im.getTimecode).map(_.toString)),
            imagedMomentOpt.flatMap(im => Option(im.getVideoReferenceUuid)),
            Option(entity.getLastUpdatedTime).map(_.toInstant)
        )

    def fromImagedMoment(entity: ImagedMomentEntity, extend: Boolean = false): Seq[Annotation] =
        entity.getObservations.asScala.map(x => from(x, extend)).toSeq

    def toEntities(
        annotations: Seq[Annotation],
        extend: Boolean = false
    ): Seq[ImagedMomentEntity] =
        if annotations.isEmpty then Nil
        else if annotations.size == 1 then Seq(annotations.head.toEntity)
        else
            val imagedMoments = annotations.map(_.toEntity)

            // Consolidate imaged moments
            val imagedMomentMap   = mutable.Map[ImagedMomentEntity, Seq[ImagedMomentEntity]]()
            val imageReferenceMap = mutable.Map[ImagedMomentEntity, Seq[ImageReferenceEntity]]()
            for im <- imagedMoments
            do
                val imOpt = imagedMomentMap.get(im)
                imOpt match
                    case None    =>
                        imagedMomentMap.put(im, Nil)
                        imageReferenceMap.put(im, im.getImageReferences.asScala.toSeq)
                    case Some(x) =>
                        imagedMomentMap.put(im, x.appended(im))
                        imageReferenceMap.put(
                            im,
                            imageReferenceMap(im).appendedAll(im.getImageReferences.asScala.toSeq)
                        )

            for
                (baseIm, xs) <- imagedMomentMap.iterator
                x            <- xs
            do
                val irs = imageReferenceMap(baseIm).distinctBy(_.getUrl())
                irs.foreach(i => baseIm.addImageReference(i))

                x.getObservations.forEach(o => baseIm.addObservation(o))

                if baseIm.getAncillaryDatum != null && x.getAncillaryDatum != null then
                    baseIm.setAncillaryDatum(x.getAncillaryDatum)

            imagedMomentMap.keys.toSeq

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
    video_reference_uuid: Option[UUID] = None,
    last_udpated: Option[java.time.Instant] = None
) extends ToCamelCase[Annotation]:

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
            video_reference_uuid,
            last_udpated
        )

final case class BulkAnnotationSC(
    activity: Option[String] = None,
    ancillary_data: Option[CachedAncillaryDatumSC] = None,
    associations: Option[Seq[AssociationSC]] = None,
    concept: Option[String] = None,
    duration_millis: Option[Long] = None,
    elapsed_time_millis: Option[Long] = None,
    group: Option[String] = None,
    imaged_moment_uuid: Option[UUID] = None,
    image_references: Option[Seq[ImageReferenceSC]] = None,
    observation_timestamp: Option[Instant] = None,
    observation_uuid: Option[UUID] = None,
    observer: Option[String] = None,
    recorded_timestamp: Option[Instant] = None,
    timecode: Option[String] = None,
    video_reference_uuid: Option[UUID] = None
) extends ToCamelCase[Annotation]:

    override def toCamelCase: Annotation =
        Annotation(
            activity,
            ancillary_data.map(_.toCamelCase),
            associations.getOrElse(Nil).map(_.toCamelCase),
            concept,
            duration_millis,
            elapsed_time_millis,
            group,
            imaged_moment_uuid,
            image_references.getOrElse(Nil).map(_.toCamelCase),
            observation_timestamp,
            observation_uuid,
            observer,
            recorded_timestamp,
            timecode,
            video_reference_uuid
        )
