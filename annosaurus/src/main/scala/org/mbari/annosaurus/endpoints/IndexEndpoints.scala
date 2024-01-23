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

import org.mbari.annosaurus.controllers.IndexController

import scala.concurrent.{ExecutionContext, Future}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import java.util.UUID
import org.mbari.annosaurus.domain.{ErrorMsg, ImagedMoment, Index, IndexSC}
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.repository.jpa.entity.IndexEntity

class IndexEndpoints(controller: IndexController)(using
    val executor: ExecutionContext,
    jwtService: JwtService
) extends Endpoints {
    
    private val base = "index"

    private val toEntity = Index.from(_: IndexEntity, false) // curried function

    val findByVideoReferenceUUID: Endpoint[Unit, (Paging, UUID), ErrorMsg, List[IndexSC], Any] =
        openEndpoint
            .get
            .in(paging)
            .in(base / "videoreference" / path[UUID]("uuid"))
            .out(jsonBody[List[IndexSC]].description("The IndexEntity objects"))
            .tag("Index")

    val findByVideoReferenceUUIDImpl: ServerEndpoint[Any, Future] =
        findByVideoReferenceUUID.serverLogic { (paging, uuid) =>
            val f = controller
                .findByVideoReferenceUUID(uuid, paging.limit, paging.offset)
                .map(xs => xs.map(_.toSnakeCase).toList)
            handleErrors(f)
        }

    val bulkUpdateRecordedTimestamps
        : Endpoint[Option[String], List[Index], ErrorMsg, List[IndexSC], Any] = secureEndpoint
        .put
        .in(base / "tapetime")
        .in(jsonBody[List[Index]].description("The IndexEntity objects"))
        .out(jsonBody[List[IndexSC]].description("The IndexEntity objects"))
        .tag("Index")

    val bulkUpdateRecordedTimestampsImpl: ServerEndpoint[Any, Future] = bulkUpdateRecordedTimestamps
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic(_ =>
            indices =>
                val f = controller
                    .bulkUpdateRecordedTimestamps(indices)
                    .map(xs => xs.map(_.toSnakeCase).toList)
                handleErrors(f)
        )

    override def all: List[Endpoint[?, ?, ?, ?, ?]] =
        List(findByVideoReferenceUUID, bulkUpdateRecordedTimestamps)

    override def allImpl: List[ServerEndpoint[Any, Future]] =
        List(findByVideoReferenceUUIDImpl, bulkUpdateRecordedTimestampsImpl)

}
