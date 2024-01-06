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
) extends ToSnakeCase[AnnotationSC]
    with ToEntity[ImagedMomentEntity] {

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

}

object Annotation extends FromEntity[ObservationEntity, Annotation] {

    override def from(entity: ObservationEntity, extend: Boolean = false): Annotation =

        val imOpt = Option(entity.getImagedMoment)

        // Do not extend data here. As that would include redundant information
        val data =
            if extend then
                imOpt
                    .flatMap(x => Option(x.getAncillaryDatum))
                    .map(x => CachedAncillaryDatum.from(x, false))
            else None

        // NOTE: An annotaiton ALWAYS includes imageReferences and associations
        // Do not extend image references here. As that would include redundant information
        val imageReferences =
            imOpt
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
            imOpt.flatMap(x => Option(x.getElapsedTime)).map(_.toMillis),
            Option(entity.getGroup),
            imOpt.flatMap(im => Option(im.getUuid)),
            imageReferences,
            Option(entity.getObservationTimestamp),
            Option(entity.getUuid),
            Option(entity.getObserver),
            imOpt.flatMap(im => Option(im.getRecordedTimestamp)),
            imOpt.flatMap(im => Option(im.getTimecode).map(_.toString)),
            imOpt.flatMap(im => Option(im.getVideoReferenceUuid))
        )

    def fromImagedMoment(entity: ImagedMomentEntity, extend: Boolean = false): Seq[Annotation] = {
        entity.getObservations.asScala.map(x => from(x, extend)).toSeq
    }

    def toEntities(
        annotations: Seq[Annotation],
        extend: Boolean = false
    ): Seq[ImagedMomentEntity] = {

        val imagedMoments = annotations.map(_.toEntity)

        // Consolidate imaged moments
        val imagedMomentMap = mutable.Map[ImagedMomentEntity, Seq[ImagedMomentEntity]]()
        for (im <- imagedMoments)
        do
            val imOpt = imagedMomentMap.get(im)
            imOpt match
                case None    => imagedMomentMap.put(im, Nil)
                case Some(x) => imagedMomentMap.put(im, x :+ im)

        for
            (baseIm, xs) <- imagedMomentMap.iterator
            x            <- xs
        do
            x.getObservations.forEach(o => baseIm.addObservation(o))
            x.getImageReferences().forEach(i => baseIm.addImageReference(i))
            if baseIm.getAncillaryDatum() != null && x.getAncillaryDatum() != null then
                baseIm.setAncillaryDatum(x.getAncillaryDatum())

        imagedMomentMap.keys.toSeq

        // val imagedMoments =
        //     for (imagedMomentUuid, annos) <- annotations
        //                                          .filter(x =>
        //                                              x.imagedMomentUuid.isDefined && x
        //                                                  .videoReferenceUuid
        //                                                  .isDefined && x.concept.isDefined
        //                                          )
        //                                          .groupBy(_.imagedMomentUuid.get)
        //     yield
        //         val a  = annos.head
        //         val tc = a.timecode.map(Timecode(_)).orNull
        //         val et = a.elapsedTimeMillis.map(Duration.ofMillis(_)).orNull
        //         val im = ImagedMomentEntity(
        //             a.videoReferenceUuid.orNull,
        //             a.recordedTimestamp.orNull,
        //             tc,
        //             et
        //         )
        //         im.setUuid(imagedMomentUuid)

        //         if extend then
        //             annos.find(_.ancillaryData.isDefined).foreach { a =>
        //                 im.setAncillaryDatum(a.ancillaryData.get.toEntity)
        //             }

        //         for a <- annos
        //         do
        //             val d   = a.durationMillis.map(Duration.ofMillis(_))
        //             val obs = ObservationEntity(
        //                 a.concept.get,
        //                 d.orNull,
        //                 a.observationTimestamp.orNull,
        //                 a.observer.orNull,
        //                 a.group.orNull,
        //                 a.activity.orNull
        //             )
        //             a.observationUuid.foreach(obs.setUuid)
        //             im.addObservation(obs)

        //             a.associations.foreach(x => obs.addAssociation(x.toEntity))
        //             val irs = im.getImageReferences.asScala
        //             a.imageReferences
        //                 .foreach(x =>
        //                     irs.find(i => i.getUrl == x.url) match
        //                         case Some(link_value) => // Do nothing. Image already exists
        //                         case None             => im.addImageReference(x.toEntity)
        //                 )
        //         im

        // imagedMoments.toSeq

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
