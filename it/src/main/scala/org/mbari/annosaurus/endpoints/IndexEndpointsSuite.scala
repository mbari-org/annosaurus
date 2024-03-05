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
import org.mbari.annosaurus.domain
import org.mbari.annosaurus.domain.{ImagedMoment, Index, IndexSC, IndexUpdate}
import sttp.model.StatusCode
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.etc.circe.CirceCodecs.given
import org.mbari.annosaurus.etc.jdk.Instants
import sttp.client3.*
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.etc.sdk.Futures.*

import java.time.{Duration, Instant}
import scala.util.Random

trait IndexEndpointsSuite extends EndpointsSuite {

    private val log = System.getLogger(getClass.getName)

    given JPADAOFactory          = daoFactory
    given jwtService: JwtService = new JwtService("mbari", "foo", "bar")
    private lazy val controller  = new IndexController(daoFactory)
    private lazy val endpoints   = new IndexEndpoints(controller)

    test("findByVideoReferenceUUID") {
        val im = TestUtils.create(1, 1).head
        runGet(
            endpoints.findByVideoReferenceUUIDImpl,
            s"http://test.com/v1/index/videoreference/${im.getVideoReferenceUuid}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val index    = checkResponse[Seq[Index]](response.body)
                val expected = Index.fromImagedMomentEntity(im)
                val obtained = index.head
                assertEquals(obtained, expected)
        )

    }

    test("bulkUpdateRecordedTimestamps (recorded_timestamp)") {
        val xs      = TestUtils.create(10)
        val ts      = Instant.parse("1968-09-22T02:00:00Z")
        val updated =
            for x <- xs
            yield
                val t = Option(x.getElapsedTime) match
                    case Some(et) => ts.plus(et)
                    case None     => ts
                IndexUpdate(x.getUuid, recordedTimestamp = Some(t))
        val body    = updated.map(_.toSnakeCase).stringify

        val jwt         = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backendStub = newBackendStub(endpoints.bulkUpdateRecordedTimestampsImpl)
        val response    = basicRequest
            .put(uri"http://test.com/v1/index/tapetime")
            .auth
            .bearer(jwt)
            .body(body)
            .send(backendStub)
            .join
        assertEquals(response.code, StatusCode.Ok)
        val index       = checkResponse[Seq[IndexSC]](response.body)
        assertEquals(index.size, updated.size)
//        println(index.stringify)
        for i <- index
        do
            val expected = i.elapsed_time_millis match
                case Some(et) => ts.plus(Duration.ofMillis(et))
                case None     => ts
            val obtained = i.recorded_timestamp.get
            assertEquals(obtained, expected)
    }

}
