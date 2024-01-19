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
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.vcr4j.time.Timecode

import java.net.URL
import java.time.Duration
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class ImageEndpoints(controller: ImageController)(using
                                                  ec: ExecutionContext,
                                                  jwtService: JwtService
) extends Endpoints {

    // GET /:uuid
    val findOneImage =
        openEndpoint
            .get
            .in("v1" / "image" / path[UUID]("uuid"))
            .out(jsonBody[ImageSC])
            .description("Find an image by its UUID")
            .name("findOneImage")
            .tag("images")

    val findOneImageImpl: ServerEndpoint[Any, Future] = findOneImage
        .serverLogic { uuid =>
            handleOption(controller.findByUUID(uuid).map(_.map(_.toSnakeCase)))
        }

    // GET /videoreference/:uuid
    val findByVideoReferenceUUID =
        openEndpoint
            .get
            .in("v1" / "image" / "videoreference" / path[UUID]("uuid"))
            .out(jsonBody[Seq[ImageSC]])
            .description("Find images by video reference UUID")
            .name("findByVideoReferenceUUID")
            .tag("images")

    val findByVideoReferenceUUIDImpl: ServerEndpoint[Any, Future] = findByVideoReferenceUUID
        .serverLogic { uuid =>
            handleErrors(controller.findByVideoReferenceUUID(uuid).map(_.map(_.toSnakeCase)))
        }

    // GET /name/:name
    val findByImageName =
        openEndpoint
            .get
            .in("v1" / "image" / "name" / path[String]("name"))
            .out(jsonBody[Seq[ImageSC]])
            .description("Find images by the image file's name")
            .name("findByImageName")
            .tag("images")

    val findByImageNameImpl: ServerEndpoint[Any, Future] = findByImageName
        .serverLogic { name =>
            handleErrors(controller.findByImageName(name).map(_.map(_.toSnakeCase)))
        }

    // GET /url/:url
    val findByImageUrl =
        openEndpoint
            .get
            .in("v1" / "image" / "url" / path[URL]("url"))
            .out(jsonBody[ImageSC])
            .description("Find images by the image file's URL")
            .name("findByImageUrl")
            .tag("images")

    val findByImageUrlImpl: ServerEndpoint[Any, Future] = findByImageUrl
        .serverLogic { url =>
            handleOption(controller.findByURL(url).map(_.map(_.toSnakeCase)))
        }

    // POST / with video_reference_uuid, timecode, elapsed_time_mills and recorded_timestamp
    val createOneImage =
        secureEndpoint
            .post
            .in("v1" / "image")
            .in(oneOfBody(jsonBody[ImageCreateSC], formBody[ImageCreateSC]))
            .out(jsonBody[ImageSC])
            .description("Create a new image")
            .name("createOneImage")
            .tag("images")

    val createOneImageImpl: ServerEndpoint[Any, Future] = createOneImage
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ =>  dto =>
            val timecode = dto.timecode.map(Timecode(_))
            val elapsedTime = dto.elapsed_time_millis.map(Duration.ofMillis)
            handleErrors(controller.create(
                dto.video_reference_uuid,
                dto.url,
                timecode,
                elapsedTime,
                dto.recorded_timestamp,
                dto.format,
                dto.width_pixels,
                dto.height_pixels,
                dto.description
            ).map(_.toSnakeCase))
        }

    // PUT /:uuid
    val updateOneImage: Endpoint[Option[String], (UUID, ImageUpdateSC), ErrorMsg, ImageSC, Any] =
        secureEndpoint
            .put
            .in("v1" / "image" / path[UUID]("uuid"))
            .in(oneOfBody(jsonBody[ImageUpdateSC], formBody[ImageUpdateSC]))
            .out(jsonBody[ImageSC])
            .description("Update an image")
            .name("updateOneImage")
            .tag("images")

    val updateOneImageImpl: ServerEndpoint[Any, Future] = updateOneImage
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ => (uuid, dto) =>
            val timecode = dto.timecode.map(Timecode(_))
            val elapsedTime = dto.elapsed_time_millis.map(Duration.ofMillis)
            handleOption(controller.update(
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
            ).map(_.map(_.toSnakeCase)))
        }


    override def all: List[Endpoint[_, _, _, _, _]] = List(
        findOneImage,
        findByVideoReferenceUUID,
        findByImageName,
        findByImageUrl,
        createOneImage,
        updateOneImage
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        findOneImageImpl,
        findByVideoReferenceUUIDImpl,
        findByImageNameImpl,
        findByImageUrlImpl,
        createOneImageImpl,
        updateOneImageImpl
    )
}
