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

import org.mbari.annosaurus.controllers.ImageReferenceController
import org.mbari.annosaurus.domain.{ErrorMsg, ImageReferenceSC}
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.etc.tapir.TapirCodecs.given
import sttp.model.StatusCode
import sttp.tapir.Endpoint
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import CustomTapirJsonCirce.*

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class ImageReferenceEndpoints(controller: ImageReferenceController)(using
    val executor: ExecutionContext,
    jwtService: JwtService
) extends Endpoints {

    private val base = "imagereferences"
    private val tag  = "Image References"

    val deleteImageByUuid: Endpoint[Option[String], UUID, ErrorMsg, Unit, Any] =
        secureEndpoint
            .delete
            .in(base / path[UUID]("imageReferenceUuid"))
            .out(statusCode(StatusCode.NoContent).and(emptyOutput))
            .name("deleteImageByUuid")
            .description("Delete an image reference by its UUID")
            .tag(tag)

    val deleteImageByUuidImpl: ServerEndpoint[Any, Future] =
        deleteImageByUuid
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic(_ =>
                uuid =>
                    handleErrors(
                        controller.delete(uuid).map(b => if b then Right(()) else Left(()))
                    )
            )

    val findImageByUuid: Endpoint[Unit, UUID, ErrorMsg, ImageReferenceSC, Any] =
        openEndpoint
            .get
            .in(base / path[UUID]("imageReferenceUuid"))
            .out(jsonBody[ImageReferenceSC])
            .tag(tag)

    val findImageByUuidImpl: ServerEndpoint[Any, Future] = findImageByUuid
        .serverLogic { uuid =>
            handleOption(
                controller
                    .findByUUID(uuid)
                    .map(x => x.map(_.toSnakeCase))
            )
        }

    val updateImageReferenceByUuid
        : Endpoint[Option[String], (UUID, ImageReferenceSC), ErrorMsg, ImageReferenceSC, Any] =
        secureEndpoint
            .put
            .in(base / path[UUID]("imageReferenceUuid"))
            .in(oneOfBody(jsonBody[ImageReferenceSC], formBody[ImageReferenceSC]))
            .out(jsonBody[ImageReferenceSC])
            .name("updateImageReferenceByUuid")
            .description("Update an image reference by its UUID and a json or form body")
            .tag(tag)

    val updateImageReferenceByUuidImpl: ServerEndpoint[Any, Future] = updateImageReferenceByUuid
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ => (uuid, imageReference) =>
            handleOption(
                controller
                    .update(
                        uuid,
                        Some(imageReference.url),
                        imageReference.description,
                        imageReference.height_pixels,
                        imageReference.width_pixels,
                        imageReference.format,
                        imageReference.imaged_moment_uuid
                    )
                    .map(x => x.map(_.toSnakeCase))
            )
        }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] =
        List(deleteImageByUuid, findImageByUuid, updateImageReferenceByUuid)

    override def allImpl: List[ServerEndpoint[Any, Future]] =
        List(deleteImageByUuidImpl, findImageByUuidImpl, updateImageReferenceByUuidImpl)
}
