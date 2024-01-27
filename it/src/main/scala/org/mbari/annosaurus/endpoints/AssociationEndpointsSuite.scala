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

import org.mbari.annosaurus.controllers.{AssociationController, TestUtils}
import org.mbari.annosaurus.domain.{Association, AssociationSC, ConceptAssociation, ConceptAssociationRequest, ConceptAssociationResponseSC, ConceptCount, RenameCountSC}
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import sttp.model.StatusCode
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import sttp.client3.*
import org.mbari.annosaurus.etc.sdk.Futures.*
import org.mbari.annosaurus.etc.sdk.Reflect

import scala.jdk.CollectionConverters.*
import scala.util.Random

trait AssociationEndpointsSuite extends EndpointsSuite {

    private val log              = System.getLogger(getClass.getName)
    given JPADAOFactory          = daoFactory
    given jwtService: JwtService = new JwtService("mbari", "foo", "bar")
    lazy val controller          = new AssociationController(daoFactory)
    lazy val endpoints           = new AssociationEndpoints(controller)

    test("findAssociationsByUuid") {
        val im = TestUtils.create(1, 1, 1).head
        val a  = im.getObservations.iterator().next().getAssociations.iterator().next()
        runGet(
            endpoints.findAssociationByUuidImpl,
            s"/v1/associations/${a.getUuid}",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[AssociationSC](response.body)
                val expected = Association.from(a, true).toSnakeCase
                assertEquals(obtained, expected)
            }
        )
    }

    test("findAssociationsByVideoReferenceUuidAndLinkName") {
        // TODO need to check with linknames that has spaces
        val im = TestUtils.create(1, 1, 1).head
        val a  = im.getObservations.iterator().next().getAssociations.iterator().next()
        runGet(
            endpoints.findAssociationsByVideoReferenceUuidAndLinkNameImpl,
            s"/v1/associations/${im.getVideoReferenceUuid}/${a.getLinkName}",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val xs       = checkResponse[Seq[AssociationSC]](response.body)
                assertEquals(xs.size, 1)
                val obtained = xs.head
                val expected = Association
                    .from(a, true)
                    .copy(
                        lastUpdated = obtained.last_updated_time,
                        observationUuid = obtained.observation_uuid,
                        imagedMomentUuid = obtained.imaged_moment_uuid
                    )
                    .toSnakeCase
                assertEquals(obtained, expected)
            }
        )
    }

    test("findAssociationsByVideoReferenceUuidAndLinkName (with concept query param)") {
        // TODO need to check with linknames that has spaces
        val im  = TestUtils.create(1, 1, 1).head
        val obs = im.getObservations.iterator().next()
        val a   = im.getObservations.iterator().next().getAssociations.iterator().next()

        // should return 1
        runGet(
            endpoints.findAssociationsByVideoReferenceUuidAndLinkNameImpl,
            s"/v1/associations/${im.getVideoReferenceUuid}/${a.getLinkName}?concept=${obs.getConcept}",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val xs       = checkResponse[Seq[AssociationSC]](response.body)
                assertEquals(xs.size, 1)
                val obtained = xs.head
                val expected = Association
                    .from(a, true)
                    .copy(
                        lastUpdated = obtained.last_updated_time,
                        observationUuid = obtained.observation_uuid,
                        imagedMomentUuid = obtained.imaged_moment_uuid
                    )
                    .toSnakeCase
                assertEquals(obtained, expected)
            }
        )

        // should return 0
        runGet(
            endpoints.findAssociationsByVideoReferenceUuidAndLinkNameImpl,
            s"/v1/associations/${im.getVideoReferenceUuid}/${a.getLinkName}?concept=robocop",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val xs = checkResponse[Seq[AssociationSC]](response.body)
                assertEquals(xs.size, 0)
            }
        )
    }

    test("createAssociation (json)") {
        val im        = TestUtils.create(1, 1).head
        val obs       = im.getObservations.iterator().next()
        val a         = TestUtils.randomAssociation()
        obs.addAssociation(a)
        val requested = Association.from(a, true).toSnakeCase
        val jwt       = jwtService.authorize("foo").orNull
        assert(jwt != null)

        val backendStub = newBackendStub(endpoints.createAssociationImpl)
        val response    = basicRequest
            .post(uri"http://test.com/v1/associations")
            .body(requested.stringify)
            .auth
            .bearer(jwt)
            .contentType("application/json")
            .send(backendStub)
            .join
//        println(response.body)
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[AssociationSC](response.body)
        val expected    =
            requested.copy(uuid = obtained.uuid, last_updated_time = obtained.last_updated_time)
        assertEquals(obtained, expected)
    }

    test("createAssociation (form)") {
        val im        = TestUtils.create(1, 1).head
        val obs       = im.getObservations.iterator().next()
        val a         = TestUtils.randomAssociation()
        obs.addAssociation(a)
        val requested = Association.from(a, true).toSnakeCase
        val body      = Reflect.toFormBody(requested)
        val jwt       = jwtService.authorize("foo").orNull
        assert(jwt != null)

        val backendStub = newBackendStub(endpoints.createAssociationImpl)
        val response    = basicRequest
            .post(uri"http://test.com/v1/associations")
            .body(body)
            .auth
            .bearer(jwt)
            .contentType("application/x-www-form-urlencoded")
            .send(backendStub)
            .join
//        println(response.body)
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[AssociationSC](response.body)
        val expected    =
            requested.copy(uuid = obtained.uuid, last_updated_time = obtained.last_updated_time)
        assertEquals(obtained, expected)
    }

    test("updateAssociation (json)") {
        val im        = TestUtils.create(1, 1, 1).head
        val obs       = im.getObservations.iterator().next()
        val a         = obs.getAssociations.iterator().next()
        val requested = Association
            .from(a, true)
            .copy(
                linkName = "foodefafa",
                linkValue = "bardebarbar",
                uuid = None
            ) // not explicitly needed by UUID is ignored anyway by the endpoint
            .toSnakeCase
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.updateAssociationImpl)
        val response    = basicRequest
            .put(uri"http://test.com/v1/associations/${a.getUuid}")
            .body(requested.stringify)
            .auth
            .bearer(jwt)
            .contentType("application/json")
            .send(backendStub)
            .join
//        println(response.body)
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[AssociationSC](response.body)
        val expected    =
            requested.copy(uuid = obtained.uuid, last_updated_time = obtained.last_updated_time)
        assertEquals(obtained, expected)
    }

    test("updateAssociation (form)") {
        val im        = TestUtils.create(1, 1, 1).head
        val obs       = im.getObservations.iterator().next()
        val a         = obs.getAssociations.iterator().next()
        val requested = Association
            .from(a, true)
            .copy(
                linkName = "foodefafa",
                linkValue = "bardebarbar",
                uuid = None
            ) // not explicitly needed by UUID is ignored anyway by the endpoint
            .toSnakeCase
        val body        = Reflect.toFormBody(requested)
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.updateAssociationImpl)
        val response    = basicRequest
            .put(uri"http://test.com/v1/associations/${a.getUuid}")
            .body(body)
            .auth
            .bearer(jwt)
            .contentType("application/x-www-form-urlencoded")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[AssociationSC](response.body)
        val expected    =
            requested.copy(uuid = obtained.uuid, last_updated_time = obtained.last_updated_time)
        assertEquals(obtained, expected)
    }

    test("updateAssociations") {
        val xs  = TestUtils.create(2, 2, 2)
        val ass = for
            x <- xs
            o <- x.getObservations.asScala
            a <- o.getAssociations.asScala
        yield a

        val expected = ass
            .map(a =>
                Association
                    .from(a, true)
                    .copy(
                        linkName = "Updated!!!",
                        toConcept = "Grimp",
                        linkValue = s"{key: ${Random.nextInt}}",
                        mimeType = Some("application/json")
                    )
            )
            .map(_.toSnakeCase)
            .sortBy(_.uuid.orNull)

        // We don't need these UUIDs in the HTTP request body
        val requested = expected.map(_.copy(imaged_moment_uuid = None, observation_uuid = None))
        val body      = requested.stringify

        val jwt = jwtService.authorize("foo").orNull
        assert(jwt != null)

        val backendStub = newBackendStub(endpoints.updateAssociationsImpl)
        val response    = basicRequest
            .put(uri"http://test.com/v1/associations/bulk")
            .body(body)
            .auth
            .bearer(jwt)
            .contentType("application/json")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[Seq[AssociationSC]](response.body)
            .sortBy(_.uuid.orNull)

        assertEquals(obtained, expected)

    }

    test("deleteAssociations") {
        val xs          = TestUtils.create(2, 2, 2)
        val dtos        = for
            x <- xs
            o <- x.getObservations.asScala
            a <- o.getAssociations.asScala
        yield Association.from(a, true)
        val uuids       = dtos.flatMap(_.uuid)
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.deleteAssociationsImpl)
        val response    = basicRequest
            .put(uri"http://test.com/v1/associations/bulk/delete")
            .body(uuids.stringify)
            .auth
            .bearer(jwt)
            .contentType("application/json")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.NoContent)

        // Make sure they're gone
        for u <- uuids
        do
            val b = controller.findByUUID(u).join
            assertEquals(b, None)
    }

    test("deleteAssociation") {
        val im          = TestUtils.create(1, 1, 1).head
        val a           = im.getObservations.asScala.head.getAssociations.asScala.head
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.deleteAssociationImpl)
        val response    = basicRequest
            .delete(uri"http://test.com/v1/associations/${a.getUuid}")
            .auth
            .bearer(jwt)
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.NoContent)

        // Make sure it's gone
        val b = controller.findByUUID(a.getUuid).join
        assertEquals(b, None)
    }

    test("countAssociationsByToConcept") {
        val xs   = TestUtils.create(2, 2, 2)
        val dtos = for
            x <- xs
            o <- x.getObservations.asScala
            a <- o.getAssociations.asScala
        yield Association
            .from(a, true)
            .copy(toConcept = "Grimp")

        val toConcept = "Pandalus"
        for
            a    <- dtos
            uuid <- a.uuid
        do controller.update(uuid, toConcept = Some(toConcept)).join

        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.countAssociationsByToConceptImpl)
        val response    = basicRequest
            .get(uri"http://test.com/v1/associations/toconcept/count/${toConcept}")
            .auth
            .bearer(jwt)
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[ConceptCount](response.body)
        assertEquals(obtained.count, dtos.size.toLong)

    }

    test("renameToConcept") {
        val xs   = TestUtils.create(2, 2, 2)
        val dtos = for
            x <- xs
            o <- x.getObservations.asScala
            a <- o.getAssociations.asScala
        yield Association.from(a, true)

        val toConcept    = "Pandalus platyceros"
        val expected     = (for
            a    <- dtos
            uuid <- a.uuid
        yield controller.update(uuid, toConcept = Some(toConcept)).join).flatten
        val newToConcept = "Pandalus"

        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.renameToConceptImpl)
        val response    = basicRequest
            .put(
                uri"http://test.com/v1/associations/toconcept/rename?old=${toConcept}&new=$newToConcept"
            )
            .auth
            .bearer(jwt)
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[RenameCountSC](response.body)
        assertEquals(obtained.count, expected.size.toLong)
        assertEquals(obtained.old_concept, toConcept)
        assertEquals(obtained.new_concept, newToConcept)

        for
            a    <- dtos
            uuid <- a.uuid
        do
            val b = controller.findByUUID(uuid).join
            assertEquals(b.get.toConcept, newToConcept)

    }

    test("findAssociationsByConceptAssociationRequest") {
        val xs   = TestUtils.create(1, 2, 2) ++ TestUtils.create(1, 2, 2)

        // rename all associations linktNames
        val linkName = "Cthulhu"
        val dtos = (for
            x <- xs
            o <- x.getObservations.asScala
            a <- o.getAssociations.asScala
        yield controller.update(a.getUuid, linkName = Some(linkName)).join).flatten

        // Verify that they were renamed
        for
            a <- dtos
            uuid <- a.uuid
        do
            val b = controller.findByUUID(uuid).join
            assertEquals(b.get.linkName, linkName)

        // build request
        val videoReferenceUuids = xs.map(_.getVideoReferenceUuid).distinct
        val car = ConceptAssociationRequest(videoReferenceUuids, linkName)

        // run request
        runPost(
            endpoints.findAssociationsByConceptAssociationRequestImpl,
            s"/v1/associations/conceptassociations",
            car.stringify,
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[ConceptAssociationResponseSC](response.body)
//                println(obtained.stringify)
                assertEquals(obtained.associations.size, dtos.size)
                // TODO check that the associations are the same
            }
        )
    }

}
