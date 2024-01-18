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

import org.mbari.annosaurus.controllers.{ImagedMomentController, TestUtils}
import org.mbari.annosaurus.domain.{Count, ImagedMoment, ImagedMomentSC}
import org.mbari.annosaurus.etc.jdk.Logging.{*, given}
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import sttp.model.StatusCode
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import sttp.client3.*
import org.mbari.annosaurus.etc.sdk.Futures.*

trait ImagedMomentEndpointsITSuite extends EndpointsSuite {

    private val log              = System.getLogger(getClass.getName)
    given JPADAOFactory          = daoFactory
    given jwtService: JwtService = new JwtService("mbari", "foo", "bar")
    private lazy val controller  = new ImagedMomentController(daoFactory)
    private lazy val endpoints   = new ImagedMomentEndpoints(controller)

    test("findAllImagedMoments") {
        val xs = TestUtils.create(2)
        runGet(
            endpoints.findAllImagedMomentsImpl,
            s"http://test.com/v1/imagedmoments?limit=10&offset=0",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val imagedMoments = checkResponse[Seq[ImagedMomentSC]](response.body)
                assert(imagedMoments.size >= 2)
            }
        )
    }

    test("countAllImagedMoments") {
        val xs = TestUtils.create(2)
        runGet(
            endpoints.countAllImagedMomentsImpl,
            s"http://test.com/v1/imagedmoments/count/all",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val count = checkResponse[Count](response.body)
                assert(count.count >= 2)
            }
        )
    }

    test("findImagedMomentsWithImages") {
        val im = TestUtils.create(1, nImageReferences = 1).head
        runGet(
            endpoints.findImagedMomentsWithImagesImpl,
            s"http://test.com/v1/imagedmoments/find/images?limit=10&offset=0",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val imagedMoments = checkResponse[Seq[ImagedMomentSC]](response.body)
                assert(imagedMoments.nonEmpty)
                val expected = ImagedMoment.from(im, true)
                val obtained = imagedMoments.filter(_.uuid.get == im.getUuid).head.toCamelCase
                assertEquals(obtained, expected)
            }
        )
    }

    test("countImagedMomentsWithImages") {
        val im = TestUtils.create(1, nImageReferences = 1).head
        runGet(
            endpoints.countImagedMomentsWithImagesImpl,
            s"http://test.com/v1/imagedmoments/count/images",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val count = checkResponse[Count](response.body)
                assert(count.count >= 1)
            }
        )
    }

    test("countImagesForVideoReference") {
        val xs = TestUtils.create(4, nImageReferences = 1)
        runGet(
            endpoints.countImagesForVideoReferenceImpl,
            s"http://test.com/v1/imagedmoments/count/images/${xs.head.getVideoReferenceUuid}",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val count = checkResponse[Count](response.body)
                assertEquals(count.count, xs.size.longValue)
            }
        )

        val uuid = java.util.UUID.randomUUID()
        runGet(
            endpoints.countImagesForVideoReferenceImpl,
            s"http://test.com/v1/imagedmoments/count/images/$uuid",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val count = checkResponse[Count](response.body)
                assertEquals(count.count, 0L)
            }
        )
    }

    test("findImagedMomentsByLinkName") {
        val im = TestUtils.create(4, 1, 2).head
        val ass = im.getObservations.iterator().next().getAssociations.iterator().next()
        val linkName = ass.getLinkName
        runGet(
            endpoints.findImagedMomentsByLinkNameImpl,
            s"http://test.com/v1/imagedmoments/find/linkname/${linkName}",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val imagedMoments = checkResponse[Seq[ImagedMomentSC]](response.body)
                assertEquals(imagedMoments.size, 1)
                val expected = ImagedMoment.from(im, true)
                val obtained = imagedMoments.head.toCamelCase
                assertEquals(obtained, expected)
            }
        )

        val linkName1 = "foo"
        runGet(
            endpoints.findImagedMomentsByLinkNameImpl,
            s"http://test.com/v1/imagedmoments/find/linkname/$linkName1",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val imagedMoments = checkResponse[Seq[ImagedMomentSC]](response.body)
                assert(imagedMoments.isEmpty)
            }
        )
    }

}
