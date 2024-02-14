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

import org.mbari.annosaurus.controllers.{CachedVideoReferenceInfoController, TestUtils}
import org.mbari.annosaurus.domain.{
    CachedVideoReferenceInfo,
    CachedVideoReferenceInfoCreateSC,
    CachedVideoReferenceInfoSC,
    CachedVideoReferenceInfoUpdateSC
}
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.etc.jdk.Instants
import org.mbari.annosaurus.etc.jdk.Logging.{*, given}
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.etc.sdk.Futures.*
import org.mbari.annosaurus.etc.sdk.Reflect
import org.mbari.annosaurus.repository.jpa.JPADAOFactory

import scala.jdk.CollectionConverters.*
import sttp.client3.*
import sttp.model.StatusCode

import java.util.UUID

trait CachedVideoReferenceInfoEndpointsSuite extends EndpointsSuite {

    private val log              = System.getLogger(getClass.getName)
    given JPADAOFactory          = daoFactory
    given jwtService: JwtService = new JwtService("mbari", "foo", "bar")
    lazy val controller          = new CachedVideoReferenceInfoController(daoFactory)
    lazy val endpoints           = new CachedVideoReferenceInfoEndpoints(controller)

    def createVideoReferenceInfo(): CachedVideoReferenceInfo = {
        val x = TestUtils.randomVideoReferenceInfo()
        controller
            .create(
                x.getVideoReferenceUuid,
                x.getPlatformName,
                x.getMissionId,
                Option(x.getMissionContact)
            )
            .join
    }

    test("findAll") {
        val xs = (0 until 3).map(_ => createVideoReferenceInfo())
        runGet(
            endpoints.findAllImpl,
            "http://test.com/v1/videoreferences",
            response => {
                assert(response.code == StatusCode.Ok)
                val ys =
                    checkResponse[Seq[CachedVideoReferenceInfoSC]](response.body).map(_.toCamelCase)
                for x <- xs
                do
                    ys.find(_.videoReferenceUuid == x.videoReferenceUuid) match
                        case Some(y) =>
                            assertEquals(y, x)
                        case None    =>
                            fail(s"Could not find ${x.videoReferenceUuid}")
            }
        )
    }

    test("findAllVideoReferenceUuids") {
        val xs = (0 until 3).map(_ => createVideoReferenceInfo())
        runGet(
            endpoints.findAllVideoReferenceUuidsImpl,
            "http://test.com/v1/videoreferences/videoreferences",
            response => {
                assert(response.code == StatusCode.Ok)
                val ys = checkResponse[Seq[UUID]](response.body)
                for x <- xs
                do assert(ys.contains(x.videoReferenceUuid))
            }
        )
    }

    test("findByUuid") {
        val x = createVideoReferenceInfo()
        runGet(
            endpoints.findByUuidImpl,
            s"http://test.com/v1/videoreferences/${x.uuid}",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val y = checkResponse[CachedVideoReferenceInfoSC](response.body).toCamelCase
                assertEquals(y, x)
            }
        )
    }

    test("findByVideoReferenceUuid") {
        val x = createVideoReferenceInfo()
        runGet(
            endpoints.findByVideoReferenceUuidImpl,
            s"http://test.com/v1/videoreferences/videoreference/${x.videoReferenceUuid}",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val y = checkResponse[CachedVideoReferenceInfoSC](response.body).toCamelCase
                assertEquals(y, x)
            }
        )
    }

    test("findAllMisstionIds") {
        val xs = (0 until 3).map(_ => createVideoReferenceInfo())
        runGet(
            endpoints.findAllMissionIdsImpl,
            "http://test.com/v1/videoreferences/missionids",
            response => {
                assert(response.code == StatusCode.Ok)
                val ys = checkResponse[Seq[String]](response.body)
                for x <- xs
                do assert(ys.contains(x.missionId.get))
            }
        )
    }

    test("findByMisstionId") {
        val x = createVideoReferenceInfo()
        runGet(
            endpoints.findByMissionIdImpl,
            s"http://test.com/v1/videoreferences/missionid/${x.missionId.get}",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val y =
                    checkResponse[Seq[CachedVideoReferenceInfoSC]](response.body).map(_.toCamelCase)
                assertEquals(y.size, 1)
                assertEquals(y.head, x)
            }
        )
    }

    test("findAllMissionContacts") {
        val xs = (0 until 3).map(_ => createVideoReferenceInfo())
        runGet(
            endpoints.findAllMissionContactsImpl,
            "http://test.com/v1/videoreferences/missioncontacts",
            response => {
                assert(response.code == StatusCode.Ok)
                val ys = checkResponse[Seq[String]](response.body)
                for x <- xs
                do assert(ys.contains(x.missionContact.get))
            }
        )
    }

    test("findByMissionContact") {
        val x = createVideoReferenceInfo()
        runGet(
            endpoints.findByMissionContactImpl,
            s"http://test.com/v1/videoreferences/missioncontact/${x.missionContact.get}",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val y =
                    checkResponse[Seq[CachedVideoReferenceInfoSC]](response.body).map(_.toCamelCase)
                assertEquals(y.size, 1)
                assertEquals(y.head, x)
            }
        )
    }

    test("createOneVideoReferenceInfo (json)") {
        val x         = TestUtils.randomVideoReferenceInfo()
        val c         = CachedVideoReferenceInfoCreateSC(
            x.getVideoReferenceUuid,
            x.getPlatformName,
            x.getMissionId,
            Option(x.getMissionContact)
        )
        val jwt       = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backend   = newBackendStub(endpoints.createOneVideoReferenceInfoImpl)
        val responese = basicRequest
            .post(uri"http://test.com/v1/videoreferences")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/json")
            .body(c.stringify)
            .send(backend)
            .join
        assertEquals(responese.code, StatusCode.Ok)
        val y         = checkResponse[CachedVideoReferenceInfoSC](responese.body).toCamelCase
        assertEquals(y.videoReferenceUuid, x.getVideoReferenceUuid)
        assertEquals(y.platformName.orNull, x.getPlatformName)
        assertEquals(y.missionId.orNull, x.getMissionId)
        assertEquals(y.missionContact.orNull, x.getMissionContact)

        controller.findByUUID(y.uuid).join match
            case Some(z) =>
                assertEquals(z, y)
            case None    =>
                fail(s"Could not find ${y.uuid}")

    }

    test("createOneVideoReferenceInfo (form)") {
        val x         = TestUtils.randomVideoReferenceInfo()
        val c         = CachedVideoReferenceInfoCreateSC(
            x.getVideoReferenceUuid,
            x.getPlatformName,
            x.getMissionId,
            Option(x.getMissionContact)
        )
        val jwt       = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backend   = newBackendStub(endpoints.createOneVideoReferenceInfoImpl)
        val responese = basicRequest
            .post(uri"http://test.com/v1/videoreferences")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(Reflect.toFormBody(c))
            .send(backend)
            .join
        assertEquals(responese.code, StatusCode.Ok)
        val y         = checkResponse[CachedVideoReferenceInfoSC](responese.body).toCamelCase
        assertEquals(y.videoReferenceUuid, x.getVideoReferenceUuid)
        assertEquals(y.platformName.orNull, x.getPlatformName)
        assertEquals(y.missionId.orNull, x.getMissionId)
        assertEquals(y.missionContact.orNull, x.getMissionContact)

        controller.findByUUID(y.uuid).join match
            case Some(z) =>
                assertEquals(z, y)
            case None    =>
                fail(s"Could not find ${y.uuid}")

    }

    test("updateOneVideoReferenceInfo (json)") {
        val x         = createVideoReferenceInfo()
        val u         = CachedVideoReferenceInfoUpdateSC(
            Some(UUID.randomUUID()),
            Some("newPlatformName"),
            Some("newMissionId"),
            Some("newMissionContact")
        )
        val jwt       = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backend   = newBackendStub(endpoints.updateOneVideoReferenceInfoImpl)
        val responese = basicRequest
            .put(uri"http://test.com/v1/videoreferences/${x.uuid}")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/json")
            .body(u.stringify)
            .send(backend)
            .join
        assertEquals(responese.code, StatusCode.Ok)
        val y         = checkResponse[CachedVideoReferenceInfoSC](responese.body).toCamelCase
        assertEquals(y.videoReferenceUuid, u.video_reference_uuid.orNull)
        assertEquals(y.platformName.orNull, u.platform_name.orNull)
        assertEquals(y.missionId.orNull, u.mission_id.orNull)
        assertEquals(y.missionContact.orNull, u.mission_contact.orNull)

        controller.findByUUID(y.uuid).join match
            case Some(z) =>
                val obtained = z.copy(lastUpdated = None)
                val expected = y.copy(lastUpdated = None)
                assertEquals(obtained, expected)
            case None    =>
                fail(s"Could not find ${y.uuid}")
    }

    test("updateOneVideoReferenceInfo (form)") {
        val x         = createVideoReferenceInfo()
        val u         = CachedVideoReferenceInfoUpdateSC(
            Some(UUID.randomUUID()),
            Some("newPlatformName2"),
            Some("newMissionId2"),
            Some("newMissionContact2")
        )
        val jwt       = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backend   = newBackendStub(endpoints.updateOneVideoReferenceInfoImpl)
        val responese = basicRequest
            .put(uri"http://test.com/v1/videoreferences/${x.uuid}")
            .header("Authorization", s"Bearer $jwt")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(Reflect.toFormBody(u))
            .send(backend)
            .join
        assertEquals(responese.code, StatusCode.Ok)
        val y         = checkResponse[CachedVideoReferenceInfoSC](responese.body).toCamelCase
        assertEquals(y.videoReferenceUuid, u.video_reference_uuid.orNull)
        assertEquals(y.platformName.orNull, u.platform_name.orNull)
        assertEquals(y.missionId.orNull, u.mission_id.orNull)
        assertEquals(y.missionContact.orNull, u.mission_contact.orNull)

        controller.findByUUID(y.uuid).join match
            case Some(z) =>
                val obtained = z.copy(lastUpdated = None)
                val expected = y.copy(lastUpdated = None)
                assertEquals(obtained, expected)
            case None    =>
                fail(s"Could not find ${y.uuid}")
    }

    test("deleteOneVideoReferenceInfo") {
        val x         = createVideoReferenceInfo()
        val jwt       = jwtService.authorize("foo").orNull
        assert(jwt != null)
        val backend   = newBackendStub(endpoints.deleteOneVideoReferenceInfoImpl)
        val responese = basicRequest
            .delete(uri"http://test.com/v1/videoreferences/${x.uuid}")
            .header("Authorization", s"Bearer $jwt")
            .send(backend)
            .join
        assertEquals(responese.code, StatusCode.NoContent)
        controller.findByUUID(x.uuid).join match
            case Some(_) =>
                fail(s"Found ${x.uuid} after delete")
            case None    =>
            // pass
    }

}
