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

import org.mbari.annosaurus.controllers.IndexController
import org.mbari.annosaurus.controllers.TestUtils
import org.mbari.annosaurus.domain.ImagedMoment
import org.mbari.annosaurus.etc.jdk.Logging
import sttp.model.StatusCode
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.etc.circe.CirceCodecs.given


trait IndexEndpointsITSuite extends EndpointsSuite {

    private val log = Logging(getClass)

    given JPADAOFactory             = daoFactory
    private val jwtService          = new JwtService("mbari", "foo", "bar")
    private lazy val controller     = new IndexController(daoFactory)
    private lazy val endpoints      = new IndexEndpoints(controller, jwtService)

    test("findByVideoReferenceUUID") {
        val im = TestUtils.create(1, 1).head
        runGet(
            endpoints.findByVideoReferenceUUIDImpl,
            s"http://test.com/v1/index/videoreference/${im.videoReferenceUUID}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val imagedMoments = checkResponse[List[ImagedMoment]](response.body)
                // assertSameMedia(media, media0)
        )

    }
  
}
