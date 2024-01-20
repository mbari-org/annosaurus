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

import org.mbari.annosaurus.controllers.ImagedMomentController
import org.mbari.annosaurus.domain.{
    AnnotationSC,
    ConceptCount,
    Count,
    CountForVideoReferenceSC,
    ErrorMsg,
    ImagedMomentSC,
    ImagedMomentTimestampUpdateSC,
    VideoTimestampSC,
    WindowRequestSC
}
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.etc.tapir.TapirCodecs.given
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.vcr4j.time.Timecode
import org.mbari.annosaurus.etc.sdk.Futures.*
import sttp.model.StatusCode

import java.time.{Duration, Instant}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class ImagedMomentEndpoints(controller: ImagedMomentController)(using
    ec: ExecutionContext,
    jwtService: JwtService
) extends Endpoints {

    val findAllImagedMoments: Endpoint[Unit, Paging, ErrorMsg, Seq[ImagedMomentSC], Any] =
        openEndpoint
            .get
            .in("v1" / "imagedmoments")
            .in(paging)
            .out(jsonBody[Seq[ImagedMomentSC]])
            .name("findAllImagedMoments")
            .description("Find all imaged moments")
            .tag("imagedmoments")

    val findAllImagedMomentsImpl: ServerEndpoint[Any, Future] =
        findAllImagedMoments
            .serverLogic { paging =>
                handleErrors(
                    controller.findAll(paging.limit, paging.offset).map(_.map(_.toSnakeCase).toSeq)
                )
            }

    val countAllImagedMoments: Endpoint[Unit, Unit, ErrorMsg, Count, Any] =
        openEndpoint
            .get
            .in("v1" / "imagedmoments" / "count" / "all")
            .out(jsonBody[Count])

    val countAllImagedMomentsImpl: ServerEndpoint[Any, Future] =
        countAllImagedMoments
            .serverLogic { _ =>
                handleErrors(controller.countAll().map(i => Count(i)))
            }

    val findImagedMomentsWithImages: Endpoint[Unit, Paging, ErrorMsg, Seq[ImagedMomentSC], Any] =
        openEndpoint
            .get
            .in("v1" / "imagedmoments" / "find" / "images")
            .in(paging)
            .out(jsonBody[Seq[ImagedMomentSC]])
            .name("findImagedMomentsWithImages")
            .description("Find all imaged moments with images")
            .tag("imagedmoments")

    val findImagedMomentsWithImagesImpl: ServerEndpoint[Any, Future] =
        findImagedMomentsWithImages
            .serverLogic { paging =>
                handleErrors(
                    controller
                        .findWithImages(paging.limit, paging.offset)
                        .map(_.map(_.toSnakeCase).toSeq)
                )
            }

    val countImagedMomentsWithImages: Endpoint[Unit, Unit, ErrorMsg, Count, Any] =
        openEndpoint
            .get
            .in("v1" / "imagedmoments" / "count" / "images")
            .out(jsonBody[Count])

    val countImagedMomentsWithImagesImpl: ServerEndpoint[Any, Future] =
        countImagedMomentsWithImages
            .serverLogic { _ =>
                handleErrors(controller.countWithImages().map(i => Count(i)))
            }

    // GET /count/images/:videoReferenceUuid
    val countImagesForVideoReference: Endpoint[Unit, UUID, ErrorMsg, Count, Any] =
        openEndpoint
            .get
            .in("v1" / "imagedmoments" / "count" / "images" / path[UUID]("videoReferenceUuid"))
            .out(jsonBody[Count])

    val countImagesForVideoReferenceImpl: ServerEndpoint[Any, Future] =
        countImagesForVideoReference
            .serverLogic { videoReferenceUuid =>
                handleErrors(
                    controller
                        .countByVideoReferenceUUIDWithImages(videoReferenceUuid)
                        .map(i => Count(i))
                )
            }

    // GET /find/linkname/:linkName
    val findImagedMomentsByLinkName
        : Endpoint[Unit, (String, Paging), ErrorMsg, Seq[ImagedMomentSC], Any] =
        openEndpoint
            .get
            .in("v1" / "imagedmoments" / "find" / "linkname" / path[String]("linkName"))
            .in(paging)
            .out(jsonBody[Seq[ImagedMomentSC]])
            .name("findImagedMomentsByLinkName")
            .description("Find all imaged moments with a given link name")
            .tag("imagedmoments")

    val findImagedMomentsByLinkNameImpl: ServerEndpoint[Any, Future] =
        findImagedMomentsByLinkName
            .serverLogic { case (linkName, paging) =>
                handleErrors(
                    controller
                        .findByLinkName(linkName, paging.limit, paging.offset)
                        .map(_.map(_.toSnakeCase).toSeq)
                )
            }

    // GET /count/linkname/:linkName
    val countImagedMomentsByLinkName: Endpoint[Unit, String, ErrorMsg, Count, Any] =
        openEndpoint
            .get
            .in("v1" / "imagedmoments" / "count" / "linkname" / path[String]("linkName"))
            .out(jsonBody[Count])

    val countImagedMomentsByLinkNameImpl: ServerEndpoint[Any, Future] =
        countImagedMomentsByLinkName
            .serverLogic { linkName =>
                handleErrors(controller.countByLinkName(linkName).map(i => Count(i)))
            }

    // GET /:uuid
    val findImagedMomentByUUID: Endpoint[Unit, UUID, ErrorMsg, ImagedMomentSC, Any] =
        openEndpoint
            .get
            .in("v1" / "imagedmoments" / path[UUID]("uuid"))
            .out(jsonBody[ImagedMomentSC])
            .name("findImagedMomentByUUID")
            .description("Find an imaged moment by UUID")
            .tag("imagedmoments")

    val findImagedMomentByUUIDImpl: ServerEndpoint[Any, Future] =
        findImagedMomentByUUID
            .serverLogic { uuid =>
                handleOption(controller.findByUUID(uuid).map(_.map(_.toSnakeCase)))
            }

    // get /concept/:name
    val findImagedMomentsByConceptName
        : Endpoint[Unit, (String, Paging), ErrorMsg, Seq[ImagedMomentSC], Any] =
        openEndpoint
            .get
            .in("v1" / "imagedmoments" / "concept" / path[String]("conceptName"))
            .in(paging)
            .out(jsonBody[Seq[ImagedMomentSC]])
            .name("findImagedMomentsByConceptName")
            .description("Find all imaged moments with a given concept name")
            .tag("imagedmoments")

    val findImagedMomentsByConceptNameImpl: ServerEndpoint[Any, Future] =
        findImagedMomentsByConceptName
            .serverLogic { case (conceptName, paging) =>
                handleErrors(
                    controller
                        .findByConcept(conceptName, paging.limit, paging.offset)
                        .map(_.map(_.toSnakeCase).toSeq)
                )
            }

    // GET /concept/images/:name
    val findImagedMomentsByConceptNameWithImages
        : Endpoint[Unit, (String, Paging), ErrorMsg, Seq[ImagedMomentSC], Any] =
        openEndpoint
            .get
            .in("v1" / "imagedmoments" / "concept" / "images" / path[String]("conceptName"))
            .in(paging)
            .out(jsonBody[Seq[ImagedMomentSC]])
            .name("findImagedMomentsByConceptNameWithImages")
            .description("Find all imaged moments with a given concept name that have images")
            .tag("imagedmoments")

    // I think this is fixed but I'm keeping this note:  This returns an imagedmoment for EACH image. If there are
    // two images for a moment, you'll get the image moment twice
    // This needs a distinct modifier
    val findImagedMomentsByConceptNameWithImagesImpl: ServerEndpoint[Any, Future] =
        findImagedMomentsByConceptNameWithImages
            .serverLogic { case (conceptName, paging) =>
                handleErrors(
                    controller
                        .findByConceptWithImages(conceptName, paging.limit, paging.offset)
                        .map(_.map(_.toSnakeCase).toSeq)
                )
            }

    // GET /videoreference/chunked/:uuid (skip)

    // GET /concept/count/:name
    val countImagedMomentsByConceptName: Endpoint[Unit, String, ErrorMsg, ConceptCount, Any] =
        openEndpoint
            .get
            .in("v1" / "imagedmoments" / "concept" / "count" / path[String]("conceptName"))
            .out(jsonBody[ConceptCount])

    val countImagedMomentsByConceptNameImpl: ServerEndpoint[Any, Future] =
        countImagedMomentsByConceptName
            .serverLogic { conceptName =>
                handleErrors(
                    controller.countByConcept(conceptName).map(i => ConceptCount(conceptName, i))
                )
            }

    // GET /concept/images/count/:name
    val countImagedMomentsByConceptNameWithImages
        : Endpoint[Unit, String, ErrorMsg, ConceptCount, Any] =
        openEndpoint
            .get
            .in(
                "v1" / "imagedmoments" / "concept" / "images" / "count" / path[String](
                    "conceptName"
                )
            )
            .out(jsonBody[ConceptCount])

    val countImagedMomentsByConceptNameWithImagesImpl: ServerEndpoint[Any, Future] =
        countImagedMomentsByConceptNameWithImages
            .serverLogic { conceptName =>
                handleErrors(
                    controller
                        .countByConceptWithImages(conceptName)
                        .map(i => ConceptCount(conceptName, i))
                )
            }

    // GET /modified/:start/:end
    val findImagedMomentsBetweenModifiedDates
        : Endpoint[Unit, (Instant, Instant, Paging), ErrorMsg, Seq[ImagedMomentSC], Any] =
        openEndpoint
            .get
            .in("v1" / "imagedmoments" / "modified" / path[Instant]("start") / path[Instant]("end"))
            .in(paging)
            .out(jsonBody[Seq[ImagedMomentSC]])
            .name("findImagedMomentsBetweenModifiedDates")
            .description("Find all imaged moments modified between two dates")
            .tag("imagedmoments")

    val findImagedMomentsBetweenModifiedDatesImpl: ServerEndpoint[Any, Future] =
        findImagedMomentsBetweenModifiedDates
            .serverLogic { case (start, end, paging) =>
                handleErrors(
                    controller
                        .findBetweenUpdatedDates(start, end, paging.limit, paging.offset)
                        .map(_.map(_.toSnakeCase).toSeq)
                )
            }

    // GET /modified/count/:start/:end
    val countImagedMomentsBetweenModifiedDates
        : Endpoint[Unit, (Instant, Instant), ErrorMsg, Count, Any] =
        openEndpoint
            .get
            .in(
                "v1" / "imagedmoments" / "modified" / "count" / path[Instant]("start") / path[
                    Instant
                ]("end")
            )
            .out(jsonBody[Count])
            .name("countImagedMomentsBetweenModifiedDates")
            .description("Count all imaged moments modified between two dates")
            .tag("imagedmoments")

    // TODO - The original returned a count with start_timestamp and end_timestamp
    val countImagedMomentsBetweenModifiedDatesImpl: ServerEndpoint[Any, Future] =
        countImagedMomentsBetweenModifiedDates
            .serverLogic { case (start, end) =>
                handleErrors(controller.countBetweenUpdatedDates(start, end).map(i => Count(i)))
            }

    // GET /counts
    val countsPerVideoReference
        : Endpoint[Unit, Unit, ErrorMsg, Seq[CountForVideoReferenceSC], Any] =
        openEndpoint
            .get
            .in("v1" / "imagedmoments" / "counts")
            .out(jsonBody[Seq[CountForVideoReferenceSC]])
            .name("countsPerVideoReference")
            .description("Count all imaged moments per video reference")
            .tag("imagedmoments")

    val countsPerVideoReferenceImpl: ServerEndpoint[Any, Future] =
        countsPerVideoReference
            .serverLogic { _ =>
                handleErrors(
                    controller
                        .countAllGroupByVideoReferenceUUID()
                        .map(_.map(c => CountForVideoReferenceSC(c._1, c._2)).toSeq)
                )
            }

    // GET /videoreference
    val findAllVideoReferenceUUIDs: Endpoint[Unit, Unit, ErrorMsg, Seq[UUID], Any] =
        openEndpoint
            .get
            .in("v1" / "imagedmoments" / "videoreference")
            .out(jsonBody[Seq[UUID]])
            .name("findAllVideoReferenceUUIDs")
            .description("Find all video reference UUIDs")
            .tag("imagedmoments")

    val findAllVideoReferenceUUIDsImpl: ServerEndpoint[Any, Future] =
        findAllVideoReferenceUUIDs
            .serverLogic { _ =>
                handleErrors(controller.findAllVideoReferenceUUIDs().map(_.toSeq))
            }

    // GET /videoreference/:uuid
    val findByVideoReferenceUuid: Endpoint[Unit, UUID, ErrorMsg, Seq[ImagedMomentSC], Any] =
        openEndpoint
            .get
            .in("v1" / "imagedmoments" / "videoreference" / path[UUID]("uuid"))
            .out(jsonBody[Seq[ImagedMomentSC]])
            .name("findByVideoReferenceUuid")
            .description("Find all imaged moments for a given video reference UUID")
            .tag("imagedmoments")

    val findByVideoReferenceUuidImpl: ServerEndpoint[Any, Future] =
        findByVideoReferenceUuid
            .serverLogic { uuid =>
                handleErrors(
                    controller.findByVideoReferenceUUID(uuid).map(_.map(_.toSnakeCase).toSeq)
                )
            }

    // GET /videoreference/modified/:uuid/:date
    val countModifiedBeforeDate
        : Endpoint[Unit, (UUID, Instant), ErrorMsg, CountForVideoReferenceSC, Any] =
        openEndpoint
            .get
            .in(
                "v1" / "imagedmoments" / "videoreference" / "modified" / path[UUID]("uuid") / path[
                    Instant
                ]("date")
            )
            .out(jsonBody[CountForVideoReferenceSC])
            .name("countModifiedBeforeDate")
            .description(
                "Count all imaged moments modifed before a given date for a given video reference UUID"
            )
            .tag("imagedmoments")

    val countModifiedBeforeDateImpl: ServerEndpoint[Any, Future] =
        countModifiedBeforeDate
            .serverLogic { case (uuid, date) =>
                handleErrors(
                    controller
                        .countModifiedBeforeDate(uuid, date)
                        .map(i => CountForVideoReferenceSC(uuid, i))
                )
            }

    // POST /windowrequest
    val findImagedMomentsByWindowRequest
        : Endpoint[Unit, (Paging, WindowRequestSC), ErrorMsg, Seq[ImagedMomentSC], Any] =
        openEndpoint
            .post
            .in("v1" / "imagedmoments" / "windowrequest")
            .in(paging)
            .in(jsonBody[WindowRequestSC])
            .out(jsonBody[Seq[ImagedMomentSC]])
            .name("findImagedMomentsByWindowRequest")
            .description("Find all imaged moments for a given window request")
            .tag("imagedmoments")

    val findImagedMomentsByWindowRequestImpl: ServerEndpoint[Any, Future] =
        findImagedMomentsByWindowRequest
            .serverLogic { case (paging, windowRequest) =>
                handleErrors(
                    controller
                        .findByWindowRequest(windowRequest.toCamelCase, paging.limit, paging.offset)
                        .map(_.map(_.toSnakeCase).toSeq)
                )
            }

    // DELETE /videoreference/:uuid
    val deleteByVideoReferenceUUID: Endpoint[Unit, UUID, ErrorMsg, CountForVideoReferenceSC, Any] =
        openEndpoint
            .delete
            .in("v1" / "imagedmoments" / "videoreference" / path[UUID]("videoReferenceUuid"))
            .out(jsonBody[CountForVideoReferenceSC])
            .name("deleteByVideoReferenceUUID")
            .description("Delete all imaged moments for a given video reference UUID")
            .tag("imagedmoments")

    val deleteByVideoReferenceUUIDImpl: ServerEndpoint[Any, Future] =
        deleteByVideoReferenceUUID
            .serverLogic { uuid =>
                handleErrors(
                    controller
                        .deleteByVideoReferenceUUID(uuid)
                        .map(i => CountForVideoReferenceSC(uuid, i))
                )
            }

    // GET /imagereference/:uuid
    val findByImageReferenceUUID: Endpoint[Unit, UUID, ErrorMsg, ImagedMomentSC, Any] =
        openEndpoint
            .get
            .in("v1" / "imagedmoments" / "imagereference" / path[UUID]("imageReferenceUuid"))
            .out(jsonBody[ImagedMomentSC])
            .name("findByImageReferenceUUID")
            .description("Find all imaged moments for a given image reference UUID")
            .tag("imagedmoments")

    val findByImageReferenceUUIDImpl: ServerEndpoint[Any, Future] =
        findByImageReferenceUUID
            .serverLogic { uuid =>
                handleOption(controller.findByImageReferenceUUID(uuid).map(_.map(_.toSnakeCase)))
            }

    // GET /observation/:uuid
    val findByObservationUUID: Endpoint[Unit, UUID, ErrorMsg, ImagedMomentSC, Any] =
        openEndpoint
            .get
            .in("v1" / "imagedmoments" / "observation" / path[UUID]("observationUuid"))
            .out(jsonBody[ImagedMomentSC])
            .name("findByObservationUUID")
            .description("Find an imaged moment for a given observation UUID")
            .tag("imagedmoments")

    val findByObservationUUIDImpl: ServerEndpoint[Any, Future] =
        findByObservationUUID
            .serverLogic { uuid =>
                handleOption(controller.findByObservationUUID(uuid).map(_.map(_.toSnakeCase)))
            }

    // PUT /:uuid
    val updateImagedMoment
        : Endpoint[Option[String], (UUID, VideoTimestampSC), ErrorMsg, ImagedMomentSC, Any] =
        secureEndpoint
            .put
            .in("v1" / "imagedmoments" / path[UUID]("uuid"))
            .in(oneOfBody(jsonBody[VideoTimestampSC], formBody[VideoTimestampSC]))
            .out(jsonBody[ImagedMomentSC])
            .name("updateImagedMoment")
            .description("Update an imaged moment")
            .tag("imagedmoments")

    val updateImagedMomentImpl: ServerEndpoint[Any, Future] =
        updateImagedMoment
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { _ => (uuid, dto) =>
                val timecode    = dto.timecode.map(s => Timecode(s))
                val elapsedTime = dto.elapsed_time_millis.map(t => Duration.ofMillis(t))
                handleErrors(
                    controller
                        .update(
                            uuid,
                            dto.video_reference_uuid,
                            timecode,
                            dto.recorded_timestamp,
                            elapsedTime
                        )
                        .map(_.toSnakeCase)
                )
            }

    // PUT /newtime/:uuid/:time
    val updateRecordedTimestampsForVideoReference: Endpoint[Option[
        String
    ], (UUID, Instant), ErrorMsg, Seq[ImagedMomentSC], Any] = secureEndpoint
        .put
        .in(
            "v1" / "imagedmoments" / "newtime" / path[UUID]("videoReferenceUuid") / path[Instant](
                "time"
            ).description("Use compact iso8601")
        )
        .out(jsonBody[Seq[ImagedMomentSC]])
        .name("updateRecordedTimestampsForVideoReference")
        .description(
            "Recalculate recorded timestamps for a given video reference UUID using a new start time and the imagedmoments elapsed time"
        )
        .tag("imagedmoments")

    val updateRecordedTimestampsForVideoReferenceImpl: ServerEndpoint[Any, Future] =
        updateRecordedTimestampsForVideoReference
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { _ => (videoReferenceUuid, time) =>
                handleErrors(
                    controller
                        .updateRecordedTimestamps(videoReferenceUuid, time)
                        .map(_.map(_.toSnakeCase).toSeq)
                )
            }

    // PUT /tapetime with json body
    val updateRecordedTimestampForObservationUuid: Endpoint[Option[String], Seq[
        AnnotationSC
    ], ErrorMsg, ImagedMomentTimestampUpdateSC, Any] = secureEndpoint
        .put
        .in("v1" / "imagedmoments" / "tapetime")
        .in(jsonBody[Seq[AnnotationSC]])
        .out(jsonBody[ImagedMomentTimestampUpdateSC])
        .name("updateRecordedTimestampForObservationUuid")
        .description(
            "Recalculate recorded timestamps for a given video reference UUID using a new start time and the imagedmoments elapsed time. Annotations need observation_uuid and recorded_timestamp fields. This is not an atomic operation"
        )
        .tag("imagedmoments")

    val updateRecordedTimestampForObservationUuidImpl: ServerEndpoint[Any, Future] =
        updateRecordedTimestampForObservationUuid
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { _ => annotations =>
                var n = 0
                for
                    a                 <- annotations
                    observationUuid   <- a.observation_uuid
                    recordedTimestamp <- a.recorded_timestamp
                do
                    controller
                        .updateRecordedTimestampByObservationUuid(
                            observationUuid,
                            recordedTimestamp
                        )
                        .join
                    n = n + 1
                handleErrors(Future.successful(ImagedMomentTimestampUpdateSC(annotations.size, n)))
            }

    // DEELTE /:uuid
    val deleteImagedMoment: Endpoint[Option[String], UUID, ErrorMsg, Unit, Any] = secureEndpoint
        .delete
        .in("v1" / "imagedmoments" / path[UUID]("uuid"))
        .out(statusCode(StatusCode.NoContent).and(emptyOutput))
        .name("deleteImagedMoment")
        .description("Delete an imaged moment")
        .tag("imagedmoments")

    val deleteImagedMomentImpl: ServerEndpoint[Any, Future] =
        deleteImagedMoment
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { _ => uuid =>
                handleErrors(
                    controller
                        .delete(uuid)
                        .map(b => if (b) StatusCode.NoContent else StatusCode.NotFound)
                )
            }

    override def all: List[Endpoint[_, _, _, _, _]] = List(
        findAllImagedMoments,
        countAllImagedMoments,
        findImagedMomentsWithImages,
        countImagedMomentsWithImages,
        countImagesForVideoReference,
        findImagedMomentsByLinkName,
        countImagedMomentsByLinkName,
        findImagedMomentByUUID,
        findImagedMomentsByConceptName,
        findImagedMomentsByConceptNameWithImages,
        countImagedMomentsByConceptName,
        countImagedMomentsByConceptNameWithImages,
        findImagedMomentsBetweenModifiedDates,
        countImagedMomentsBetweenModifiedDates,
        countsPerVideoReference,
        findAllVideoReferenceUUIDs,
        findByVideoReferenceUuid,
        countModifiedBeforeDate,
        findByImageReferenceUUID,
        findByObservationUUID,
        updateImagedMoment,
        updateRecordedTimestampsForVideoReference,
        updateRecordedTimestampForObservationUuid,
        deleteImagedMoment,
        deleteByVideoReferenceUUID,
        findImagedMomentsByWindowRequest
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        findAllImagedMomentsImpl,
        countAllImagedMomentsImpl,
        findImagedMomentsWithImagesImpl,
        countImagedMomentsWithImagesImpl,
        countImagesForVideoReferenceImpl,
        findImagedMomentsByLinkNameImpl,
        countImagedMomentsByLinkNameImpl,
        findImagedMomentByUUIDImpl,
        findImagedMomentsByConceptNameImpl,
        findImagedMomentsByConceptNameWithImagesImpl,
        countImagedMomentsByConceptNameImpl,
        countImagedMomentsByConceptNameWithImagesImpl,
        findImagedMomentsBetweenModifiedDatesImpl,
        countImagedMomentsBetweenModifiedDatesImpl,
        countsPerVideoReferenceImpl,
        findAllVideoReferenceUUIDsImpl,
        findByVideoReferenceUuidImpl,
        countModifiedBeforeDateImpl,
        findByImageReferenceUUIDImpl,
        findByObservationUUIDImpl,
        updateImagedMomentImpl,
        updateRecordedTimestampsForVideoReferenceImpl,
        updateRecordedTimestampForObservationUuidImpl,
        deleteImagedMomentImpl,
        deleteByVideoReferenceUUIDImpl,
        findImagedMomentsByWindowRequestImpl
    )
}
