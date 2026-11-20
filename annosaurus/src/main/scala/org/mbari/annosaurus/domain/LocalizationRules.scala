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
 * Validation and normalization of localizations. Never contradicts the database CHECK constraints. The server never
 * sets `truncated`; it is passed through untouched.
 */
object LocalizationRules:

    /** Tolerance within which coordinates are snapped onto the frame edges 0 and 1. */
    val Eps: Double = 1e-6

    /** Minimum width/height of a bounding box. */
    val MinExtent: Double = 1e-9

    private def snap(v: Double): Double =
        if math.abs(v) <= Eps then 0.0
        else if math.abs(v - 1.0) <= Eps then 1.0
        else v

    private def inFrame(v: Double): Boolean = v >= 0.0 && v <= 1.0

    /** Column lengths (characters, counted as String.length) of the free-text fields. */
    val MaxNameLength: Int    = 128
    val MaxCommentLength: Int = 1024

    private def checkLengths(l: Localization, errors: collection.mutable.Builder[String, Seq[String]]): Unit =
        val limited = Seq(
            ("generator", l.generator, MaxNameLength),
            ("verifier", l.verifier, MaxNameLength),
            ("observer", l.observer, MaxNameLength),
            ("part", l.part, MaxNameLength),
            ("comment", l.comment, MaxCommentLength)
        )
        for (name, value, max) <- limited; v <- value if v.length > max do
            errors += s"$name must be at most $max characters, got ${v.length}"

    /**
     * A point submitted as a single vertex pair (`vertices = [x, y]` with no x/y) is accepted and converted to the
     * canonical form: x and y set, vertices absent. Anything else is left for validation to judge.
     */
    private def canonicalPoint(l: Localization): Localization =
        (l.localizationType, l.x, l.y, l.vertices) match
            case (t, None, None, Some(Seq(x, y))) if t == LocalizationType.Point.value =>
                l.copy(x = Some(x), y = Some(y), vertices = None)
            case _                                                                      => l

    def normalize(localization: Localization): Either[Seq[String], Localization] =
        val l      = canonicalPoint(localization)
        val errors = Seq.newBuilder[String]

        l.confidence.foreach { c =>
            if !(c >= 0.0 && c <= 1.0) then errors += s"confidence must be in [0, 1], got $c"
        }
        checkLengths(l, errors)

        val extent: Option[(Double, Double, Double, Double)] =
            LocalizationType.fromValue(l.localizationType) match
                case Left(msg)                       =>
                    errors += msg
                    None
                case Right(LocalizationType.BoundingBox) => boundingBox(l, errors)
                case Right(LocalizationType.Point)       => point(l, errors)
                case Right(LocalizationType.Line)        => vertexShape(l, 2, "line", errors)
                case Right(LocalizationType.Polygon)     => vertexShape(l, 3, "polygon", errors)
                case Right(LocalizationType.Mask)        => mask(l, errors)

        val errs = errors.result()
        if errs.nonEmpty then Left(errs)
        else
            extent match
                case Some((x, y, w, h)) =>
                    val vs     = normalizedVertices(l)
                    val shaped = l.copy(x = Some(x), y = Some(y), width = Some(w), height = Some(h), vertices = vs)
                    // Mask fields on a non-mask type are ignored (never stored: the shape CHECK forbids mask_data)
                    if l.localizationType == LocalizationType.Mask.value then Right(shaped)
                    else
                        Right(
                            shaped.copy(
                                maskEncoding = None,
                                maskData = None,
                                maskWidthPixels = None,
                                maskHeightPixels = None
                            )
                        )
                case None               => Left(Seq("Unable to determine extent"))

    /** Snapped, duplicate-collapsed (and, for a polygon, opened) vertices for line/polygon; others unchanged. */
    private def normalizedVertices(l: Localization): Option[Seq[Double]] =
        l.localizationType match
            case "line" | "polygon" => l.vertices.map(vs => shapePairs(l.localizationType, vs).flatMap(p => Seq(p._1, p._2)))
            case _                  => l.vertices

    /**
     * Snapped vertex pairs with consecutive duplicates collapsed. A polygon's first vertex is not repeated (spec 6.1),
     * so a polygon sent explicitly closed has its closing vertex dropped, once. Lines are never opened. Idempotent:
     * after collapsing, the second-to-last pair differs from the last, which equals the first, so the new last pair
     * cannot equal the first.
     */
    private def shapePairs(tpe: String, vs: Seq[Double]): Vector[(Double, Double)] =
        val pairs = collapse(vs.map(snap))
        if tpe == "polygon" && pairs.size > 1 && pairs.last == pairs.head then pairs.init else pairs

    private def collapse(vs: Seq[Double]): Vector[(Double, Double)] =
        vs.grouped(2).map(g => (g(0), g(1))).foldLeft(Vector.empty[(Double, Double)]) { (acc, p) =>
            if acc.lastOption.contains(p) then acc else acc :+ p
        }

    /** Extent from snapped edges; when neither edge moved the supplied extent is kept bit-for-bit. */
    private def extentOf(near: Double, extent: Double, snappedNear: Double): Double =
        val far        = near + extent
        val snappedFar = snap(far)
        if snappedNear == near && snappedFar == far then extent else snappedFar - snappedNear

    private def boundingBox(
        l: Localization,
        errors: collection.mutable.Builder[String, Seq[String]]
    ): Option[(Double, Double, Double, Double)] =
        if l.vertices.isDefined then errors += "bounding_box must not have vertices"
        (l.x, l.y, l.width, l.height) match
            case (Some(x0), Some(y0), Some(w0), Some(h0)) =>
                if !(x0.isFinite && y0.isFinite && w0.isFinite && h0.isFinite) then
                    errors += "bounding_box coordinates must be finite"
                    None
                else
                    // Snap each edge, then derive the extent from the snapped edges.
                    val x = snap(x0)
                    val y = snap(y0)
                    val w = extentOf(x0, w0, x)
                    val h = extentOf(y0, h0, y)
                    if !inFrame(x) || !inFrame(y) then errors += "bounding_box x and y must be in [0, 1]"
                    if w < MinExtent then errors += "bounding_box width must be positive"
                    if h < MinExtent then errors += "bounding_box height must be positive"
                    if x + w > 1.0 + 1e-12 then errors += "bounding_box x + width must be <= 1"
                    if y + h > 1.0 + 1e-12 then errors += "bounding_box y + height must be <= 1"
                    Some((x, y, w, h))
            case _                                        =>
                errors += "bounding_box requires x, y, width and height"
                None

    private def point(
        l: Localization,
        errors: collection.mutable.Builder[String, Seq[String]]
    ): Option[(Double, Double, Double, Double)] =
        if l.vertices.isDefined then errors += "point must not have vertices"
        if l.width.exists(_ != 0.0) then errors += "point width must be absent or 0"
        if l.height.exists(_ != 0.0) then errors += "point height must be absent or 0"
        (l.x, l.y) match
            case (Some(x0), Some(y0)) =>
                if !(x0.isFinite && y0.isFinite) then
                    errors += "point coordinates must be finite"
                    None
                else
                    val x = snap(x0)
                    val y = snap(y0)
                    if !inFrame(x) || !inFrame(y) then errors += "point x and y must be in [0, 1]"
                    Some((x, y, 0.0, 0.0))
            case _                    =>
                errors += "point requires x and y"
                None

    private def vertexShape(
        l: Localization,
        minPairs: Int,
        name: String,
        errors: collection.mutable.Builder[String, Seq[String]]
    ): Option[(Double, Double, Double, Double)] =
        l.vertices match
            case None     =>
                errors += s"$name requires vertices"
                None
            case Some(vs) =>
                if vs.length % 2 != 0 then
                    errors += s"$name vertices must have an even length"
                    None
                else if !vs.forall(_.isFinite) then
                    errors += s"$name vertices must be finite"
                    None
                else
                    val snapped = vs.map(snap)
                    if !snapped.forall(inFrame) then errors += s"$name vertices must be in [0, 1]"
                    val pairs   = shapePairs(name, vs)
                    if pairs.size < minPairs then
                        errors += s"$name requires at least $minPairs distinct consecutive vertices, got ${pairs.size}"
                    if pairs.isEmpty then None
                    else
                        val xs = pairs.map(_._1)
                        val ys = pairs.map(_._2)
                        Some((xs.min, ys.min, xs.max - xs.min, ys.max - ys.min))

    private def mask(
        l: Localization,
        errors: collection.mutable.Builder[String, Seq[String]]
    ): Option[(Double, Double, Double, Double)] =
        if l.vertices.isDefined then errors += "mask must not have vertices"
        val encodingOk = l.maskEncoding.exists(MaskEncodings.contains)
        if !encodingOk then errors += s"mask requires maskEncoding to be one of: ${MaskEncodings.mkString(", ")}"
        if !l.maskData.exists(_.nonEmpty) then errors += "mask requires non-empty maskData"
        if !l.maskWidthPixels.exists(_ > 0) then errors += "mask requires maskWidthPixels > 0"
        if !l.maskHeightPixels.exists(_ > 0) then errors += "mask requires maskHeightPixels > 0"
        // The mask is a pixel raster; its extent is the box of the set pixels over the raster size.
        (l.maskData.filter(_.nonEmpty), l.maskWidthPixels.filter(_ > 0), l.maskHeightPixels.filter(_ > 0)) match
            case (Some(data), Some(w), Some(h)) if encodingOk =>
                CocoRle.decodeCounts(data).flatMap(CocoRle.extent(_, h, w)) match
                    case Right((minX, minY, maxX, maxY)) =>
                        val wd = w.toDouble
                        val hd = h.toDouble
                        Some((minX / wd, minY / hd, (maxX - minX) / wd, (maxY - minY) / hd))
                    case Left(msg)                       =>
                        errors += s"mask maskData is not a valid coco_rle mask of ${w}x$h pixels: $msg"
                        None
            case _                                            => None
