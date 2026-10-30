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

import io.circe.Json
import io.circe.parser.parse
import org.mbari.annosaurus.controllers.{LocalizationController, TestUtils}
import org.mbari.annosaurus.domain.{Conflict, LocalizationSC}
import org.mbari.annosaurus.etc.circe.CirceCodecs.given
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import sttp.model.StatusCode

import java.util.UUID
import scala.jdk.CollectionConverters.*

trait LocalizationEndpointsSuite extends EndpointsSuite:

    given JPADAOFactory          = daoFactory
    given jwtService: JwtService = new JwtService("mbari", "foo", "bar")
    lazy val controller          = new LocalizationController(daoFactory)
    lazy val endpoints           = new LocalizationEndpoints(controller)

    private def jwt: Option[String] = Some(jwtService.authorize("foo").getOrElse(fail("no jwt")))

    private def newObservationUuid(): UUID =
        TestUtils.create(1, 1).head.getObservations.iterator().next().getUuid

    private val bbox =
        """{"localization_type":"bounding_box","x":0.1,"y":0.1,"width":0.2,"height":0.2,"generator":"test"}"""

    private def putBbox(obsUuid: UUID, body: String = bbox): LocalizationSC =
        var out: Option[LocalizationSC] = None
        runPut(
            endpoints.upsertLocalizationImpl,
            s"/v1/localizations/$obsUuid/bounding_box",
            body,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                out = Some(checkResponse[LocalizationSC](response.body))
            ,
            jwt
        )
        out.get

    private def putMask(obsUuid: UUID, data: String, w: Int, h: Int): LocalizationSC =
        val body = Json
            .obj(
                "localization_type"  -> Json.fromString("mask"),
                "mask_encoding"      -> Json.fromString("coco_rle"),
                "mask_data"          -> Json.fromString(data),
                "mask_width_pixels"  -> Json.fromInt(w),
                "mask_height_pixels" -> Json.fromInt(h)
            )
            .noSpaces
        var out: Option[LocalizationSC] = None
        runPut(
            endpoints.upsertLocalizationImpl,
            s"/v1/localizations/$obsUuid/mask",
            body,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                out = Some(checkResponse[LocalizationSC](response.body))
            ,
            jwt
        )
        out.get

    private def assertStoredMask(created: LocalizationSC, data: String, w: Int, h: Int): Unit =
        assertEquals(created.mask_encoding, Some("coco_rle"))
        assertEquals(created.mask_data, Some(data))
        assertEquals(created.mask_width_pixels, Some(w))
        assertEquals(created.mask_height_pixels, Some(h))
        runGet(
            endpoints.findLocalizationByUuidImpl,
            s"/v1/localizations/${created.uuid.get}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                assertEquals(checkResponse[LocalizationSC](response.body), created)
        )

    test("upsertLocalization of a mask derives the extent from the RLE and round-trips the mask fields") {
        // 6 wide x 4 high, a[1:3, 2:5] = 1; pycocotools 2.0.11 counts b"9220003", pixel box (2, 1, 5, 3)
        val created = putMask(newObservationUuid(), "9220003", 6, 4)
        assertEquals(
            (created.x, created.y, created.width, created.height),
            (Some(2.0 / 6), Some(1.0 / 4), Some(3.0 / 6), Some(2.0 / 4))
        )
        assertStoredMask(created, "9220003", 6, 4)
    }

    test("upsertLocalization of a mask with a long RLE string round-trips it unchanged") {
        assertEquals(LongMask.length, 6985)
        val created = putMask(newObservationUuid(), LongMask, 400, 300)
        assertEquals(
            (created.x, created.y, created.width, created.height),
            (Some(30.0 / 400), Some(20.0 / 300), Some(340.0 / 400), Some(260.0 / 300))
        )
        assertStoredMask(created, LongMask, 400, 300)
    }

    test("upsertLocalization of an all-background mask returns 400") {
        // a = np.zeros((4, 6)) -> b"h0"
        runPut(
            endpoints.upsertLocalizationImpl,
            s"/v1/localizations/${newObservationUuid()}/mask",
            """{"localization_type":"mask","mask_encoding":"coco_rle","mask_data":"h0","mask_width_pixels":6,"mask_height_pixels":4}""",
            response => assertEquals(response.code, StatusCode.BadRequest),
            jwt
        )
    }

    test("upsertLocalization creates and findByObservationUuid returns it") {
        val obsUuid = newObservationUuid()
        val created = putBbox(obsUuid)
        assert(created.uuid.isDefined)
        assertEquals(created.observation_uuid, Some(obsUuid))
        runGet(
            endpoints.findLocalizationsByObservationUuidImpl,
            s"/v1/localizations/observation/$obsUuid",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val xs = checkResponse[Seq[LocalizationSC]](response.body)
                assertEquals(xs.size, 1)
                assertEquals(xs.head.uuid, created.uuid)
        )
    }

    test("findLocalizationByUuid and findLocalizationsByVideoReferenceUuid") {
        val im      = TestUtils.create(1, 1).head
        val obsUuid = im.getObservations.iterator().next().getUuid
        val created = putBbox(obsUuid)
        runGet(
            endpoints.findLocalizationByUuidImpl,
            s"/v1/localizations/${created.uuid.get}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                assertEquals(checkResponse[LocalizationSC](response.body), created)
        )
        runGet(
            endpoints.findLocalizationsByVideoReferenceUuidImpl,
            s"/v1/localizations/videoreference/${im.getVideoReferenceUuid}?limit=10&offset=0",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val xs = checkResponse[Seq[LocalizationSC]](response.body)
                assertEquals(xs.map(_.uuid), Seq(created.uuid))
        )
    }

    test("upsertLocalization with a stale last_updated_time returns 409 and leaves the row unchanged") {
        val obsUuid = newObservationUuid()
        val created = putBbox(obsUuid)
        val stale   = """{"localization_type":"bounding_box","x":0.5,"y":0.5,"width":0.1,"height":0.1,""" +
            """"last_updated_time":"2001-01-01T00:00:00Z"}"""
        runPut(
            endpoints.upsertLocalizationImpl,
            s"/v1/localizations/$obsUuid/bounding_box",
            stale,
            response =>
                assertEquals(response.code, StatusCode.Conflict)
                val conflict = checkResponse[Conflict](Right(response.body.merge))
                assertEquals(conflict.current, Some(created))
            ,
            jwt
        )
        runGet(
            endpoints.findLocalizationByUuidImpl,
            s"/v1/localizations/${created.uuid.get}",
            response => assertEquals(checkResponse[LocalizationSC](response.body), created)
        )
    }

    test("upsertLocalization with the current last_updated_time updates the row") {
        val obsUuid = newObservationUuid()
        val created = putBbox(obsUuid)
        val body    = Json
            .obj(
                "localization_type" -> Json.fromString("bounding_box"),
                "x"                 -> Json.fromDoubleOrNull(0.3),
                "y"                 -> Json.fromDoubleOrNull(0.3),
                "width"             -> Json.fromDoubleOrNull(0.1),
                "height"            -> Json.fromDoubleOrNull(0.1),
                "last_updated_time" -> Json.fromString(created.last_updated_time.get.toString)
            )
            .noSpaces
        val updated = putBbox(obsUuid, body)
        assertEquals(updated.uuid, created.uuid)
        assertEquals(updated.x, Some(0.3))
    }

    // ---- I1: the token under either spelling --------------------------------------------------------

    private def putStatus(obsUuid: UUID, tpe: String, body: String): (StatusCode, String) =
        var out: (StatusCode, String) = (StatusCode.InternalServerError, "")
        runPut(
            endpoints.upsertLocalizationImpl,
            s"/v1/localizations/$obsUuid/$tpe",
            body,
            response => out = (response.code, response.body.merge),
            jwt
        )
        out

    private def getLocalization(uuid: UUID): LocalizationSC =
        var out: Option[LocalizationSC] = None
        runGet(
            endpoints.findLocalizationByUuidImpl,
            s"/v1/localizations/$uuid",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                out = Some(checkResponse[LocalizationSC](response.body))
        )
        out.get

    private def bboxWithToken(name: String, token: String, x: Double = 0.5): String =
        s"""{"localization_type":"bounding_box","x":$x,"y":0.5,"width":0.1,"height":0.1,"$name":"$token"}"""

    test("upsertLocalization with a stale last_updated_timestamp (spec name) returns 409 and leaves the row unchanged") {
        val obsUuid = newObservationUuid()
        val created = putBbox(obsUuid)
        val (code, body) = putStatus(obsUuid, "bounding_box", bboxWithToken("last_updated_timestamp", "2001-01-01T00:00:00Z"))
        assertEquals(code, StatusCode.Conflict, body)
        assertEquals(checkResponse[Conflict](Right(body)).current, Some(created))
        assertEquals(getLocalization(created.uuid.get), created)
    }

    test("upsertLocalization with a stale last_updated_time still returns 409") {
        val obsUuid = newObservationUuid()
        val created = putBbox(obsUuid)
        val (code, body) = putStatus(obsUuid, "bounding_box", bboxWithToken("last_updated_time", "2001-01-01T00:00:00Z"))
        assertEquals(code, StatusCode.Conflict, body)
        assertEquals(getLocalization(created.uuid.get), created)
    }

    test("upsertLocalization with the current token under either name succeeds") {
        val obsUuid = newObservationUuid()
        val created = putBbox(obsUuid)
        val (c1, b1) = putStatus(obsUuid, "bounding_box", bboxWithToken("last_updated_timestamp", created.last_updated_time.get.toString, 0.3))
        assertEquals(c1, StatusCode.Ok, b1)
        val u1 = checkResponse[LocalizationSC](Right(b1))
        assertEquals(u1.x, Some(0.3))
        val (c2, b2) = putStatus(obsUuid, "bounding_box", bboxWithToken("last_updated_time", u1.last_updated_time.get.toString, 0.4))
        assertEquals(c2, StatusCode.Ok, b2)
        assertEquals(checkResponse[LocalizationSC](Right(b2)).x, Some(0.4))
    }

    test("when both token names are sent, last_updated_timestamp wins") {
        val obsUuid = newObservationUuid()
        val created = putBbox(obsUuid)
        val current = created.last_updated_time.get.toString
        // spec name stale, other name current -> 409
        val stale   = s"""{"localization_type":"bounding_box","x":0.5,"y":0.5,"width":0.1,"height":0.1,""" +
            s""""last_updated_time":"$current","last_updated_timestamp":"2001-01-01T00:00:00Z"}"""
        assertEquals(putStatus(obsUuid, "bounding_box", stale)._1, StatusCode.Conflict)
        // spec name current, other name stale -> 200
        val fresh   = s"""{"localization_type":"bounding_box","x":0.5,"y":0.5,"width":0.1,"height":0.1,""" +
            s""""last_updated_time":"2001-01-01T00:00:00Z","last_updated_timestamp":"$current"}"""
        assertEquals(putStatus(obsUuid, "bounding_box", fresh)._1, StatusCode.Ok)
    }

    // ---- I3: tokens returned by writes stay usable (independent of the host clock's precision) --------

    test("the token returned by each write is accepted by the next write, and matches a read back") {
        val obsUuid = newObservationUuid()
        var current = putBbox(obsUuid)
        for i <- 1 to 3 do
            val stored = getLocalization(current.uuid.get)
            val diff   = java.time.Duration.between(current.last_updated_time.get, stored.last_updated_time.get).abs
            assert(diff.compareTo(java.time.Duration.ofMillis(1)) < 0, s"write token ${current.last_updated_time} vs stored ${stored.last_updated_time}")
            val (code, body) =
                putStatus(obsUuid, "bounding_box", bboxWithToken("last_updated_timestamp", current.last_updated_time.get.toString, 0.1 * i))
            assertEquals(code, StatusCode.Ok, body)
            current = checkResponse[LocalizationSC](Right(body))
    }

    // ---- I4: mask fields ignored on non-mask types; over-length text is a 400 -------------------------

    test("upsertLocalization of a bounding box with mask fields returns 200 and stores no mask fields") {
        val obsUuid = newObservationUuid()
        val body    = """{"localization_type":"bounding_box","x":0.1,"y":0.1,"width":0.2,"height":0.2,""" +
            """"mask_encoding":"coco_rle","mask_data":"9220003","mask_width_pixels":6,"mask_height_pixels":4}"""
        val (code, out) = putStatus(obsUuid, "bounding_box", body)
        assertEquals(code, StatusCode.Ok, out)
        val created = checkResponse[LocalizationSC](Right(out))
        val stored  = getLocalization(created.uuid.get)
        assertEquals(
            (stored.mask_encoding, stored.mask_data, stored.mask_width_pixels, stored.mask_height_pixels),
            (None, None, None, None)
        )
        assert(!parse(out).toOption.get.noSpaces.contains("mask_"), out)
    }

    test("upsertLocalization with a 129-character generator returns 400, not 500") {
        val obsUuid = newObservationUuid()
        val body    = s"""{"localization_type":"bounding_box","x":0.1,"y":0.1,"width":0.2,"height":0.2,"generator":"${"g" * 129}"}"""
        val (code, out) = putStatus(obsUuid, "bounding_box", body)
        assertEquals(code, StatusCode.BadRequest, out)
        assert(out.contains("generator"), out)
    }

    test("upsertLocalization with a 1025-character comment returns 400") {
        val obsUuid = newObservationUuid()
        val body    = s"""{"localization_type":"point","x":0.1,"y":0.1,"comment":"${"c" * 1025}"}"""
        assertEquals(putStatus(obsUuid, "point", body)._1, StatusCode.BadRequest)
    }

    test("bulkUpsertLocalizations with one over-length item returns 400 naming its index and writes nothing") {
        val a    = newObservationUuid()
        val b    = newObservationUuid()
        val body =
            s"""[{"observation_uuid":"$a","localization_type":"point","x":0.5,"y":0.5},
               |{"observation_uuid":"$b","localization_type":"point","x":0.5,"y":0.5,"verifier":"${"v" * 129}"}]""".stripMargin
        runPost(
            endpoints.bulkUpsertLocalizationsImpl,
            "/v1/localizations/bulk",
            body,
            response =>
                assertEquals(response.code, StatusCode.BadRequest)
                assert(response.body.merge.contains("item 1"), response.body.merge)
                assert(response.body.merge.contains("verifier"), response.body.merge)
            ,
            jwt
        )
        for o <- Seq(a, b) do
            runGet(
                endpoints.findLocalizationsByObservationUuidImpl,
                s"/v1/localizations/observation/$o",
                response => assertEquals(checkResponse[Seq[LocalizationSC]](response.body), Seq.empty)
            )
    }

    // ---- I8: closed polygons are stored open --------------------------------------------------------

    test("upsertLocalization of an explicitly closed polygon returns the open polygon") {
        val obsUuid = newObservationUuid()
        val body    = """{"localization_type":"polygon","vertices":[0.1,0.1,0.5,0.1,0.5,0.5,0.1,0.1]}"""
        val (code, out) = putStatus(obsUuid, "polygon", body)
        assertEquals(code, StatusCode.Ok, out)
        val created = checkResponse[LocalizationSC](Right(out))
        assertEquals(created.vertices, Some(Seq(0.1, 0.1, 0.5, 0.1, 0.5, 0.5)))
        assertEquals(getLocalization(created.uuid.get).vertices, Some(Seq(0.1, 0.1, 0.5, 0.1, 0.5, 0.5)))
    }

    // ---- I6: paging of findLocalizationsByVideoReferenceUuid ----------------------------------------

    test("findLocalizationsByVideoReferenceUuid pages deterministically and clamps a huge limit") {
        val im  = TestUtils.create(1, 5).head
        val obs = im.getObservations.iterator().asScala.toSeq
        obs.foreach(o => putBbox(o.getUuid))
        def page(query: String): Seq[LocalizationSC] =
            var out: Seq[LocalizationSC] = Nil
            runGet(
                endpoints.findLocalizationsByVideoReferenceUuidImpl,
                s"/v1/localizations/videoreference/${im.getVideoReferenceUuid}$query",
                response =>
                    assertEquals(response.code, StatusCode.Ok, response.body.toString)
                    out = checkResponse[Seq[LocalizationSC]](response.body)
            )
            out
        val all = page("")
        assertEquals(all.size, 5)
        assertEquals(page("?limit=20000").map(_.uuid), all.map(_.uuid))
        assertEquals(page("?limit=2&offset=1").map(_.uuid), all.slice(1, 3).map(_.uuid))
        assertEquals(page("?limit=2&offset=1").map(_.uuid), all.slice(1, 3).map(_.uuid))
        assertEquals(page("?limit=10&offset=4").map(_.uuid), all.slice(4, 5).map(_.uuid))
    }

    test("upsertLocalization without a token returns 401") {
        runPut(
            endpoints.upsertLocalizationImpl,
            s"/v1/localizations/${UUID.randomUUID()}/bounding_box",
            bbox,
            response => assertEquals(response.code, StatusCode.Unauthorized)
        )
    }

    test("upsertLocalization with an invalid bounding box returns 400") {
        val obsUuid = newObservationUuid()
        runPut(
            endpoints.upsertLocalizationImpl,
            s"/v1/localizations/$obsUuid/bounding_box",
            """{"localization_type":"bounding_box","x":0.9,"y":0.1,"width":0.5,"height":0.2}""",
            response => assertEquals(response.code, StatusCode.BadRequest),
            jwt
        )
    }

    test("upsertLocalization for an unknown observation returns 404") {
        runPut(
            endpoints.upsertLocalizationImpl,
            s"/v1/localizations/${UUID.randomUUID()}/bounding_box",
            bbox,
            response => assertEquals(response.code, StatusCode.NotFound),
            jwt
        )
    }

    test("deleteLocalization removes it, then findByUuid returns 404") {
        val obsUuid = newObservationUuid()
        val created = putBbox(obsUuid)
        runDelete(
            endpoints.deleteLocalizationImpl,
            s"/v1/localizations/$obsUuid/bounding_box",
            response => assertEquals(response.code, StatusCode.Ok),
            jwt
        )
        runGet(
            endpoints.findLocalizationByUuidImpl,
            s"/v1/localizations/${created.uuid.get}",
            response => assertEquals(response.code, StatusCode.NotFound)
        )
        runDelete(
            endpoints.deleteLocalizationImpl,
            s"/v1/localizations/$obsUuid/bounding_box",
            response => assertEquals(response.code, StatusCode.NotFound),
            jwt
        )
    }

    test("deleteLocalization without a token returns 401") {
        runDelete(
            endpoints.deleteLocalizationImpl,
            s"/v1/localizations/${UUID.randomUUID()}/bounding_box",
            response => assertEquals(response.code, StatusCode.Unauthorized)
        )
    }

    test("bulkUpsertLocalizations succeeds for valid items") {
        val a    = newObservationUuid()
        val b    = newObservationUuid()
        val body =
            s"""[{"observation_uuid":"$a","localization_type":"point","x":0.5,"y":0.5},
               |{"observation_uuid":"$b","localization_type":"bounding_box","x":0.1,"y":0.1,"width":0.2,"height":0.2}]""".stripMargin
        runPost(
            endpoints.bulkUpsertLocalizationsImpl,
            "/v1/localizations/bulk",
            body,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                assertEquals(checkResponse[Seq[LocalizationSC]](response.body).size, 2)
            ,
            jwt
        )
    }

    test("bulkUpsertLocalizations with one invalid item returns 400 and writes nothing") {
        val a    = newObservationUuid()
        val b    = newObservationUuid()
        val body =
            s"""[{"observation_uuid":"$a","localization_type":"point","x":0.5,"y":0.5},
               |{"observation_uuid":"$b","localization_type":"bounding_box","x":0.9,"y":0.1,"width":0.5,"height":0.2}]""".stripMargin
        runPost(
            endpoints.bulkUpsertLocalizationsImpl,
            "/v1/localizations/bulk",
            body,
            response =>
                assertEquals(response.code, StatusCode.BadRequest)
                assert(response.body.merge.contains("item 1"), response.body.merge)
            ,
            jwt
        )
        runGet(
            endpoints.findLocalizationsByObservationUuidImpl,
            s"/v1/localizations/observation/$a",
            response => assertEquals(checkResponse[Seq[LocalizationSC]](response.body), Seq.empty)
        )
    }

    test("bulkUpsertLocalizations without a token returns 401") {
        runPost(
            endpoints.bulkUpsertLocalizationsImpl,
            "/v1/localizations/bulk",
            "[]",
            response => assertEquals(response.code, StatusCode.Unauthorized)
        )
    }

    test("list/types returns the five types in spec order") {
        runGet(
            endpoints.listLocalizationTypesImpl,
            "/v1/localizations/list/types",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                assertEquals(
                    checkResponse[Seq[String]](response.body),
                    Seq("bounding_box", "point", "line", "polygon", "mask")
                )
        )
    }

    test("list/maskencodings returns the mask encodings") {
        runGet(
            endpoints.listMaskEncodingsImpl,
            "/v1/localizations/list/maskencodings",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                assertEquals(checkResponse[Seq[String]](response.body), Seq("coco_rle"))
        )
    }

    test("list/constraints returns both lists") {
        runGet(
            endpoints.listLocalizationConstraintsImpl,
            "/v1/localizations/list/constraints",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val json = parse(response.body.getOrElse(fail("no body"))).toOption.get
                assertEquals(
                    json.hcursor.get[Seq[String]]("localization_types"),
                    Right(Seq("bounding_box", "point", "line", "polygon", "mask"))
                )
                assertEquals(json.hcursor.get[Seq[String]]("mask_encodings"), Right(Seq("coco_rle")))
        )
    }

    /**
     * 400 wide x 300 high sparse noise, generated with pycocotools 2.0.11 / numpy 2.5.4:
     * {{{
     * rng = np.random.default_rng(13)
     * a = np.zeros((300, 400)); a[20:280, 30:370] = (rng.random((260, 340)) < 0.03)
     * M.encode(np.asfortranarray(a.astype(np.uint8)))["counts"]  # 6985 chars, pixel box (30, 20, 370, 280)
     * }}}
     */
    private val LongMask: String =
        """ni8120;0l10QN0^30mL0[10TN030S20QN0<0H040B0m40QK0]10F0U10lM080_40dK020h00RO0:0X30`L0e00oN05050n00H0[O
          |0B0i20]M0L020l00^10jN0D0TO0Q30cM0i00m40_J0U10:0lN0ZO0E000Z20QN0<0B0R40mN0oL0f10ZN0h00J0TO0j30SM0K0XO
          |0b10A090lN0W40SL0R10mN040E0g00M0\O1l1O\N0S20gN0A0Q20TM010>0X10WO0\O0n00TO0?0F0m00YO0Z10oM0V10QO0H010
          |a00J0H0>0_O0Q20]N0g00N0]O0B0L060l00Z10SO0J0kN0f10bN18O00A0j01EO_30kL0b10fN0QO0P30bN0dN0:0YO0^10H0lN0
          |d00X30\L0Z10S40kJ0C0T90hG0L0g40eJ0G0X10n00ZO0]O0]20iL0]10D0_O0K0;0^30SL0H1]50VKOC0b30[O0o10aK0e30SL0
          |60d20SM0f00TO02090U10eN0000050X30kN0kM0n10e10ZL0J0c00<0]O0J0e00RO0S10J0SO0d10QO0A0M0?0B041LO20a00b00
          |TO0?0J0^O0V20VN0A0a10T10QN0G0h30aL0]O0X10VO011COP20I0jN060e10bM0k10[N0[10QO0=0:0nN0R20iM0d00UO0n10VN
          |0?0P10cN0M0=0I060c0040nN0M0T20oM0407090=0QO0?020E0n20^M0\O0K0T10K0`20gL080l00VO0Y40mK0_O0<0<0b10A0YN
          |0l0000X30eK0[10_20`L0n10mM0:000K0m00>0ZN0W10hN040e20jM0=0;0QO0f00XO0a10lN0V50bK0cN0`10dN0f10G0R10g10
          |fK060]10fN0Z10ZO0J1BOJ090c10@0G000VO0=0A0E0U10Z10TO0nN0@0Q20QO0L0X20eM0E0^O060;000>0O0@0B0f00j10lM0E
          |0o10[N0=0M0N080n20[L0T10QO0i10bN010K0P20[N0[O080e10kN0[O090N0h20`N0U10gN0]N0Y40[O0YM0[O0L0N0`10aN0f0
          |0:0oN02000b20^M030J010P70XI0b10nN0n30^K0g11ZNOP10k00bN0[O0n20eM1GOo10`00jM0^10YN0F21Nj30`M0N0QO0_10G
          |0[O0f00gN1f2OVM0j10hN0O0o00RO0R10hN011OOI0f00WO0K0k00F030_O0b00ZO0<030i20mL0o20`1040^K0R10Y10WN0W10R
          |O0ZO0V10?0b00]N0QO020b00R30TM0h30eK0M040T20\N0^10K0ZN0O031_1O]N020T30ZM0E1:O20G0B0X10PO0l00nN010J020
          |_20`M0c10iN0E1Q1Oa10m00bL010b00A0<1>OSO0h20i00fL0I0k00J0J0L0<0e30bK010j00D0i10^20]K0d00C0f10aN0m00bN
          |040o10RN0J0d00G0k00XO0A0105020b10hN0_O0l20UO0D0hN040J0l20_N0_N0h10WO040\20aL0O0L0g10`N0`00l00]N0T10b
          |10@0iM080]10R30XN0XM0J000a00f00]O0_O0\50eJ01050M0?0C0O0h00B0a10ZN0B0g30RM0]30nK0k00K0^31kKO`00g1080^
          |O0XO0YO0\10fN0_O0^10hN0o10\O0XO1]O0S1O@0A0g00[O0C070l00RO0L0`0060\O0m00g00oN0H0M0c00QO0Y20mM0K0l00_O
          |0;0V10^N1JOM0`00S10WO010VO0c00f20PN0eN0500010X20kM0Q50WL0F0TO0k00d20nL0S10VN0?090ZO030O0J1;O=0Z40PK0
          |Y10ZO1S1OaN070[10kN0@17OM0e00H0]O0N0j00o00_N0i10kN0J0g00hN1k0O[O0M0a30WM0B0l00k00hM040m20mM0V10jM0i0
          |0_70`J0ZN0n30iM0]M0e20TM0<0<0<0_O0]30WL0\10SO0_O050I020i00UO060f00QO0c00G0L0K0k30PM0`10jM0<13O_O0]40
          |RL0k00b00SN0O090O0=0I0[10oN0^10nM070I0J0T20QN060H0?0h10TO0V11^2OYK0G0;0g00\O0>0^50PL0jM0U10C0ZO16OC0
          |\20cM0]10dN0Y20WN060i20bL0Y10QO0G030`10j70eF0\10hN0:0`50iJ0O0@0R30RO1aNOUO0L0;080^10L0YN050g00G060H0
          |K0Z10c00fM0?19O_O0L0K0>0B051;Om00[20XL0M000708030d10SO0UO0S10bN0]30VN0lN0;0XO0`20aM040>0D12OH0k20`M0
          |Z10hN0I0S10Z20YL0n20]M090]O0`00A0U12`0NXO0h20gL0Q40iM0]N0^O0f00_O0Q10Q2090_M0nN030`00I0i00Z20dL1i0OY
          |10hM0=1HO10h00]O0f00PO0c20hM0<060YO0P30oL0=0R20;0RM0Q30ZO0m00[M0K0?0D0M0a10ZN0c00[O0?0YO0:01050I1a2O
          |XM0e10^O0e00^O0C0m00gN0Q10\O0>0[O0E080<020L0T10YN0b10=0Y20UM0kN020X10TN0U10UO0N0Q30lL0V10P10hM0i00]O
          |0`00^O0n10a00_M0M0U30eM0k00lN0T10[N0g00`20RO1PNO80b0000XO0=0L0D0c00J0n10cM0?0ZO0305090@0m20XN0[10^M0
          |00W10TO0F0c10^N0k00TO0Z10kN0[20VN0<0RO0`10[N0P10S31eLOc10oM0K0o10gN0f10O0SN0L0[10VN0007040E0V20^N0Q2
          |0eM0?0f00RO13Ob10WO0A0RO0b00H0G0j00S70lH0D0C000W20f10eM0\O0J0;0ZO0\50YJ0P3080nM0YO040P2010YM0V20^O09
          |0XN0j00h10mM0]O0o1050lM1S1Of10ZN0lN0c00T20^M0h30XL0Q10S20]N0WO0n00^N0c00\O0c10\N0O0M0m00XO0K020N040j
          |20XN0mN0a02[ON80Q10lN0d00WO0R10oN0f20\N0j10XM0O0:0D0Q10WO0Q20gM0h00b00VO0B0;0:0G0F0Z20RN0X10Y10jM0E0
          |A0L0f10a20jK0a10a20YL0>0^10S20kK0K0:0H0Y10nN06070[O0g00i00_N0W20F0VN0f10n00]M0d10G0[O0\O0`00`00RO040
          |V10P1000bN0XO0e00b30[L010G010]O0e00^O0H0`00o00bN0g00b00j00VN0h00]O0l30hN0PM1`6OgI0o10lM0m10;0SO0oN0a
          |11kNOX30QM0RO0`00W11oNOSO0\10iN0[10Q81aIO10mL0U10e00WN010e00n00bN0>0C0Y10YO0A080c10\O0gN0=0;060\20fL
          |0J0M0Y10PO020[10f00YN0Y20gM07020TO0P10VO0n00YO0B060h10H0bN0a00:0UO0H0Q1050S10TN0D0P10h10cM0I030Z10>0
          |TO0>0d10WM0K0d10h00lM0c00T10bN0n00YN020i00G030]O0k00Y10SN05040Z10lN090\O0b00L1n1OI0aM000<0g21PMOc30a
          |L0?0:0T11WNO00D0m00Y20gL0V20RN020e01e0OA0o10mM0XO0X30_L0m31TLO0031NO`00R10TO1KOh20[M0k10bM0n00>0n10e
          |L0g10iN0B0\10K0kN080H090S30jN0g00QM0g10\O0TO0m20VM0G010>0]O020:0e20SN000E0?0ZO0]O0n00P;0kF0XM0:0E0m0
          |0XO1m1Oi00]N0c10XN0m10gM0^O0\20[O0P20QM0F0S10mN090ZO0g10`N0T10lN0M0b00I0[O080b10^N0Q50lJ040:06070^O0
          |`00`00g00ZN0N0=070i20RO0[N0oN070m10ZO0bN0j20WO0iM03040K0e001080PO0[70RI0`00J0G0f0050?0mN0k00eN0L0R10
          |@0E0P30lM0mN0n00\O020X10mN0I0=0\O0;0P20YN0H030H0L0`00E0j0050gN0c10jN0l00E0ZO0K0?0F0k00>0]N070Z10ZO0E
          |0\30RM0g10dM0H0e00k00aN0X1050Y2080SL090a00YO0M0T10g00ZN0L0S30E0hM0G0h10`01]MOa006050]O0X10\10jM0e20R
          |N0Y40YK1b1O80mM030]O0c0080k10SM090\20RO0D0PO0a10]N0R40lK0?0[O0f00B010I0]10U10jM0\10WN0V10a40ZJ0[20SN
          |0k00M0iN0f00T20TM0901010=0O0[O0m0060^O0`20]M0X10XN0U10P10PN0L0b00o30RL0`10bN1FOm00c01nNOL0O0E070f00]
          |1040ZM0c00e00\O0c10RO0c00RN0?0K0e20bN0fN00050W40^L0PO000l00B020B0V10kN0e10`20aM0jN040^O0U10YO0V20aN0
          |kN0K080f70ZH061IO]10oN0k10WN0\O0U20^N0A020H00080>0^O000S20kM02020M0b20[M0Q10SO0V10iN0U10U40hJ060h00_
          |O0I0m31_LOn00^O010TO0H0R40_L0N0G0m00TO0U20eM0_30XM0G0N0f00YO0<080:0b42iKNUO0T30UM0PO0n00XO0=0b10J0XN
          |0c10PO0@0V10g20mL0R20V10\L0ZO0U10QO0d10;0^O0^O0oN0<000f10kM0603030l40ZM0WN0\O0O0h00UO0M0_30SN0aN0<1>
          |Ol50^J0=09020C0B0nN0W10YO0`00<0W10XO0`00SO0mN0g20SO0mM0S30_O0W10bL0=080c10aM0710Om00B0VO000O0]10VO0m
          |10jM0k10QO0ZO0i01T1OQN0X10kN080i1080ZO0m00g00mL0J020h00oN0Q10g20TL0:1HOK0<0Y10n00\M0]20W10]N0RN070o1
          |0kM0V10J0mN0n10VN0U10f20RL030V10UO0h00E0UO010U30aM0?0SO0H0h00@0k00:0RO0:0_10VN0J0f00nN0T10c20QM0W10n
          |N0M0^O0F0e60jI0d30PL000h00A0O0K0e00B0;0D0T20[O0UN080K0j00F0@0N0905010V51aJOR10XO0`0060lN0f31\LO60b00
          |B0Q10U50cI0N0`10_N0[10lN0n00lN0O0K0h10WO080^O0H000L0g00UO020M0l00QO0^30SM0I0Y10cN0K0e00k00`N0\30VO0X
          |O0WN0M0T10nN0o20PN0M020I0H0j00eN080K0a20cN020i00jN010U20oM0B0f20h00XL0`00\O0;0E0P20ZO0aN0=0[30ZL050M
          |0j0020QO0\10dN0[30VM0P20jM0b00G0b10E0\N0H0g00E0e00YO0l20UM0i20ZM0k00ZO0C0o10b20jK0@0U10kN040S10WO0`0
          |0o40mJ0e10_N0e00`N07030d00=0]O0C0j00S20oM0_O040m50iL0^L0_10dN0O080K0X10eN0`00g00SO0D0o00]O0e00QO0[10
          |bN080P10D0P10SN080?0_60^I0K050N0J0M0W50kJ0]20^N0\O0Y30UN0PN0e10fN0[10V20YM0L060kN071;OF04050a30YM1=O
          |YN050=0P20gM0^10^N0;0j00SO0C0d40`M0kM0Y10n20UL0;0d00E0SO0N010a00K0C0R10[10A0lN0UO0c00E1S1OG0l00QO0Q1
          |0SO0V10RN0L0l10dN0XO0a00I0<0:090ZO0_O0n00=0dN0m21UMON0N0b00d10PN0M0^20TO0dN020=0A0W10eN1a3OcN0UN091[
          |OO<0B0:05070k20b10XL0_O0a10]O0iN0Q10hN0=0T10UN0m30UL0X10QO0_10^O1?O[O0M0A0G0n01:O_O0S20TM0205050A0_1
          |1POOa00g00cN0S20QN0X20nM0b10nM0l10UN030e00VO0j00<0V10A0RN0L000400040Q10d20bL0N0H0K0Z21;OgM0c10YN0i00
          |S10_N0<0WO0Q10UO0j00T30oL0oN0d00^O0Q10jN0L030\10D0d00XN0e1050k10^N0_O0kN0R10G0Z30jK0J07040A030a00B0U
          |10b10YO0oM090_10?0J0e00oM0_O0a10hN0K010D0k00D0P20UN0X10dN0:020m20\N0YN0D0l10h10]L000]1030mN0c00`20nL
          |0j003040\N0f00J0Y50ZK0\O1a1O`N0h10X20jL0YO0L0E0P20[O0UO0W10]O0WO0C0l20ZM1h1OYN030M0`00S40[K040F0=0E0
          |U10_O0:0WO0d00\O050O0M0b20jM0]O0a00`00UO060m00E0_10_N0YO0]10_N0f00S10YO0\1030RN0_O020L0g20PO0^N0>0\O
          |0?0f10iN0e01c0ON0RN0U10A0B0c00YO0N0K0O0]20aM0o00QO0a00C07090C000G050P10^11YOOPO0d00_O0QO090i00G0^10k
          |M050?0N0I0N0K0M0Q10[O0A0m00L0S2041fMO\O040`11ROO^10ZN0W10_N0M000M0a00B0S10ZO0[50ZJ0f00\O0`10eN1i00m0
          |OSO0I0O0I0a00g0070kN0WO0S10QO0K0L0j60QK0\N0R10Q40RK0W10lN060B0h50UJ080H0T20^N1FOI070l10]O0iN0j0030=0
          |O0]N0f00]10PN0^10YN06070Y50QL0YN0:020=0XO0702010d20SM0f10l10G0VM0A0c0020=0b00G0nN0bi8""".stripMargin.replace("\n", "")
