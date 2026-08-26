# Bounding box coordinate convention: normalized storage, and where quantization belongs

## TL;DR

We already agree on the important part: **top-left origin, zero-based pixel indexing, and a continuous half-open coordinate model** where `1.0` is the outer edge of the last pixel rather than its index. That matches every major ML training format.

The `round` vs `floor`/`ceil` disagreement is the wrong axis, though. **Both proposed versions round `x` and `width` independently**, and width is a derived quantity — `floor(x) + ceil(w) != ceil(x+w)`. The fix is to split one method into three operations with different contracts:

1. **normalized <-> pixel (float)** — pure scaling, no rounding, exactly invertible. This is what we store and what we hand to a training pipeline.
2. **pixel -> whole-pixel region** — needed only to actually *crop* an image. Quantize the **edges** (`floor` the min, `ceil` the max) and derive width.
3. **point -> pixel index** — `floor` plus a clamp to `[0, W-1]`.

---

## 1. The agreed coordinate model

Coordinates are **continuous**, origin at the image's top-left. An integer coordinate `k` names the *edge before* pixel `k`. Pixel `i` therefore covers the half-open interval `[i, i+1)`, and the whole image is exactly `[0,1] x [0,1]`.

For a 1920-wide image: `x = 0.0` is the left edge of pixel 0, `x = 1.0` is pixel-space 1920, which is the right edge of pixel 1919. There is no pixel *at* 1920 — 1920 is the envelope boundary, not an index.

This is the model used by YOLO/Ultralytics, COCO, torchvision, TF Object Detection, and Albumentations.

## 2. Why neither rounding proposal is correct

Both versions apply a rounding function to `x` and to `width` separately. Because `width` is `xMax - x`, that composition is not sound: `floor(x) + ceil(w) != ceil(x + w)`, and likewise for `round`.

Worked examples in pixel space:

| pixel-space box | version 1 (`round`) | version 2 (`floor`/`ceil`) | edge-based |
|---|---|---|---|
| `x=15.9, w=10.0` -> far edge 25.9 | `[16, 26)` OK | `[15, 25)` — **drops pixel 25, which is 90% covered** | `[15, 26)` OK |
| `x=10.4, w=10.4` -> far edge 20.8 | `[10, 20)` — **drops pixel 20, which is 80% covered** | `[10, 21)` OK | `[10, 21)` OK |
| `x=0.5, w=1.0` -> far edge 1.5 | `[1, 2)` | `[0, 1)` — **wrong pixel entirely** | `[0, 2)` OK |

Note the failures point in *both* directions depending on the fractional parts, so neither version is consistently "covering" or consistently "nearest". The 10x10 / `(0.97, 0.97, 0.01, 0.01)` example in the second proposal happens to come out right, but only by luck of those particular fractions.

## 3. What ML training pipelines actually do: they don't round

- **YOLO / Ultralytics** — normalized `cx cy w h`, floats in `[0,1]`.
- **COCO** — pixel `[x, y, width, height]`, **floats**, top-left origin.
- **torchvision** — float `xyxy`, with `area = (x2-x1) * (y2-y1)`, i.e. pure half-open.
- **TF Object Detection API** — normalized `[ymin, xmin, ymax, xmax]`.
- **Albumentations** — chains many transforms in float and quantizes only at the very end.
- **Pascal VOC** is the odd one out: 1-based, *inclusive* `xmax`/`ymax`. This is the origin of most conversion bugs in the wild (`width = xmax - xmin + 1`). We should not adopt it.

The reason they stay in float is that augmentation pipelines chain many transforms; rounding at each step accumulates drift, and the loss is unrecoverable. Any `int` in our conversion API should therefore be a deliberate, named, terminal operation — not the default.

## 4. A real floating-point hazard

An integer -> normalized -> integer round trip with a bare `floor` shifts boxes on many ordinary image widths:

```
(15.0 / 22.0) * 22.0  ==  14.999999999999998   ->  floor gives 14, not 15
(13.0 / 23.0) * 23.0  ==  12.999999999999998
(31.0 / 39.0) * 39.0  ==  30.999999999999996
```

Exhaustive check over all `0 <= i < W` for `W` up to 8000 finds many such cases. Since human-drawn boxes arrive from the UI as integer pixels, this is a live bug, not a theoretical one. The mitigation is to snap values within a small tolerance onto the nearest integer before flooring. Worst-case real error is around `1e-11` even for 100k-pixel images, so a `1e-6` tolerance is safe and sits well below any meaningful sub-pixel precision.

## 5. Constraints

Adopting the second proposal's list, with one amendment:

- `0 <= x`, `0 <= y`
- `w > 0`, `h > 0` for boxes (zero-extent annotations are a separate `NormalizedPoint` type)
- `x + w <= 1`, `y + h <= 1`

**Amendment:** real footage has organisms entering and leaving the frame, so annotations will legitimately arrive clipped at the edge. Validate on ingest, but provide an explicit `clampToImage()` rather than rejecting outright — and pair it with a `truncated` flag or equivalent, or downstream training will treat a half-visible animal as a whole one.

Validation should use the same `1e-6` tolerance, otherwise accumulated double error will reject boxes that are geometrically fine.

---

## 6. Proposed Java

### `NormalizedBoundingBox` — the storage form

```java
/**
 * A bounding box in normalized image coordinates.
 *
 * <p>Coordinate model — matches YOLO, COCO, torchvision, TF Object Detection and
 * Albumentations: coordinates are <em>continuous</em>, origin at the image's top-left,
 * and an integer coordinate {@code k} names the edge <em>before</em> pixel {@code k}.
 * Pixel {@code i} therefore covers the half-open interval {@code [i, i+1)} and the whole
 * image is exactly {@code [0,1] x [0,1]}: {@code 1.0} is the outer edge of the last
 * pixel, not its index.
 *
 * <p>This type is the storage form and is resolution-independent. Conversion to and from
 * {@link PixelBoundingBox} is exact; quantizing to whole pixels is lossy and lives in
 * {@link PixelBoundingBox#toCropRegion}.
 */
public record NormalizedBoundingBox(double x, double y, double width, double height) {

    /** Tolerance for accumulated double error. Coupled to the writer's %.6f — see section 7. */
    static final double EPS = 1e-6;

    /**
     * Smallest permitted extent. Guards against corrupt and subnormal data (a 5e-324-wide
     * box halves to zero), and is unreachable by real annotations: 1e-9 is about 2e-6 px
     * on a 1920-wide image.
     */
    static final double MIN_EXTENT = 1e-9;

    public NormalizedBoundingBox {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(width, "width");
        requireFinite(height, "height");
        if (width < MIN_EXTENT || height < MIN_EXTENT)
            throw new IllegalArgumentException(
                    "width and height must be >= " + MIN_EXTENT
                            + " (a zero-extent annotation is a NormalizedPoint): "
                            + width + " x " + height);
        if (x < -EPS || y < -EPS)
            throw new IllegalArgumentException("x and y must be >= 0: " + x + ", " + y);
        if (x + width > 1 + EPS || y + height > 1 + EPS)
            throw new IllegalArgumentException(
                    "box must not extend past the image: xMax=" + (x + width)
                            + ", yMax=" + (y + height));
    }

    public double xMax() { return x + width; }
    public double yMax() { return y + height; }
    public double centerX() { return x + width / 2; }
    public double centerY() { return y + height / 2; }

    /** Exact, lossless. Inverse of {@link PixelBoundingBox#normalized}. */
    public PixelBoundingBox toPixels(int imageWidth, int imageHeight) {
        requireDims(imageWidth, imageHeight);
        return new PixelBoundingBox(
                x * imageWidth,
                y * imageHeight,
                width * imageWidth,
                height * imageHeight);
    }

    /** For truncated objects whose annotation ran slightly past the frame edge. */
    public NormalizedBoundingBox clampToImage() {
        double x0 = Math.max(0.0, x);
        double y0 = Math.max(0.0, y);
        double x1 = Math.min(1.0, xMax());
        double y1 = Math.min(1.0, yMax());
        return new NormalizedBoundingBox(x0, y0, x1 - x0, y1 - y0);
    }

    private static void requireFinite(double v, String name) {
        if (!Double.isFinite(v)) throw new IllegalArgumentException(name + " must be finite: " + v);
    }

    static void requireDims(int imageWidth, int imageHeight) {
        if (imageWidth <= 0 || imageHeight <= 0)
            throw new IllegalArgumentException(
                    "image dimensions must be > 0: " + imageWidth + " x " + imageHeight);
    }
}
```

### `PixelBoundingBox` — the float pixel-space form, and the only place quantization happens

```java
/**
 * A bounding box in continuous pixel-space coordinates — the COCO {@code bbox} and
 * torchvision {@code xywh} form. Deliberately {@code double}: this is the form training
 * and augmentation pipelines operate in, and rounding here is both lossy and premature.
 */
public record PixelBoundingBox(double x, double y, double width, double height) {

    public double xMax() { return x + width; }
    public double yMax() { return y + height; }

    /** Exact inverse of {@link NormalizedBoundingBox#toPixels}. */
    public NormalizedBoundingBox normalized(int imageWidth, int imageHeight) {
        NormalizedBoundingBox.requireDims(imageWidth, imageHeight);
        return new NormalizedBoundingBox(
                x / imageWidth,
                y / imageHeight,
                width / imageWidth,
                height / imageHeight);
    }

    /**
     * The smallest set of whole pixels that fully contains this box — the only correct
     * place to quantize, and only when you are actually cropping or rasterizing.
     *
     * <p>Quantizes the box's <em>edges</em>, never its origin and extent independently:
     * {@code floor(min)} and {@code ceil(max)}, with width derived. Applying
     * {@code floor} to x and {@code ceil} to width separately is not equivalent —
     * {@code floor(15.9) + ceil(10.0) = 25} drops a pixel that is 90% covered, while
     * {@code ceil(25.9) = 26} keeps it.
     */
    public CropRegion toCropRegion(int imageWidth, int imageHeight) {
        NormalizedBoundingBox.requireDims(imageWidth, imageHeight);
        int x0 = clamp((int) Math.floor(snap(x)),      0,      imageWidth  - 1);
        int y0 = clamp((int) Math.floor(snap(y)),      0,      imageHeight - 1);
        int x1 = clamp((int) Math.ceil (snap(xMax())), x0 + 1, imageWidth);
        int y1 = clamp((int) Math.ceil (snap(yMax())), y0 + 1, imageHeight);
        return new CropRegion(x0, y0, x1 - x0, y1 - y0);
    }

    /**
     * Snaps values a hair off an integer back onto it before flooring.
     *
     * <p>Not cosmetic: {@code (15.0/22.0) * 22.0 == 14.999999999999998}, so a bare
     * {@code floor} shifts an integer-sourced box one pixel left on many ordinary image
     * widths. The largest real double error here is ~1e-11 even for 100k-pixel images,
     * so a 1e-6 tolerance is safe and well below any meaningful sub-pixel precision.
     */
    static double snap(double v) {
        double r = Math.rint(v);
        return Math.abs(v - r) < NormalizedBoundingBox.EPS ? r : v;
    }

    static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
```

### `CropRegion` — terminal, ready to index a buffer

```java
/**
 * A half-open region of whole pixels, ready to index an image buffer:
 * columns {@code [x, x+width)}, rows {@code [y, y+height)}. Terminal — do not convert
 * back to normalized coordinates, since the quantization that produced it is lossy.
 * Always at least 1x1, and always inside the image it was derived against.
 */
public record CropRegion(int x, int y, int width, int height) {
    public CropRegion {
        if (x < 0 || y < 0) throw new IllegalArgumentException("x and y must be >= 0");
        if (width < 1 || height < 1) throw new IllegalArgumentException("must be at least 1x1");
    }
    public int xMaxExclusive() { return x + width; }
    public int yMaxExclusive() { return y + height; }
}
```

### `NormalizedPoint` — zero-extent annotations

```java
/**
 * A zero-extent annotation. Points relax the {@code w > 0, h > 0} constraint, so the
 * quantization has to clamp: {@code x = 1.0} floors to {@code imageWidth}, which is one
 * past the last valid index.
 */
public record NormalizedPoint(double x, double y) {
    public NormalizedPoint {
        if (!(x >= -NormalizedBoundingBox.EPS && x <= 1 + NormalizedBoundingBox.EPS)
                || !(y >= -NormalizedBoundingBox.EPS && y <= 1 + NormalizedBoundingBox.EPS))
            throw new IllegalArgumentException("point must lie in [0,1]: " + x + ", " + y);
    }

    /** The pixel containing this point. */
    public int pixelX(int imageWidth) {
        NormalizedBoundingBox.requireDims(imageWidth, 1);
        return PixelBoundingBox.clamp(
                (int) Math.floor(PixelBoundingBox.snap(x * imageWidth)), 0, imageWidth - 1);
    }

    public int pixelY(int imageHeight) {
        NormalizedBoundingBox.requireDims(1, imageHeight);
        return PixelBoundingBox.clamp(
                (int) Math.floor(PixelBoundingBox.snap(y * imageHeight)), 0, imageHeight - 1);
    }
}
```

---

## 7. YOLO conversion

YOLO/Darknet is the one format that is *already* normalized, so it is a pure geometry
change rather than a scaling change — but it is **center-based**, and its constraints are
looser than ours in a way that makes the two directions asymmetric.

### The asymmetry

| direction | can it fail? | why |
|---|---|---|
| `NormalizedBoundingBox` -> YOLO | **never** | our invariants (`x >= 0`, `x + w <= 1`) imply `cx` in `[w/2, 1 - w/2]`, which is strictly inside YOLO's `[0,1]` |
| YOLO -> `NormalizedBoundingBox` | **yes** | YOLO only requires `cx, cy, w, h` in `[0,1]`, so `cx = 0.02, w = 0.10` is a legal YOLO box whose left edge is at `x = -0.03` |

That second row is the truncated-object case from section 5 arriving through the front
door. So `toNormalized()` needs an explicit policy, and there are two, both legitimate:

- **strict** — throw. Correct when ingesting our own round-tripped data, where an
  out-of-bounds box means something upstream is broken.
- **visible** — clip to the image. Correct when ingesting third-party label files.
  **This moves the center**, which is semantically right for "what pixels can be seen"
  and wrong for "where the animal actually is" — another argument for the `truncated`
  flag in the open items.

### Two measured facts that shape the code

**The center round trip is not bit-exact.** `x -> cx = x + w/2 -> x' = cx - w/2` returns
the identical double only about 43% of the time over 2M random valid boxes, because the
intermediate addition rounds. The worst observed absolute error is `1.11e-16` — around
2e-13 of a pixel on a 1920-wide image, so it is numerically irrelevant, but it means
**`equals()` is the wrong way to compare a round-tripped box.** Round-trip tests have to
assert within a tolerance, and if these records are ever used as `Map` keys or in a
`Set`, a round trip will produce a distinct key.

**A degenerate extent can clip to nothing.** The tempting invariant — "a valid YOLO box
always overlaps the image, since `cx >= 0` and `w > 0` imply `cx + w/2 > 0`" — holds in
exact arithmetic but not in floating point: `cx = 1.0, w = 5e-324` gives a visible width
of exactly `0.0`, because halving the smallest subnormal underflows. Rather than leak that
into an `Optional` return, the fix is a floor on the extent: a `MIN_EXTENT` of `1e-9`
(about 2e-6 px on a 1920-wide image) is unreachable by real annotation data and restores
the invariant for every non-corrupt input. This applies to `NormalizedBoundingBox` too —
see the amendment below.

**The serialization precision and the validation tolerance are coupled.** Writing `%.6f`
bounds each stored value's error at `5e-7`; propagating through `x = cx - w/2` gives a
worst-case edge error of `7.5e-7` (measured: `7.495e-07`). That fits under `EPS = 1e-6`,
but with only ~25% headroom — **so `EPS` and the `%.6f` in the writer must move together.**
Dropping to `%.5f` requires `EPS = 1e-5`.

### Amendments to section 6

- **Remove `toYoloLabel` and `fromYoloLabel` from `NormalizedBoundingBox`.** They are
  superseded by the type below; two ways to do this is how class indices drift.
- **Add `MIN_EXTENT` to `NormalizedBoundingBox`** and use it in the constructor:

```java
    /** Tolerance for accumulated double error. Coupled to the writer's %.6f — see section 7. */
    static final double EPS = 1e-6;

    /**
     * Smallest permitted extent. Guards against corrupt and subnormal data (a 5e-324-wide
     * box halves to zero), and is unreachable by real annotations: 1e-9 is about 2e-6 px
     * on a 1920-wide image.
     */
    static final double MIN_EXTENT = 1e-9;

    // ...in the compact constructor, replacing the `width <= 0 || height <= 0` check:
        if (width < MIN_EXTENT || height < MIN_EXTENT)
            throw new IllegalArgumentException(
                    "width and height must be >= " + MIN_EXTENT
                            + " (a zero-extent annotation is a NormalizedPoint): "
                            + width + " x " + height);
```

### `YoloBoundingBox`

```java
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * A bounding box in the YOLO / Darknet label format: a zero-based class index plus
 * <em>center-based</em> normalized geometry, with an optional detection confidence.
 *
 * <p>Wire form is one line per object, space delimited:
 * {@code <classId> <centerX> <centerY> <width> <height> [confidence]}. The trailing
 * confidence is what Ultralytics emits for predictions with {@code save_conf=True}; it is
 * absent in ground-truth label files. One file per image, and an <em>empty</em> file is
 * meaningful — it declares a background image with no objects.
 *
 * <p>Scope: axis-aligned boxes only. Ultralytics' oriented-box (OBB) format is a different
 * eight-value shape ({@code classId x1 y1 x2 y2 x3 y3 x4 y4}) and is not handled here.
 *
 * <p>Class indices are meaningless without the {@link YoloClassMap} they were produced
 * against.
 */
public record YoloBoundingBox(
        int classId,
        double centerX,
        double centerY,
        double width,
        double height,
        OptionalDouble confidence) {

    public YoloBoundingBox {
        Objects.requireNonNull(confidence, "confidence must be OptionalDouble.empty(), not null");
        if (classId < 0) throw new IllegalArgumentException("classId must be >= 0: " + classId);
        requireUnit(centerX, "centerX");
        requireUnit(centerY, "centerY");
        requireExtent(width, "width");
        requireExtent(height, "height");
        if (confidence.isPresent()) requireUnit(confidence.getAsDouble(), "confidence");
    }

    public static YoloBoundingBox of(int classId, double cx, double cy, double w, double h) {
        return new YoloBoundingBox(classId, cx, cy, w, h, OptionalDouble.empty());
    }

    public static YoloBoundingBox of(
            int classId, double cx, double cy, double w, double h, double confidence) {
        return new YoloBoundingBox(classId, cx, cy, w, h, OptionalDouble.of(confidence));
    }

    // ---- conversion -------------------------------------------------------------------

    /**
     * Always succeeds: {@link NormalizedBoundingBox}'s invariants are strictly tighter
     * than YOLO's, so {@code cx} necessarily lands in {@code [w/2, 1 - w/2]}.
     */
    public static YoloBoundingBox from(NormalizedBoundingBox box, int classId) {
        return of(classId, box.centerX(), box.centerY(), box.width(), box.height());
    }

    public static YoloBoundingBox from(
            NormalizedBoundingBox box, int classId, double confidence) {
        return of(classId, box.centerX(), box.centerY(), box.width(), box.height(), confidence);
    }

    /** True when {@link #toNormalized()} will succeed. */
    public boolean isWithinImage() {
        double eps = NormalizedBoundingBox.EPS;
        return centerX - width / 2 >= -eps
                && centerY - height / 2 >= -eps
                && centerX + width / 2 <= 1 + eps
                && centerY + height / 2 <= 1 + eps;
    }

    /**
     * Strict corner-form conversion.
     *
     * <p>Throws {@link IllegalArgumentException} when the box extends past the image edge —
     * legal in YOLO, illegal in {@link NormalizedBoundingBox}. Use this when reading data
     * we wrote ourselves, where an out-of-bounds box means something upstream is broken;
     * use {@link #toNormalizedVisible()} when reading third-party label files.
     *
     * <p>Not bit-exact against {@link #from}: the center round trip drifts by up to ~1e-16,
     * so compare round-tripped boxes within a tolerance rather than with {@code equals}.
     */
    public NormalizedBoundingBox toNormalized() {
        return new NormalizedBoundingBox(
                centerX - width / 2, centerY - height / 2, width, height);
    }

    /**
     * The visible part of a truncated box, clipped to the image.
     *
     * <p><strong>This moves the center and shrinks the extent</strong>, which is correct
     * for "which pixels can be seen" and wrong for "where the object actually is". Record
     * a truncation flag alongside the result if the distinction matters downstream.
     *
     * <p>Throws if the clipped remainder falls below
     * {@link NormalizedBoundingBox#MIN_EXTENT} — reachable only for corrupt sub-pixel
     * input, and a box with no usable visible area is not an annotation.
     */
    public NormalizedBoundingBox toNormalizedVisible() {
        double x0 = Math.max(0.0, centerX - width / 2);
        double y0 = Math.max(0.0, centerY - height / 2);
        double x1 = Math.min(1.0, centerX + width / 2);
        double y1 = Math.min(1.0, centerY + height / 2);
        return new NormalizedBoundingBox(x0, y0, x1 - x0, y1 - y0);
    }

    // ---- label file text --------------------------------------------------------------

    /**
     * One YOLO label-file line.
     *
     * <p>{@code Locale.ROOT} is not optional: under a locale with a comma decimal
     * separator this silently emits {@code 0,500000} and corrupts the whole dataset.
     * The {@code %.6f} precision is coupled to {@link NormalizedBoundingBox#EPS} — see
     * section 7.
     */
    public String toLabelLine() {
        String geometry = String.format(Locale.ROOT, "%d %.6f %.6f %.6f %.6f",
                classId, centerX, centerY, width, height);
        return confidence.isEmpty()
                ? geometry
                : geometry + String.format(Locale.ROOT, " %.6f", confidence.getAsDouble());
    }

    /** Parses one line. {@code Double.parseDouble} is locale-independent by specification. */
    public static YoloBoundingBox parseLabelLine(String line) {
        String[] f = line.strip().split("\\s+");
        if (f.length != 5 && f.length != 6)
            throw new IllegalArgumentException("expected 5 fields, or 6 with confidence, got "
                    + f.length + ": \"" + line + "\"");
        try {
            int classId = Integer.parseInt(f[0]);
            double cx = Double.parseDouble(f[1]);
            double cy = Double.parseDouble(f[2]);
            double w = Double.parseDouble(f[3]);
            double h = Double.parseDouble(f[4]);
            return f.length == 6
                    ? of(classId, cx, cy, w, h, Double.parseDouble(f[5]))
                    : of(classId, cx, cy, w, h);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("malformed YOLO label line: \"" + line + "\"", e);
        }
    }

    /**
     * Parses a whole label file. Blank lines are skipped and {@code \R} handles CRLF, since
     * label files routinely arrive from Windows annotation tools. An empty or all-blank
     * file yields an empty list, which is valid data: a background image with no objects.
     */
    public static List<YoloBoundingBox> parseLabelFile(String contents) {
        List<YoloBoundingBox> boxes = new ArrayList<>();
        String[] lines = contents.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].isBlank()) continue;
            try {
                boxes.add(parseLabelLine(lines[i]));
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("line " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        return List.copyOf(boxes);
    }

    public static String formatLabelFile(List<YoloBoundingBox> boxes) {
        StringBuilder sb = new StringBuilder();
        for (YoloBoundingBox box : boxes) sb.append(box.toLabelLine()).append('\n');
        return sb.toString();
    }

    // ---- validation -------------------------------------------------------------------

    private static void requireUnit(double v, String name) {
        double eps = NormalizedBoundingBox.EPS;
        if (!Double.isFinite(v) || v < -eps || v > 1 + eps)
            throw new IllegalArgumentException(name + " must be normalized to [0,1]: " + v);
    }

    private static void requireExtent(double v, String name) {
        if (!Double.isFinite(v) || v < NormalizedBoundingBox.MIN_EXTENT || v > 1 + NormalizedBoundingBox.EPS)
            throw new IllegalArgumentException(name + " must be in ["
                    + NormalizedBoundingBox.MIN_EXTENT + ", 1]: " + v);
    }
}
```

### `YoloClassMap`

A bare `int classId` is not portable data — it only means something next to the ordered
name list it was generated from. Getting this wrong is the classic silent failure: add a
concept mid-list on a re-export and every previously exported label now names a different
animal, with no error anywhere.

```java
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps concept names to the zero-based class indices used by {@link YoloBoundingBox}.
 *
 * <p><strong>Indices must be stable for the life of a dataset.</strong> Persist this map
 * (as the {@code names} block of the dataset's {@code data.yaml}) next to the exported
 * labels, and extend it only by appending. Re-deriving it from whatever concepts happen to
 * be present in a later export will silently relabel every existing annotation.
 */
public final class YoloClassMap {

    private final List<String> names;
    private final Map<String, Integer> indices;

    private YoloClassMap(List<String> names) {
        this.names = List.copyOf(names);
        Map<String, Integer> byName = new LinkedHashMap<>();
        for (int i = 0; i < this.names.size(); i++) {
            String name = this.names.get(i);
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("blank concept name at index " + i);
            if (byName.putIfAbsent(name, i) != null)
                throw new IllegalArgumentException(
                        "duplicate concept \"" + name + "\" at index " + i);
        }
        this.indices = Map.copyOf(byName);
    }

    /** List order defines the indices. This is the form to persist and reload. */
    public static YoloClassMap of(List<String> conceptsInIndexOrder) {
        return new YoloClassMap(conceptsInIndexOrder);
    }

    /** Deterministic ordering for a first export from an unordered concept set. */
    public static YoloClassMap sorted(Collection<String> concepts) {
        return new YoloClassMap(concepts.stream().distinct().sorted().toList());
    }

    /** Appends unknown concepts, preserving every existing index. */
    public YoloClassMap extendedWith(Collection<String> concepts) {
        List<String> extended = new java.util.ArrayList<>(names);
        concepts.stream().distinct().sorted()
                .filter(c -> !indices.containsKey(c))
                .forEach(extended::add);
        return new YoloClassMap(extended);
    }

    public int indexOf(String concept) {
        Integer index = indices.get(concept);
        if (index == null)
            throw new IllegalArgumentException("concept not in class map: \"" + concept + "\"");
        return index;
    }

    public String conceptOf(int classId) {
        if (classId < 0 || classId >= names.size())
            throw new IllegalArgumentException("class index out of range [0, "
                    + (names.size() - 1) + "]: " + classId);
        return names.get(classId);
    }

    public int size() { return names.size(); }

    public List<String> names() { return names; }

    /**
     * The {@code names:} block of an Ultralytics {@code data.yaml}. Values are quoted
     * because concept names may contain colons, which would otherwise break the parse.
     */
    public String toDataYamlNames() {
        StringBuilder sb = new StringBuilder("names:\n");
        for (int i = 0; i < names.size(); i++) {
            String escaped = names.get(i).replace("\\", "\\\\").replace("\"", "\\\"");
            sb.append("  ").append(i).append(": \"").append(escaped).append("\"\n");
        }
        return sb.toString();
    }
}
```

### Usage

```java
YoloClassMap classes = YoloClassMap.of(List.of("Sebastes", "Rathbunaster californicus"));

// export
String labelFile = YoloBoundingBox.formatLabelFile(
        annotations.stream()
                .map(a -> YoloBoundingBox.from(a.box(), classes.indexOf(a.concept())))
                .toList());

// import
for (YoloBoundingBox yolo : YoloBoundingBox.parseLabelFile(labelFile)) {
    NormalizedBoundingBox box = yolo.isWithinImage()
            ? yolo.toNormalized()
            : yolo.toNormalizedVisible();   // truncated at the frame edge
    String concept = classes.conceptOf(yolo.classId());
}
```

## 8. Pascal VOC conversion

VOC is the hard one, and not for arithmetic reasons. **It has no single coordinate
convention.** The original devkit is 1-based with inclusive maxima; a large amount of
tooling that emits files labelled "Pascal VOC XML" is not. Guessing wrong shifts every box
by one pixel *and* mis-sizes it by one pixel — negligible on a 400 px box, but a 10% error
on a 20 px organism, which is a lot of what we annotate.

### The three conventions in the wild

| convention | coordinate range | `xmax` means | width |
|---|---|---|---|
| `ONE_BASED_INCLUSIVE` | `1 .. W` | last included pixel | `xmax - xmin + 1` |
| `ZERO_BASED_EXCLUSIVE` | `0 .. W` | exclusive far edge | `xmax - xmin` |
| `ZERO_BASED_INCLUSIVE` | `0 .. W-1` | last included pixel | `xmax - xmin + 1` |

The first is the official one, and the devkit's own MATLAB proves it rather than us having
to take it on faith — its overlap routine computes `iw = min(...) - max(...) + 1` and its
area `(xmax-xmin+1) * (ymax-ymin+1)`. Our implementation reproduces those numbers exactly
on a real VOC2012 annotation (the `2008_000008` horse, `xmin=53 xmax=471`): width 419,
area 139946.

The same stored numbers under all three readings:

```
ONE_BASED_INCLUSIVE    -> x=52 w=419
ZERO_BASED_EXCLUSIVE   -> x=53 w=418
ZERO_BASED_INCLUSIVE   -> x=53 w=419
```

No two agree. This is why `VocBoundingBox` holds the four numbers **uninterpreted** and
takes a `VocConvention` at every conversion boundary — it is not possible to convert a VOC
box in this API without stating which reading applies.

### Inferring the convention, and admitting when you can't

`VocConventionSniffer` eliminates conventions on legality: a convention is ruled out the
moment the data contains a coordinate it could not have produced. Two observations are
decisive, and both come from boxes that touch an image edge:

- a coordinate of `0` rules out `ONE_BASED_INCLUSIVE`
- a coordinate equal to the image dimension rules out `ZERO_BASED_INCLUSIVE`

**It frequently cannot decide, and that is the correct behaviour.** A dataset whose
smallest coordinate is 1 and whose largest equals the image dimension is legal under both
`ONE_BASED_INCLUSIVE` and `ZERO_BASED_EXCLUSIVE` — the two readings differ by exactly the
one pixel value the data does not contain. Nothing in the numbers can separate them, so the
sniffer returns no answer and prints its evidence:

```
1 boxes examined; smallest coordinate 1.0
  saw coordinate == size  -> rules out ZERO_BASED_INCLUSIVE
  AMBIGUOUS between [ZERO_BASED_EXCLUSIVE, ONE_BASED_INCLUSIVE]; supply the convention explicitly
```

A confident guess here would be a silent one-pixel corruption of the whole corpus, so the
convention must come from the data's provenance and be recorded with it. The sniffer is a
tool for informing that decision, not for making it.

### What VOC gets right that YOLO doesn't

Three of the open items from earlier sections are things VOC already solved, and we should
adopt its semantics rather than invent our own:

- **`<size>` is inside the annotation.** VOC stores the source image dimensions with the
  boxes — exactly the provenance normalized coordinates need, and exactly what YOLO omits.
- **`truncated` exists natively.** The flag we identified as an open item is already part
  of the format. Adopting its meaning gets interoperability for free.
- **`name` is a concept string**, so VOC needs no `YoloClassMap` and cannot suffer the
  class-index drift failure. The format is self-describing where YOLO is not.

And one trap: **`difficult` must survive ingest.** The official VOC evaluation *excludes*
difficult objects entirely — they count neither as detections to be found nor as false
positives when detected. Dropping the flag on import silently changes every metric computed
against the data, and quietly breaks comparability with any published number. Hence
`scorableObjects()`.

### Direction of loss

VOC is already a pixel-space format, so `toPixels` needs no image size and is exact.
Quantizing back to VOC's integer grid reuses `toCropRegion`, which makes the important
direction exact: **VOC integers -> normalized -> VOC integers round trips bit-exactly**,
verified over 6M random boxes across all three conventions and image sizes up to 4000 px.
Float-sourced boxes (ML detections) are enlarged to the covering grid by strictly less than
one pixel per edge, on the same "never lose annotated area" rule as section 6.

### `VocConvention`

```java
/**
 * Which of the mutually incompatible interpretations of Pascal VOC {@code <bndbox>}
 * coordinates a file uses.
 *
 * <p>Unlike YOLO, Pascal VOC has no single convention. The original devkit is 1-based with
 * inclusive maxima; a large amount of tooling that emits "Pascal VOC XML" is not. Guessing
 * wrong shifts every box by a pixel <em>and</em> mis-sizes it by a pixel — negligible on a
 * 400 px box, but a 10% error on a 20 px organism.
 *
 * <p>Both axes are captured as an origin offset and an inclusivity flag, from which the
 * continuous, 0-based, half-open edges used by {@link PixelBoundingBox} are derived.
 */
public enum VocConvention {

    /**
     * The official VOC devkit: coordinates run {@code 1..width}, and {@code xmax}/{@code ymax}
     * name the last <em>included</em> pixel. The devkit's own overlap code is the proof —
     * {@code iw = min(...) - max(...) + 1} and {@code area = (xmax-xmin+1)*(ymax-ymin+1)}.
     * Use for VOC2007-2012 ground truth and anything derived from the MATLAB devkit.
     */
    ONE_BASED_INCLUSIVE(1, true),

    /**
     * Coordinates run {@code 0..width} with {@code xmax}/{@code ymax} as the exclusive far
     * edge — the same continuous half-open model we use internally. Common output of modern
     * converters and annotation platforms.
     */
    ZERO_BASED_EXCLUSIVE(0, false),

    /**
     * Coordinates run {@code 0..width-1} with {@code xmax}/{@code ymax} naming the last
     * included pixel. The awkward hybrid produced by several GUI annotation tools.
     */
    ZERO_BASED_INCLUSIVE(0, true);

    private final int origin;
    private final boolean inclusive;

    VocConvention(int origin, boolean inclusive) {
        this.origin = origin;
        this.inclusive = inclusive;
    }

    /** Continuous 0-based edge for a stored minimum coordinate. */
    double minEdge(double storedMin) {
        return storedMin - origin;
    }

    /** Continuous 0-based <em>exclusive</em> edge for a stored maximum coordinate. */
    double maxEdge(double storedMax) {
        return storedMax - origin + (inclusive ? 1 : 0);
    }

    /** Inverse of {@link #minEdge}. */
    int storedMin(int minEdge) {
        return minEdge + origin;
    }

    /** Inverse of {@link #maxEdge}. */
    int storedMax(int maxEdgeExclusive) {
        return maxEdgeExclusive + origin - (inclusive ? 1 : 0);
    }

    /**
     * The largest legal stored maximum coordinate for an image of this extent:
     * {@code W} for both {@code ONE_BASED_INCLUSIVE} (the last 1-based pixel) and
     * {@code ZERO_BASED_EXCLUSIVE} (the far edge), but {@code W - 1} for
     * {@code ZERO_BASED_INCLUSIVE} (the last 0-based pixel index).
     */
    public int maxLegalCoordinate(int imageExtent) {
        return inclusive ? imageExtent - 1 + origin : imageExtent;
    }

    /** The smallest legal stored coordinate. */
    public int minLegalCoordinate() {
        return origin;
    }
}
```

### `VocBoundingBox`

```java
/**
 * The raw four numbers from a Pascal VOC {@code <bndbox>} element, exactly as stored.
 *
 * <p>These numbers are <strong>meaningless without a {@link VocConvention}</strong> —
 * {@code xmax=471} is the last included pixel under the devkit's convention and the
 * exclusive far edge under others. This type therefore holds them uninterpreted and takes
 * the convention as an argument at every conversion boundary, so it is impossible to
 * convert without stating which reading applies.
 *
 * <p>Held as {@code double} rather than {@code int}: the VOC schema implies integers, but
 * plenty of exporters write fractional coordinates, and silently truncating them on ingest
 * loses sub-pixel detection precision.
 */
public record VocBoundingBox(double xmin, double ymin, double xmax, double ymax) {

    public VocBoundingBox {
        requireFinite(xmin, "xmin");
        requireFinite(ymin, "ymin");
        requireFinite(xmax, "xmax");
        requireFinite(ymax, "ymax");
        if (xmax < xmin || ymax < ymin)
            throw new IllegalArgumentException(
                    "max must be >= min: [" + xmin + "," + ymin + "," + xmax + "," + ymax + "]");
    }

    /**
     * Exact conversion to continuous, 0-based, half-open pixel space. Needs no image size:
     * VOC is already a pixel-space format.
     */
    public PixelBoundingBox toPixels(VocConvention convention) {
        double x0 = convention.minEdge(xmin);
        double y0 = convention.minEdge(ymin);
        return new PixelBoundingBox(x0, y0,
                convention.maxEdge(xmax) - x0,
                convention.maxEdge(ymax) - y0);
    }

    public NormalizedBoundingBox toNormalized(
            VocConvention convention, int imageWidth, int imageHeight) {
        return toPixels(convention).normalized(imageWidth, imageHeight);
    }

    /**
     * Quantizes a continuous box back to VOC's integer grid.
     *
     * <p>Lossy in one direction only. VOC integers -> normalized -> VOC integers is exact,
     * because the edges land back on integers and {@code floor}/{@code ceil} are then
     * identities. Float-sourced boxes (ML detections) are enlarged to the covering pixel
     * grid, by strictly less than one pixel per edge, on the same "never lose annotated
     * area" rule as {@link PixelBoundingBox#toCropRegion}.
     */
    public static VocBoundingBox from(
            NormalizedBoundingBox box, VocConvention convention, int imageWidth, int imageHeight) {
        return from(box.toPixels(imageWidth, imageHeight).toCropRegion(imageWidth, imageHeight),
                convention);
    }

    public static VocBoundingBox from(CropRegion region, VocConvention convention) {
        return new VocBoundingBox(
                convention.storedMin(region.x()),
                convention.storedMin(region.y()),
                convention.storedMax(region.xMaxExclusive()),
                convention.storedMax(region.yMaxExclusive()));
    }

    /** True when every coordinate is legal for this convention and image size. */
    public boolean isLegalFor(VocConvention convention, int imageWidth, int imageHeight) {
        int lo = convention.minLegalCoordinate();
        return xmin >= lo && ymin >= lo
                && xmax <= convention.maxLegalCoordinate(imageWidth)
                && ymax <= convention.maxLegalCoordinate(imageHeight);
    }

    private static void requireFinite(double v, String name) {
        if (!Double.isFinite(v)) throw new IllegalArgumentException(name + " must be finite: " + v);
    }
}
```

### `VocObject`

```java
/**
 * One {@code <object>} from a Pascal VOC annotation.
 *
 * <p>Two of these fields answer questions left open by the normalized-coordinate design:
 * VOC carries {@code truncated} natively (the object extends beyond the image), so the
 * truncation flag we need does not have to be invented — adopting VOC's semantics gives
 * interoperability for free.
 *
 * <p>{@code difficult} is not cosmetic and must survive ingest: the official VOC evaluation
 * <strong>excludes difficult objects entirely</strong>, counting them neither as detections
 * to be found nor as false positives when detected. Dropping the flag on import silently
 * changes every metric computed against the data.
 *
 * <p>Note that {@code name} is a concept <em>string</em>, so VOC needs no equivalent of
 * {@link YoloClassMap} — the format is self-describing where YOLO is not.
 */
public record VocObject(
        String name,
        VocBoundingBox box,
        boolean truncated,
        boolean difficult,
        boolean occluded,
        String pose) {

    /** VOC's own default when {@code <pose>} is absent. */
    public static final String POSE_UNSPECIFIED = "Unspecified";

    public VocObject {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("object name (concept) must not be blank");
        if (box == null) throw new IllegalArgumentException("box must not be null");
        if (pose == null || pose.isBlank()) pose = POSE_UNSPECIFIED;
    }

    public static VocObject of(String name, VocBoundingBox box) {
        return new VocObject(name, box, false, false, false, POSE_UNSPECIFIED);
    }

    public VocObject withTruncated(boolean value) {
        return new VocObject(name, box, value, difficult, occluded, pose);
    }

    public VocObject withDifficult(boolean value) {
        return new VocObject(name, box, truncated, value, occluded, pose);
    }
}
```

### `VocAnnotation`

```java
import java.util.List;

/**
 * A whole Pascal VOC annotation file — one per image.
 *
 * <p>Note {@code width}/{@code height}: VOC stores the source image dimensions <em>inside
 * the annotation</em>, which is exactly the provenance the normalized-coordinate design
 * needs and which YOLO omits entirely. Anything we store should keep this.
 */
public record VocAnnotation(
        String folder,
        String filename,
        int width,
        int height,
        int depth,
        boolean segmented,
        List<VocObject> objects) {

    public VocAnnotation {
        if (filename == null || filename.isBlank())
            throw new IllegalArgumentException("filename must not be blank");
        NormalizedBoundingBox.requireDims(width, height);
        if (depth <= 0) throw new IllegalArgumentException("depth must be > 0: " + depth);
        folder = folder == null ? "" : folder;
        objects = objects == null ? List.of() : List.copyOf(objects);
    }

    public static VocAnnotation of(
            String filename, int width, int height, List<VocObject> objects) {
        return new VocAnnotation("", filename, width, height, 3, false, objects);
    }

    /** Objects the official VOC evaluation protocol would score, i.e. excluding difficult ones. */
    public List<VocObject> scorableObjects() {
        return objects.stream().filter(o -> !o.difficult()).toList();
    }

    /** True when every object's box is legal under this convention for this image size. */
    public boolean isConsistentWith(VocConvention convention) {
        return objects.stream().allMatch(o -> o.box().isLegalFor(convention, width, height));
    }
}
```

### `VocConventionSniffer`

```java
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Infers which {@link VocConvention} a set of Pascal VOC files uses, by <em>elimination on
 * legality</em>: a convention is ruled out the moment a file contains a coordinate that
 * convention could not have produced.
 *
 * <p>This deliberately does not always answer. A dataset whose smallest coordinate is 1 and
 * whose largest equals the image dimension is legal under both {@code ONE_BASED_INCLUSIVE}
 * and {@code ZERO_BASED_EXCLUSIVE}, and no amount of staring at the numbers distinguishes
 * them — the two readings differ by exactly the one pixel the data does not contain. In
 * that case {@link #inferred()} is empty and a human has to supply the convention from the
 * data's provenance. Reporting the ambiguity is the point; a confident guess here would be
 * a silent one-pixel corruption of the whole corpus.
 *
 * <p>The decisive observations are the boxes that touch an image edge, so a small sample or
 * one with no edge-touching objects will usually be ambiguous.
 */
public record VocConventionSniffer(
        Set<VocConvention> candidates,
        double minCoordinate,
        double maxOvershoot,
        boolean sawZero,
        boolean sawFullExtent,
        int boxesExamined) {

    public static VocConventionSniffer of(Collection<VocAnnotation> annotations) {
        Set<VocConvention> candidates = EnumSet.allOf(VocConvention.class);
        double minCoord = Double.POSITIVE_INFINITY;
        double maxOvershoot = Double.NEGATIVE_INFINITY;
        boolean sawZero = false, sawFullExtent = false;
        int examined = 0;

        for (VocAnnotation ann : annotations) {
            for (VocObject obj : ann.objects()) {
                VocBoundingBox b = obj.box();
                examined++;
                minCoord = Math.min(minCoord, Math.min(b.xmin(), b.ymin()));
                sawZero |= b.xmin() == 0 || b.ymin() == 0;
                sawFullExtent |= b.xmax() == ann.width() || b.ymax() == ann.height();
                maxOvershoot = Math.max(maxOvershoot,
                        Math.max(b.xmax() - ann.width(), b.ymax() - ann.height()));
                candidates.removeIf(c -> !b.isLegalFor(c, ann.width(), ann.height()));
            }
        }
        return new VocConventionSniffer(
                Set.copyOf(candidates),
                examined == 0 ? Double.NaN : minCoord,
                examined == 0 ? Double.NaN : maxOvershoot,
                sawZero, sawFullExtent, examined);
    }

    /** Present only when exactly one convention survives elimination. */
    public Optional<VocConvention> inferred() {
        return candidates.size() == 1 ? Optional.of(candidates.iterator().next()) : Optional.empty();
    }

    /** A human-readable account of what was observed and what it rules out. */
    public String report() {
        if (boxesExamined == 0) return "no boxes examined; convention cannot be inferred";
        StringBuilder sb = new StringBuilder();
        sb.append(boxesExamined).append(" boxes examined; smallest coordinate ")
          .append(minCoordinate).append('\n');
        if (sawZero) sb.append("  saw coordinate 0        -> rules out ONE_BASED_INCLUSIVE\n");
        if (sawFullExtent) sb.append("  saw coordinate == size  -> rules out ZERO_BASED_INCLUSIVE\n");
        if (maxOvershoot > 0)
            sb.append("  saw coordinate ").append(maxOvershoot)
              .append(" past the image edge -> data is malformed under every convention\n");
        if (candidates.isEmpty())
            sb.append("  NO convention explains this data; inspect it before importing");
        else if (candidates.size() == 1)
            sb.append("  inferred: ").append(candidates.iterator().next());
        else
            sb.append("  AMBIGUOUS between ").append(List.copyOf(candidates))
              .append("; supply the convention explicitly");
        return sb.toString();
    }
}
```

### `VocXml`

One note on the parser before the code: annotation files are **third-party input**, and
routinely arrive as a user-uploaded zip of somebody else's dataset. A default
`DocumentBuilderFactory` will resolve an external entity pointed at a local file or an
internal URL, so the hardening below is not boilerplate. Disallowing the DOCTYPE
declaration outright is the strongest single mitigation — it removes entity declarations
altogether, and VOC files have no legitimate use for a DTD.

```java
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/** Reads and writes the Pascal VOC annotation XML. */
public final class VocXml {

    private VocXml() {}

    // ---- reading ----------------------------------------------------------------------

    /**
     * Parses a VOC annotation document.
     *
     * <p>The parser is hardened against XXE. This is not boilerplate: annotation files are
     * third-party input, frequently arriving as a user-uploaded zip of someone else's
     * dataset, and a default {@code DocumentBuilderFactory} will happily resolve an external
     * entity pointed at a local file or an internal URL. Disallowing the DOCTYPE
     * declaration outright is the strongest single mitigation, since it removes entity
     * declarations altogether; VOC files have no legitimate use for a DTD.
     */
    public static VocAnnotation parse(String xml) {
        Document doc = parseSecurely(xml);
        Element root = doc.getDocumentElement();
        Element size = child(root, "size");
        if (size == null)
            throw new IllegalArgumentException("VOC annotation is missing <size>; "
                    + "image dimensions are required to normalize its boxes");

        List<VocObject> objects = new ArrayList<>();
        NodeList nodes = root.getElementsByTagName("object");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element obj = (Element) nodes.item(i);
            Element bnd = child(obj, "bndbox");
            if (bnd == null) continue;   // segmentation-only object; no box to import
            objects.add(new VocObject(
                    text(obj, "name", null),
                    new VocBoundingBox(
                            number(bnd, "xmin"), number(bnd, "ymin"),
                            number(bnd, "xmax"), number(bnd, "ymax")),
                    flag(obj, "truncated"),
                    flag(obj, "difficult"),
                    flag(obj, "occluded"),
                    text(obj, "pose", VocObject.POSE_UNSPECIFIED)));
        }
        return new VocAnnotation(
                text(root, "folder", ""),
                text(root, "filename", null),
                (int) number(size, "width"),
                (int) number(size, "height"),
                child(size, "depth") == null ? 3 : (int) number(size, "depth"),
                flag(root, "segmented"),
                objects);
    }

    private static Document parseSecurely(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver((publicId, systemId) -> {
                throw new SAXException("external entities are not permitted: " + systemId);
            });
            // Replace the default handler, which prints "[Fatal Error]" to stderr before
            // throwing. Malformed uploads are routine; they belong in the caller's error
            // path, not in the service log.
            builder.setErrorHandler(new org.xml.sax.ErrorHandler() {
                public void warning(SAXParseException e) { }
                public void error(SAXParseException e) throws SAXException { throw e; }
                public void fatalError(SAXParseException e) throws SAXException { throw e; }
            });
            return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException("could not parse VOC annotation XML: " + e.getMessage(), e);
        }
    }

    private static Element child(Element parent, String tag) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling())
            if (n instanceof Element e && e.getTagName().equals(tag)) return e;
        return null;
    }

    private static String text(Element parent, String tag, String fallback) {
        Element e = child(parent, tag);
        if (e == null || e.getTextContent().isBlank()) {
            if (fallback == null)
                throw new IllegalArgumentException("VOC annotation is missing required <" + tag + ">");
            return fallback;
        }
        return e.getTextContent().strip();
    }

    private static double number(Element parent, String tag) {
        String raw = text(parent, tag, null);
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("<" + tag + "> is not a number: \"" + raw + "\"", e);
        }
    }

    /** VOC writes these as 0/1, but "true"/"false" appears in the wild. Absent means false. */
    private static boolean flag(Element parent, String tag) {
        Element e = child(parent, tag);
        if (e == null) return false;
        String raw = e.getTextContent().strip();
        return raw.equals("1") || raw.equalsIgnoreCase("true");
    }

    // ---- writing ----------------------------------------------------------------------

    public static String write(VocAnnotation annotation) {
        StringBuilder sb = new StringBuilder("<annotation>\n");
        sb.append("\t<folder>").append(escape(annotation.folder())).append("</folder>\n");
        sb.append("\t<filename>").append(escape(annotation.filename())).append("</filename>\n");
        sb.append("\t<size>\n")
          .append("\t\t<width>").append(annotation.width()).append("</width>\n")
          .append("\t\t<height>").append(annotation.height()).append("</height>\n")
          .append("\t\t<depth>").append(annotation.depth()).append("</depth>\n")
          .append("\t</size>\n");
        sb.append("\t<segmented>").append(annotation.segmented() ? 1 : 0).append("</segmented>\n");
        for (VocObject o : annotation.objects()) {
            sb.append("\t<object>\n")
              .append("\t\t<name>").append(escape(o.name())).append("</name>\n")
              .append("\t\t<pose>").append(escape(o.pose())).append("</pose>\n")
              .append("\t\t<truncated>").append(o.truncated() ? 1 : 0).append("</truncated>\n")
              .append("\t\t<occluded>").append(o.occluded() ? 1 : 0).append("</occluded>\n")
              .append("\t\t<bndbox>\n")
              .append("\t\t\t<xmin>").append(coord(o.box().xmin())).append("</xmin>\n")
              .append("\t\t\t<ymin>").append(coord(o.box().ymin())).append("</ymin>\n")
              .append("\t\t\t<xmax>").append(coord(o.box().xmax())).append("</xmax>\n")
              .append("\t\t\t<ymax>").append(coord(o.box().ymax())).append("</ymax>\n")
              .append("\t\t</bndbox>\n")
              .append("\t\t<difficult>").append(o.difficult() ? 1 : 0).append("</difficult>\n")
              .append("\t</object>\n");
        }
        return sb.append("</annotation>\n").toString();
    }

    /** Integral values are written without a decimal point, as VOC readers expect. */
    private static String coord(double v) {
        return v == Math.rint(v) && !Double.isInfinite(v)
                ? Long.toString((long) v)
                : Double.toString(v);
    }

    /**
     * Escapes XML text content. Necessary because concept names are free text: an
     * unescaped {@code &} in a species name produces a file no parser will read back.
     */
    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
```

### Usage

```java
// import — convention must be stated, or inferred and confirmed
var sniff = VocConventionSniffer.of(annotations);
System.out.println(sniff.report());
VocConvention convention = sniff.inferred()
        .orElseThrow(() -> new IllegalStateException("state the convention explicitly"));

for (VocAnnotation ann : annotations) {
    for (VocObject obj : ann.scorableObjects()) {          // excludes difficult
        NormalizedBoundingBox box =
                obj.box().toNormalized(convention, ann.width(), ann.height());
        // obj.name() is the concept; obj.truncated() is the flag we wanted anyway
    }
}

// export
var objects = annotations.stream()
        .map(a -> VocObject.of(a.concept(),
                VocBoundingBox.from(a.box(), VocConvention.ONE_BASED_INCLUSIVE, w, h))
                .withTruncated(a.isTruncated()))
        .toList();
String xml = VocXml.write(VocAnnotation.of("image.png", w, h, objects));
```

## 9. Open items

- [x] ~~**Store the source image dimensions on the annotation record.**~~ Settled: adopt VOC's `<size>` semantics (section 8). Without them we cannot validate a normalized box, cannot emit COCO, and cannot detect that the image was later re-encoded at a different size. Normalized coordinates survive resizes but *hide* the fact that a resize happened, which matters when a human placed a box at a specific resolution.
- [x] ~~**Decide whether stored boxes may be clipped**, and add a `truncated` flag.~~ Settled: adopt VOC's `truncated` semantics (section 8).
- [ ] **Confirm the serialized precision.** `%.6f` in the YOLO writer is ~0.002 px on a 1920-wide image, which is fine; but if the wire/DB format is also 6 decimal places, that is a deliberate quantization we should state rather than inherit.
- [ ] **Persist the `YoloClassMap` with every export** and never re-derive it. Appending is safe; reordering silently relabels the corpus.
- [ ] **Record the `VocConvention` with every VOC import**, and refuse to guess when the sniffer is ambiguous.
- [ ] **Preserve `difficult` on ingest** so evaluation stays comparable with published VOC numbers.
- [ ] **Round-trip property tests**: `normalize(toPixels(b)) == b` within `EPS` for random boxes, and `toCropRegion` contains the float box for random boxes and image sizes. For the YOLO round trip, assert within `EPS` rather than with `equals` — it is not bit-exact.