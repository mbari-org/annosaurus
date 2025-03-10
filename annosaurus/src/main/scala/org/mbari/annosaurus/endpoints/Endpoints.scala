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

package org.mbari.annosaurus.endpoints

import io.circe.Printer
import org.mbari.annosaurus.domain.*
import org.mbari.annosaurus.etc.circe.CirceCodecs
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.etc.jdk.Loggers.given
import org.mbari.annosaurus.etc.jwt.JwtService
import sttp.model.StatusCode
import sttp.model.headers.WWWAuthenticateChallenge
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import java.net.{URI, URL}
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class Paging(offset: Option[Int] = Some(0), limit: Option[Int] = Some(100))

object CustomTapirJsonCirce extends TapirJsonCirce:
    override def jsonPrinter: Printer = CirceCodecs.CustomPrinter

trait Endpoints:

    import CustomTapirJsonCirce.*

    val log: System.Logger = System.getLogger(getClass.getName)

    implicit lazy val sAnnotation: Schema[Annotation]                                   = Schema.derived[Annotation]
    implicit lazy val sAnnotationSc: Schema[AnnotationSC]                               = Schema.derived[AnnotationSC]
    implicit lazy val sAnnotationCreate: Schema[AnnotationCreate]                       =
        Schema.derived[AnnotationCreate]
    implicit lazy val sAnnotationCreateSc: Schema[AnnotationCreateSC]                   =
        Schema.derived[AnnotationCreateSC]
    implicit lazy val sAnnotationUpdate: Schema[AnnotationUpdate]                       = Schema.derived[AnnotationUpdate]
    implicit lazy val sAnnotationUpdateSc: Schema[AnnotationUpdateSC]                   =
        Schema.derived[AnnotationUpdateSC]
    implicit lazy val sAssociation: Schema[Association]                                 = Schema.derived[Association]
    implicit lazy val sAssociationSc: Schema[AssociationSC]                             = Schema.derived[AssociationSC]
    implicit lazy val sAssociationUpdateSc: Schema[AssociationUpdateSC]                 =
        Schema.derived[AssociationUpdateSC]
    implicit lazy val sCachedAncillaryDatum: Schema[CachedAncillaryDatum]               =
        Schema.derived[CachedAncillaryDatum]
    implicit lazy val sCachedAncillaryDatumSC: Schema[CachedAncillaryDatumSC]           =
        Schema.derived[CachedAncillaryDatumSC]
    implicit lazy val sCachedVideoReferenceInfo: Schema[CachedVideoReferenceInfo]       =
        Schema.derived[CachedVideoReferenceInfo]
    implicit lazy val sCachedVideoReferenceInfoSc: Schema[CachedVideoReferenceInfoSC]   =
        Schema.derived[CachedVideoReferenceInfoSC]
    implicit lazy val sConceptAssociationRequest: Schema[ConceptAssociationRequest]     =
        Schema.derived[ConceptAssociationRequest]
    implicit lazy val sConceptAssociationRequestSc: Schema[ConceptAssociationRequestSC] =
        Schema.derived[ConceptAssociationRequestSC]
    implicit lazy val sConceptCount: Schema[ConceptCount]                               = Schema.derived[ConceptCount]
    implicit lazy val sConcurrentRequest: Schema[ConcurrentRequest]                     =
        Schema.derived[ConcurrentRequest]
    implicit lazy val sConcurrentRequestSc: Schema[ConcurrentRequestSC]                 =
        Schema.derived[ConcurrentRequestSC]
    implicit lazy val sConcurrentRequestCountSc: Schema[ConcurrentRequestCountSC]       =
        Schema.derived[ConcurrentRequestCountSC]
    implicit lazy val sCount: Schema[Count]                                             = Schema.derived[Count]
    implicit lazy val sDeleteCountSc: Schema[DeleteCountSC]                             = Schema.derived[DeleteCountSC]
    implicit lazy val sDepthHistogram: Schema[DepthHistogram]                           = Schema.derived[DepthHistogram]
    implicit lazy val sGeographicRange: Schema[GeographicRange]                         = Schema.derived[GeographicRange]
    implicit lazy val sGeographicRangeSc: Schema[GeographicRangeSC]                     =
        Schema.derived[GeographicRangeSC]
    implicit lazy val sImageSc: Schema[ImageSC]                                         = Schema.derived[ImageSC]
    implicit lazy val sImageReference: Schema[ImageReference]                           = Schema.derived[ImageReference]
    implicit lazy val sImageReferenceSc: Schema[ImageReferenceSC]                       = Schema.derived[ImageReferenceSC]
    implicit lazy val sImagedMoment: Schema[ImagedMoment]                               = Schema.derived[ImagedMoment]
    implicit lazy val sImagedMomentSc: Schema[ImagedMomentSC]                           = Schema.derived[ImagedMomentSC]
    implicit lazy val sMultiRequest: Schema[MultiRequest]                               = Schema.derived[MultiRequest]
    implicit lazy val sMultiRequestSc: Schema[MultiRequestSC]                           = Schema.derived[MultiRequestSC]
    implicit lazy val sMultiRequestCountSc: Schema[MultiRequestCountSC]                 =
        Schema.derived[MultiRequestCountSC]
    implicit lazy val sObservation: Schema[Observation]                                 = Schema.derived[Observation]
    implicit lazy val sObservationSc: Schema[ObservationSC]                             = Schema.derived[ObservationSC]
    implicit lazy val sObservationUpdateSc: Schema[ObservationUpdateSC]                 =
        Schema.derived[ObservationUpdateSC]
    implicit lazy val sQcrC: Schema[QueryConstraintsResponseSC[Count]]                  =
        Schema.derived[QueryConstraintsResponseSC[Count]]
    implicit lazy val sQcrA: Schema[QueryConstraintsResponseSC[Seq[AnnotationSC]]]      =
        Schema.derived[QueryConstraintsResponseSC[Seq[AnnotationSC]]]
    implicit lazy val sQcrDh: Schema[QueryConstraintsResponseSC[DepthHistogramSC]]      =
        Schema.derived[QueryConstraintsResponseSC[DepthHistogramSC]]
    implicit lazy val sQcrGr: Schema[QueryConstraintsResponseSC[GeographicRangeSC]]     =
        Schema.derived[QueryConstraintsResponseSC[GeographicRangeSC]]
    implicit lazy val sQueryContraints: Schema[QueryConstraints]                        = Schema.derived[QueryConstraints]
    implicit lazy val sQueryContraintsSc: Schema[QueryConstraintsSC]                    =
        Schema.derived[QueryConstraintsSC]
    implicit lazy val sRenameConcept: Schema[RenameConcept]                             = Schema.derived[RenameConcept]
    implicit lazy val sURI: Schema[URI]                                                 = Schema.string
    implicit lazy val sURL: Schema[URL]                                                 = Schema.string
    implicit lazy val sInstant: Schema[Instant]                                         = Schema.string
    implicit lazy val sBulkAnnotationSc: Schema[BulkAnnotationSC]                       = Schema.derived[BulkAnnotationSC]
//    implicit lazy val sConstraintDate: Schema[Constraint.Date]                          = Schema.derived[Constraint.Date]
//    implicit lazy val sConstraintInString: Schema[Constraint.In[String]]                =
//        Schema.derived[Constraint.In[String]]
//    implicit lazy val sConstraintLike: Schema[Constraint.Like]                          = Schema.derived[Constraint.Like]
//    implicit lazy val sConstraintMax: Schema[Constraint.Max]                            = Schema.derived[Constraint.Max]
//    implicit lazy val sConstraintMin: Schema[Constraint.Min]                            = Schema.derived[Constraint.Min]
//    implicit lazy val sConstraintMinMax: Schema[Constraint.MinMax]                      =
//        Schema.derived[Constraint.MinMax]
//    implicit lazy val sConstraintIsNull: Schema[Constraint.IsNull]                      =
//        Schema.derived[Constraint.IsNull]
//    implicit lazy val sConstraint: Schema[Constraint]                                   = Schema.string
//    implicit lazy val sConstraints: Schema[Query]                                 = Schema.derived[Query]
    implicit lazy val sConstraintRequest: Schema[ConstraintRequest]                     = Schema.derived[ConstraintRequest]
    implicit lazy val sQueryRequest: Schema[QueryRequest]                               = Schema.derived[QueryRequest]
//    given Schema[Option[URL]]                              = Schema.string
//    implicit lazy val sOptCAD: Schema[Option[CachedAncillaryDatumSC]]                     = Schema.derived[Option[CachedAncillaryDatumSC]]
//    implicit lazy val sOptDouble: Schema[Option[Double]]                                 = Schema.derived[Option[Double]]
//    implicit lazy val sOptIR: Schema[Option[ImageReferenceSC]]                           = Schema.derived[Option[ImageReferenceSC]]
//    implicit lazy val sOptInt: Schema[Option[Int]]                                       = Schema.derived[Option[Int]]
//    implicit lazy val sOptString: Schema[Option[String]]                                 = Schema.derived[Option[String]]
//    implicit lazy val sOptURI: Schema[Option[URI]]                                       = Schema.derived[Option[URI]]
//    implicit lazy val sOptInstant: Schema[Option[Instant]]                               = Schema.derived[Option[Instant]]

    def all: List[Endpoint[?, ?, ?, ?, ?]]
    def allImpl: List[ServerEndpoint[Any, Future]]

    def handleErrors[T](f: Future[T])(using ec: ExecutionContext): Future[Either[ErrorMsg, T]] =
        f.transform:
            case Success(value)     =>
                // log.atError.log(value.toString())
                Success(Right(value))
            case Failure(exception) =>
                log.atError.withCause(exception).log("Error")
                Success(Left(ServerError(exception.getMessage)))

    def handleEitherAsync[T](
        f: => Either[Throwable, T]
    )(using ec: ExecutionContext): Future[Either[ErrorMsg, T]] =
        Future {
            f match
                case Right(value) => Right(value)
                case Left(e)      => Left(ServerError(e.getMessage))
        }

    def handleOption[T](f: Future[Option[T]])(using
        ec: ExecutionContext
    ): Future[Either[ErrorMsg, T]] =
        f.transform:
            case Success(Some(value)) => Success(Right(value))
            case Success(None)        => Success(Left(NotFound("Not found")))
            case Failure(exception)   =>
                log.atError.withCause(exception).log("Error")
                Success(Left(ServerError(exception.getMessage)))

    // hard coded ATM, but could be configurable
    val baseEndpoint: Endpoint[Unit, Unit, Unit, Unit, Any] = endpoint.in("v1")

    val secureEndpoint: Endpoint[Option[String], Unit, ErrorMsg, Unit, Any] = baseEndpoint
        .securityIn(auth.bearer[Option[String]](WWWAuthenticateChallenge.bearer))
        .errorOut(
            oneOf[ErrorMsg](
                oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[BadRequest])),
                oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[NotFound])),
                oneOfVariant(statusCode(StatusCode.InternalServerError).and(jsonBody[ServerError])),
                oneOfVariant(statusCode(StatusCode.Unauthorized).and(jsonBody[Unauthorized]))
            )
        )

    val paging: EndpointInput[Paging] =
        query[Option[Int]]("offset")
            .and(query[Option[Int]]("limit"))
            .mapTo[Paging]

    val openEndpoint: Endpoint[Unit, Unit, ErrorMsg, Unit, Any] = baseEndpoint.errorOut(
        oneOf[ErrorMsg](
            oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[BadRequest])),
            oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[NotFound])),
            oneOfVariant(statusCode(StatusCode.InternalServerError).and(jsonBody[ServerError]))
        )
    )

    def verify(
        jwtOpt: Option[String]
    )(using jwtService: JwtService, ec: ExecutionContext): Future[Either[Unauthorized, Unit]] =
        jwtOpt match
            case None      => Future(Left(Unauthorized("Missing token")))
            case Some(jwt) =>
                Future(
                    if jwtService.verify(jwt) then Right(())
                    else Left(Unauthorized("Invalid token"))
                )
