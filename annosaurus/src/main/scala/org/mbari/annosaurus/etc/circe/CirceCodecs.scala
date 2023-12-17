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

package org.mbari.annosaurus.etc.circe

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

import org.mbari.annosaurus.util.HexUtil
import org.mbari.annosaurus.domain.*

import java.net.{URI, URL}
import scala.util.Try

import org.mbari.annosaurus.model.simple.{HealthStatus => OldHealthStatus}

object CirceCodecs {
    implicit val byteArrayEncoder: Encoder[Array[Byte]] = new Encoder[Array[Byte]] {
        final def apply(xs: Array[Byte]): Json =
            Json.fromString(HexUtil.toHex(xs))
    }
    implicit val byteArrayDecoder: Decoder[Array[Byte]] = Decoder
        .decodeString
        .emapTry(str => Try(HexUtil.fromHex(str)))

    implicit val urlDecoder: Decoder[URL] = Decoder
        .decodeString
        .emapTry(str => Try(new URL(str)))
    implicit val urlEncoder: Encoder[URL] = Encoder
        .encodeString
        .contramap(_.toString)

    implicit val uriDecoder: Decoder[URI] = Decoder
        .decodeString
        .emapTry(s => Try(URI.create(s)))
    implicit val uriEncoder: Encoder[URI] = Encoder
        .encodeString
        .contramap[URI](_.toString)

    given Decoder[ErrorMsg] = deriveDecoder
    given Encoder[ErrorMsg] = deriveEncoder

    given Decoder[BadRequest] = deriveDecoder
    given Encoder[BadRequest] = deriveEncoder

    given Decoder[HealthStatus] = deriveDecoder
    given Encoder[HealthStatus] = deriveEncoder

    given Decoder[OldHealthStatus] = deriveDecoder
    given Encoder[OldHealthStatus] = deriveEncoder

    // given Decoder[ServiceStatus] = deriveDecoder
    // given Encoder[ServiceStatus] = deriveEncoder

    given Decoder[StatusMsg] = deriveDecoder
    given Encoder[StatusMsg] = deriveEncoder

    given Decoder[NotFound] = deriveDecoder
    given Encoder[NotFound] = deriveEncoder

    given Decoder[ServerError] = deriveDecoder
    given Encoder[ServerError] = deriveEncoder

    given Decoder[Unauthorized] = deriveDecoder
    given Encoder[Unauthorized] = deriveEncoder

    given queryConstraintsScDecoder: Decoder[QueryConstraintsSC]     = deriveDecoder
    given queryConstraintsScEncoder: Encoder[QueryConstraintsSC]     = deriveEncoder
    private val queryConstraintsCcDecoder: Decoder[QueryConstraints] =
        deriveDecoder // or queryConstraintsScDecoder.map(_.toCamelCase)
    given queryConstraintsEncoder: Encoder[QueryConstraints] = deriveEncoder
    given queryConstraintsDecoder: Decoder[QueryConstraints] =
        queryConstraintsCcDecoder or queryConstraintsScDecoder.map(_.toCamelCase)

    given depthHistogramScDecoder: Decoder[DepthHistogramSC]     = deriveDecoder
    given depthHistogramScEncoder: Encoder[DepthHistogramSC]     = deriveEncoder
    private val depthHistogramCcDecoder: Decoder[DepthHistogram] =
        deriveDecoder // or depthHistogramScDecoder.map(_.toCamelCase)
    given depthHistogramEncoder: Encoder[DepthHistogram] = deriveEncoder
    given depthHistogramDecoder: Decoder[DepthHistogram] =
        depthHistogramCcDecoder or depthHistogramScDecoder.map(_.toCamelCase)

    given timeHistogramScDecoder: Decoder[TimeHistogramSC]     = deriveDecoder
    given timeHistogramScEncoder: Encoder[TimeHistogramSC]     = deriveEncoder
    private val timeHistogramCcDecoder: Decoder[TimeHistogram] =
        deriveDecoder // or timeHistogramScDecoder.map(_.toCamelCase)
    given timeHistogramEncoder: Encoder[TimeHistogram] = deriveEncoder
    given timeHistogramDecoder: Decoder[TimeHistogram] =
        timeHistogramCcDecoder or timeHistogramScDecoder.map(_.toCamelCase)

    given qcrDepthHistogramScDecoder: Decoder[QueryConstraintsResponseSC[DepthHistogramSC]] =
        deriveDecoder
    given qcrDepthHistogramScEncoder: Encoder[QueryConstraintsResponseSC[DepthHistogramSC]] =
        deriveEncoder
    given qcrDepthHistogramDecoder: Decoder[QueryConstraintsResponse[DepthHistogram]]       =
        deriveDecoder
    given qcrDepthHistogramEncoder: Encoder[QueryConstraintsResponse[DepthHistogram]]       =
        deriveEncoder

    given qcrTimeHistogramScDecoder: Decoder[QueryConstraintsResponseSC[TimeHistogramSC]] =
        deriveDecoder
    given qcrTimeHistogramScEncoder: Encoder[QueryConstraintsResponseSC[TimeHistogramSC]] =
        deriveEncoder
    given qcrTimeHistogramDecoder: Decoder[QueryConstraintsResponse[TimeHistogram]]       = deriveDecoder
    given qcrTimeHistogramEncoder: Encoder[QueryConstraintsResponse[TimeHistogram]]       = deriveEncoder

    given associationScDecoder: Decoder[AssociationSC]     = deriveDecoder
    given associationScEncoder: Encoder[AssociationSC]     = deriveEncoder
    private val associationCcDecoder: Decoder[Association] =
        deriveDecoder // or associationScDecoder.map(_.toCamelCase)
    given associationEncoder: Encoder[Association] = deriveEncoder
    given associationDecoder: Decoder[Association] =
        associationCcDecoder or associationScDecoder.map(_.toCamelCase)
    // given alDecoder: Decoder[List[Association]] = deriveDecoder

    given imageReferenceScDecoder: Decoder[ImageReferenceSC]     = deriveDecoder
    given imageReferenceScEncoder: Encoder[ImageReferenceSC]     = deriveEncoder
    private val imageReferenceCcDecoder: Decoder[ImageReference] =
        deriveDecoder // or imageReferenceScDecoder.map(_.toCamelCase)
    given imageReferenceEncoder: Encoder[ImageReference] = deriveEncoder
    given imageReferenceDecoder: Decoder[ImageReference] =
        imageReferenceCcDecoder or imageReferenceScDecoder.map(_.toCamelCase)

    given observationScDecoder: Decoder[ObservationSC]     = deriveDecoder
    given observationScEncoder: Encoder[ObservationSC]     = deriveEncoder
    private val observationCcDecoder: Decoder[Observation] =
        deriveDecoder // or observationScDecoder.map(_.toCamelCase)
    given observationEncoder: Encoder[Observation] = deriveEncoder
    given observationDecoder: Decoder[Observation] =
        observationCcDecoder or observationScDecoder.map(_.toCamelCase)

    given ancillaryDatumScDecoder: Decoder[CachedAncillaryDatumSC]     = deriveDecoder
    given ancillaryDatumScEncoder: Encoder[CachedAncillaryDatumSC]     = deriveEncoder
    private val ancillaryDatumCcDecoder: Decoder[CachedAncillaryDatum] =
        deriveDecoder // or ancillaryDatumScDecoder.map(_.toCamelCase)
    given ancillaryDatumEncoder: Encoder[CachedAncillaryDatum] = deriveEncoder
    given ancillaryDatumDecoder: Decoder[CachedAncillaryDatum] =
        ancillaryDatumCcDecoder or ancillaryDatumScDecoder.map(_.toCamelCase)

    given imagedMomentScDecoder: Decoder[ImagedMomentSC]     = deriveDecoder
    given imagedMomentScEncoder: Encoder[ImagedMomentSC]     = deriveEncoder
    private val imagedMomentCcDecoder: Decoder[ImagedMoment] =
        deriveDecoder // or imagedMomentScDecoder.map(_.toCamelCase)
    given imagedMomentEncoder: Encoder[ImagedMoment] = deriveEncoder
    given imagedMomentDecoder: Decoder[ImagedMoment] =
        imagedMomentCcDecoder or imagedMomentScDecoder.map(_.toCamelCase)

    given annotationScDecoder: Decoder[AnnotationSC]     = deriveDecoder
    given annotationScEncoder: Encoder[AnnotationSC]     = deriveEncoder
    private val annotationCcDecoder: Decoder[Annotation] =
        deriveDecoder // or annotationScDecoder.map(_.toCamelCase)
    given annotationEncoder: Encoder[Annotation] = deriveEncoder
    given annotationDecoder: Decoder[Annotation] =
        annotationCcDecoder or annotationScDecoder.map(_.toCamelCase)

    given authorizationScDecoder: Decoder[AuthorizationSC]     = deriveDecoder
    given authorizationScEncoder: Encoder[AuthorizationSC]     = deriveEncoder
    private val authorizationCcDecoder: Decoder[Authorization] =
        deriveDecoder // or authorizationScDecoder.map(_.toCamelCase)
    given authorizationEncoder: Encoder[Authorization] = deriveEncoder
    given authorizationDecoder: Decoder[Authorization] =
        authorizationCcDecoder or authorizationScDecoder.map(_.toCamelCase)

    private val printer = Printer.noSpaces.copy(dropNullValues = true)

    @deprecated("Use stringify[T: Encoder] instead", "2021-11-23T11:00:00")
    def print[T: Encoder](t: T): String = printer.print(t.asJson)

    /** Convert a circe Json object to a JSON string
      *
      * @param value
      *   Any value with an implicit circe coder in scope
      */
    extension (json: Json) def stringify: String = printer.print(json)

    /** Convert an object to a JSON string
      *
      * @param value
      *   Any value with an implicit circe coder in scope
      */
    extension [T: Encoder](value: T) def stringify: String = Encoder[T].apply(value).stringify

    extension [T: Decoder](jsonString: String)
        def toJson: Either[ParsingFailure, Json] = parser.parse(jsonString);

    extension (jsonString: String)
        def reify[T: Decoder]: Either[Error, T] =
            for
                json   <- jsonString.toJson
                result <- Decoder[T].apply(json.hcursor)
            yield result

}
