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

/**
 * Test vectors were generated with pycocotools 2.0.11 / numpy 2.5.4, not written by hand:
 *
 * {{{
 * import numpy as np
 * from pycocotools import mask as M
 * def enc(a): return M.encode(np.asfortranarray(a.astype(np.uint8)))["counts"].decode()
 * def box(a): ys, xs = np.nonzero(a); return (xs.min(), ys.min(), xs.max() + 1, ys.max() + 1)
 * def runs(a):  # run lengths of the column-major raster, starting with a background run
 *     out, cur, n = [], 0, 0
 *     for v in a.flatten(order="F").astype(np.uint8):
 *         if v == cur: n += 1
 *         else: out.append(n); cur = v; n = 1
 *     return out + [n]
 * H, W = 4, 6
 * a = np.zeros((H, W)); a[1:3, 2:5] = 1                                    # interior
 * a = np.zeros((H, W)); a[0:2, 0:2] = 1                                    # topLeft
 * a = np.zeros((H, W)); a[2:4, 3:6] = 1                                    # bottomRight
 * a = np.zeros((H, W)); a[2, 3] = 1                                        # single
 * a = np.zeros((H, W)); a[0, 1] = 1; a[3, 4:6] = 1                         # twoRegions
 * a = np.zeros((H, W)); a[1:3, 1] = 1; a[0, 2] = 1; a[3, 3] = 1; a[2, 4] = 1  # staggered
 * a = np.zeros((H, W)); a[2:4, 1] = 1; a[0:3, 2] = 1                       # crossesColumn
 * a = np.zeros((H, W))                                                     # empty
 * a = np.ones((H, W))                                                      # full
 * a = np.zeros((60, 100)); a[5:20, 10:40] = 1; a[30:55, 50:90] = 1; a[12:45, 70:72] = 1; a[58, 3] = 1  # large
 * }}}
 */
class CocoRleSuite extends munit.FunSuite:

    /** name, height, width, counts string, expected runs, expected (minX, minY, maxXExclusive, maxYExclusive) */
    private case class Vec(name: String, h: Int, w: Int, counts: String, runs: Vector[Long], box: (Int, Int, Int, Int))

    private val vectors = Seq(
        Vec("interior", 4, 6, "9220003", Vector(9, 2, 2, 2, 2, 2, 5), (2, 1, 5, 3)),
        Vec("topLeft", 4, 6, "0220`0", Vector(0, 2, 2, 2, 18), (0, 0, 2, 2)),
        Vec("bottomRight", 4, 6, ">22000", Vector(14, 2, 2, 2, 2, 2), (3, 2, 6, 4)),
        Vec("single", 4, 6, ">19", Vector(14, 1, 9), (3, 2, 4, 3)),
        Vec("twoRegions", 4, 6, "41>0E0", Vector(4, 1, 14, 1, 3, 1), (1, 0, 6, 4)),
        Vec("staggered", 4, 6, "521O50L03", Vector(5, 2, 1, 1, 6, 1, 2, 1, 5), (1, 0, 5, 4)),
        Vec("crossesColumn", 4, 6, "65=", Vector(6, 5, 13), (1, 0, 3, 4)),
        Vec("full", 4, 6, "0h0", Vector(0, 24), (0, 0, 6, 4))
    )

    vectors.foreach { v =>
        test(s"${v.name}: decodeCounts matches pycocotools runs and extent matches the numpy box") {
            val counts = CocoRle.decodeCounts(v.counts)
            assertEquals(counts, Right(v.runs))
            assertEquals(CocoRle.extent(v.runs, v.h, v.w), Right(v.box))
        }
    }

    // 100 wide x 60 high: delta-coded runs (m > 2) with multi-character values
    private val Large =
        """^71^;>oE000000000000000000000000000000000000000000000000000000000ac0:U\O0000000000000000000000000000000000000^Ob000b0^O0000000000000000000000000000000000ja0"""

    private val LargeRuns: Vector[Long] = Vector[Long](
        238, 1, 366, 15, 45, 15, 45, 15, 45, 15, 45, 15, 45, 15, 45, 15, 45, 15, 45, 15, 45, 15, 45, 15, 45,
        15, 45, 15, 45, 15, 45, 15, 45, 15, 45, 15, 45, 15, 45, 15, 45, 15, 45, 15, 45, 15, 45, 15, 45, 15,
        45, 15, 45, 15, 45, 15, 45, 15, 45, 15, 45, 15, 670, 25, 35, 25, 35, 25, 35, 25, 35, 25, 35, 25, 35,
        25, 35, 25, 35, 25, 35, 25, 35, 25, 35, 25, 35, 25, 35, 25, 35, 25, 35, 25, 35, 25, 35, 25, 35, 25,
        35, 25, 17, 43, 17, 43, 35, 25, 35, 25, 35, 25, 35, 25, 35, 25, 35, 25, 35, 25, 35, 25, 35, 25, 35,
        25, 35, 25, 35, 25, 35, 25, 35, 25, 35, 25, 35, 25, 35, 25, 35, 25, 605
    )

    test("large: delta-coded multi-character runs decode to the pycocotools runs and box") {
        assertEquals(LargeRuns.size, 143)
        assertEquals(LargeRuns.sum, 6000L)
        val counts = CocoRle.decodeCounts(Large)
        assertEquals(counts, Right(LargeRuns))
        assertEquals(counts.flatMap(CocoRle.extent(_, 60, 100)), Right((3, 5, 90, 59)))
    }

    test("empty mask (all background) is rejected") {
        val counts = CocoRle.decodeCounts("h0")
        assertEquals(counts, Right(Vector(24L)))
        assert(CocoRle.extent(counts.toOption.get, 4, 6).isLeft)
    }

    test("run sum different from width * height is rejected") {
        val runs = CocoRle.decodeCounts("9220003").toOption.get
        assert(CocoRle.extent(runs, 4, 5).isLeft)
        assert(CocoRle.extent(runs, 5, 6).isLeft)
    }

    test("non-positive frame size is rejected") {
        assert(CocoRle.extent(Vector(0L, 24L), 0, 6).isLeft)
        assert(CocoRle.extent(Vector(0L, 24L), 4, -6).isLeft)
    }

    test("negative counts are rejected by extent") {
        assert(CocoRle.extent(Vector(10L, -2L, 16L), 4, 6).isLeft)
    }

    test("empty string is rejected") {
        assert(CocoRle.decodeCounts("").isLeft)
    }

    test("truncated string (continuation bit set on the last character) is rejected") {
        // 'h' = 48 + 0x38 has the continuation bit 0x20 set, so nothing may end on it
        assert(CocoRle.decodeCounts("h").isLeft)
        assert(CocoRle.decodeCounts("9220003h").isLeft)
    }

    test("characters outside '0'..'o' are rejected") {
        assert(CocoRle.decodeCounts("92/0003").isLeft) // '/' = 47
        assert(CocoRle.decodeCounts("9220p03").isLeft) // 'p' = 112
        assert(CocoRle.decodeCounts("9220 03").isLeft)
    }

    test("non-ASCII characters are rejected") {
        assert(CocoRle.decodeCounts("92é0003").isLeft)
        assert(CocoRle.decodeCounts("9220003ÿ").isLeft)
    }

    test("values decoding to a negative run are rejected") {
        // 'O' = 48 + 0x1f: a single chunk with the sign bit set decodes to -1 as the first run
        assert(CocoRle.decodeCounts("O").isLeft)
        // third run (m = 2) is not delta coded, so a negative value there is a negative run
        assert(CocoRle.decodeCounts("11O").isLeft)
        // delta that drives run 3 below zero: runs 1, 1, then 1 + (-2)
        assert(CocoRle.decodeCounts("111N").isLeft)
    }

    test("overflowing values are rejected") {
        // eight continuation chunks exceed the 32-bit run lengths pycocotools can produce
        assert(CocoRle.decodeCounts("ooooooo0").isLeft)
        assert(CocoRle.decodeCounts("oooooooooooooo0").isLeft)
        // seven chunks decoding to 2^32 + 2^30 - 1, above the uint32 maximum
        assert(CocoRle.decodeCounts("oooooo4").isLeft)
    }
