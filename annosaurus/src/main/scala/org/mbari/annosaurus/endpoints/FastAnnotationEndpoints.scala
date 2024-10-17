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

import org.mbari.annosaurus.domain.{
    AnnotationSC,
    ConcurrentRequestSC,
    Count,
    DeleteCount,
    DeleteCountSC,
    ErrorMsg,
    GeographicRangeSC,
    ImageSC,
    MultiRequestSC,
    QueryConstraints,
    QueryConstraintsResponseSC,
    QueryConstraintsSC
}
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.repository.jdbc.JdbcRepository
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import CustomTapirJsonCirce.*
import org.mbari.annosaurus.etc.circe.CirceCodecs.given

class FastAnnotationEndpoints(jdbcRepository: JdbcRepository)(using
    ec: ExecutionContext,
    jwtService: JwtService
) extends Endpoints:

    private val base = "fast"
    private val tag  = "Fast Annotation Queries"

    // GET / limit offset
    val findAllAnnotations: Endpoint[Unit, (Paging, Option[Boolean]), ErrorMsg, Seq[AnnotationSC], Any] =
        openEndpoint
            .get
            .in(base)
            .in(paging)
            .in(query[Option[Boolean]]("data"))
            .out(jsonBody[Seq[AnnotationSC]])
            .name("findAllAnnotations")
            .description("Find all annotations")
            .tag(tag)

    val findAllAnnotationsImpl: ServerEndpoint[Any, Future] = findAllAnnotations
        .serverLogic { (paging, data) =>
            handleErrors(
                Future(
                    jdbcRepository
                        .findAll(paging.limit, paging.offset, data.getOrElse(false))
                        .map(_.toSnakeCase)
                )
            )
        }

    // POST / queryconstranints json
    val findAnnotationsByQueryConstraints =
        openEndpoint
            .post
            .in(base)
            .in(jsonBody[QueryConstraints])
            .out(jsonBody[QueryConstraintsResponseSC[Seq[AnnotationSC]]])
            .name("findAnnotationsByQueryConstraints")
            .description(
                "Find annotations by query constraints. Constraints can be snake_case or camelCase. The response will be in snake_case."
            )
            .tag(tag)

    val findAnnotationsByQueryConstraintsImpl: ServerEndpoint[Any, Future] =
        findAnnotationsByQueryConstraints
            .serverLogic { queryConstraints =>
                handleErrors(
                    Future {
                        val annos = jdbcRepository
                            .findByQueryConstraint(queryConstraints)
                            .map(_.toSnakeCase)
                        QueryConstraintsResponseSC(queryConstraints.toSnakeCase, annos)
                    }
                )
            }

    // POST /georange queryconstraints json
    val findGeoRangeByQueryConstraints: Endpoint[Unit, QueryConstraints, ErrorMsg, QueryConstraintsResponseSC[
        GeographicRangeSC
    ], Any] =
        openEndpoint
            .post
            .in(base / "georange")
            .in(jsonBody[QueryConstraints])
            .out(jsonBody[QueryConstraintsResponseSC[GeographicRangeSC]])
            .description(
                "Find annotations by query constraints. Constraints can be snake_case or camelCase. The response will be in snake_case."
            )
            .tag(tag)

    val findGeoRangeByQueryConstraintsImpl: ServerEndpoint[Any, Future] =
        findGeoRangeByQueryConstraints
            .serverLogic { queryConstraints =>
                handleOption(
                    Future {
                        jdbcRepository
                            .findGeographicRangeByQueryConstraint(queryConstraints)
                            .map(_.toSnakeCase)
                            .map(r => QueryConstraintsResponseSC(queryConstraints.toSnakeCase, r))
                    }
                )
            }

    // POST /count queryconstraints json
    val countAnnotationsByQueryConstraints
        : Endpoint[Unit, QueryConstraints, ErrorMsg, QueryConstraintsResponseSC[Count], Any] =
        openEndpoint
            .post
            .in(base / "count")
            .in(jsonBody[QueryConstraints])
            .out(jsonBody[QueryConstraintsResponseSC[Count]])
            .name("countAnnotationsByQueryConstraints")
            .description(
                "Count annotations by query constraints. Constraints can be snake_case or camelCase. The response will be in snake_case."
            )
            .tag(tag)

    val countAnnotationsByQueryConstraintsImpl: ServerEndpoint[Any, Future] =
        countAnnotationsByQueryConstraints
            .serverLogic { queryConstraints =>
                handleErrors(
                    Future {
                        val count =
                            jdbcRepository.countByQueryConstraint(queryConstraints)
                        QueryConstraintsResponseSC(queryConstraints.toSnakeCase, Count(count))
                    }
                )
            }

    // GET /count
    val countAllAnnotations: Endpoint[Unit, Unit, ErrorMsg, Count, Any] =
        openEndpoint
            .get
            .in(base / "count")
            .out(jsonBody[Count])
            .name("countAllAnnotations")
            .description("Count all annotations")
            .tag(tag)

    val countAllAnnotationsImpl: ServerEndpoint[Any, Future] = countAllAnnotations
        .serverLogic { _ =>
            handleErrors(
                Future {
                    val count = jdbcRepository.countAll()
                    Count(count)
                }
            )
        }

    // GET /videoreference/:uuid
    val findAnnotationsByVideoReferenceUuid = openEndpoint
        .get
        .in(base / "videoreference" / path[UUID]("videoReferenceUuid"))
        .in(paging)
        .in(query[Option[Boolean]]("data"))
        .out(jsonBody[Seq[AnnotationSC]])
        .name("findAnnotationsByVideoReferenceUuid")
        .description("Find annotations by video reference UUID")
        .tag(tag)

    val findAnnotationsByVideoReferenceUuidImpl: ServerEndpoint[Any, Future] =
        findAnnotationsByVideoReferenceUuid
            .serverLogic { (uuid, paging, data) =>
                handleErrors(
                    Future(
                        jdbcRepository
                            .findByVideoReferenceUuid(
                                uuid,
                                paging.limit,
                                paging.offset,
                                data.getOrElse(false)
                            )
                            .map(_.toSnakeCase)
                    )
                )
            }

    // GET /images/videoreference/:uuid
    val findImagesByVideoReferenceUuid: Endpoint[Unit, (UUID, Paging), ErrorMsg, Seq[ImageSC], Any] = openEndpoint
        .get
        .in(base / "images" / "videoreference" / path[UUID]("videoReferenceUuid"))
        .in(paging)
        .out(jsonBody[Seq[ImageSC]])
        .name("findImagesByVideoReferenceUuid")
        .description("Find annotations with images by video reference UUID")
        .tag(tag)

    val findImagesByVideoReferenceUuidImpl: ServerEndpoint[Any, Future] =
        findImagesByVideoReferenceUuid
            .serverLogic { (uuid, paging) =>
                handleErrors(
                    Future(
                        jdbcRepository
                            .findImagesByVideoReferenceUuid(uuid, paging.limit, paging.offset)
                            .map(_.toSnakeCase)
                    )
                )
            }

    // GET /images/count/videoreference/:uuid/
    val countImagesByVideoReferenceUuid: Endpoint[Unit, UUID, ErrorMsg, Count, Any] = openEndpoint
        .get
        .in(base / "images" / "count" / "videoreference" / path[UUID]("videoReferenceUuid"))
        .out(jsonBody[Count])
        .name("countImagesByVideoReferenceUuid")
        .description("Count annotations with images by video reference UUID")
        .tag(tag)

    val countImagesByVideoReferenceUuidImpl: ServerEndpoint[Any, Future] =
        countImagesByVideoReferenceUuid
            .serverLogic { uuid =>
                handleErrors(
                    Future {
                        val count = jdbcRepository.countImagesByVideoReferenceUuid(uuid)
                        Count(count)
                    }
                )
            }

    // GET /concept/:concept
    val findAnnotationsByConcept: Endpoint[Unit, (String, Paging, Option[Boolean]), ErrorMsg, Seq[AnnotationSC], Any] =
        openEndpoint
            .get
            .in(base / "concept" / path[String]("concept"))
            .in(paging)
            .in(query[Option[Boolean]]("data"))
            .out(jsonBody[Seq[AnnotationSC]])
            .name("findAnnotationsByConcept")
            .description("Find annotations by concept")
            .tag(tag)

    val findAnnotationsByConceptImpl: ServerEndpoint[Any, Future] = findAnnotationsByConcept
        .serverLogic { (concept, paging, data) =>
            handleErrors(
                Future(
                    jdbcRepository
                        .findByConcept(concept, paging.limit, paging.offset, data.getOrElse(false))
                        .map(_.toSnakeCase)
                )
            )
        }

    // GET /concept/images/:concept/
    val findAnnotationsWithImagesByConcept
        : Endpoint[Unit, (String, Paging, Option[Boolean]), ErrorMsg, Seq[AnnotationSC], Any] =
        openEndpoint
            .get
            .in(base / "concept" / "images" / path[String]("concept"))
            .in(paging)
            .in(query[Option[Boolean]]("data"))
            .out(jsonBody[Seq[AnnotationSC]])
            .name("findAnnotationsWithImagesByConcept")
            .description("Find annotations with images by concept")
            .tag(tag)

    val findAnnotationsWithImagesByConceptImpl: ServerEndpoint[Any, Future] =
        findAnnotationsWithImagesByConcept
            .serverLogic { (concept, paging, data) =>
                handleErrors(
                    Future(
                        jdbcRepository
                            .findByConceptWithImages(
                                concept,
                                paging.limit,
                                paging.offset,
                                data.getOrElse(false)
                            )
                            .map(_.toSnakeCase)
                    )
                )
            }

    // GET /toconcept/images/:toconcept/
    val findAnnotationsWithImagesByToConcept
        : Endpoint[Unit, (String, Paging, Option[Boolean]), ErrorMsg, Seq[AnnotationSC], Any] =
        openEndpoint
            .get
            .in(base / "toconcept" / "images" / path[String]("toconcept"))
            .in(paging)
            .in(query[Option[Boolean]]("data"))
            .out(jsonBody[Seq[AnnotationSC]])
            .name("findAnnotationsWithImagesByToConcept")
            .description("Find annotations with images by to concept")
            .tag(tag)

    val findAnnotationsWithImagesByToConceptImpl: ServerEndpoint[Any, Future] =
        findAnnotationsWithImagesByToConcept
            .serverLogic { (toConcept, paging, data) =>
                handleErrors(
                    Future(
                        jdbcRepository
                            .findByToConceptWithImages(
                                toConcept,
                                paging.limit,
                                paging.offset,
                                data.getOrElse(false)
                            )
                            .map(_.toSnakeCase)
                    )
                )
            }

    // GET /imagedmoments/concept/images/:concept
    val findImageMomentUuidsByConcept: Endpoint[Unit, (String, Paging), ErrorMsg, Seq[UUID], Any] =
        openEndpoint
            .get
            .in(base / "imagedmoments" / "concept" / "images" / path[String]("concept"))
            .in(paging)
            .out(jsonBody[Seq[UUID]])
            .name("findImageMomentUuidsByConcept")
            .description(
                "Find the UUIDS of image moments by concept. Only include image moments with images. Sorted by recorded timestamp."
            )
            .tag(tag)

    val findImagedMomentUuidsByConceptImpl: ServerEndpoint[Any, Future] =
        findImageMomentUuidsByConcept
            .serverLogic { (concept, paging) =>
                handleErrors(
                    Future(
                        jdbcRepository.findImagedMomentUuidsByConceptWithImages(
                            concept,
                            paging.limit,
                            paging.offset
                        )
                    )
                )
            }

    // GET /imagedmoments/toconcept/images/:toconcept
    val findImagedMomentUuidsByToConcept: Endpoint[Unit, (String, Paging), ErrorMsg, Seq[UUID], Any] =
        openEndpoint
            .get
            .in(
                base / "imagedmoments" / "toconcept" / "images" / path[String]("toconcept")
            )
            .in(paging)
            .out(jsonBody[Seq[UUID]])
            .name("findImagedMomentUuidsByToConcept")
            .description(
                "Find image moment UUIDs by to concept. Only include image moments with images. Sorted by recorded timestamp."
            )
            .tag(tag)

    val findImagedMomentUuidsByToConceptImpl: ServerEndpoint[Any, Future] =
        findImagedMomentUuidsByToConcept
            .serverLogic { (toConcept, paging) =>
                handleErrors(
                    Future(
                        jdbcRepository.findImagedMomentUuidsByToConceptWithImages(
                            toConcept,
                            paging.limit,
                            paging.offset
                        )
                    )
                )
            }

    // GET /details/:link_name/:link_value
    val findAnnotationsByLinkNameAndLinkValue
        : Endpoint[Unit, (String, String, Option[Boolean]), ErrorMsg, Seq[AnnotationSC], Any] =
        openEndpoint
            .get
            .in(base / "details" / path[String]("link_name") / path[String]("link_value"))
            .out(jsonBody[Seq[AnnotationSC]])
            .in(query[Option[Boolean]]("data"))
            .name("findAnnotationsByLinkNameAndLinkValue")
            .description("Find annotations by link name and link value")
            .tag(tag)

    val findAnnotationsByLinkNameAndLinkValueImpl: ServerEndpoint[Any, Future] =
        findAnnotationsByLinkNameAndLinkValue
            .serverLogic { (linkName, linkValue, data) =>
                handleErrors(
                    Future(
                        jdbcRepository
                            .findByLinkNameAndLinkValue(linkName, linkValue, data.getOrElse(false))
                            .map(_.toSnakeCase)
                    )
                )
            }

    // DELETE /videoreference/:uuid
    val deleteAnnotationsByVideoReferenceUuid: Endpoint[Unit, UUID, ErrorMsg, DeleteCountSC, Any] =
        openEndpoint
            .delete
            .in(base / "videoreference" / path[UUID]("videoReferenceUuid"))
            .out(jsonBody[DeleteCountSC])
            .name("deleteAnnotationsByVideoReferenceUuid")
            .description("Delete annotations by video reference UUID")
            .tag(tag)

    val deleteAnnotationsByVideoReferenceUuidImpl: ServerEndpoint[Any, Future] =
        deleteAnnotationsByVideoReferenceUuid
            .serverLogic { uuid =>
                handleErrors(
                    Future {
                        val count = jdbcRepository.deleteByVideoReferenceUuid(uuid)
                        count.toSnakeCase
                    }
                )
            }

    // POST /concurrent limit offset concurrentrequest json
    val findAnnotationsByConcurrentRequest =
        openEndpoint
            .post
            .in(base / "concurrent")
            .in(paging)
            .in(query[Option[Boolean]]("data"))
            .in(jsonBody[ConcurrentRequestSC])
            .out(jsonBody[Seq[AnnotationSC]])
            .name("findAnnotationsByConcurrentRequest")
            .description("Find annotations by concurrent request")
            .tag(tag)

    val findAnnotationsByConcurrentRequestImpl: ServerEndpoint[Any, Future] =
        findAnnotationsByConcurrentRequest
            .serverLogic { (paging, data, concurrentRequest) =>
                handleErrors(
                    Future(
                        jdbcRepository
                            .findByConcurrentRequest(
                                concurrentRequest.toCamelCase,
                                paging.limit,
                                paging.offset,
                                data.getOrElse(false)
                            )
                            .map(_.toSnakeCase)
                    )
                )
            }

    // POST /multi limit offset multirequest json
    val findAnnotationsByMultiRequest: Endpoint[Unit, (Paging, Option[Boolean], MultiRequestSC), ErrorMsg, Seq[
        AnnotationSC
    ], Any] =
        openEndpoint
            .post
            .in(base / "multi")
            .in(paging)
            .in(query[Option[Boolean]]("data"))
            .in(jsonBody[MultiRequestSC])
            .out(jsonBody[Seq[AnnotationSC]])
            .name("findAnnotationsByMultiRequest")
            .description("Find annotations by multi request")
            .tag(tag)

    val findAnnotationsByMultiRequestImpl: ServerEndpoint[Any, Future] =
        findAnnotationsByMultiRequest
            .serverLogic { (paging, data, multiRequest) =>
                handleErrors(
                    Future(
                        jdbcRepository
                            .findByMultiRequest(
                                multiRequest.toCamelCase,
                                paging.limit,
                                paging.offset,
                                data.getOrElse(false)
                            )
                            .map(_.toSnakeCase)
                    )
                )
            }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        findAnnotationsWithImagesByConcept,
        findAnnotationsByConcept,
        findAnnotationsByConcurrentRequest,
        countAnnotationsByQueryConstraints,
        countAllAnnotations,
        findAnnotationsByLinkNameAndLinkValue,
        findGeoRangeByQueryConstraints,
        findImageMomentUuidsByConcept,
        findImagedMomentUuidsByToConcept,
        countImagesByVideoReferenceUuid,
        findImagesByVideoReferenceUuid,
        findAnnotationsByMultiRequest,
        findAnnotationsWithImagesByToConcept,
        findAnnotationsByVideoReferenceUuid,
        deleteAnnotationsByVideoReferenceUuid,
        findAnnotationsByQueryConstraints,
        findAllAnnotations
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        findAnnotationsWithImagesByConceptImpl,
        findAnnotationsByConceptImpl,
        findAnnotationsByConcurrentRequestImpl,
        countAnnotationsByQueryConstraintsImpl,
        countAllAnnotationsImpl,
        findAnnotationsByLinkNameAndLinkValueImpl,
        findGeoRangeByQueryConstraintsImpl,
        findImagedMomentUuidsByConceptImpl,
        findImagedMomentUuidsByToConceptImpl,
        countImagesByVideoReferenceUuidImpl,
        findImagesByVideoReferenceUuidImpl,
        findAnnotationsByMultiRequestImpl,
        findAnnotationsWithImagesByToConceptImpl,
        findAnnotationsByVideoReferenceUuidImpl,
        deleteAnnotationsByVideoReferenceUuidImpl,
        findAllAnnotationsImpl,
        findAnnotationsByQueryConstraintsImpl
    )
