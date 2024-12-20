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

import org.mbari.annosaurus.controllers.{ImageReferenceController, TestUtils}
import org.mbari.annosaurus.domain.{ImageReference, ImageReferenceSC}
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.etc.sdk.Futures.*
import org.mbari.annosaurus.etc.sdk.Reflect
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import sttp.client3.*
import sttp.model.StatusCode

import scala.jdk.CollectionConverters.*

trait ImageReferenceEndpointsSuite extends EndpointsSuite:

    given JPADAOFactory          = daoFactory
    private val log              = System.getLogger(getClass.getName)
    given jwtService: JwtService = new JwtService("mbari", "foo", "bar")
    lazy val controller          = new ImageReferenceController(daoFactory)
    lazy val endpoints           = new ImageReferenceEndpoints(controller)

    test("deleteImageByUuid") {
        val im             = TestUtils.create(1, 0, 0, 1).head
        val imageReference = im.getImageReferences.asScala.head
        val jwt            = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub    = newBackendStub(endpoints.deleteImageByUuidImpl)
        val response       = basicRequest
            .delete(uri"http://test.com/v1/imagereferences/${imageReference.getUuid}")
            .auth
            .bearer(jwt)
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.NoContent)

        val image = controller.findByUUID(imageReference.getUuid).join
        assert(image.isEmpty)
    }

    test("findImageByUuid") {
        val im             = TestUtils.create(1, 0, 0, 1).head
        val imageReference = im.getImageReferences.asScala.head
        runGet(
            endpoints.findImageByUuidImpl,
            s"http://test.com/v1/imagereferences/${imageReference.getUuid}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[ImageReferenceSC](response.body).toCamelCase
                val expected = ImageReference.from(imageReference, true)
                assertEquals(obtained, expected)
        )
    }

    test("updateImageReferenceByUuid (json)") {
        val im             = TestUtils.create(1, 0, 0, 1).head
        val imageReference = im.getImageReferences.asScala.head
        val newValues      = TestUtils.randomImageReference()
        val dto            = ImageReference.from(newValues, false)
        val body           = dto.toSnakeCase.stringify
        val jwt            = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub    = newBackendStub(endpoints.updateImageReferenceByUuidImpl)
        val response       = basicRequest
            .put(uri"http://test.com/v1/imagereferences/${imageReference.getUuid}")
            .auth
            .bearer(jwt)
            .body(body)
            .contentType("application/json")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained       = checkResponse[ImageReferenceSC](response.body).toCamelCase
        val expected       = dto.copy(
            imagedMomentUuid = Some(im.getUuid),
            uuid = Some(imageReference.getUuid),
            lastUpdated = obtained.lastUpdated
        )
        assertEquals(obtained, expected)
    }

    test("updateImageReferenceByUuid (form)") {
        val im             = TestUtils.create(1, 0, 0, 1).head
        val imageReference = im.getImageReferences.asScala.head
        val newValues      = TestUtils.randomImageReference()
        val dto            = ImageReference.from(newValues, false)
        val body           = Reflect.toFormBody(dto.toSnakeCase)
//        println("------- " + body)
        val jwt            = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub    = newBackendStub(endpoints.updateImageReferenceByUuidImpl)
        val response       = basicRequest
            .put(uri"http://test.com/v1/imagereferences/${imageReference.getUuid}")
            .auth
            .bearer(jwt)
            .body(body)
            .contentType("application/x-www-form-urlencoded")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained       = checkResponse[ImageReferenceSC](response.body).toCamelCase
        val expected       = dto.copy(
            imagedMomentUuid = Some(im.getUuid),
            uuid = Some(imageReference.getUuid),
            lastUpdated = obtained.lastUpdated
        )
        assertEquals(obtained, expected)
    }
