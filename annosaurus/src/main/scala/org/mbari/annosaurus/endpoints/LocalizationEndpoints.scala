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

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.mbari.annosaurus.controllers.{
    BulkLocalizationException,
    LocalizationController,
    LocalizationValidationException,
    StaleLocalizationException
}
import org.mbari.annosaurus.domain.{
    BadRequest,
    Conflict,
    ErrorMsg,
    Localization,
    LocalizationSC,
    LocalizationType,
    MaskEncodings,
    NotFound,
    ServerError
}
import org.mbari.annosaurus.endpoints.CustomTapirJsonCirce.*
import org.mbari.annosaurus.etc.circe.CirceCodecs.given
import org.mbari.annosaurus.etc.jdk.Loggers.given
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.repository.NotFoundInDatastoreException
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/** The values a client may use when building a localization. */
final case class LocalizationConstraints(localization_types: Seq[String], mask_encodings: Seq[String])

object LocalizationConstraints:
    given Encoder[LocalizationConstraints] = deriveEncoder
    given Decoder[LocalizationConstraints] = deriveDecoder

class LocalizationEndpoints(controller: LocalizationController)(using
    ec: ExecutionContext,
    jwtService: JwtService
) extends Endpoints:

    private val base = "localizations"
    private val tag  = "Localizations"

    /** Maps the controller's failures onto the API's error responses. Anything else is a 500. */
    private def handleLocalizationErrors[T](f: Future[T]): Future[Either[ErrorMsg, T]] =
        f.transform:
            case Success(value)                              => Success(Right(value))
            case Failure(e: LocalizationValidationException) => Success(Left(BadRequest(e.getMessage)))
            case Failure(e: BulkLocalizationException)       => Success(Left(BadRequest(e.getMessage)))
            case Failure(e: StaleLocalizationException)      =>
                Success(Left(Conflict(e.getMessage, Some(e.current.toSnakeCase))))
            case Failure(e: NotFoundInDatastoreException)    => Success(Left(NotFound(e.getMessage)))
            case Failure(e)                                  =>
                log.atError.withCause(e).log("Error")
                Success(Left(ServerError(e.getMessage)))

    // GET /:uuid
    val findLocalizationByUuid: Endpoint[Unit, UUID, ErrorMsg, LocalizationSC, Any] = openEndpoint
        .get
        .in(base / path[UUID]("localizationUuid"))
        .out(jsonBody[LocalizationSC])
        .name("findLocalizationByUuid")
        .description("Find a localization by its UUID")
        .tag(tag)

    val findLocalizationByUuidImpl: ServerEndpoint[Any, Future] = findLocalizationByUuid
        .serverLogic { uuid => handleOption(controller.findByUUID(uuid).map(_.map(_.toSnakeCase))) }

    // GET /observation/:observationUuid
    val findLocalizationsByObservationUuid: Endpoint[Unit, UUID, ErrorMsg, Seq[LocalizationSC], Any] =
        openEndpoint
            .get
            .in(base / "observation" / path[UUID]("observationUuid"))
            .out(jsonBody[Seq[LocalizationSC]])
            .name("findLocalizationsByObservationUuid")
            .description("Find the localizations of an observation")
            .tag(tag)

    val findLocalizationsByObservationUuidImpl: ServerEndpoint[Any, Future] =
        findLocalizationsByObservationUuid
            .serverLogic { uuid =>
                handleErrors(controller.findByObservationUuid(uuid).map(_.map(_.toSnakeCase)))
            }

    // GET /videoreference/:videoReferenceUuid
    val findLocalizationsByVideoReferenceUuid: Endpoint[Unit, (UUID, Paging), ErrorMsg, Seq[LocalizationSC], Any] =
        openEndpoint
            .get
            .in(base / "videoreference" / path[UUID]("videoReferenceUuid"))
            .in(paging)
            .out(jsonBody[Seq[LocalizationSC]])
            .name("findLocalizationsByVideoReferenceUuid")
            .description("Find the localizations of a video reference, paged (default limit 1000, larger limits are clamped to 10000)")
            .tag(tag)

    val findLocalizationsByVideoReferenceUuidImpl: ServerEndpoint[Any, Future] =
        findLocalizationsByVideoReferenceUuid
            .serverLogic { (uuid, page) =>
                handleErrors(
                    controller.findByVideoReferenceUuid(uuid, page.limit, page.offset).map(_.map(_.toSnakeCase))
                )
            }

    // PUT /:observationUuid/:localizationType
    val upsertLocalization: Endpoint[Option[String], (UUID, String, Localization), ErrorMsg, LocalizationSC, Any] =
        secureEndpoint
            .put
            .in(base / path[UUID]("observationUuid") / path[String]("localizationType"))
            .in(jsonBody[Localization])
            .out(jsonBody[LocalizationSC])
            .name("upsertLocalization")
            .description(
                "Create or replace the localization of the given type on an observation. " +
                    "The optional stale-write token in the body, sent as last_updated_timestamp (preferred) or " +
                    "last_updated_time (the name responses use), must match the stored row, otherwise 409 is " +
                    "returned with the current row. If both are sent, last_updated_timestamp wins."
            )
            .tag(tag)

    val upsertLocalizationImpl: ServerEndpoint[Any, Future] = upsertLocalization
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ => (observationUuid, localizationType, localization) =>
            handleLocalizationErrors(
                controller.upsert(observationUuid, localizationType, localization).map(_.toSnakeCase)
            )
        }

    // DELETE /:observationUuid/:localizationType
    val deleteLocalization: Endpoint[Option[String], (UUID, String), ErrorMsg, Unit, Any] =
        secureEndpoint
            .delete
            .in(base / path[UUID]("observationUuid") / path[String]("localizationType"))
            .out(statusCode(StatusCode.Ok).and(emptyOutput))
            .name("deleteLocalization")
            .description("Delete the localization of the given type from an observation")
            .tag(tag)

    val deleteLocalizationImpl: ServerEndpoint[Any, Future] = deleteLocalization
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ => (observationUuid, localizationType) =>
            controller
                .delete(observationUuid, localizationType)
                .transform:
                    case Success(true)  => Success(Right(()))
                    case Success(false) => Success(Left(NotFound("Localization not found")))
                    case Failure(e)     =>
                        log.atError.withCause(e).log("Error")
                        Success(Left(ServerError(e.getMessage)))
        }

    // POST /bulk
    val bulkUpsertLocalizations: Endpoint[Option[String], Seq[Localization], ErrorMsg, Seq[LocalizationSC], Any] =
        secureEndpoint
            .post
            .in(base / "bulk")
            .in(jsonBody[Seq[Localization]])
            .out(jsonBody[Seq[LocalizationSC]])
            .name("bulkUpsertLocalizations")
            .description(
                "Create or replace many localizations, each carrying an observation_uuid. " +
                    "All or nothing: if any item is invalid, nothing is written and the failing indexes are named."
            )
            .tag(tag)

    val bulkUpsertLocalizationsImpl: ServerEndpoint[Any, Future] = bulkUpsertLocalizations
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ => items =>
            handleLocalizationErrors(controller.bulkUpsert(items).map(_.map(_.toSnakeCase)))
        }

    // GET /list/types
    val listLocalizationTypes: Endpoint[Unit, Unit, ErrorMsg, Seq[String], Any] = openEndpoint
        .get
        .in(base / "list" / "types")
        .out(jsonBody[Seq[String]])
        .name("listLocalizationTypes")
        .description("The valid localization types")
        .tag(tag)

    val listLocalizationTypesImpl: ServerEndpoint[Any, Future] = listLocalizationTypes
        .serverLogic { _ => Future(Right(LocalizationType.all.map(_.value))) }

    // GET /list/maskencodings
    val listMaskEncodings: Endpoint[Unit, Unit, ErrorMsg, Seq[String], Any] = openEndpoint
        .get
        .in(base / "list" / "maskencodings")
        .out(jsonBody[Seq[String]])
        .name("listMaskEncodings")
        .description("The valid mask encodings")
        .tag(tag)

    val listMaskEncodingsImpl: ServerEndpoint[Any, Future] = listMaskEncodings
        .serverLogic { _ => Future(Right(MaskEncodings)) }

    // GET /list/constraints
    val listLocalizationConstraints: Endpoint[Unit, Unit, ErrorMsg, LocalizationConstraints, Any] = openEndpoint
        .get
        .in(base / "list" / "constraints")
        .out(jsonBody[LocalizationConstraints])
        .name("listLocalizationConstraints")
        .description("The valid localization types and mask encodings")
        .tag(tag)

    val listLocalizationConstraintsImpl: ServerEndpoint[Any, Future] = listLocalizationConstraints
        .serverLogic { _ =>
            Future(Right(LocalizationConstraints(LocalizationType.all.map(_.value), MaskEncodings)))
        }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        findLocalizationByUuid,
        findLocalizationsByObservationUuid,
        findLocalizationsByVideoReferenceUuid,
        upsertLocalization,
        deleteLocalization,
        bulkUpsertLocalizations,
        listLocalizationTypes,
        listMaskEncodings,
        listLocalizationConstraints
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        findLocalizationByUuidImpl,
        findLocalizationsByObservationUuidImpl,
        findLocalizationsByVideoReferenceUuidImpl,
        upsertLocalizationImpl,
        deleteLocalizationImpl,
        bulkUpsertLocalizationsImpl,
        listLocalizationTypesImpl,
        listMaskEncodingsImpl,
        listLocalizationConstraintsImpl
    )
