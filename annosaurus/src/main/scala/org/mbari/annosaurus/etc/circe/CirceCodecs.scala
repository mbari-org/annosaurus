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

import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import org.mbari.annosaurus.util.HexUtil
import org.mbari.annosaurus.domain.*
import org.mbari.annosaurus.repository.query.{Constraint, Query, JDBC}

import java.net.{URI, URL}
import java.time.Instant
import scala.util.Try

object CirceCodecs {
    given Encoder[Array[Byte]] = (xs: Array[Byte]) => Json.fromString(HexUtil.toHex(xs))
    given Decoder[Array[Byte]] = Decoder
        .decodeString
        .emapTry(str => Try(HexUtil.fromHex(str)))

    given Decoder[URL] = Decoder
        .decodeString
        .emapTry(str => Try(URI.create(str).toURL))
    given Encoder[URL] = Encoder
        .encodeString
        .contramap(_.toString)

    given Decoder[URI] = Decoder
        .decodeString
        .emapTry(s => Try(URI.create(s)))
    given Encoder[URI] = Encoder
        .encodeString
        .contramap[URI](_.toString)

    given Decoder[ErrorMsg] = deriveDecoder
    given Encoder[ErrorMsg] = deriveEncoder

    given Decoder[BadRequest] = deriveDecoder
    given Encoder[BadRequest] = deriveEncoder

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

    given Decoder[Count] = deriveDecoder
    given Encoder[Count] = deriveEncoder

    given queryConstraintsScDecoder: Decoder[QueryConstraintsSC]     = deriveDecoder
    given queryConstraintsScEncoder: Encoder[QueryConstraintsSC]     = deriveEncoder
    private val queryConstraintsCcDecoder: Decoder[QueryConstraints] = deriveDecoder
    given queryConstraintsEncoder: Encoder[QueryConstraints]         = deriveEncoder
    given queryConstraintsDecoder: Decoder[QueryConstraints]         =
        queryConstraintsCcDecoder or queryConstraintsScDecoder.map(_.toCamelCase)

    given depthHistogramScDecoder: Decoder[DepthHistogramSC]     = deriveDecoder
    given depthHistogramScEncoder: Encoder[DepthHistogramSC]     = deriveEncoder
    private val depthHistogramCcDecoder: Decoder[DepthHistogram] = deriveDecoder
    given depthHistogramEncoder: Encoder[DepthHistogram]         = deriveEncoder
    given depthHistogramDecoder: Decoder[DepthHistogram]         =
        depthHistogramCcDecoder or depthHistogramScDecoder.map(_.toCamelCase)

    given timeHistogramScDecoder: Decoder[TimeHistogramSC]     = deriveDecoder
    given timeHistogramScEncoder: Encoder[TimeHistogramSC]     = deriveEncoder
    private val timeHistogramCcDecoder: Decoder[TimeHistogram] = deriveDecoder
    given timeHistogramEncoder: Encoder[TimeHistogram]         = deriveEncoder
    given timeHistogramDecoder: Decoder[TimeHistogram]         =
        timeHistogramCcDecoder or timeHistogramScDecoder.map(_.toCamelCase)

    given qcrCountScDecoder: Decoder[QueryConstraintsResponseSC[Count]] = deriveDecoder
    given qcrCountScEncoder: Encoder[QueryConstraintsResponseSC[Count]] = deriveEncoder

    given qcrAnnotationsScDecoder: Decoder[QueryConstraintsResponseSC[Seq[AnnotationSC]]] =
        deriveDecoder
    given qcrAnnotationsScEncoder: Encoder[QueryConstraintsResponseSC[Seq[AnnotationSC]]] =
        deriveEncoder

    given qcrGeographicRangeScDecoder: Decoder[QueryConstraintsResponseSC[GeographicRangeSC]] =
        deriveDecoder
    given qcrGeographicRangeScEncoder: Encoder[QueryConstraintsResponseSC[GeographicRangeSC]] =
        deriveEncoder
    given qcrGeographicRangeDecoder: Decoder[QueryConstraintsResponse[GeographicRange]]       =
        deriveDecoder
    given qcrGeographicRangeEncoder: Encoder[QueryConstraintsResponse[GeographicRange]]       =
        deriveEncoder

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
    private val associationCcDecoder: Decoder[Association] = deriveDecoder
    given associationEncoder: Encoder[Association]         = deriveEncoder
    given associationDecoder: Decoder[Association]         =
        associationCcDecoder or associationScDecoder.map(_.toCamelCase)

    given imageReferenceScDecoder: Decoder[ImageReferenceSC]     = deriveDecoder
    given imageReferenceScEncoder: Encoder[ImageReferenceSC]     = deriveEncoder
    private val imageReferenceCcDecoder: Decoder[ImageReference] = deriveDecoder
    given imageReferenceEncoder: Encoder[ImageReference]         = deriveEncoder
    given imageReferenceDecoder: Decoder[ImageReference]         =
        imageReferenceCcDecoder or imageReferenceScDecoder.map(_.toCamelCase)

    given observationScDecoder: Decoder[ObservationSC]     = deriveDecoder
    given observationScEncoder: Encoder[ObservationSC]     = deriveEncoder
    private val observationCcDecoder: Decoder[Observation] = deriveDecoder
    given observationEncoder: Encoder[Observation]         = deriveEncoder
    given observationDecoder: Decoder[Observation]         =
        observationCcDecoder or observationScDecoder.map(_.toCamelCase)

    given ancillaryDatumScDecoder: Decoder[CachedAncillaryDatumSC]     = deriveDecoder
    given ancillaryDatumScEncoder: Encoder[CachedAncillaryDatumSC]     = deriveEncoder
    private val ancillaryDatumCcDecoder: Decoder[CachedAncillaryDatum] = deriveDecoder
    given ancillaryDatumEncoder: Encoder[CachedAncillaryDatum]         = deriveEncoder
    given ancillaryDatumDecoder: Decoder[CachedAncillaryDatum]         =
        ancillaryDatumCcDecoder or ancillaryDatumScDecoder.map(_.toCamelCase)

    given indexScDecoder: Decoder[IndexSC]     = deriveDecoder
    given indexScEncoder: Encoder[IndexSC]     = deriveEncoder
    private val indexCcDecoder: Decoder[Index] = deriveDecoder
    given indexEncoder: Encoder[Index]         = deriveEncoder
    given indexDecoder: Decoder[Index]         = indexCcDecoder or indexScDecoder.map(_.toCamelCase)

    given imagedMomentScDecoder: Decoder[ImagedMomentSC]     = deriveDecoder
    given imagedMomentScEncoder: Encoder[ImagedMomentSC]     = deriveEncoder
    private val imagedMomentCcDecoder: Decoder[ImagedMoment] = deriveDecoder
    given imagedMomentEncoder: Encoder[ImagedMoment]         = deriveEncoder
    given imagedMomentDecoder: Decoder[ImagedMoment]         =
        imagedMomentCcDecoder or imagedMomentScDecoder.map(_.toCamelCase)

    given annotationScDecoder: Decoder[AnnotationSC]     = deriveDecoder
    given annotationScEncoder: Encoder[AnnotationSC]     = deriveEncoder
    private val annotationCcDecoder: Decoder[Annotation] = deriveDecoder
    given annotationEncoder: Encoder[Annotation]         = deriveEncoder
    given annotationDecoder: Decoder[Annotation]         =
        annotationCcDecoder or annotationScDecoder.map(_.toCamelCase)

    given annotationCreateScDecoder: Decoder[AnnotationCreateSC]     = deriveDecoder
    given annotationCreateScEncoder: Encoder[AnnotationCreateSC]     = deriveEncoder
    private val annotationCreateCcDecoder: Decoder[AnnotationCreate] = deriveDecoder
    given annotationCreateEncoder: Encoder[AnnotationCreate]         = deriveEncoder
    given annotationCreateDecoder: Decoder[AnnotationCreate]         =
        annotationCreateCcDecoder or annotationCreateScDecoder.map(_.toCamelCase)

    given annotationUpdateScDecoder: Decoder[AnnotationUpdateSC]     = deriveDecoder
    given annotationUpdateScEncoder: Encoder[AnnotationUpdateSC]     = deriveEncoder
    private val annotationUpdateCcDecoder: Decoder[AnnotationUpdate] = deriveDecoder
    given annotationUpdateEncoder: Encoder[AnnotationUpdate]         = deriveEncoder
    given annotationUpdateDecoder: Decoder[AnnotationUpdate]         =
        annotationUpdateCcDecoder or annotationUpdateScDecoder.map(_.toCamelCase)

    given imageScDecoder: Decoder[ImageSC]     = deriveDecoder
    given imageScEncoder: Encoder[ImageSC]     = deriveEncoder
    private val imageCcDecoder: Decoder[Image] = deriveDecoder
    given imageEncoder: Encoder[Image]         = deriveEncoder
    given imageDecoder: Decoder[Image]         = imageCcDecoder or imageScDecoder.map(_.toCamelCase)

    given cachedVideoReferenceInfoScDecoder: Decoder[CachedVideoReferenceInfoSC]     = deriveDecoder
    given cachedVideoReferenceInfoScEncoder: Encoder[CachedVideoReferenceInfoSC]     = deriveEncoder
    private val cachedVideoReferenceInfoCcDecoder: Decoder[CachedVideoReferenceInfo] = deriveDecoder
    given cachedVideoReferenceInfoEncoder: Encoder[CachedVideoReferenceInfo]         = deriveEncoder
    given cachedVideoReferenceInfoDecoder: Decoder[CachedVideoReferenceInfo]         =
        cachedVideoReferenceInfoCcDecoder or cachedVideoReferenceInfoScDecoder.map(_.toCamelCase)

    given authorizationScDecoder: Decoder[AuthorizationSC]     = deriveDecoder
    given authorizationScEncoder: Encoder[AuthorizationSC]     = deriveEncoder
    private val authorizationCcDecoder: Decoder[Authorization] = deriveDecoder
    given authorizationEncoder: Encoder[Authorization]         = deriveEncoder
    given authorizationDecoder: Decoder[Authorization]         =
        authorizationCcDecoder or authorizationScDecoder.map(_.toCamelCase)

    given conceptAssociationScDecoder: Decoder[ConceptAssociationSC]     = deriveDecoder
    given conceptAssociationScEncoder: Encoder[ConceptAssociationSC]     = deriveEncoder
    private val conceptAssociationCcDecoder: Decoder[ConceptAssociation] = deriveDecoder
    given conceptAssociationEncoder: Encoder[ConceptAssociation]         = deriveEncoder
    given conceptAssociationDecoder: Decoder[ConceptAssociation]         =
        conceptAssociationCcDecoder or conceptAssociationScDecoder.map(_.toCamelCase)

    given conceptAssociationRequestScDecoder: Decoder[ConceptAssociationRequestSC]     = deriveDecoder
    given conceptAssociationRequestScEncoder: Encoder[ConceptAssociationRequestSC]     = deriveEncoder
    private val conceptAssociationRequestCcDecoder: Decoder[ConceptAssociationRequest] =
        deriveDecoder
    given conceptAssociationRequestEncoder: Encoder[ConceptAssociationRequest]         = deriveEncoder
    given conceptAssociationRequestDecoder: Decoder[ConceptAssociationRequest]         =
        conceptAssociationRequestCcDecoder or conceptAssociationRequestScDecoder.map(_.toCamelCase)

    given conceptAssociationResponseScDecoder: Decoder[ConceptAssociationResponseSC]     = deriveDecoder
    given conceptAssociationResponseScEncoder: Encoder[ConceptAssociationResponseSC]     = deriveEncoder
    private val conceptAssociationResponseCcDecoder: Decoder[ConceptAssociationResponse] =
        deriveDecoder
    given conceptAssociationResponseEncoder: Encoder[ConceptAssociationResponse]         = deriveEncoder
    given conceptAssociationResponseDecoder: Decoder[ConceptAssociationResponse]         =
        conceptAssociationResponseCcDecoder or conceptAssociationResponseScDecoder.map(
            _.toCamelCase
        )

    given concurrentRequestScDecoder: Decoder[ConcurrentRequestSC]     = deriveDecoder
    given concurrentRequestScEncoder: Encoder[ConcurrentRequestSC]     = deriveEncoder
    private val concurrentRequestCcDecoder: Decoder[ConcurrentRequest] = deriveDecoder
    given concurrentRequestEncoder: Encoder[ConcurrentRequest]         = deriveEncoder
    given concurrentRequestDecoder: Decoder[ConcurrentRequest]         =
        concurrentRequestCcDecoder or concurrentRequestScDecoder.map(_.toCamelCase)

    given concurrentRequestCountScDecoder: Decoder[ConcurrentRequestCountSC] = deriveDecoder
    given concurrentRequestCountScEncoder: Encoder[ConcurrentRequestCountSC] = deriveEncoder

    given moveImagedMomentsScDecoder: Decoder[MoveImagedMomentsSC]     = deriveDecoder
    given moveImagedMomentsScEncoder: Encoder[MoveImagedMomentsSC]     = deriveEncoder
    private val moveImagedMomentsCcDecoder: Decoder[MoveImagedMoments] = deriveDecoder
    given moveImagedMomentsEncoder: Encoder[MoveImagedMoments]         = deriveEncoder
    given moveImagedMomentsDecoder: Decoder[MoveImagedMoments]         =
        moveImagedMomentsCcDecoder or moveImagedMomentsScDecoder.map(_.toCamelCase)

    given multiRequestScDecoder: Decoder[MultiRequestSC]     = deriveDecoder
    given multiRequestScEncoder: Encoder[MultiRequestSC]     = deriveEncoder
    private val multiRequestCcDecoder: Decoder[MultiRequest] = deriveDecoder
    given multiRequestEncoder: Encoder[MultiRequest]         = deriveEncoder
    given multiRequestDecoder: Decoder[MultiRequest]         =
        multiRequestCcDecoder or multiRequestScDecoder.map(_.toCamelCase)

    given multiRequestCountScDecoder: Decoder[MultiRequestCountSC] = deriveDecoder
    given multiRequestCountScEncoder: Encoder[MultiRequestCountSC] = deriveEncoder

    given windowRequestScDecoder: Decoder[WindowRequestSC]     = deriveDecoder
    given windowRequestScEncoder: Encoder[WindowRequestSC]     = deriveEncoder
    private val windowRequestCcDecoder: Decoder[WindowRequest] = deriveDecoder
    given windowRequestEncoder: Encoder[WindowRequest]         = deriveEncoder
    given windowRequestDecoder: Decoder[WindowRequest]         =
        windowRequestCcDecoder or windowRequestScDecoder.map(_.toCamelCase)

    given deleteCOuntScDecoder: Decoder[DeleteCountSC] = deriveDecoder
    given deleteCountScEncoder: Encoder[DeleteCountSC] = deriveEncoder
    given deleteCountDecoder: Decoder[DeleteCount]     = deriveDecoder
    given deleteCountEncoder: Encoder[DeleteCount]     = deriveEncoder

    given geographicRangeScEncoder: Encoder[GeographicRangeSC] = deriveEncoder
    given geographicRangeScDecoder: Decoder[GeographicRangeSC] = deriveDecoder

    given geographicRangeEncoder: Encoder[GeographicRange] = deriveEncoder
    given geographicRangeDecoder: Decoder[GeographicRange] = deriveDecoder

    given healthStatusEncoder: Encoder[HealthStatus] = deriveEncoder
    given healthStatusDecoder: Decoder[HealthStatus] = deriveDecoder

    given conceptCountEncoder: Encoder[ConceptCount] = deriveEncoder
    given conceptCountDecoder: Decoder[ConceptCount] = deriveDecoder

    given renameCountScEncoder: Encoder[RenameCountSC] = deriveEncoder
    given renameCountScDecoder: Decoder[RenameCountSC] = deriveDecoder

    given renameConceptEncoder: Encoder[RenameConcept] = deriveEncoder
    given renameConceptDecoder: Decoder[RenameConcept] = deriveDecoder

    given dataDeleteCountScEncoder: Encoder[CountForVideoReferenceSC] = deriveEncoder
    given dataDeleteCountScDecoder: Decoder[CountForVideoReferenceSC] = deriveDecoder

    given imagedMomentUpdateScEncoder: Encoder[VideoTimestampSC] = deriveEncoder
    given imagedMomentUpdateScDecoder: Decoder[VideoTimestampSC] = deriveDecoder

    given imagedMomentTimestampUpdateScEncoder: Encoder[ImagedMomentTimestampUpdateSC] =
        deriveEncoder
    given imagedMomentTimestampUpdateScDecoder: Decoder[ImagedMomentTimestampUpdateSC] =
        deriveDecoder

    given observationUpdateScEncoder: Encoder[ObservationUpdateSC] = deriveEncoder
    given observationUpdateScDecoder: Decoder[ObservationUpdateSC] = deriveDecoder

    given imageCreateScEncoder: Encoder[ImageCreateSC] = deriveEncoder
    given imageCreateScDecoder: Decoder[ImageCreateSC] = deriveDecoder

    given imageUpdateScEncoder: Encoder[ImageUpdateSC] = deriveEncoder
    given imageUpdateScDecoder: Decoder[ImageUpdateSC] = deriveDecoder

    given videoInfoCreateScEncoder: Encoder[CachedVideoReferenceInfoCreateSC] = deriveEncoder
    given videoInfoCreateScDecoder: Decoder[CachedVideoReferenceInfoCreateSC] = deriveDecoder

    given videoInfoUpdateScEncoder: Encoder[CachedVideoReferenceInfoUpdateSC] = deriveEncoder
    given videoInfoUpdateScDecoder: Decoder[CachedVideoReferenceInfoUpdateSC] = deriveDecoder

    given indexUpdateScEncoder: Encoder[IndexUpdateSC] = deriveEncoder
    given indexUpdateScDecoder: Decoder[IndexUpdateSC] = deriveDecoder

    given bulkAnnotationScEncoder: Encoder[BulkAnnotationSC] = deriveEncoder
    given bulkAnnotationScDecoder: Decoder[BulkAnnotationSC] = deriveDecoder

    given associationUpdateScEncoder: Encoder[AssociationUpdateSC] = deriveEncoder
    given associationUpdateScDecoder: Decoder[AssociationUpdateSC] = deriveDecoder

    given observationsUpdateScEncoder: Encoder[ObservationsUpdateSC]     = deriveEncoder
    given observationsUpdateScDecoder: Decoder[ObservationsUpdateSC]     = deriveDecoder
    given observationsUpdateEncoder: Encoder[ObservationsUpdate]         = deriveEncoder
    private val observationsUpdateCcDecoder: Decoder[ObservationsUpdate] = deriveDecoder
    given observationsUpdateDecoder: Decoder[ObservationsUpdate]         =
        observationsUpdateCcDecoder or observationsUpdateScDecoder.map(_.toCamelCase)

    // Custom Decoder for Constraint
    given constraintDecoder: Decoder[Constraint] = (c: HCursor) => {
        for {
            columnName <- c.downField("columnName").as[String]
            // Determine which constraint key is present
            constraint <- {
                if (c.downField("in").succeeded) {
                    c.downField("in").as[List[String]].map(Constraint.In(columnName, _))
                }
                else if (c.downField("like").succeeded) {
                    c.downField("like").as[String].map(Constraint.Like(columnName, _))
                }
                else if (c.downField("between").succeeded) {
                    // Attempt to decode between as List[Int] first
                    c.downField("between")
                        .as[List[Double]]
                        .map(xs => Constraint.MinMax(columnName, xs.head, xs.last))
                        .orElse {
                            // If decoding as Int fails, try decoding as List[Instant]
                            c.downField("between")
                                .as[List[Instant]]
                                .map(xs => Constraint.Date(columnName, xs.head, xs.last))
                        }
                }
                else if (c.downField("minmax").succeeded) {
                    c.downField("minmax")
                        .as[List[Double]]
                        .map(xs => Constraint.MinMax(columnName, xs.head, xs.last))
                }
                else if (c.downField("min").succeeded) {
                    c.downField("min").as[Double].map(Constraint.Min(columnName, _))
                }
                else if (c.downField("max").succeeded) {
                    c.downField("max").as[Double].map(Constraint.Max(columnName, _))
                }
                else if (c.downField("isnull").succeeded) {
                    c.downField("isnull").as[Boolean].map(Constraint.IsNull(columnName, _))
                }
                else {
                    Left(DecodingFailure("Unknown constraint type", c.history))
                }
            }
        } yield constraint
    }

    given Encoder[Constraint.Date]       = deriveEncoder
    given Encoder[Constraint.In[String]] = deriveEncoder
    given Encoder[Constraint.Like]       = deriveEncoder
    given Encoder[Constraint.Max]        = deriveEncoder
    given Encoder[Constraint.Min]        = deriveEncoder
    given Encoder[Constraint.MinMax]     = deriveEncoder
    given Encoder[Constraint.IsNull]     = deriveEncoder
    given Encoder[List[Constraint]]      = deriveEncoder

    // THis is needed to handle the trait Constraint used in Constraints
    given constraintEncoder: Encoder[Constraint] = Encoder.instance[Constraint] {
        case c: Constraint.In[String] => c.asJson
        case c: Constraint.Like       => c.asJson
        case c: Constraint.Min        => c.asJson
        case c: Constraint.Max        => c.asJson
        case c: Constraint.MinMax     => c.asJson
        case c: Constraint.IsNull     => c.asJson
        case c: Constraint.Date       => c.asJson
    }

    given constraintsDecoder: Decoder[Query] = deriveDecoder
    given constraintsEncoder: Encoder[Query] = deriveEncoder

    given constraintRequestDecoder: Decoder[ConstraintRequest] = deriveDecoder
    given constraintRequestEncoder: Encoder[ConstraintRequest] = deriveEncoder
    given queryRequestDecoder: Decoder[QueryRequest]  = deriveDecoder
    given queryRequestEncoder: Encoder[QueryRequest]  = deriveEncoder
    given jdbcMetadataDecoder: Decoder[JDBC.Metadata] = deriveDecoder
    given jdbcMetadataEncoder: Encoder[JDBC.Metadata] = deriveEncoder

    val CustomPrinter: Printer = Printer(
        dropNullValues = true,
        indent = ""
    )

//    @deprecated("Use stringify[T: Encoder] instead", "2021-11-23T11:00:00")
//    def print[T: Encoder](t: T): String = CustomPrinter.print(t.asJson)

    /** Convert a circe Json object to a JSON string
      *
      * @param value
      *   Any value with an implicit circe coder in scope
      */
    extension (json: Json) def stringify: String = CustomPrinter.print(json)

    /** Convert an object to a JSON string
      *
      * @param value
      *   Any value with an implicit circe coder in scope
      */
    extension [T: Encoder](value: T)
        def stringify: String = Encoder[T]
            .apply(value)
            .deepDropNullValues
            .stringify

    extension [T: Decoder](jsonString: String)
        def toJson: Either[ParsingFailure, Json] = parser.parse(jsonString);

    extension (jsonString: String)
        def reify[T: Decoder]: Either[Error, T] =
            for
                json   <- jsonString.toJson
                result <- Decoder[T].apply(json.hcursor)
            yield result

}
