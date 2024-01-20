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

import org.mbari.annosaurus.controllers.ObservationController
import org.mbari.annosaurus.domain.{ConceptCount, CountForVideoReferenceSC, ErrorMsg, ObservationSC, ObservationUpdateSC, RenameCountSC}
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.etc.tapir.TapirCodecs.given
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import sttp.model.StatusCode

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class ObservationEndpoints(controller: ObservationController)(using
    ec: ExecutionContext,
    jwtService: JwtService
) extends Endpoints {

    // GET /:uuid
    val findObservationByUuid: Endpoint[Unit, UUID, ErrorMsg, ObservationSC, Any] =
        openEndpoint
            .get
            .in("v1" / "observations" / path[UUID]("uuid"))
            .out(jsonBody[ObservationSC])
            .name("findObservationByUuid")
            .description("Find an observation by its UUID")
            .tag("observations")

    val findObservationByUuidImpl: ServerEndpoint[Any, Future] =
        findObservationByUuid
            .serverLogic { uuid =>
                handleOption(controller.findByUUID(uuid).map(_.map(_.toSnakeCase)))
            }

    // GET /videoreference/:uuid
    val findObservationsByVideoReferenceUuid: Endpoint[Unit, (UUID, Paging), ErrorMsg, Seq[ObservationSC], Any] =
        openEndpoint
            .get
            .in("v1" / "observations" / "videoreference" / path[UUID]("videoReferenceUuid"))
            .in(paging)
            .out(jsonBody[Seq[ObservationSC]])
            .name("findObservationsByVideoReferenceUuid")
            .description("Find observations by video reference UUID")
            .tag("observations")

    val findObservationsByVideoReferenceUuidImpl: ServerEndpoint[Any, Future] =
        findObservationsByVideoReferenceUuid
            .serverLogic { (uuid, paging) =>
                handleErrors(
                    controller
                        .findByVideoReferenceUuid(uuid, paging.limit, paging.offset)
                        .map(_.map(_.toSnakeCase).toSeq)
                )
            }

    // GET /activities
    val findActivities: Endpoint[Unit, Unit, ErrorMsg, Seq[String], Any] =
        openEndpoint
            .get
            .in("v1" / "observations" / "activities")
            .out(jsonBody[Seq[String]])
            .name("findActivities")
            .description("List all activities found in the database")
            .tag("observations")

    val findActivitiesImpl: ServerEndpoint[Any, Future] =
        findActivities
            .serverLogic { paging =>
                handleErrors(controller.findAllActivities.map(_.toSeq))
            }

    // GET /association/:uuid
    val findObservationByAssociationUuid: Endpoint[Unit, UUID, ErrorMsg, ObservationSC, Any] =
        openEndpoint
            .get
            .in("v1" / "observations" / "association" / path[UUID]("associationUuid"))
            .out(jsonBody[ObservationSC])
            .name("findObservationByAssociationUuid")
            .description("Find an observation by one of its association UUIDs")
            .tag("observations")

    val findObservationByAssociationUuidImpl: ServerEndpoint[Any, Future] =
        findObservationByAssociationUuid
            .serverLogic { uuid =>
                handleOption(controller.findByAssociationUuid(uuid).map(_.map(_.toSnakeCase)))
            }

    // GET /concepts
    val findAllConcepts: Endpoint[Unit, Unit, ErrorMsg, Seq[String], Any] =
        openEndpoint
            .get
            .in("v1" / "observations" / "concepts")
            .out(jsonBody[Seq[String]])
            .name("findAllConcepts")
            .description("List all concepts found in the database")
            .tag("observations")

    val findAllConceptsImpl: ServerEndpoint[Any, Future] =
        findAllConcepts
            .serverLogic { paging =>
                handleErrors(controller.findAllConcepts.map(_.toSeq))
            }

    // GET /concept/:videoReferenceUuid
    val findConceptsByVideoReferenceUuid: Endpoint[Unit, UUID, ErrorMsg, Seq[String], Any] =
        openEndpoint
            .get
            .in("v1" / "observations" / "concepts" / path[UUID]("videoReferenceUuid"))
            .out(jsonBody[Seq[String]])
            .name("findConceptsByVideoReferenceUuid")
            .description("List all concepts used to annotation in a given video reference UUID")
            .tag("observations")

    val findConceptsByVideoReferenceUuidImpl: ServerEndpoint[Any, Future] =
        findConceptsByVideoReferenceUuid
            .serverLogic { uuid =>
                handleErrors(controller.findAllConceptsByVideoReferenceUuid(uuid).map(_.toSeq))
            }

    // GET /concept/count/:concept
    val countObservationsByConcept: Endpoint[Unit, String, ErrorMsg, ConceptCount, Any] =
        openEndpoint
            .get
            .in("v1" / "observations" / "concept" / "count" / path[String]("concept"))
            .out(jsonBody[ConceptCount])
            .name("countObservationsByConcept")
            .description("Count the number of observations for a given concept")
            .tag("observations")

    val countObservationsByConceptImpl: ServerEndpoint[Any, Future] =
        countObservationsByConcept
            .serverLogic { concept =>
                handleErrors(controller.countByConcept(concept).map(i => ConceptCount(concept, i)))
            }

    // GET concept/images/count/:concept
    val countImagesByConcept: Endpoint[Unit, String, ErrorMsg, ConceptCount, Any] =
        openEndpoint
            .get
            .in("v1" / "observations" / "concept" / "images" / "count" / path[String]("concept"))
            .out(jsonBody[ConceptCount])
            .name("countImagesByConcept")
            .description("Count the number of observations with images for a given concept")
            .tag("observations")

    val countImagesByConceptImpl: ServerEndpoint[Any, Future] =
        countImagesByConcept
            .serverLogic { concept =>
                handleErrors(
                    controller.countByConceptWithImages(concept).map(i => ConceptCount(concept, i))
                )
            }

    // GET /groups
    val findGroups: Endpoint[Unit, Unit, ErrorMsg, Seq[String], Any] =
        openEndpoint
            .get
            .in("v1" / "observations" / "groups")
            .out(jsonBody[Seq[String]])
            .name("findGroups")
            .description("List all groups found in the database")
            .tag("observations")

    val findGroupsImpl: ServerEndpoint[Any, Future] =
        findGroups
            .serverLogic { paging =>
                handleErrors(controller.findAllGroups.map(_.toSeq))
            }

    // GET /videoreferene/count/:videoReferenceUuid optional start and end query params
    val countByVideoReferenceUuid: Endpoint[Unit, (UUID, Option[Instant], Option[Instant]), ErrorMsg, CountForVideoReferenceSC, Any] =
        openEndpoint
            .get
            .in(
                "v1" / "observations" / "videoreference" / "count" / path[UUID](
                    "videoReferenceUuid"
                )
            )
            .in(
                query[Option[Instant]]("start").description(
                    "Start timestamp as compact ISO-8601 string"
                )
            )
            .in(
                query[Option[Instant]]("end").description(
                    "End timestamp as compact ISO-8601 string"
                )
            )
            .out(jsonBody[CountForVideoReferenceSC])
            .name("countByVideoReferenceUuid")
            .description(
                "Count the number of observations for a given video reference UUID. If start and end query params are provided, then the count will be limited to observations between those timestamps"
            )
            .tag("observations")

    val countByVideoReferenceUuidImpl: ServerEndpoint[Any, Future] =
        countByVideoReferenceUuid
            .serverLogic { (uuid, start, end) =>
                val f = if (start.isDefined && end.isDefined) {
                    controller.countByVideoReferenceUuidAndTimestamps(uuid, start.get, end.get)
                }
                else {
                    controller.countByVideoReferenceUuid(uuid)
                }
                handleErrors(f.map(i => CountForVideoReferenceSC(uuid, i)))
            }

    // GET/ counts
    val countAllGroupByVideoReferenceUuid: Endpoint[Unit, Unit, ErrorMsg, Seq[CountForVideoReferenceSC], Any] =
        openEndpoint
            .get
            .in("v1" / "observations" / "counts")
            .out(jsonBody[Seq[CountForVideoReferenceSC]])
            .name("countAllGroupByVideoReferenceUuid")
            .description("Count the number of observations for all video reference UUIDs")
            .tag("observations")

    val countAllGroupByVideoReferenceUuidImpl: ServerEndpoint[Any, Future] =
        countAllGroupByVideoReferenceUuid
            .serverLogic { paging =>
                handleErrors(
                    controller
                        .countAllGroupByVideoReferenceUuid()
                        .map(_.map(CountForVideoReferenceSC.apply.tupled).toSeq)
                )
            }

    // PUT /concept/rename
    val renameConcept: Endpoint[Option[String], (String, String), ErrorMsg, RenameCountSC, Any] =
        secureEndpoint
            .put
            .in("v1" / "observations" / "concept" / "rename")
            .in(query[String]("old").description("The old concept name"))
            .in(query[String]("new").description("The new concept name"))
            .out(jsonBody[RenameCountSC])
            .name("renameConcept")
            .description("Rename a concept in all observations")
            .tag("observations")

    val renameConceptImpl: ServerEndpoint[Any, Future] =
        renameConcept
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { _ => (oldConcept, newConcept) =>
                handleErrors(
                    controller
                        .updateConcept(oldConcept, newConcept)
                        .map(i => RenameCountSC(oldConcept, newConcept, i))
                )
            }

    // PUT /:uuid
    val updateOneObservation: Endpoint[Option[String], (UUID, ObservationUpdateSC), ErrorMsg, ObservationSC, Any] =
        secureEndpoint
            .put
            .in("v1" / "observations" / path[UUID]("uuid"))
            .in(oneOfBody(jsonBody[ObservationUpdateSC], formBody[ObservationUpdateSC]))
            .out(jsonBody[ObservationSC])
            .name("updateOneObservation")
            .description("Update an observation. If the observation timestamp is not provided, then it will be set to the current time")
            .tag("observations")

    val updateOneObservationImpl: ServerEndpoint[Any, Future] =
        updateOneObservation
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { _ => (uuid, dto) =>
                val observationTimestamp = dto.observation_timestamp.getOrElse(Instant.now())
                handleOption(
                    controller
                        .update(
                            uuid,
                            dto.concept,
                            dto.observer,
                            observationTimestamp,
                            dto.duration,
                            dto.group,
                            dto.activity,
                            dto.imaged_moment_uuid
                        )
                        .map(_.map(_.toSnakeCase))
                )
            }

    // PUT /delete/duration/:uuid
    val deleteDuration: Endpoint[Option[String], UUID, ErrorMsg, ObservationSC, Any] =
        secureEndpoint
            .put
            .in("v1" / "observations" / "delete" / "duration" / path[UUID]("uuid"))
            .out(jsonBody[ObservationSC])
            .name("deleteDuration")
            .description("Delete the duration of an observation")
            .tag("observations")

    val deleteDurationImpl: ServerEndpoint[Any, Future] =
        deleteDuration
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { _ => uuid =>
                handleOption(controller.deleteDuration(uuid).map(_.map(_.toSnakeCase)))
            }

    // DELETE /:uuid
    val deleteOneObservation: Endpoint[Option[String], UUID, ErrorMsg, Unit, Any] = secureEndpoint
        .delete
        .in("v1" / "observations" / path[UUID]("uuid"))
        .out(statusCode(StatusCode.NoContent).and(emptyOutput))
        .name("deleteOneObservation")
        .description("Delete an observation")
        .tag("observations")

    val deleteOneObservationImpl: ServerEndpoint[Any, Future] =
        deleteOneObservation
            .serverSecurityLogic(jwtOpt => verify(jwtOpt))
            .serverLogic { _ => uuid =>
                handleErrors(controller.delete(uuid).map(b => if b then Right(()) else Left(())))
            }

    // POST /delete with json body

    override def all: List[Endpoint[_, _, _, _, _]] = List(
        findObservationByUuid,
        findObservationsByVideoReferenceUuid,
        findObservationByAssociationUuid,
        findActivities,
        findAllConcepts,
        findConceptsByVideoReferenceUuid,
        countObservationsByConcept,
        countImagesByConcept,
        findGroups,
        countByVideoReferenceUuid,
        countAllGroupByVideoReferenceUuid,
        renameConcept,
        updateOneObservation,
        deleteDuration,
        deleteOneObservation
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        findObservationByUuidImpl,
        findObservationsByVideoReferenceUuidImpl,
        findObservationByAssociationUuidImpl,
        findActivitiesImpl,
        findAllConceptsImpl,
        findConceptsByVideoReferenceUuidImpl,
        countObservationsByConceptImpl,
        countImagesByConceptImpl,
        findGroupsImpl,
        countByVideoReferenceUuidImpl,
        countAllGroupByVideoReferenceUuidImpl,
        renameConceptImpl,
        updateOneObservationImpl,
        deleteDurationImpl,
        deleteOneObservationImpl
    )
}