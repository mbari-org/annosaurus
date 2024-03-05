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

import org.mbari.annosaurus.controllers.CachedAncillaryDatumController
import org.mbari.annosaurus.domain.{CachedAncillaryDatumSC, CountForVideoReferenceSC, ErrorMsg}
import org.mbari.annosaurus.etc.jwt.JwtService
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import CustomTapirJsonCirce.*

import java.time.Duration
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class CachedAncillaryDatumEndpoints(controller: CachedAncillaryDatumController)(using
    ec: ExecutionContext,
    jwtService: JwtService
) extends Endpoints {

    private val base = "ancillarydata"
    private val tag  = "Ancillary Data"

    // GET /:uuid
    val findDataByUuid: Endpoint[Unit, UUID, ErrorMsg, CachedAncillaryDatumSC, Any] = openEndpoint
        .get
        .in(base / path[UUID]("ancillaryDataUuid"))
        .out(jsonBody[CachedAncillaryDatumSC])
        .name("findDataByUuid")
        .description("Find ancillary data by UUID")
        .tag(tag)

    val findDataByUuidImpl: ServerEndpoint[Any, Future] =
        findDataByUuid
            .serverLogic { uuid =>
                handleOption(controller.findByUUID(uuid).map(_.map(_.toSnakeCase)))
            }

    // GET /videoreference/:uuid
    val findDataByVideoReferenceUuid
        : Endpoint[Unit, UUID, ErrorMsg, Seq[CachedAncillaryDatumSC], Any] = openEndpoint
        .get
        .in(base / "videoreference" / path[UUID]("videoReferenceUuid"))
        .out(jsonBody[Seq[CachedAncillaryDatumSC]])
        .name("findDataByVideoReferenceUuid")
        .description("Find ancillary data by video reference UUID")
        .tag(tag)

    val findDataByVideoReferenceUuidImpl: ServerEndpoint[Any, Future] =
        findDataByVideoReferenceUuid
            .serverLogic { uuid =>
                handleErrors(controller.findByVideoReferenceUUID(uuid).map(_.map(_.toSnakeCase)))
            }

    // GET /imagedmoment/:uuid
    val findDataByImagedMomentUuid: Endpoint[Unit, UUID, ErrorMsg, CachedAncillaryDatumSC, Any] =
        openEndpoint
            .get
            .in(base / "imagedmoment" / path[UUID]("imagedMomentUuid"))
            .out(jsonBody[CachedAncillaryDatumSC])
            .name("findDataByImagedMomentUuid")
            .description("Find ancillary data by imaged moment UUID")
            .tag(tag)

    val findDataByImagedMomentUuidImpl: ServerEndpoint[Any, Future] =
        findDataByImagedMomentUuid
            .serverLogic { uuid =>
                handleOption(controller.findByImagedMomentUUID(uuid).map(_.map(_.toSnakeCase)))
            }

    // GET /observation/:uuid
    val findDataByObservationUuid: Endpoint[Unit, UUID, ErrorMsg, CachedAncillaryDatumSC, Any] =
        openEndpoint
            .get
            .in(base / "observation" / path[UUID]("observationUuid"))
            .out(jsonBody[CachedAncillaryDatumSC])
            .name("findDataByObservationUuid")
            .description("Find ancillary data by observation UUID")
            .tag(tag)

    val findDataByObservationUuidImpl: ServerEndpoint[Any, Future] =
        findDataByObservationUuid
            .serverLogic { uuid =>
                handleOption(controller.findByObservationUUID(uuid).map(_.map(_.toSnakeCase)))
            }

    // POST / form or json body
    val createOneDatum
        : Endpoint[Option[String], CachedAncillaryDatumSC, ErrorMsg, CachedAncillaryDatumSC, Any] =
        secureEndpoint
            .post
            .in(base)
            .in(oneOfBody(jsonBody[CachedAncillaryDatumSC], formBody[CachedAncillaryDatumSC]))
            .out(jsonBody[CachedAncillaryDatumSC])
            .name("createOneDatum")
            .description("Create one ancillary data")
            .tag(tag)

    val createOneDatumImpl: ServerEndpoint[Any, Future] =
        createOneDatum
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { - => data =>
                handleOption(
                    controller
                        .create(data.toCamelCase)
                        .map(_.map(_.toSnakeCase))
                )
            }

    // POST /bulk json body
    val createOrUpdateManyData: Endpoint[Option[String], Seq[CachedAncillaryDatumSC], ErrorMsg, Seq[
        CachedAncillaryDatumSC
    ], Any] = secureEndpoint
        .post
        .in(base / "bulk")
        .in(jsonBody[Seq[CachedAncillaryDatumSC]])
        .out(jsonBody[Seq[CachedAncillaryDatumSC]])
        .name("createOrUpdateManyData")
        .description("Create many ancillary data")
        .tag(tag)

    val createOrUpdateManyDataImpl: ServerEndpoint[Any, Future] =
        createOrUpdateManyData
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { - => data =>
                handleErrors(
                    controller
                        .bulkCreateOrUpdate(data.map(_.toCamelCase))
                        .map(_.map(_.toSnakeCase))
                )
            }

    // PUT /merge/:uuid json body
    val mergeManyData
        : Endpoint[Option[String], (UUID, Seq[CachedAncillaryDatumSC], Option[Int]), ErrorMsg, Seq[
            CachedAncillaryDatumSC
        ], Any] = secureEndpoint
        .put
        .in(base / "merge" / path[UUID]("videoReferenceUuid"))
        .in(jsonBody[Seq[CachedAncillaryDatumSC]])
        .in(query[Option[Int]]("window").description("Window in seconds to merge data. Default is +/-7.5."))
        .out(jsonBody[Seq[CachedAncillaryDatumSC]])
        .name("mergeManyData")
        .description("Merge one ancillary data")
        .tag(tag)

    val mergeManyDataImpl: ServerEndpoint[Any, Future] =
        mergeManyData
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { - => (uuid, data, window) =>
                val windowMillis = window.getOrElse(7500)
                val tolerance    = Duration.ofMillis(windowMillis)
                handleErrors(
                    controller
                        .merge(data.map(_.toCamelCase), uuid, tolerance)
                        .map(_.map(_.toSnakeCase))
                )
            }

    // PUT /:uuid form or json body
    val updateOneDatum: Endpoint[Option[
        String
    ], (UUID, CachedAncillaryDatumSC), ErrorMsg, CachedAncillaryDatumSC, Any] = secureEndpoint
        .put
        .in(base / path[UUID]("ancillaryDataUuid"))
        .in(oneOfBody(jsonBody[CachedAncillaryDatumSC], formBody[CachedAncillaryDatumSC]))
        .out(jsonBody[CachedAncillaryDatumSC])
        .name("updateOneDatum")
        .description("Update one ancillary data")
        .tag(tag)

    val updateOneDatumImpl: ServerEndpoint[Any, Future] =
        updateOneDatum
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { - => (uuid, data) =>
                handleOption(
                    controller
                        .update(uuid, data.toCamelCase)
                        .map(_.map(_.toSnakeCase))
                )
            }

    // DELETE /videoreference/:uuid
    val deleteDataByVideoReferenceUuid
        : Endpoint[Option[String], UUID, ErrorMsg, CountForVideoReferenceSC, Any] = secureEndpoint
        .delete
        .in(base / "videoreference" / path[UUID]("videoReferenceUuid"))
        .out(jsonBody[CountForVideoReferenceSC])
        .name("deleteDataByVideoReferenceUuid")
        .description("Delete ancillary data by video reference UUID")
        .tag(tag)

    val deleteDataByVideoReferenceUuidImpl: ServerEndpoint[Any, Future] =
        deleteDataByVideoReferenceUuid
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { - => uuid =>
                handleErrors(
                    controller
                        .deleteByVideoReferenceUuid(uuid)
                        .map(n => CountForVideoReferenceSC(uuid, n))
                )
            }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        createOrUpdateManyData,
        findDataByImagedMomentUuid,
        mergeManyData,
        findDataByObservationUuid,
        findDataByVideoReferenceUuid,
        deleteDataByVideoReferenceUuid,
        findDataByUuid,
        updateOneDatum,
        createOneDatum
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        createOrUpdateManyDataImpl,
        findDataByImagedMomentUuidImpl,
        mergeManyDataImpl,
        findDataByObservationUuidImpl,
        findDataByVideoReferenceUuidImpl,
        deleteDataByVideoReferenceUuidImpl,
        findDataByUuidImpl,
        updateOneDatumImpl,
        createOneDatumImpl
    )
}
