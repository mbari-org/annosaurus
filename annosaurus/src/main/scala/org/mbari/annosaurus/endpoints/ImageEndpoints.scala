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

import org.mbari.annosaurus.controllers.ImageController
import org.mbari.annosaurus.domain.{ErrorMsg, ImageCreateSC, ImageSC, ImageUpdateSC}
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.etc.tapir.TapirCodecs.given
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.vcr4j.time.Timecode
import CustomTapirJsonCirce.*

import java.net.URL
import java.time.Duration
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class ImageEndpoints(controller: ImageController)(using
    ec: ExecutionContext,
    jwtService: JwtService
) extends Endpoints {
    
    
    private val base = "images"
    private val tag = "Images"

    // GET /:uuid
    val findOneImage =
        openEndpoint
            .get
            .in(base / path[UUID]("imageReferenceUuid"))
            .out(jsonBody[ImageSC])
            .description("Find an image by its UUID")
            .name("findOneImage")
            .tag(tag)

    val findOneImageImpl: ServerEndpoint[Any, Future] = findOneImage
        .serverLogic { uuid =>
            handleOption(controller.findByUUID(uuid).map(_.map(_.toSnakeCase)))
        }

    // GET /videoreference/:uuid
    val findByVideoReferenceUUID =
        openEndpoint
            .get
            .in(base / "videoreference" / path[UUID]("videoReferenceUuid"))
            .out(jsonBody[Seq[ImageSC]])
            .description("Find images by video reference UUID")
            .name("findByVideoReferenceUUID")
            .tag(tag)

    val findByVideoReferenceUUIDImpl: ServerEndpoint[Any, Future] = findByVideoReferenceUUID
        .serverLogic { uuid =>
            handleErrors(controller.findByVideoReferenceUUID(uuid).map(_.map(_.toSnakeCase)))
        }

    // GET /name/:name
    val findByImageName =
        openEndpoint
            .get
            .in(base / "name" / path[String]("imageFileName"))
            .out(jsonBody[Seq[ImageSC]])
            .description("Find images by the image file's name")
            .name("findByImageName")
            .tag(tag)

    val findByImageNameImpl: ServerEndpoint[Any, Future] = findByImageName
        .serverLogic { name =>
            handleErrors(controller.findByImageName(name).map(_.map(_.toSnakeCase)))
        }

    // GET /url/:url
    val findByImageUrl =
        openEndpoint
            .get
            .in(base / "url" / path[URL]("url"))
            .out(jsonBody[ImageSC])
            .description("Find images by the image file's URL")
            .name("findByImageUrl")
            .tag(tag)

    val findByImageUrlImpl: ServerEndpoint[Any, Future] = findByImageUrl
        .serverLogic { url =>
            handleOption(controller.findByURL(url).map(_.map(_.toSnakeCase)))
        }

    // POST / with video_reference_uuid, timecode, elapsed_time_mills and recorded_timestamp
    val createOneImage =
        secureEndpoint
            .post
            .in(base)
            .in(oneOfBody(jsonBody[ImageCreateSC], formBody[ImageCreateSC]))
            .out(jsonBody[ImageSC])
            .description("Create a new image")
            .name("createOneImage")
            .tag(tag)

    val createOneImageImpl: ServerEndpoint[Any, Future] = createOneImage
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ => dto =>
            val timecode    = dto.timecode.map(Timecode(_))
            val elapsedTime = dto.elapsed_time_millis.map(Duration.ofMillis)
            handleErrors(
                controller
                    .create(
                        dto.video_reference_uuid,
                        dto.url,
                        timecode,
                        elapsedTime,
                        dto.recorded_timestamp,
                        dto.format,
                        dto.width_pixels,
                        dto.height_pixels,
                        dto.description
                    )
                    .map(_.toSnakeCase)
            )
        }

    // PUT /:uuid
    val updateOneImage: Endpoint[Option[String], (UUID, ImageUpdateSC), ErrorMsg, ImageSC, Any] =
        secureEndpoint
            .put
            .in(base / path[UUID]("imageReferenceUuid"))
            .in(oneOfBody(jsonBody[ImageUpdateSC], formBody[ImageUpdateSC]))
            .out(jsonBody[ImageSC])
            .description("Update an image")
            .name("updateOneImage")
            .tag(tag)

    val updateOneImageImpl: ServerEndpoint[Any, Future] = updateOneImage
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ => (uuid, dto) =>
            val timecode    = dto.timecode.map(Timecode(_))
            val elapsedTime = dto.elapsed_time_millis.map(Duration.ofMillis)
            handleOption(
                controller
                    .update(
                        uuid,
                        dto.video_reference_uuid,
                        dto.url,
                        timecode,
                        elapsedTime,
                        dto.recorded_timestamp,
                        dto.format,
                        dto.width_pixels,
                        dto.height_pixels,
                        dto.description
                    )
                    .map(_.map(_.toSnakeCase))
            )
        }

    override def all: List[Endpoint[_, _, _, _, _]] = List(
        findByImageName,
        findByImageUrl,
        findByVideoReferenceUUID,
        findOneImage,
        updateOneImage,
        createOneImage
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        findByImageNameImpl,
        findByImageUrlImpl,
        findByVideoReferenceUUIDImpl,
        findOneImageImpl,
        updateOneImageImpl,
        createOneImageImpl
    )
}
