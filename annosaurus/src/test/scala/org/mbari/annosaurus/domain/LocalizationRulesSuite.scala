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

class LocalizationRulesSuite extends munit.FunSuite:

    import LocalizationRules.{Eps, normalize}

    private val Tol = 1e-12

    private def bbox(x: Double, y: Double, w: Double, h: Double): Localization =
        Localization("bounding_box", x = Some(x), y = Some(y), width = Some(w), height = Some(h))

    private def polygon(vs: Double*): Localization =
        Localization("polygon", vertices = Some(vs))

    // 6 wide x 4 high mask with a[1:3, 2:5] set; pycocotools 2.0.11:
    // M.encode(np.asfortranarray(a.astype(np.uint8)))["counts"] == b"9220003", pixel box (2, 1, 5, 3)
    private def mask: Localization =
        Localization(
            "mask",
            maskEncoding = Some("coco_rle"),
            maskData = Some("9220003"),
            maskWidthPixels = Some(6),
            maskHeightPixels = Some(4)
        )

    test("bbox inside the frame is unchanged") {
        val r = normalize(bbox(0.1, 0.2, 0.3, 0.4))
        assertEquals(r, Right(bbox(0.1, 0.2, 0.3, 0.4)))
    }

    test("edge snapping, not origin/extent snapping") {
        val r = normalize(bbox(0.0, -7.8e-7, 0.5, 1.00000135))
        r match
            case Right(l) =>
                assertEquals(l.y, Some(0.0))
                assert(l.y.get + l.height.get <= 1.0 + Eps)
            case Left(e)  => fail(s"expected Right but got $e")
    }

    test("x + width beyond 1 + eps is rejected") {
        assert(normalize(bbox(0.5, 0.1, 0.6, 0.2)).isLeft)
    }

    test("width below MinExtent is rejected") {
        assert(normalize(bbox(0.1, 0.1, 1e-10, 0.2)).isLeft)
    }

    test("point with nonzero width is rejected") {
        assert(normalize(Localization("point", x = Some(0.5), y = Some(0.5), width = Some(0.1))).isLeft)
    }

    test("point x = 1.0 is accepted") {
        val r = normalize(Localization("point", x = Some(1.0), y = Some(0.5)))
        assert(r.isRight)
        assertEquals(r.toOption.get.width, Some(0.0))
        assertEquals(r.toOption.get.height, Some(0.0))
    }

    test("point submitted as a single vertex pair is converted to x, y") {
        val r = normalize(Localization("point", vertices = Some(Seq(0.25, 0.75)))).toOption.get
        assertEquals(r.x, Some(0.25))
        assertEquals(r.y, Some(0.75))
        assertEquals(r.width, Some(0.0))
        assertEquals(r.height, Some(0.0))
        assertEquals(r.vertices, None)
    }

    test("point with more than one vertex pair, or both x/y and vertices, is rejected") {
        assert(normalize(Localization("point", vertices = Some(Seq(0.1, 0.1, 0.2, 0.2)))).isLeft)
        assert(normalize(Localization("point", x = Some(0.5), y = Some(0.5), vertices = Some(Seq(0.5, 0.5)))).isLeft)
    }

    test("polygon collapses consecutive duplicate vertices") {
        val in  = polygon(0.46, 0.51, 0.24, 0.42, 0.63, 0.67, 0.89, 0.48, 0.89, 0.48)
        val r   = normalize(in)
        val out = r.toOption.getOrElse(fail(s"expected Right but got $r"))
        assertEquals(out.vertices.map(_.size), Some(8))
        assertEquals(normalize(out), Right(out))
    }

    test("line derives extent; a horizontal line has height 0") {
        val out = normalize(Localization("line", vertices = Some(Seq(0.1, 0.5, 0.9, 0.5)))).toOption.get
        assertEqualsDouble(out.width.get, 0.8, Tol)
        assertEquals(out.height, Some(0.0))
        assertEquals(out.x, Some(0.1))
        assertEquals(out.y, Some(0.5))
    }

    test("polygon with 2 pairs, odd-length vertices, NaN, and 1.2 are rejected") {
        assert(normalize(polygon(0.1, 0.1, 0.2, 0.2)).isLeft)
        assert(normalize(polygon(0.1, 0.1, 0.2, 0.2, 0.3, 0.3, 0.4)).isLeft)
        assert(normalize(polygon(0.1, 0.1, 0.2, 0.2, Double.NaN, 0.3)).isLeft)
        assert(normalize(polygon(0.1, 0.1, 0.2, 0.2, 1.2, 0.3)).isLeft)
    }

    test("confidence 43.0 is rejected, 0.43 accepted") {
        assert(normalize(bbox(0.1, 0.1, 0.2, 0.2).copy(confidence = Some(43.0))).isLeft)
        assert(normalize(bbox(0.1, 0.1, 0.2, 0.2).copy(confidence = Some(0.43))).isRight)
    }

    test("client-supplied extent on a polygon is ignored") {
        val in  = polygon(0.2, 0.2, 0.4, 0.2, 0.4, 0.6).copy(
            x = Some(0.9),
            y = Some(0.9),
            width = Some(5.0),
            height = Some(-1.0)
        )
        val out = normalize(in).toOption.get
        assertEquals(out.x, Some(0.2))
        assertEquals(out.y, Some(0.2))
        assertEqualsDouble(out.width.get, 0.2, Tol)
        assertEqualsDouble(out.height.get, 0.4, Tol)
    }

    test("valid mask is accepted with extent = pixel box / (width, height)") {
        val out = normalize(mask).toOption.get
        assertEquals(
            (out.x, out.y, out.width, out.height),
            (Some(2.0 / 6), Some(1.0 / 4), Some(3.0 / 6), Some(2.0 / 4))
        )
        assertEquals(out.maskData, Some("9220003"))
    }

    test("fully set mask has the full-frame extent") {
        // a = np.ones((4, 6)) -> b"0h0"
        val out = normalize(mask.copy(maskData = Some("0h0"))).toOption.get
        assertEquals((out.x, out.y, out.width, out.height), (Some(0.0), Some(0.0), Some(1.0), Some(1.0)))
    }

    test("mask with wrong encoding, empty data, zero pixel size, or vertices is rejected") {
        assert(normalize(mask.copy(maskEncoding = Some("png"))).isLeft)
        assert(normalize(mask.copy(maskData = Some(""))).isLeft)
        assert(normalize(mask.copy(maskWidthPixels = Some(0))).isLeft)
        assert(normalize(mask.copy(maskHeightPixels = None)).isLeft)
        assert(normalize(mask.copy(vertices = Some(Seq(0.1, 0.1)))).isLeft)
    }

    test("mask whose run sum differs from width * height is rejected") {
        assert(normalize(mask.copy(maskWidthPixels = Some(5))).isLeft)
        assert(normalize(mask.copy(maskHeightPixels = Some(5))).isLeft)
    }

    test("mask with malformed RLE is rejected") {
        assert(normalize(mask.copy(maskData = Some("abc"))).isLeft)
        assert(normalize(mask.copy(maskData = Some("9220003h"))).isLeft)
    }

    test("all-background mask is rejected") {
        // a = np.zeros((4, 6)) -> b"h0"
        assert(normalize(mask.copy(maskData = Some("h0"))).isLeft)
    }

    test("mask errors are accumulated") {
        val r = normalize(mask.copy(maskData = Some("h0"), vertices = Some(Seq(0.1, 0.1)), confidence = Some(2.0)))
        assertEquals(r.left.toOption.map(_.size), Some(3))
    }

    test("unknown type, bbox with vertices, and line without vertices are rejected") {
        assert(normalize(Localization("circle")).isLeft)
        assert(normalize(bbox(0.1, 0.1, 0.2, 0.2).copy(vertices = Some(Seq(0.1, 0.1)))).isLeft)
        assert(normalize(Localization("line")).isLeft)
    }

    test("all errors are returned, not just the first") {
        val r = normalize(bbox(0.1, 0.1, 0.0, 0.2).copy(confidence = Some(2.0)))
        assertEquals(r.left.toOption.map(_.size), Some(2))
    }

    test("truncated is left untouched") {
        val out = normalize(bbox(0.1, 0.1, 0.2, 0.2).copy(truncated = true)).toOption.get
        assert(out.truncated)
    }

    // ---- I8: closed polygons --------------------------------------------------------------------

    test("an explicitly closed polygon is stored without the repeated closing vertex") {
        val out = normalize(polygon(0.1, 0.1, 0.5, 0.1, 0.5, 0.5, 0.1, 0.1)).toOption.get
        assertEquals(out.vertices, Some(Seq(0.1, 0.1, 0.5, 0.1, 0.5, 0.5)))
        assertEquals(normalize(out), Right(out))
    }

    test("a closed polygon with a trailing duplicate is stripped once after collapsing") {
        val out = normalize(polygon(0.1, 0.1, 0.5, 0.1, 0.5, 0.5, 0.1, 0.1, 0.1, 0.1)).toOption.get
        assertEquals(out.vertices, Some(Seq(0.1, 0.1, 0.5, 0.1, 0.5, 0.5)))
    }

    test("polygon [A,B,A] is rejected as having too few vertices") {
        val r = normalize(polygon(0.1, 0.1, 0.5, 0.5, 0.1, 0.1))
        assert(r.isLeft, r.toString)
        assert(r.left.toOption.get.exists(_.contains("at least 3")), r.toString)
    }

    test("an open polygon is unchanged") {
        val in = polygon(0.1, 0.1, 0.5, 0.1, 0.5, 0.5)
        assertEquals(normalize(in).toOption.get.vertices, in.vertices)
    }

    test("a line whose last vertex equals its first is NOT stripped") {
        val in  = Localization("line", vertices = Some(Seq(0.1, 0.1, 0.5, 0.5, 0.1, 0.1)))
        val out = normalize(in).toOption.get
        assertEquals(out.vertices, in.vertices)
    }

    // ---- I4: mask fields and lengths ------------------------------------------------------------

    private val maskFields = mask.copy(localizationType = "x")

    private def withMaskFields(l: Localization): Localization =
        l.copy(
            maskEncoding = maskFields.maskEncoding,
            maskData = maskFields.maskData,
            maskWidthPixels = maskFields.maskWidthPixels,
            maskHeightPixels = maskFields.maskHeightPixels
        )

    test("mask fields are cleared on bounding_box, point, line and polygon") {
        val shapes = Seq(
            bbox(0.1, 0.1, 0.2, 0.2),
            Localization("point", x = Some(0.5), y = Some(0.5)),
            Localization("line", vertices = Some(Seq(0.1, 0.1, 0.5, 0.5))),
            polygon(0.1, 0.1, 0.5, 0.1, 0.5, 0.5)
        )
        for s <- shapes do
            val out = normalize(withMaskFields(s)).toOption.getOrElse(fail(s"expected Right for ${s.localizationType}"))
            assertEquals(
                (out.maskEncoding, out.maskData, out.maskWidthPixels, out.maskHeightPixels),
                (None, None, None, None),
                s.localizationType
            )
    }

    test("mask fields are kept on a mask") {
        val out = normalize(mask).toOption.get
        assertEquals(out.maskEncoding, Some("coco_rle"))
        assertEquals(out.maskWidthPixels, Some(6))
        assertEquals(out.maskHeightPixels, Some(4))
    }

    test("generator, verifier, observer and part longer than 128 characters are rejected; 128 is accepted") {
        val ok   = "a" * 128
        val long = "a" * 129
        val base = bbox(0.1, 0.1, 0.2, 0.2)
        val setters: Seq[(String, String => Localization)] = Seq(
            "generator" -> (s => base.copy(generator = Some(s))),
            "verifier"  -> (s => base.copy(verifier = Some(s))),
            "observer"  -> (s => base.copy(observer = Some(s))),
            "part"      -> (s => base.copy(part = Some(s)))
        )
        for (name, set) <- setters do
            assert(normalize(set(ok)).isRight, name)
            val r = normalize(set(long))
            assert(r.isLeft, name)
            assert(r.left.toOption.get.exists(_.contains(name)), r.toString)
    }

    test("comment longer than 1024 characters is rejected; 1024 is accepted") {
        val base = bbox(0.1, 0.1, 0.2, 0.2)
        assert(normalize(base.copy(comment = Some("c" * 1024))).isRight)
        val r = normalize(base.copy(comment = Some("c" * 1025)))
        assert(r.isLeft)
        assert(r.left.toOption.get.exists(_.contains("comment")), r.toString)
    }
