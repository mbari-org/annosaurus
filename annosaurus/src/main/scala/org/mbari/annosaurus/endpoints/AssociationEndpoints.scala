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

import org.mbari.annosaurus.controllers.AssociationController
import org.mbari.annosaurus.domain.{
    AssociationSC,
    AssociationUpdateSC,
    BadRequest,
    ConceptAssociationRequest,
    ConceptAssociationResponseSC,
    ConceptCount,
    ErrorMsg,
    RenameConcept,
    RenameCountSC
}
import org.mbari.annosaurus.etc.jwt.JwtService
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import sttp.model.StatusCode
import CustomTapirJsonCirce.*

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class AssociationEndpoints(controller: AssociationController)(using
    ec: ExecutionContext,
    jwtService: JwtService
) extends Endpoints {

    private val base = "associations"
    private val tag  = "Associations"

    // GET /:uuid
    val findAssociationByUuid: Endpoint[Unit, UUID, ErrorMsg, AssociationSC, Any] = openEndpoint
        .get
        .in(base / path[UUID]("associationUuid"))
        .out(jsonBody[AssociationSC])
        .name("findAssociationByUuid")
        .description("Find an association by its UUID")
        .tag(tag)

    val findAssociationByUuidImpl: ServerEndpoint[Any, Future] = findAssociationByUuid
        .serverLogic { uuid => handleOption(controller.findByUUID(uuid).map(_.map(_.toSnakeCase))) }

    // GET /:videoReferenceUuid/:linkName
    val findAssociationsByVideoReferenceUuidAndLinkName
        : Endpoint[Unit, (UUID, String, Option[String]), ErrorMsg, Seq[AssociationSC], Any] =
        openEndpoint
            .get
            .in(base / path[UUID]("videoReferenceUuid") / path[String]("linkName"))
            .in(query[Option[String]]("concept"))
            .out(jsonBody[Seq[AssociationSC]])
            .name("findAssociationsByVideoReferenceUuidAndLinkName")
            .description("Find associations by its videoReferenceUuid and linkName")
            .tag(tag)

    val findAssociationsByVideoReferenceUuidAndLinkNameImpl: ServerEndpoint[Any, Future] =
        findAssociationsByVideoReferenceUuidAndLinkName
            .serverLogic { (videoReferenceUuid, linkName, concept) =>
                handleErrors(
                    controller
                        .findByLinkNameAndVideoReferenceUuidAndConcept(
                            linkName,
                            videoReferenceUuid,
                            concept
                        )
                        .map(_.map(_.toSnakeCase).toSeq)
                )
            }

    // POST form or json body
    val createAssociation: Endpoint[Option[String], AssociationSC, ErrorMsg, AssociationSC, Any] =
        secureEndpoint
            .post
            .in(base)
            .in(oneOfBody(jsonBody[AssociationSC], formBody[AssociationSC]))
            .out(jsonBody[AssociationSC])
            .name("createAssociation")
            .description("Create an association")
            .tag(tag)

    val createAssociationImpl: ServerEndpoint[Any, Future] = createAssociation
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic(_ =>
            assoc =>
                val a = assoc.toCamelCase
                a.observationUuid match
                    case None       => Future(Left(BadRequest("An observation_uuid is required")))
                    case Some(uuid) =>
                        val mediaType = a.mimeType.getOrElse("text/plain")
                        handleErrors(
                            controller
                                .create(uuid, a.linkName, a.toConcept, a.linkValue, mediaType)
                                .map(_.toSnakeCase)
                        )
        )

    // PUT /:uuid form or json body
    val updateAssociation
        : Endpoint[Option[String], (UUID, AssociationUpdateSC), ErrorMsg, AssociationSC, Any] =
        secureEndpoint
            .put
            .in(base / path[UUID]("associationUuid"))
            .in(oneOfBody(jsonBody[AssociationUpdateSC], formBody[AssociationUpdateSC]))
            .out(jsonBody[AssociationSC])
            .name("updateAssociation")
            .description("Update an association")
            .tag(tag)

    val updateAssociationImpl: ServerEndpoint[Any, Future] = updateAssociation
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ => (uuid, a) =>
            handleOption(
                controller
                    .update(
                        uuid,
                        a.observation_uuid,
                        a.link_name,
                        a.to_concept,
                        a.link_value,
                        a.mime_type
                    )
                    .map(_.map(_.toSnakeCase))
            )
        }

    // PUT /bulk json body
    val updateAssociations
        : Endpoint[Option[String], Seq[AssociationSC], ErrorMsg, Seq[AssociationSC], Any] =
        secureEndpoint
            .put
            .in(base / "bulk")
            .in(jsonBody[Seq[AssociationSC]])
            .out(jsonBody[Seq[AssociationSC]])
            .name("updateAssociations")
            .description("Update a list of associations")
            .tag(tag)

    val updateAssociationsImpl: ServerEndpoint[Any, Future] = updateAssociations
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ => associations =>
            val assocs = associations.map(_.toCamelCase)
            handleErrors(controller.bulkUpdate(assocs).map(_.map(_.toSnakeCase).toSeq))
        }

    // DELETE json body of uuids
    val deleteAssociations: Endpoint[Option[String], Seq[UUID], ErrorMsg, Unit, Any] =
        secureEndpoint
            .post
            .in(base / "delete")
            .in(jsonBody[Seq[UUID]])
            .out(statusCode(StatusCode.NoContent).and(emptyOutput))
            .name("deleteAssociations")
            .description("Delete a list of associations by their UUIDs")
            .tag(tag)

    val deleteAssociationsImpl: ServerEndpoint[Any, Future] = deleteAssociations
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ => uuids =>
            if (uuids.isEmpty) Future(Left(BadRequest("No UUIDs provided")))
            else handleErrors(controller.bulkDelete(uuids))
        }

    // DELETE /:uuid
    val deleteAssociation: Endpoint[Option[String], UUID, ErrorMsg, Unit, Any] = secureEndpoint
        .delete
        .in(base / path[UUID]("associationUuid"))
        .out(statusCode(StatusCode.NoContent).and(emptyOutput))
        .name("deleteAssociation")
        .description("Delete an association")
        .tag(tag)

    val deleteAssociationImpl: ServerEndpoint[Any, Future] = deleteAssociation
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ => uuid =>
            handleErrors(controller.delete(uuid).map(_ => ()))
        }

    // GET /toconcept/count/:concept
    val countAssociationsByToConcept: Endpoint[Unit, String, ErrorMsg, ConceptCount, Any] =
        openEndpoint
            .get
            .in(base / "toconcept" / "count" / path[String]("concept"))
            .out(jsonBody[ConceptCount])
            .name("countAssociationsByToConcept")
            .description("Count associations by toConcept")
            .tag(tag)

    val countAssociationsByToConceptImpl: ServerEndpoint[Any, Future] = countAssociationsByToConcept
        .serverLogic { concept =>
            handleErrors(controller.countByToConcept(concept).map(c => ConceptCount(concept, c)))
        }

    // TODO mbari-org/annosaurus#18
    // PUT /toconcept/rename form or json body of oldConcept, newConcept
    val renameToConcept: Endpoint[Option[String], RenameConcept, ErrorMsg, RenameCountSC, Any] =
        secureEndpoint
            .put
            .in(base / "toconcept" / "rename")
            .in(oneOfBody(jsonBody[RenameConcept], formBody[RenameConcept]))
            .out(jsonBody[RenameCountSC])
            .name("renameToConcept")
            .description("Rename toConcept")
            .tag(tag)

    val renameToConceptImpl: ServerEndpoint[Any, Future] = renameToConcept
        .serverSecurityLogic(jwtOpt => verify(jwtOpt))
        .serverLogic { _ => renameConcept =>
            handleErrors(
                controller
                    .updateToConcept(renameConcept.old, renameConcept.`new`)
                    .map(RenameCountSC(renameConcept.old, renameConcept.`new`, _))
            )
        }

    // POST /conceptassociations
    val findAssociationsByConceptAssociationRequest =
        openEndpoint
            .post
            .in(base / "conceptassociations")
            .in(jsonBody[ConceptAssociationRequest])
            .out(jsonBody[ConceptAssociationResponseSC])
            .name("findAssociationsByConceptAssociationRequest")
            .description(
                "Find associations by concept association request. Request can be camelCase or snake_case. Response will be snake_case."
            )
            .tag(tag)

    val findAssociationsByConceptAssociationRequestImpl: ServerEndpoint[Any, Future] =
        findAssociationsByConceptAssociationRequest
            .serverLogic { conceptAssociationRequest =>
                handleErrors(
                    controller
                        .findByConceptAssociationRequest(conceptAssociationRequest)
                        .map(a => {
                            a.toSnakeCase
                        })
                )
            }

    override def all: List[Endpoint[?, ?, ?, ?, ?]] = List(
        deleteAssociations,
        updateAssociations,
        findAssociationsByConceptAssociationRequest,
        countAssociationsByToConcept,
        renameToConcept,
        findAssociationsByVideoReferenceUuidAndLinkName,
        findAssociationByUuid,
        createAssociation,
        updateAssociation,
        deleteAssociation
    )

    override def allImpl: List[ServerEndpoint[Any, Future]] = List(
        deleteAssociationsImpl,
        updateAssociationsImpl,
        findAssociationsByConceptAssociationRequestImpl,
        countAssociationsByToConceptImpl,
        renameToConceptImpl,
        findAssociationsByVideoReferenceUuidAndLinkNameImpl,
        findAssociationByUuidImpl,
        createAssociationImpl,
        updateAssociationImpl,
        deleteAssociationImpl
    )
}
