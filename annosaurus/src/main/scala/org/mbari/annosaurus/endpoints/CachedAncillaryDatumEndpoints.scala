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
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import sttp.model.StatusCode

import java.time.Duration
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class CachedAncillaryDatumEndpoints(controller: CachedAncillaryDatumController)(using
    ec: ExecutionContext,
    jwtService: JwtService
) extends Endpoints {

    // GET /:uuid
    val findDataByUuid: Endpoint[Unit, UUID, ErrorMsg, CachedAncillaryDatumSC, Any] = openEndpoint
        .get
        .in("v1" / "ancillarydata" / path[UUID]("uuid"))
        .out(jsonBody[CachedAncillaryDatumSC])
        .name("findDataByUuid")
        .description("Find ancillary data by UUID")
        .tag("Ancillary Data")

    val findDataByUuidImpl: ServerEndpoint[Any, Future] =
        findDataByUuid
            .serverLogic { uuid =>
                handleOption(controller.findByUUID(uuid).map(_.map(_.toSnakeCase)))
            }

    // GET /videoreference/:uuid
    val findDataByVideoReferenceUuid
        : Endpoint[Unit, UUID, ErrorMsg, Seq[CachedAncillaryDatumSC], Any] = openEndpoint
        .get
        .in("v1" / "ancillarydata" / "videoreference" / path[UUID]("videoReferenceUuid"))
        .out(jsonBody[Seq[CachedAncillaryDatumSC]])
        .name("findDataByVideoReferenceUuid")
        .description("Find ancillary data by video reference UUID")
        .tag("Ancillary Data")

    val findDataByVideoReferenceUuidImpl: ServerEndpoint[Any, Future] =
        findDataByVideoReferenceUuid
            .serverLogic { uuid =>
                handleErrors(controller.findByVideoReferenceUUID(uuid).map(_.map(_.toSnakeCase)))
            }

    // GET /imagedmoment/:uuid
    val findDataByImagedMomentUuid: Endpoint[Unit, UUID, ErrorMsg, CachedAncillaryDatumSC, Any] =
        openEndpoint
            .get
            .in("v1" / "ancillarydata" / "imagedmoment" / path[UUID]("imagedMomentUuid"))
            .out(jsonBody[CachedAncillaryDatumSC])
            .name("findDataByImagedMomentUuid")
            .description("Find ancillary data by imaged moment UUID")
            .tag("Ancillary Data")

    val findDataByImagedMomentUuidImpl: ServerEndpoint[Any, Future] =
        findDataByImagedMomentUuid
            .serverLogic { uuid =>
                handleOption(controller.findByImagedMomentUUID(uuid).map(_.map(_.toSnakeCase)))
            }

    // GET /observation/:uuid
    val findDataByObservationUuid: Endpoint[Unit, UUID, ErrorMsg, CachedAncillaryDatumSC, Any] =
        openEndpoint
            .get
            .in("v1" / "ancillarydata" / "observation" / path[UUID]("observationUuid"))
            .out(jsonBody[CachedAncillaryDatumSC])
            .name("findDataByObservationUuid")
            .description("Find ancillary data by observation UUID")
            .tag("Ancillary Data")

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
            .in("v1" / "ancillarydata")
            .in(oneOfBody(jsonBody[CachedAncillaryDatumSC], formBody[CachedAncillaryDatumSC]))
            .out(jsonBody[CachedAncillaryDatumSC])
            .name("createOneDatum")
            .description("Create one ancillary data")
            .tag("Ancillary Data")

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
        .in("v1" / "ancillarydata" / "bulk")
        .in(jsonBody[Seq[CachedAncillaryDatumSC]])
        .out(jsonBody[Seq[CachedAncillaryDatumSC]])
        .name("createOrUpdateManyData")
        .description("Create many ancillary data")
        .tag("Ancillary Data")

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
        .in("v1" / "ancillarydata" / "merge" / path[UUID]("videoReferenceUuid"))
        .in(jsonBody[Seq[CachedAncillaryDatumSC]])
        .in(query[Option[Int]]("window").description("Window in seconds to merge data"))
        .out(jsonBody[Seq[CachedAncillaryDatumSC]])
        .name("mergeManyData")
        .description("Merge one ancillary data")
        .tag("Ancillary Data")

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
        .in("v1" / "ancillarydata" / path[UUID]("uuid"))
        .in(oneOfBody(jsonBody[CachedAncillaryDatumSC], formBody[CachedAncillaryDatumSC]))
        .out(jsonBody[CachedAncillaryDatumSC])
        .name("updateOneDatum")
        .description("Update one ancillary data")
        .tag("Ancillary Data")

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
        .in("v1" / "ancillarydata" / "videoreference" / path[UUID]("videoReferenceUuid"))
        .out(jsonBody[CountForVideoReferenceSC])
        .name("deleteDataByVideoReferenceUuid")
        .description("Delete ancillary data by video reference UUID")
        .tag("Ancillary Data")

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

    override def all: List[Endpoint[_, _, _, _, _]] = List(
        findDataByUuid,
        findDataByVideoReferenceUuid,
        findDataByImagedMomentUuid,
        findDataByObservationUuid,
        createOneDatum,
        createOrUpdateManyData,
        mergeManyData,
        updateOneDatum,
        deleteDataByVideoReferenceUuid
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        findDataByUuidImpl,
        findDataByVideoReferenceUuidImpl,
        findDataByImagedMomentUuidImpl,
        findDataByObservationUuidImpl,
        createOneDatumImpl,
        createOrUpdateManyDataImpl,
        mergeManyDataImpl,
        updateOneDatumImpl,
        deleteDataByVideoReferenceUuidImpl
    )
}
