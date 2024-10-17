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

import org.junit.Assert.*
import org.mbari.annosaurus.controllers.{CachedAncillaryDatumController, TestUtils}
import org.mbari.annosaurus.domain.{CachedAncillaryDatum, CachedAncillaryDatumSC, CountForVideoReferenceSC}
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.etc.sdk.Futures.*
import org.mbari.annosaurus.etc.sdk.Reflect
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import sttp.client3.*
import sttp.model.StatusCode

trait CachedAncillaryDatumEndpointsSuite extends EndpointsSuite:

    private val log = System.getLogger(getClass.getName)

    given JPADAOFactory = daoFactory

    given jwtService: JwtService = new JwtService("mbari", "foo", "bar")

    private lazy val controller = new CachedAncillaryDatumController(daoFactory)
    private lazy val endpoints  = new CachedAncillaryDatumEndpoints(controller)

    test("findDataByUuid") {
        val im = TestUtils.create(1, includeData = true).head
        val d  = im.getAncillaryDatum
        runGet(
            endpoints.findDataByUuidImpl,
            s"http://test.com/v1/ancillarydata/${d.getUuid}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[CachedAncillaryDatumSC](response.body).toCamelCase
                val expected = CachedAncillaryDatum.from(d, true)
                assertEquals(obtained, expected)
        )
    }

    test("findDataByVideoReferenceUuid") {
        val im = TestUtils.create(1, includeData = true).head
        val d  = im.getAncillaryDatum
        runGet(
            endpoints.findDataByVideoReferenceUuidImpl,
            s"http://test.com/v1/ancillarydata/videoreference/${im.getVideoReferenceUuid}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained =
                    checkResponse[List[CachedAncillaryDatumSC]](response.body).map(_.toCamelCase)
                val expected = List(CachedAncillaryDatum.from(d, true))
                assertEquals(obtained, expected)
        )
    }

    test("findDataByImagedMomentUuid") {
        val im = TestUtils.create(1, includeData = true).head
        val d  = im.getAncillaryDatum
        runGet(
            endpoints.findDataByImagedMomentUuidImpl,
            s"http://test.com/v1/ancillarydata/imagedmoment/${im.getUuid}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[CachedAncillaryDatumSC](response.body).toCamelCase
                val expected = CachedAncillaryDatum.from(d, true)
                assertEquals(obtained, expected)
        )
    }

    test("findDataByObservationUuid") {
        val im  = TestUtils.create(1, 1, includeData = true).head
        val obs = im.getObservations.iterator().next()
        val d   = im.getAncillaryDatum
        runGet(
            endpoints.findDataByObservationUuidImpl,
            s"http://test.com/v1/ancillarydata/observation/${obs.getUuid}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[CachedAncillaryDatumSC](response.body).toCamelCase
                val expected = CachedAncillaryDatum.from(d, true)
                assertEquals(obtained, expected)
        )
    }

    test("createOneDatum (json)") {
        val im          = TestUtils.create(1).head
        val d           = CachedAncillaryDatum
            .from(TestUtils.randomData())
            .copy(imagedMomentUuid = Some(im.getUuid))
            .toSnakeCase
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.createOneDatumImpl)
        val response    = basicRequest
            .post(uri"http://test.com/v1/ancillarydata")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/json")
            .body(d.stringify)
            .send(backendStub)
            .join

        assertEquals(response.code, StatusCode.Ok)
    }

    test("createOneDatum (form)") {
        val im          = TestUtils.create(1).head
        val d           = CachedAncillaryDatum
            .from(TestUtils.randomData())
            .copy(imagedMomentUuid = Some(im.getUuid))
            .toSnakeCase
        val body        = Reflect.toFormBody(d)
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.createOneDatumImpl)
        val response    = basicRequest
            .post(uri"http://test.com/v1/ancillarydata")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(body)
            .send(backendStub)
            .join

        assertEquals(response.code, StatusCode.Ok)
    }

    test("createorUpdateManyData") {
        val xs = TestUtils.create(2, includeData = true) ++ TestUtils.create(2)
        val d  = xs
            .map(im =>
                Option(im.getAncillaryDatum) match
                    case Some(d) => CachedAncillaryDatum.from(d, true)
                    case None    =>
                        CachedAncillaryDatum
                            .from(TestUtils.randomData())
                            .copy(imagedMomentUuid = Some(im.getUuid))
            )
            .map(x => x.copy(latitude = Some(25.345)))
            .map(_.toSnakeCase)

        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.createOrUpdateManyDataImpl)
        val response    = basicRequest
            .post(uri"http://test.com/v1/ancillarydata/bulk")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/json")
            .body(d.stringify)
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[List[CachedAncillaryDatumSC]](response.body)
        assertEquals(obtained.size, 4)
        for x <- obtained
        do assertEqualsDouble(x.latitude.getOrElse(-1000d), 25.345, 0.0001)
    }

    test("mergeManyData") {
        val xs = TestUtils.create(4, includeData = true)
        val d  = xs
            .map(im =>
                CachedAncillaryDatum.from(im.getAncillaryDatum, true)
            ) // extend! we need the recordedTimestamp in the CachecAncillaryDatum
            .map(x => x.copy(latitude = Some(25.345)))
            .map(_.toSnakeCase)
        val videoReferenceUuid = xs.head.getVideoReferenceUuid

        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.mergeManyDataImpl)
        val response    = basicRequest
            .put(uri"http://test.com/v1/ancillarydata/merge/$videoReferenceUuid")
            .auth
            .bearer(jwt)
            .contentType("application/json")
            .body(d.stringify)
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[List[CachedAncillaryDatumSC]](response.body)
        assertEquals(obtained.size, 4)
        for x <- obtained
        do assertEqualsDouble(x.latitude.getOrElse(-1000d), 25.345, 0.0001)
    }

    test("updateOneDatum (json)") {
        val im          = TestUtils.create(1, includeData = true).head
        val d           = CachedAncillaryDatum
            .from(im.getAncillaryDatum)
            .copy(latitude = Some(25.345))
            .toSnakeCase
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.updateOneDatumImpl)
        val response    = basicRequest
            .put(uri"http://test.com/v1/ancillarydata/${d.uuid}")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/json")
            .body(d.stringify)
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[CachedAncillaryDatumSC](response.body)
        assertEqualsDouble(obtained.latitude.getOrElse(-1000d), 25.345, 0.0001)
    }

    test("updateOneDatum (form)") {
        val im          = TestUtils.create(1, includeData = true).head
        val d           = CachedAncillaryDatum
            .from(im.getAncillaryDatum)
            .copy(latitude = Some(25.345))
            .toSnakeCase
        val body        = Reflect.toFormBody(d)
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.updateOneDatumImpl)
        val response    = basicRequest
            .put(uri"http://test.com/v1/ancillarydata/${d.uuid}")
            .auth
            .bearer(jwt)
            .contentType("application/x-www-form-urlencoded")
            .body(body)
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[CachedAncillaryDatumSC](response.body)
        assertEqualsDouble(obtained.latitude.getOrElse(-1000d), 25.345, 0.0001)
    }

    test("deleteDataByVideoReferenceUuid") {
        val xs                 = TestUtils.create(2, includeData = true)
        val videoReferenceUuid = xs.head.getVideoReferenceUuid
        val jwt                = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub        = newBackendStub(endpoints.deleteDataByVideoReferenceUuidImpl)
        val response           = basicRequest
            .delete(uri"http://test.com/v1/ancillarydata/videoreference/$videoReferenceUuid")
            .header("Authorization", s"Bearer $jwt")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        assertEquals(response.code, StatusCode.Ok)
        val deleteCount        = checkResponse[CountForVideoReferenceSC](response.body)
        assertEquals(deleteCount.count, 2)
    }
