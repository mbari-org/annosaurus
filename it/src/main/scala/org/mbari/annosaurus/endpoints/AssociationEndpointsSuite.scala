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
import org.mbari.annosaurus.domain.{Association, AssociationSC}
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import sttp.model.StatusCode
import org.mbari.annosaurus.etc.circe.CirceCodecs.{given, *}
import sttp.client3.*
import org.mbari.annosaurus.etc.sdk.Futures.*

import scala.jdk.CollectionConverters.*

trait AssociationEndpointsSuite extends EndpointsSuite {

    private val log = System.getLogger(getClass.getName)
    given JPADAOFactory = daoFactory
    given jwtService: JwtService = new JwtService("mbari", "foo", "bar")
    lazy val controller = new AssociationController(daoFactory)
    lazy val endpoints = new AssociationEndpoints(controller)

    test("findAssociationsByUuid") {
        val im = TestUtils.create(1, 1, 1).head
        val a = im.getObservations.iterator().next().getAssociations.iterator().next()
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
        val a = im.getObservations.iterator().next().getAssociations.iterator().next()
        runGet(
            endpoints.findAssociationsByVideoReferenceUuidAndLinkNameImpl,
            s"/v1/associations/${im.getVideoReferenceUuid}/${a.getLinkName}",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val xs = checkResponse[Seq[AssociationSC]](response.body)
                assertEquals(xs.size, 1)
                val obtained = xs.head
                val expected = Association.from(a, true)
                    .copy(lastUpdated = obtained.last_updated_time,
                        observationUuid = obtained.observation_uuid,
                        imagedMomentUuid = obtained.imaged_moment_uuid)
                    .toSnakeCase
                assertEquals(obtained, expected)
            }
        )
    }

    test("findAssociationsByVideoReferenceUuidAndLinkName (with concept query param") {
        // TODO need to check with linknames that has spaces
        val im = TestUtils.create(1, 1, 1).head
        val obs = im.getObservations.iterator().next()
        val a = im.getObservations.iterator().next().getAssociations.iterator().next()

        // should return 1
        runGet(
            endpoints.findAssociationsByVideoReferenceUuidAndLinkNameImpl,
            s"/v1/associations/${im.getVideoReferenceUuid}/${a.getLinkName}?concept=${obs.getConcept}",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val xs = checkResponse[Seq[AssociationSC]](response.body)
                assertEquals(xs.size, 1)
                val obtained = xs.head
                val expected = Association.from(a, true)
                    .copy(lastUpdated = obtained.last_updated_time,
                        observationUuid = obtained.observation_uuid,
                        imagedMomentUuid = obtained.imaged_moment_uuid)
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
        val im = TestUtils.create(1, 1).head
        val obs = im.getObservations.iterator().next()
        val a = TestUtils.randomAssociation()
        obs.addAssociation(a)
        val requested = Association.from(a, true).toSnakeCase
        val jwt = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.createAssociationImpl)
        val response = basicRequest
            .post(uri"http://test.com/v1/associations")
            .body(requested.stringify)
            .auth.bearer(jwt)
            .contentType("application/json")
            .send(backendStub)
            .join
        println(response.body)
        assertEquals(response.code, StatusCode.Ok)
        val obtained = checkResponse[AssociationSC](response.body)
        val expected = requested.copy(uuid = obtained.uuid,
            last_updated_time = obtained.last_updated_time)
        assertEquals(obtained, expected)
    }

    test("updateAssociation") {
        fail("not implemented")
    }

    test("updateAssociations") {
        fail("not implemented")
    }

    test("deleteAssociations") {
        fail("not implemented")
    }

    test("deleteAssociation") {
        fail("not implemented")
    }

    test("countAssociationsByToConcept") {
        fail("not implemented")
    }

    test("renameToConcept") {
        fail("not implemented")
    }

    test("findAssociationsByConceptAssociationRequest") {
        fail("not implemented")
    }



}
