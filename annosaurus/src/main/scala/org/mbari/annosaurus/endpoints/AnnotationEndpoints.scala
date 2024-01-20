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

import org.mbari.annosaurus.controllers.AnnotationController
import org.mbari.annosaurus.domain.{
    Annotation,
    AnnotationCreateSC,
    AnnotationSC,
    ConcurrentRequest,
    ErrorMsg,
    MultiRequest
}
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.etc.tapir.TapirCodecs.given
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class AnnotationEndpoints(controller: AnnotationController)(using
    ec: ExecutionContext,
    jwtService: JwtService
) extends Endpoints {

//    GET /: uuid
    val findAnnotationByUuid: Endpoint[Unit, UUID, ErrorMsg, AnnotationSC, Any] =
        openEndpoint
            .get
            .in("v1" / "annotations" / path[UUID]("uuid"))
//            .in(oneOfBody(jsonBody[AnnotationSC], formBody[AnnotationSC]))
            .out(jsonBody[AnnotationSC])
            .name("findAnnotationByUuid")
            .description("Find an annotation by its UUID")
            .tag("Annotation")

    val findAnnotationByUuidImpl: ServerEndpoint[Any, Future] =
        findAnnotationByUuid
            .serverLogic { uuid =>
                handleOption(controller.findByUUID(uuid).map(x => x.map(_.toSnakeCase)))
            }

    val findAnnotationByImageReferenceUuid: Endpoint[Unit, UUID, ErrorMsg, Seq[AnnotationSC], Any] =
        openEndpoint
            .get
            .in("v1" / "annotations" / "imagereference" / path[UUID]("uuid"))
            .out(jsonBody[Seq[AnnotationSC]])
            .name("findAnnotationByImageReferenceUuid")
            .description("Find an annotation by its image reference UUID")
            .tag("Annotation")

    val findAnnotationByImageReferenceUuidImpl: ServerEndpoint[Any, Future] =
        findAnnotationByImageReferenceUuid
            .serverLogic { uuid =>
                handleErrors(
                    controller.findByImageReferenceUUID(uuid).map(xs => xs.map(_.toSnakeCase).toSeq)
                )
            }

    val findAnnotationsByVideoReferenceUuid
        : Endpoint[Unit, (UUID, Paging), ErrorMsg, Seq[AnnotationSC], Any] =
        openEndpoint
            .get
            .in("v1" / "annotations" / "videoreference" / path[UUID]("uuid"))
            .in(paging)
            .out(jsonBody[Seq[AnnotationSC]])
            .name("findAnnotationsByVideoReferenceUuid")
            .description("Find annotations by its video reference UUID")
            .tag("Annotation")

    val findAnnotationsByVideoReferenceUuidImpl: ServerEndpoint[Any, Future] =
        findAnnotationsByVideoReferenceUuid
            .serverLogic { (uuid, page) =>
                handleErrors(
                    controller
                        .findByVideoReferenceUuid(uuid, page.limit, page.offset)
                        .map(xs => xs.map(_.toSnakeCase))
                )
            }

    //    GET / videoreference / chunked /: uuid
    // TOOD do we need this endpoint anymore?

    //    POST /
    val createAnnotation
        : Endpoint[Option[String], AnnotationCreateSC, ErrorMsg, AnnotationSC, Any] =
        secureEndpoint
            .post
            .in("v1" / "annotations")
            .in(oneOfBody(jsonBody[AnnotationCreateSC], formBody[AnnotationCreateSC]))
            .out(jsonBody[AnnotationSC])
            .name("createAnnotation")
            .description("Create a new annotation")
            .tag("Annotation")

    val createAnnotationImpl: ServerEndpoint[Any, Future] =
        createAnnotation
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { _ => annotationCreate =>
                val annotation = annotationCreate.toCamelCase.toAnnotation
                handleOption(
                    controller
                        .bulkCreate(Seq(annotation))
                        .map(xs => xs.headOption.map(_.toSnakeCase))
                )
            }

//        POST / bulk
    val bulkCreateAnnotations
        : Endpoint[Option[String], Seq[Annotation], ErrorMsg, Seq[AnnotationSC], Any] =
        secureEndpoint
            .post
            .in("v1" / "annotations" / "bulk")
            .in(jsonBody[Seq[Annotation]])
            .out(jsonBody[Seq[AnnotationSC]])
            .name("bulkCreateAnnotations")
            .description("Create a new annotation")
            .tag("Annotation")

    val bulkCreateAnnotationsImpl: ServerEndpoint[Any, Future] =
        bulkCreateAnnotations
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { _ => annotations =>
                handleErrors(controller.bulkCreate(annotations).map(xs => xs.map(_.toSnakeCase)))
            }
//    POST / concurrent

    // Use fast annotation instead
//    val findConcurrentAnnotations =
//        openEndpoint
//            .post
//            .in("v1" / "annotations" / "concurrent")
//            .in(jsonBody[ConcurrentRequest])
//            .out(jsonBody[Seq[AnnotationSC]])
//            .name("findConcurrentAnnotations")
//            .description("Find concurrent annotations")
//            .tag("Annotation")
//
//    val findConcurrentAnnotationsImpl: ServerEndpoint[Any, Future] =
//        findConcurrentAnnotations
//            .serverLogic { concurrentRequest =>
//                handleErrors(controller.(concurrentRequest).map(xs => xs.map(_.toSnakeCase)))
//            }

//    POST / concurrent / count
    val countByConcurrentRequest =
        openEndpoint
            .post
            .in("v1" / "annotations" / "concurrent" / "count")
            .in(jsonBody[ConcurrentRequest])
            .out(jsonBody[Long])
            .name("countConcurrentAnnotations")
            .description("Count concurrent annotations")
            .tag("Annotation")

    val countByConcurrentRequestImpl: ServerEndpoint[Any, Future] =
        countByConcurrentRequest
            .serverLogic { concurrentRequest =>
                handleErrors(controller.countByConcurrentRequest(concurrentRequest))
            }

//    POST / multi
// Use fast annotation instead
//    val findMultiAnnotations =
//        openEndpoint
//            .post
//            .in("v1" / "annotations" / "multi")
//            .in(jsonBody[Seq[AnnotationSC]])
//            .out(jsonBody[Seq[AnnotationSC]])
//            .name("findMultiAnnotations")
//            .description("Find multiple annotations")
//            .tag("Annotation")
//
//    val findMultiAnnotationsImpl: ServerEndpoint[Any, Future] =
//        findMultiAnnotations
//            .serverLogic { annotations =>
//                handleErrors(controller.findBy(annotations).map(xs => xs.map(_.toSnakeCase)))
//            }

//    POST / multi / count
    val countByMultiRequest: Endpoint[Unit, MultiRequest, ErrorMsg, Long, Any] =
        openEndpoint
            .post
            .in("v1" / "annotations" / "multi" / "count")
            .in(jsonBody[MultiRequest])
            .out(jsonBody[Long])
            .name("countMultiAnnotations")
            .description("Count multiple annotations. JSON body can be snake_case or camelCase")
            .tag("Annotation")

    val countByMultiRequestImpl: ServerEndpoint[Any, Future] =
        countByMultiRequest
            .serverLogic { multiRequest =>
                handleErrors(controller.countByMultiRequest(multiRequest))
            }

//    PUT /: uuid
    val updateAnnotation
        : Endpoint[Option[String], (UUID, AnnotationCreateSC), ErrorMsg, AnnotationSC, Any] =
        secureEndpoint
            .put
            .in("v1" / "annotations" / path[UUID]("uuid"))
            .in(oneOfBody(jsonBody[AnnotationCreateSC], formBody[AnnotationCreateSC]))
            .out(jsonBody[AnnotationSC])
            .name("updateAnnotation")
            .description("Update an annotation")
            .tag("Annotation")

    val updateAnnotationImpl: ServerEndpoint[Any, Future] =
        updateAnnotation
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { _ => (uuid, annotationCreate) =>
                val annotation = annotationCreate.toCamelCase.toAnnotation
                handleOption(controller.update(uuid, annotation).map(x => x.map(_.toSnakeCase)))
            }
//    PUT / bulk
    val bulkUpdateAnnotations
        : Endpoint[Option[String], Seq[Annotation], ErrorMsg, Seq[AnnotationSC], Any] =
        secureEndpoint
            .put
            .in("v1" / "annotations" / "bulk")
            .in(jsonBody[Seq[Annotation]])
            .out(jsonBody[Seq[AnnotationSC]])
            .name("bulkUpdateAnnotations")
            .description("Update multiple annotations")
            .tag("Annotation")

    val bulkUpdateAnnotationsImpl: ServerEndpoint[Any, Future] =
        bulkUpdateAnnotations
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { _ => annotations =>
                handleErrors(
                    controller.bulkUpdate(annotations).map(xs => xs.map(_.toSnakeCase).toSeq)
                )
            }
//    PUT / tapetime
// use fast annotation instead

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        findAnnotationByUuid,
        findAnnotationByImageReferenceUuid,
        findAnnotationsByVideoReferenceUuid,
        createAnnotation,
        bulkCreateAnnotations,
        countByConcurrentRequest,
        countByMultiRequest,
        updateAnnotation,
        bulkUpdateAnnotations
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        findAnnotationByUuidImpl,
        findAnnotationByImageReferenceUuidImpl,
        findAnnotationsByVideoReferenceUuidImpl,
        createAnnotationImpl,
        bulkCreateAnnotationsImpl,
        countByConcurrentRequestImpl,
        countByMultiRequestImpl,
        updateAnnotationImpl,
        bulkUpdateAnnotationsImpl
    )
}
