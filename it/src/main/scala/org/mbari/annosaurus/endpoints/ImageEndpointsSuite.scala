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

import org.mbari.annosaurus.controllers.{ImageController, TestUtils}
import org.mbari.annosaurus.domain.{Image, ImageCreateSC, ImageSC, ImageUpdateSC}
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.etc.sdk.Futures.*
import org.mbari.annosaurus.etc.sdk.Reflect
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import sttp.client3.*
import sttp.model.StatusCode

import java.net.{URI, URLEncoder}
import java.nio.charset.StandardCharsets

trait ImageEndpointsSuite extends EndpointsSuite:

    private val log              = System.getLogger(getClass.getName)
    given JPADAOFactory          = daoFactory
    given jwtService: JwtService = new JwtService("mbari", "foo", "bar")
    lazy val controller          = new ImageController(daoFactory)
    lazy val endpoints           = new ImageEndpoints(controller)

    test("findOneImage") {
        val im = TestUtils.create(1, 0, 0, 1).head
        val ir = im.getImageReferences.iterator.next()
        runGet(
            endpoints.findOneImageImpl,
            s"/v1/images/${ir.getUuid}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[ImageSC](response.body)
                val expected = Image.from(ir, true).toSnakeCase
                assertEquals(obtained, expected)
        )
    }

    test("findByVideoReferenceUuid (single)") {
        val im = TestUtils.create(1, 0, 0, 1).head
        val ir = im.getImageReferences.iterator.next()
        runGet(
            endpoints.findByVideoReferenceUUIDImpl,
            s"/v1/images/videoreference/${im.getVideoReferenceUuid}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[ImageSC]](response.body)
                assertEquals(obtained.size, 1)
                val expected = Image.from(ir, true).toSnakeCase
                assertEquals(obtained.head, expected)
        )
    }

    test("findByVideoReferenceUuid (many)") {
        val im = TestUtils.create(5, 0, 0, 2).head
        val ir = im.getImageReferences.iterator.next()
        runGet(
            endpoints.findByVideoReferenceUUIDImpl,
            s"/v1/images/videoreference/${im.getVideoReferenceUuid}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[ImageSC]](response.body)
                assertEquals(obtained.size, 10)
        )
    }

    test("findByImageName") {
        val im        = TestUtils.create(1, 0, 0, 1).head
        val ir        = im.getImageReferences.iterator.next()
        val imageName = ir.getUrl.toExternalForm.split("/").last
        runGet(
            endpoints.findByImageNameImpl,
            s"/v1/images/name/${imageName}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[ImageSC]](response.body)
                assertEquals(obtained.size, 1)
                val expected = Image.from(ir, true).toSnakeCase
                assertEquals(obtained.head, expected)
        )
    }

    test("findByImageUrl") {
        val im  = TestUtils.create(1, 0, 0, 1).head
        val ir  = im.getImageReferences.iterator.next()
        val url = URLEncoder.encode(ir.getUrl.toExternalForm, StandardCharsets.UTF_8)
//        println(url)
        runGet(
            endpoints.findByImageUrlImpl,
            s"/v1/images/url/${url}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[ImageSC](response.body)
                val expected = Image.from(ir, true).toSnakeCase
                assertEquals(obtained, expected)
        )
    }

    test("createOne (json)") {
        val im          = TestUtils.create(1).head
        val ir          = TestUtils.randomImageReference()
        im.addImageReference(ir)
        val ic          = ImageCreateSC.from(im, true).head
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.createOneImageImpl)
        val response    = basicRequest
            .post(uri"http://localhost:8080/v1/images")
            .body(ic.stringify)
            .auth
            .bearer(jwt)
            .contentType("application/json")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[ImageSC](response.body)
        val expected    = Image
            .from(ir, true)
            .copy(imageReferenceUuid = obtained.image_reference_uuid)
            .toSnakeCase
        assertEquals(obtained, expected)
    }

    test("createOne (form)") {
        val im          = TestUtils.create(1).head
        val ir          = TestUtils.randomImageReference()
        im.addImageReference(ir)
        val ic          = ImageCreateSC.from(im, true).head
        val body        = Reflect.toFormBody(ic)
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.createOneImageImpl)
        val response    = basicRequest
            .post(uri"http://localhost:8080/v1/images")
            .body(body)
            .auth
            .bearer(jwt)
            .contentType("application/x-www-form-urlencoded")
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val obtained    = checkResponse[ImageSC](response.body)
        val expected    = Image
            .from(ir, true)
            .copy(imageReferenceUuid = obtained.image_reference_uuid)
            .toSnakeCase
        assertEquals(obtained, expected)
    }

    test("updateOne (json)") {

        val im          = TestUtils.create(1, 0, 0, 1).head
        val ir          = im.getImageReferences.iterator.next()
        ir.setUrl(URI.create("http://foo.com/hello.png").toURL)
        ir.setDescription("foo")
        val expected    = Image.from(ir, true).toSnakeCase
        val update      = ImageUpdateSC.from(ir)
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.updateOneImageImpl)
        val response    = basicRequest
            .put(uri"/v1/images/${ir.getUuid}")
            .body(update.stringify)
            .auth
            .bearer(jwt)
            .contentType("application/json")
            .send(backendStub)
            .join

        val a = controller.findByUUID(ir.getUuid).join
//        println(a)

        assertEquals(response.code, StatusCode.Ok)
        val obtained = checkResponse[ImageSC](response.body)
        assertEquals(obtained, expected)

    }

    test("updateOne (form)") {

        val im          = TestUtils.create(1, 0, 0, 1).head
        val ir          = im.getImageReferences.iterator.next()
        ir.setUrl(URI.create("http://foo.com/hellofoo.png").toURL)
        ir.setDescription("foofoo")
        val expected    = Image.from(ir, true).toSnakeCase
        val update      = ImageUpdateSC.from(ir)
        val body        = Reflect.toFormBody(update)
        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.updateOneImageImpl)
        val response    = basicRequest
            .put(uri"/v1/images/${ir.getUuid}")
            .body(body)
            .auth
            .bearer(jwt)
            .contentType("application/x-www-form-urlencoded")
            .send(backendStub)
            .join

        val a = controller.findByUUID(ir.getUuid).join
//        println(a)

        assertEquals(response.code, StatusCode.Ok)
        val obtained = checkResponse[ImageSC](response.body)
        assertEquals(obtained, expected)

    }
