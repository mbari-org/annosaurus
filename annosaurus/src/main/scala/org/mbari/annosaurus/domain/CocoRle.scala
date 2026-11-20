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
 * COCO run-length encoded masks (the `counts` string of a pycocotools RLE). Pixels are in column-major order (pixel
 * index = x * height + y) and runs alternate starting with a background run.
 */
object CocoRle:

    /** pycocotools stores run lengths as 32-bit unsigned integers. */
    private val MaxRun: Long = 0xffffffffL

    /** 7 chunks of 5 bits cover any delta between two 32-bit run lengths (34 bits including sign). */
    private val MaxChunks: Int = 7

    /**
     * Decode a compressed `counts` string into run lengths, exactly as pycocotools `rleFrString`: each character `c`
     * carries `(c - 48) & 0x1f` as 5 data bits with 0x20 as the continuation flag; bit 0x10 of the last chunk is the
     * sign; for run index m > 2 the value is a delta from run m - 2.
     */
    def decodeCounts(compressed: String): Either[String, Vector[Long]] =
        if compressed.isEmpty then Left("RLE counts string is empty")
        else
            val cnts = Vector.newBuilder[Long]
            val prev = new Array[Long](2) // runs m - 2 and m - 1
            var m    = 0
            var p    = 0
            var err  = Option.empty[String]
            while err.isEmpty && p < compressed.length do
                var x    = 0L
                var k    = 0
                var more = true
                while err.isEmpty && more do
                    if p >= compressed.length then err = Some("RLE counts string is truncated")
                    else if k >= MaxChunks then err = Some(s"RLE value at run $m is too long")
                    else
                        val ch = compressed.charAt(p)
                        if ch < '0' || ch > 'o' then err = Some(s"Invalid RLE character at position $p")
                        else
                            val c = ch - 48
                            x |= (c & 0x1fL) << (5 * k)
                            more = (c & 0x20) != 0
                            p += 1
                            k += 1
                            if !more && (c & 0x10) != 0 then x |= -1L << (5 * k)
                if err.isEmpty then
                    if m > 2 then x += prev(0)
                    if x < 0 || x > MaxRun then err = Some(s"RLE run $m has invalid length $x")
                    else
                        cnts += x
                        prev(0) = prev(1)
                        prev(1) = x
                        m += 1
            err.toLeft(cnts.result())

    /**
     * Pixel bounding box `(minX, minY, maxXExclusive, maxYExclusive)` of the set pixels, computed from run boundaries
     * without materializing the raster. The runs must sum to `height * width` and at least one pixel must be set.
     */
    def extent(counts: Vector[Long], height: Int, width: Int): Either[String, (Int, Int, Int, Int)] =
        if height <= 0 || width <= 0 then Left(s"Mask size must be positive, got ${width}x$height")
        else if counts.exists(_ < 0) then Left("RLE run lengths must be non-negative")
        else
            val total = counts.sum
            val size  = height.toLong * width
            if total != size then Left(s"RLE runs sum to $total, expected ${width}x$height = $size")
            else
                var minX = Long.MaxValue
                var minY = Long.MaxValue
                var maxX = -1L
                var maxY = -1L
                var pos  = 0L
                for (n, i) <- counts.zipWithIndex do
                    if i % 2 == 1 && n > 0 then
                        val last = pos + n - 1
                        val c0   = pos / height
                        val c1   = last / height
                        // A run that crosses into the next column covers row h - 1 of one column and row 0 of the next.
                        val (r0, r1) = if c0 == c1 then (pos % height, last % height) else (0L, height - 1L)
                        minX = math.min(minX, c0)
                        maxX = math.max(maxX, c1)
                        minY = math.min(minY, r0)
                        maxY = math.max(maxY, r1)
                    pos += n
                if maxX < 0 then Left("Mask has no set pixels")
                else Right((minX.toInt, minY.toInt, (maxX + 1).toInt, (maxY + 1).toInt))
