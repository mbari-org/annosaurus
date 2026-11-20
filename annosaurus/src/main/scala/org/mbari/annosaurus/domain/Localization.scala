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

import io.circe.parser.decode
import io.circe.syntax.*
import org.mbari.annosaurus.repository.jpa.entity.LocalizationEntity
import org.mbari.annosaurus.repository.jpa.entity.extensions.*

import java.time.Instant
import java.util.UUID

/**
 * A localization (bounding box, point, line, polygon or mask) on an observation. Coordinates are normalized to [0, 1].
 * x/y/width/height are optional because the server derives them for line/polygon/mask. Vertices are an interleaved
 * array [x0, y0, x1, y1, ...].
 */
final case class Localization(
    localizationType: String,
    x: Option[Double] = None,
    y: Option[Double] = None,
    width: Option[Double] = None,
    height: Option[Double] = None,
    vertices: Option[Seq[Double]] = None,
    maskEncoding: Option[String] = None,
    maskData: Option[String] = None,
    maskWidthPixels: Option[Int] = None,
    maskHeightPixels: Option[Int] = None,
    generator: Option[String] = None,
    confidence: Option[Double] = None,
    observer: Option[String] = None,
    observationTimestamp: Option[Instant] = None,
    verifier: Option[String] = None,
    verificationTimestamp: Option[Instant] = None,
    comment: Option[String] = None,
    part: Option[String] = None,
    training: Boolean = false,
    truncated: Boolean = false,
    difficult: Boolean = false,
    uuid: Option[UUID] = None,
    observationUuid: Option[UUID] = None,
    lastUpdated: Option[Instant] = None
) extends ToSnakeCase[LocalizationSC]
    with ToEntity[LocalizationEntity]:

    override def toSnakeCase: LocalizationSC =
        LocalizationSC(
            localizationType,
            x,
            y,
            width,
            height,
            vertices,
            maskEncoding,
            maskData,
            maskWidthPixels,
            maskHeightPixels,
            generator,
            confidence,
            observer,
            observationTimestamp,
            verifier,
            verificationTimestamp,
            comment,
            part,
            training,
            truncated,
            difficult,
            uuid,
            observationUuid,
            lastUpdated
        )

    /** The parent observation and lastUpdated are not set; attach it with ObservationEntity.addLocalization. */
    override def toEntity: LocalizationEntity =
        val e = new LocalizationEntity
        e.setLocalizationType(localizationType)
        x.foreach(v => e.setX(v))
        y.foreach(v => e.setY(v))
        width.foreach(v => e.setWidth(v))
        height.foreach(v => e.setHeight(v))
        vertices.foreach(vs => e.setVertices(vs.asJson.noSpaces))
        maskEncoding.foreach(e.setMaskEncoding)
        maskData.foreach(e.setMaskData)
        maskWidthPixels.foreach(v => e.setMaskWidthPixels(v))
        maskHeightPixels.foreach(v => e.setMaskHeightPixels(v))
        generator.foreach(e.setGenerator)
        confidence.foreach(v => e.setConfidence(v.toFloat))
        observer.foreach(e.setObserver)
        observationTimestamp.foreach(e.setObservationTimestamp)
        verifier.foreach(e.setVerifier)
        verificationTimestamp.foreach(e.setVerificationTimestamp)
        comment.foreach(e.setComment)
        part.foreach(e.setPart)
        e.setTraining(training)
        e.setTruncated(truncated)
        e.setDifficult(difficult)
        uuid.foreach(e.setUuid)
        e

object Localization:

    def from(entity: LocalizationEntity): Localization =
        Localization(
            localizationType = entity.getLocalizationType,
            x = Option(entity.getX).map(_.doubleValue),
            y = Option(entity.getY).map(_.doubleValue),
            width = Option(entity.getWidth).map(_.doubleValue),
            height = Option(entity.getHeight).map(_.doubleValue),
            vertices = Option(entity.getVertices).map(parseVertices),
            maskEncoding = Option(entity.getMaskEncoding),
            maskData = Option(entity.getMaskData),
            maskWidthPixels = Option(entity.getMaskWidthPixels).map(_.intValue),
            maskHeightPixels = Option(entity.getMaskHeightPixels).map(_.intValue),
            generator = Option(entity.getGenerator),
            // The column is single precision. Widen through the decimal string so 0.9f reads back as 0.9.
            confidence = Option(entity.getConfidence).map(_.toString.toDouble),
            observer = Option(entity.getObserver),
            observationTimestamp = Option(entity.getObservationTimestamp),
            verifier = Option(entity.getVerifier),
            verificationTimestamp = Option(entity.getVerificationTimestamp),
            comment = Option(entity.getComment),
            part = Option(entity.getPart),
            training = entity.isTraining,
            truncated = entity.isTruncated,
            difficult = entity.isDifficult,
            uuid = Option(entity.getUuid),
            observationUuid = Option(entity.getObservation).flatMap(o => Option(o.getUuid)),
            lastUpdated = entity.lastUpdated
        )

    /** Parses the JSON text stored in the vertices column: [x0,y0,x1,y1,...]. */
    def parseVertices(json: String): Seq[Double] =
        decode[Seq[Double]](json).fold(
            e => throw new IllegalStateException(s"Invalid localization vertices JSON: $json", e),
            identity
        )

final case class LocalizationSC(
    localization_type: String,
    x: Option[Double] = None,
    y: Option[Double] = None,
    width: Option[Double] = None,
    height: Option[Double] = None,
    vertices: Option[Seq[Double]] = None,
    mask_encoding: Option[String] = None,
    mask_data: Option[String] = None,
    mask_width_pixels: Option[Int] = None,
    mask_height_pixels: Option[Int] = None,
    generator: Option[String] = None,
    confidence: Option[Double] = None,
    observer: Option[String] = None,
    observation_timestamp: Option[Instant] = None,
    verifier: Option[String] = None,
    verification_timestamp: Option[Instant] = None,
    comment: Option[String] = None,
    part: Option[String] = None,
    training: Boolean = false,
    truncated: Boolean = false,
    difficult: Boolean = false,
    uuid: Option[UUID] = None,
    observation_uuid: Option[UUID] = None,
    last_updated_time: Option[Instant] = None
) extends ToCamelCase[Localization]:

    def toCamelCase: Localization =
        Localization(
            localization_type,
            x,
            y,
            width,
            height,
            vertices,
            mask_encoding,
            mask_data,
            mask_width_pixels,
            mask_height_pixels,
            generator,
            confidence,
            observer,
            observation_timestamp,
            verifier,
            verification_timestamp,
            comment,
            part,
            training,
            truncated,
            difficult,
            uuid,
            observation_uuid,
            last_updated_time
        )
