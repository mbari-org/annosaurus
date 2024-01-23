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

import org.mbari.annosaurus.controllers.CachedVideoReferenceInfoController
import org.mbari.annosaurus.domain.{
    CachedVideoReferenceInfo,
    CachedVideoReferenceInfoCreateSC,
    CachedVideoReferenceInfoSC,
    CachedVideoReferenceInfoUpdateSC,
    ErrorMsg
}
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.etc.tapir.TapirCodecs.given
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import sttp.model.StatusCode

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class CachedVideoReferenceInfoEndpoints(controller: CachedVideoReferenceInfoController)(using
    ec: ExecutionContext,
    jwtService: JwtService
) extends Endpoints {

    private val tag = "Video Information"
    private val base = "videoreferences"

    val findAll: Endpoint[Unit, Paging, ErrorMsg, Seq[CachedVideoReferenceInfoSC], Any] =
        openEndpoint
            .get
            .in(base)
            .in(paging)
            .out(jsonBody[Seq[CachedVideoReferenceInfoSC]])
            .name("findAll")
            .description("Find all video references")
            .tag(tag)

    val findAllImpl: ServerEndpoint[Any, Future] =
        findAll
            .serverLogic { page =>
                handleErrors(
                    controller.findAll(page.limit, page.offset).map(_.map(_.toSnakeCase).toSeq)
                )
            }

    // GET /videoreferences
    val findAllVideoReferenceUuids: Endpoint[Unit, Unit, ErrorMsg, Seq[UUID], Any] =
        openEndpoint
            .get
            .in(base / "videoreferences")
            .out(jsonBody[Seq[UUID]])
            .name("findAllVideoReferenceUuids")
            .description("Find all video reference UUIDs")
            .tag(tag)

    val findAllVideoReferenceUuidsImpl: ServerEndpoint[Any, Future] =
        findAllVideoReferenceUuids
            .serverLogic { _ =>
                handleErrors(controller.findAllVideoReferenceUUIDs().map(_.toSeq))
            }

    // GET /:uuid
    val findByUuid: Endpoint[Unit, UUID, ErrorMsg, CachedVideoReferenceInfoSC, Any] =
        openEndpoint
            .get
            .in(base / path[UUID]("uuid"))
            .out(jsonBody[CachedVideoReferenceInfoSC])
            .name("findByUuid")
            .description("Find a video reference by UUID")
            .tag(tag)

    val findByUuidImpl: ServerEndpoint[Any, Future] =
        findByUuid
            .serverLogic { uuid =>
                handleOption(controller.findByUUID(uuid).map(_.map(_.toSnakeCase)))
            }

    // GET /videoreference/:uuid
    val findByVideoReferenceUuid: Endpoint[Unit, UUID, ErrorMsg, CachedVideoReferenceInfoSC, Any] =
        openEndpoint
            .get
            .in(base / "videoreference" / path[UUID]("uuid"))
            .out(jsonBody[CachedVideoReferenceInfoSC])
            .name("findByVideoReferenceUuid")
            .description("Find a video reference by a video reference UUID")
            .tag(tag)

    val findByVideoReferenceUuidImpl: ServerEndpoint[Any, Future] =
        findByVideoReferenceUuid
            .serverLogic { uuid =>
                handleOption(controller.findByVideoReferenceUUID(uuid).map(_.map(_.toSnakeCase)))
            }

    // GET /missionids
    val findAllMissionIds: Endpoint[Unit, Unit, ErrorMsg, Seq[String], Any] =
        openEndpoint
            .get
            .in(base / "missionids")
            .out(jsonBody[Seq[String]])
            .name("findAllMissionIds")
            .description("Find all mission IDs")
            .tag(tag)

    val findAllMissionIdsImpl: ServerEndpoint[Any, Future] =
        findAllMissionIds
            .serverLogic { _ =>
                handleErrors(controller.findAllMissionIds().map(_.toSeq))
            }

    // GET /missionid/:missionid
    val findByMissionId: Endpoint[Unit, String, ErrorMsg, Seq[CachedVideoReferenceInfoSC], Any] =
        openEndpoint
            .get
            .in(base / "missionid" / path[String]("missionid"))
            .out(jsonBody[Seq[CachedVideoReferenceInfoSC]])
            .name("findByMissionId")
            .description("Find video references by mission ID")
            .tag(tag)

    val findByMissionIdImpl: ServerEndpoint[Any, Future] =
        findByMissionId
            .serverLogic { missionId =>
                handleErrors(controller.findByMissionId(missionId).map(_.map(_.toSnakeCase).toSeq))
            }

    // GET /missioncontacts
    val findAllMissionContacts: Endpoint[Unit, Unit, ErrorMsg, Seq[String], Any] =
        openEndpoint
            .get
            .in(base / "missioncontacts")
            .out(jsonBody[Seq[String]])
            .name("findAllMissionContacts")
            .description("Find all mission contacts")
            .tag(tag)

    val findAllMissionContactsImpl: ServerEndpoint[Any, Future] =
        findAllMissionContacts
            .serverLogic { _ =>
                handleErrors(controller.findAllMissionContacts().map(_.toSeq))
            }

    // GET /missioncontact/:missioncontact
    val findByMissionContact
        : Endpoint[Unit, String, ErrorMsg, Seq[CachedVideoReferenceInfoSC], Any] =
        openEndpoint
            .get
            .in(base / "missioncontact" / path[String]("missioncontact"))
            .out(jsonBody[Seq[CachedVideoReferenceInfoSC]])
            .name("findByMissionContact")
            .description("Find video references by mission contact")
            .tag(tag)

    val findByMissionContactImpl: ServerEndpoint[Any, Future] =
        findByMissionContact
            .serverLogic { missionContact =>
                handleErrors(
                    controller.findByMissionContact(missionContact).map(_.map(_.toSnakeCase).toSeq)
                )
            }

    // POST / json or form body
    val createOneVideoReferenceInfo: Endpoint[Option[
        String
    ], CachedVideoReferenceInfoCreateSC, ErrorMsg, CachedVideoReferenceInfoSC, Any] =
        secureEndpoint
            .post
            .in(base)
            .in(
                oneOfBody(
                    jsonBody[CachedVideoReferenceInfoCreateSC],
                    formBody[CachedVideoReferenceInfoCreateSC]
                )
            )
            .out(jsonBody[CachedVideoReferenceInfoSC])
            .name("createOneVideoReferenceInfo")
            .description("Create a video reference")
            .tag(tag)

    val createOneVideoReferenceInfoImpl: ServerEndpoint[Any, Future] =
        createOneVideoReferenceInfo
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { _ => dto =>
                handleErrors(
                    controller
                        .create(
                            dto.video_reference_uuid,
                            dto.platform_name,
                            dto.mission_id,
                            dto.mission_contact
                        )
                        .map(_.toSnakeCase)
                )
            }

    // PUT /:uuid json or form body
    val updateOneVideoReferenceInfo: Endpoint[Option[
        String
    ], (UUID, CachedVideoReferenceInfoUpdateSC), ErrorMsg, CachedVideoReferenceInfoSC, Any] =
        secureEndpoint
            .put
            .in(base / path[UUID]("uuid"))
            .in(
                oneOfBody(
                    jsonBody[CachedVideoReferenceInfoUpdateSC],
                    formBody[CachedVideoReferenceInfoUpdateSC]
                )
            )
            .out(jsonBody[CachedVideoReferenceInfoSC])
            .name("updateOneVideoReferenceInfo")
            .description("Update a video reference")
            .tag(tag)

    val updateOneVideoReferenceInfoImpl: ServerEndpoint[Any, Future] =
        updateOneVideoReferenceInfo
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { _ => (uuid, dto) =>
                handleOption(
                    controller
                        .update(
                            uuid,
                            dto.video_reference_uuid,
                            dto.platform_name,
                            dto.mission_id,
                            dto.mission_contact
                        )
                        .map(_.map(_.toSnakeCase))
                )
            }

    // DELETE /:uuid
    val deleteOneVideoReferenceInfo: Endpoint[Option[String], UUID, ErrorMsg, Unit, Any] =
        secureEndpoint
            .delete
            .in(base / path[UUID]("uuid"))
            .out(statusCode(StatusCode.NoContent).and(emptyOutput))
            .name("deleteOneVideoReferenceInfo")
            .description("Delete a video reference")
            .tag(tag)

    val deleteOneVideoReferenceInfoImpl: ServerEndpoint[Any, Future] =
        deleteOneVideoReferenceInfo
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { _ => uuid =>
                handleErrors(
                    controller
                        .delete(uuid)
                        .map(b => if (b) StatusCode.NoContent else StatusCode.NotFound)
                )
            }

    override def all: List[Endpoint[_, _, _, _, _]] = List(
        findAll,
        findAllVideoReferenceUuids,
        findByUuid,
        findByVideoReferenceUuid,
        findAllMissionIds,
        findByMissionId,
        findAllMissionContacts,
        findByMissionContact,
        createOneVideoReferenceInfo,
        updateOneVideoReferenceInfo,
        deleteOneVideoReferenceInfo
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        findAllImpl,
        findAllVideoReferenceUuidsImpl,
        findByUuidImpl,
        findByVideoReferenceUuidImpl,
        findAllMissionIdsImpl,
        findByMissionIdImpl,
        findAllMissionContactsImpl,
        findByMissionContactImpl,
        createOneVideoReferenceInfoImpl,
        updateOneVideoReferenceInfoImpl,
        deleteOneVideoReferenceInfoImpl
    )
}
