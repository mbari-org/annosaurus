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

package org.mbari.annosaurus.domain

import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.repository.jpa.entity.ObservationEntity

import java.time.Instant
import java.util.UUID

class LocalizationSuite extends munit.FunSuite:

    test("LocalizationType.fromValue"):
        assertEquals(LocalizationType.fromValue("bounding_box"), Right(LocalizationType.BoundingBox))
        assert(LocalizationType.fromValue("circle").isLeft)

    test("camelCase/snake_case round trip"):
        val cc1 = Localization(
            localizationType = "bounding_box",
            x = Some(0.1),
            y = Some(0.2),
            width = Some(0.3),
            height = Some(0.4),
            vertices = Some(Seq(0.1, 0.2, 0.3, 0.4)),
            maskEncoding = Some("coco_rle"),
            maskData = Some("abc"),
            maskWidthPixels = Some(10),
            maskHeightPixels = Some(20),
            generator = Some("yolo"),
            confidence = Some(0.9),
            observer = Some("brian"),
            observationTimestamp = Some(Instant.parse("2026-01-01T00:00:00Z")),
            verifier = Some("kwalz"),
            verificationTimestamp = Some(Instant.parse("2026-01-02T00:00:00Z")),
            comment = Some("c"),
            part = Some("tail"),
            training = true,
            truncated = true,
            difficult = true,
            uuid = Some(UUID.randomUUID()),
            observationUuid = Some(UUID.randomUUID()),
            lastUpdated = Some(Instant.parse("2026-01-03T00:00:00Z"))
        )
        val sc1 = cc1.toSnakeCase
        assertEquals(sc1.localization_type, "bounding_box")
        assertEquals(sc1.last_updated_time, cc1.lastUpdated)
        assertEquals(sc1.toCamelCase, cc1)

    test("decode snake_case JSON and re-encode without nulls"):
        val json = """{"localization_type":"point","x":0.5,"y":0.25,"verifier":"kwalz"}"""
        val loc  = json.reify[Localization].fold(e => fail(e.getMessage), identity)
        assertEquals(loc.localizationType, "point")
        assertEquals(loc.x, Some(0.5))
        assertEquals(loc.verifier, Some("kwalz"))
        val out  = loc.toSnakeCase.stringify
        assert(!out.contains("null"), out)
        assert(out.contains("localization_type"), out)

    test("vertices survive toEntity then from"):
        val loc = Localization(localizationType = "polygon", vertices = Some(Seq(0.1, 0.2, 0.3, 0.4)))
        val e   = loc.toEntity
        assertEquals(e.getVertices, "[0.1,0.2,0.3,0.4]")
        assertEquals(Localization.from(e).vertices, Some(Seq(0.1, 0.2, 0.3, 0.4)))

    test("fully populated Localization survives toEntity then from"):
        val observationUuid = UUID.randomUUID()
        val loc             = Localization(
            localizationType = "mask",
            x = Some(0.1),
            y = Some(0.2),
            width = Some(0.3),
            height = Some(0.4),
            vertices = Some(Seq(0.1, 0.2, 0.3, 0.4)),
            maskEncoding = Some("coco_rle"),
            maskData = Some("abc"),
            maskWidthPixels = Some(10),
            maskHeightPixels = Some(20),
            generator = Some("yolo"),
            confidence = Some(0.9),
            observer = Some("brian"),
            observationTimestamp = Some(Instant.parse("2026-01-01T00:00:00Z")),
            verifier = Some("kwalz"),
            verificationTimestamp = Some(Instant.parse("2026-01-02T00:00:00Z")),
            comment = Some("c"),
            part = Some("tail"),
            training = true,
            truncated = true,
            difficult = true,
            uuid = Some(UUID.randomUUID()),
            observationUuid = Some(observationUuid)
        )
        val e               = loc.toEntity
        // toEntity cannot build the parent observation; attach one as the DAO would
        val obs             = ObservationEntity()
        obs.setUuid(observationUuid)
        obs.addLocalization(e)
        assertEquals(Localization.from(e), loc)

    test("from leaves absent optional fields as None"):
        val loc = Localization(localizationType = "point", x = Some(0.5), y = Some(0.5), width = Some(0.0), height = Some(0.0))
        assertEquals(Localization.from(loc.toEntity), loc)

    // ---- I1: the stale-write token is accepted under every spelling --------------------------------

    private val T1 = Instant.parse("2026-03-04T05:06:07.123456Z")
    private val T2 = Instant.parse("2001-01-01T00:00:00Z")

    private def token(json: String): Option[Instant] =
        json.reify[Localization].fold(e => fail(e.getMessage), identity).lastUpdated

    test("snake_case token under last_updated_timestamp (spec name)"):
        assertEquals(token(s"""{"localization_type":"point","x":0.5,"y":0.5,"last_updated_timestamp":"$T1"}"""), Some(T1))

    test("snake_case token under last_updated_time"):
        assertEquals(token(s"""{"localization_type":"point","x":0.5,"y":0.5,"last_updated_time":"$T1"}"""), Some(T1))

    test("camelCase token under lastUpdated, last_updated_timestamp and last_updated_time"):
        assertEquals(token(s"""{"localizationType":"point","x":0.5,"y":0.5,"lastUpdated":"$T1"}"""), Some(T1))
        assertEquals(token(s"""{"localizationType":"point","x":0.5,"y":0.5,"last_updated_timestamp":"$T1"}"""), Some(T1))
        assertEquals(token(s"""{"localizationType":"point","x":0.5,"y":0.5,"last_updated_time":"$T1"}"""), Some(T1))

    test("when both spellings are present and differ, last_updated_timestamp wins"):
        assertEquals(
            token(s"""{"localization_type":"point","last_updated_time":"$T2","last_updated_timestamp":"$T1"}"""),
            Some(T1)
        )
        assertEquals(
            token(s"""{"localization_type":"point","last_updated_timestamp":"$T1","last_updated_time":"$T2"}"""),
            Some(T1)
        )
        assertEquals(
            token(s"""{"localizationType":"point","lastUpdated":"$T2","last_updated_timestamp":"$T1"}"""),
            Some(T1)
        )

    test("no token decodes as None, and responses still emit last_updated_time"):
        assertEquals(token("""{"localization_type":"point","x":0.5,"y":0.5}"""), None)
        val out = Localization("point", lastUpdated = Some(T1)).toSnakeCase.stringify
        assert(out.contains("\"last_updated_time\""), out)
        assert(!out.contains("last_updated_timestamp"), out)
