# Proposal: First-class localizations

- **Status:** Draft, revised after review (vars-feedback#338)
- **Date:** 2026-08-25 (revised 2026-10-12)
- **Affects:** schema, `Annotation` payload, a new endpoint group, Flyway migrations, a SQL migration routine, ZeroMQ subscribers, **every client that reads or writes localizations**
- **Does not affect:** the association model for non-localization link names
- **Companion:** [bounding-box-coordinates.md](bounding-box-coordinates.md) — coordinate convention and format conversions (YOLO, VOC, COCO)

## 1. Summary

Localizations (bounding boxes, points, lines, polygons, segmentation masks) are stored today as
`associations` rows whose `link_value` holds a JSON blob. Nothing in the database or the service
constrains that blob's shape, its cardinality, or its coordinate space. Different tools write
different JSON for the same concept and overwrite each other silently.

This proposal promotes localizations to a first-class table, `localizations`, whose natural key is
`(observation_uuid, localization_type)`. An observation may carry **at most one localization of each
type** — up to one bounding box, one point, one line, one polygon, and one mask. Geometry becomes
real typed columns rather than negotiable JSON keys, and coordinates are **normalized to `[0,1]`**
rather than pixels, so a localization no longer depends on the size of any particular image or video.

This is a **full changeover, not a coexistence**:

- The localization endpoints are the **only** way to create, edit, or delete a localization.
- Localization link names are **no longer accepted or returned by the association endpoints.**
- Existing localization associations are converted by a **SQL migration routine** (§8), which is what makes
  the changeover possible.

## 2. The problem, in the project's own data

There is no occurrence of the string `localization` anywhere in the codebase. Localizations exist
purely as a convention agreed between tools. Four real link names are in use:

| `link_name` | `link_value` (real examples) |
|---|---|
| `bounding box` | `{"x": 982, "y": 1, "width": 97, "height": 132, "generator": "mbari452k_yolov8", "confidence": 43.0}` |
| `bounding box` | `{"x": 0, "y": 266, "width": 26, "height": 23, "generator": "mbari452k_yolov8", "confidence": 17.0, "verifier": "kwalz"}` |
| `localization-point` | `{"x": [2128], "y": [2529]}` |
| `localization-line` | `{"x": [1630, 4381], "y": [2312, 1645]}` |
| `localization-polygon` | `{"x": [2208,1167,...,4252], "y": [1525,1263,...,1449]}` |

Seven distinct defects are visible in those five rows alone.

**2.1 — Two naming schemes.** `bounding box` is space-separated and unprefixed; the others use a
hyphenated `localization-` prefix. There is no way to enumerate "all localization link names" without
a hard-coded list that each tool maintains privately.

**2.2 — Two geometry encodings for the same idea.** A bounding box uses scalar keys
(`"x": 982`). Every other type uses parallel arrays (`"x": [2128]`). A point is an array of
length one. Any consumer must branch on `link_name` before it can even begin parsing.

**2.3 — Parallel arrays can desynchronize.** (i.e. separate x and y columns) Nothing enforces
`len(x) == len(y)`. A truncated or partially-written value produces a polygon that cannot be
interpreted, and no constraint anywhere detects it. The failure surfaces at read time, in
whichever tool happens to read it next.

**2.4 — Optional provenance keys appear and disappear.** `verifier` is present in one bounding box
and absent in the other. `generator` and `confidence` are present on boxes and absent on points,
lines, and polygons. Some boxes also carry a `tags` array (e.g. `["training"]`) that is understood
only by a couple of tools. A tool that round-trips a localization through its own model silently drops
every key it does not know about. This is the mechanism behind the tug-of-war: it is not malice,
it is lossy deserialization.

**2.5 — `confidence` has no declared scale.** The observed values `43.0` and `17.0` imply a 0–100
percentage, but nothing states it. That scale is an artifact of the visual-events / `.tracks.tar.gz`
format; softmax/sigmoid output is canonically 0–1, and a tool emitting a native score would write
`0.43`, which is indistinguishable from a legitimate 0.43%.

**2.6 — Pixels with no declared frame.** `x: 982` is meaningless without knowing which raster it
indexes into, and the examples above carry no image reference at all. Pixel coordinates also bind a
localization to one resolution: a box drawn on a 4K framegrab cannot be shown on a lower-resolution
proxy video without first discovering the original dimensions. We operate across a wide range of
resolutions, and this has caused repeated bugs.

**2.7 — `link_value` is `varchar(1024)`, and that is a live ceiling.** The 10-vertex polygon above
serializes to roughly 111 characters, about 11 characters per vertex at 4-digit coordinates. The
column therefore tops out near **90–100 vertices**. A detailed outline is silently truncated or
rejected at the database layer. Segmentation masks are not merely awkward under this limit — a
COCO RLE for a single modest organism runs 0.5–2 KB, so masks are effectively **impossible** to
store today.

Additionally, nothing prevents an observation from having two `bounding box` associations. Which one
is authoritative is undefined.

## 3. Goals and non-goals

**Goals**

1. Make the content of a localization non-negotiable: typed columns, declared units, validated at write.
2. Enforce "at most one of each type per observation" in the database, not by convention.
3. Make coordinates **resolution-independent**, so validation and rendering need no image or video dimensions.
4. Remove the 1024-character ceiling so polygons and masks are storable.
5. Give writes a concurrency story so a second writer cannot silently discard a first writer's edit.
6. Have **one write path**: the localization endpoints. No second representation to keep in sync.
7. Convert every existing localization association, and account for every one that cannot be converted.

**Non-goals**

- Changing the association model for anything that is not a localization.
- Adding geospatial (PostGIS) indexing or geometry types. Image coordinates are not geographic.
- Resolving video or image dimensions inside the annosaurus service. That stays with vampire-squid;
  only the migration routine (§8.2) reads vampire-squid's `video_references` table, once, in SQL.
- Converting to pixel, YOLO, VOC, or COCO formats server-side. Those are client concerns; the
  reference conversions live in [bounding-box-coordinates.md](bounding-box-coordinates.md).
- Backward compatibility for legacy localization associations. This is a deliberate breaking change (§7).

## 4. Design decisions

| # | Decision | Rationale |
|---|---|---|
| D-1 | New table is the **sole** source of truth; localization associations are retired | Ends the two-writers problem without a sync layer |
| D-2 | **One table**, `localization_type` discriminator, hybrid columns | Uniform cross-type queries; bbox/point fully columnar; one entity, one DAO, one endpoint group |
| D-3 | **Normalized `[0,1]` coordinates**; no link to `image_references` | Resolution-independent; validated without dimensions. Rests on the media assumption in §6.2 |
| D-4 | Provenance as real nullable columns | A column cannot be dropped by a tool that does not know about it |
| D-5 | `confidence` on a **0–1** scale | Matches ML convention; migration divides legacy values by 100 |
| D-6 | Masks stored **inline** as COCO RLE | Atomic with the annotation; no object store, no orphaned blobs |
| D-7 | Vertices stored as **interleaved pairs** `[x0,y0,x1,y1,…]` | One array cannot desynchronize against itself (fixes §2.3) |
| D-8 | **Legacy association pathway removed** (reads and writes); existing rows converted by a SQL migration routine | Chosen explicitly. A read projection is not feasible: reproducing legacy pixel JSON would need image dimensions the service does not have (§7) |
| D-9 | No cross-table imaged-moment integrity to enforce | With no `image_reference_uuid`, a localization cannot point at another moment's image. Historical cases are surveyed, then discarded (§8.1) |
| D-10 | `training` is a boolean column, not a `tags` array | The only tag in use is `training`. Revisit if a second tag appears |
| D-11 | `truncated` boolean for boxes clipped at the frame edge | Lets the box stay valid (`x + width <= 1`) without hiding that the object was cut off. Same meaning as VOC's `truncated` |
| D-12 | Migration is a **SQL routine**, run by an operator, not by Flyway at startup | vampire-squid's `video_references` is reachable from SQL (same database, or `M3_VIDEO_ASSETS.dbo` on the same server). A routine can be dry-run, reviewed, and re-run as a delta pass at cutover |

### 4.1 Why the natural key does most of the work

`UNIQUE (observation_uuid, localization_type)` is the load-bearing constraint. Because at most one
bounding box can exist per observation, the write becomes an **idempotent upsert against a natural
key** rather than a `POST` that can create a duplicate. That single change removes an entire class of
conflict: two tools writing a box for the same observation now provably converge on one row instead
of racing to create two, and a version token on that row (§7.2) turns a silent overwrite into an
explicit `409`.

## 5. Schema

### 5.1 PostgreSQL

```sql
CREATE TABLE "localizations" (
    "uuid"                    uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
    "observation_uuid"        uuid NOT NULL,
    "localization_type"       varchar(32) NOT NULL,

    -- Axis-aligned extent, normalized to [0,1] with a top-left origin.
    -- ALWAYS populated; server-derived for line / polygon / mask so that
    -- every type is spatially queryable.
    "x"                       double precision NOT NULL,
    "y"                       double precision NOT NULL,
    "width"                   double precision NOT NULL,   -- 0 for point
    "height"                  double precision NOT NULL,   -- 0 for point

    -- line / polygon: interleaved JSON pairs [x0,y0,x1,y1,...], normalized
    "vertices"                text NULL,

    -- mask: a pixel raster covering the WHOLE frame, so these are pixels, not normalized
    "mask_encoding"           varchar(32) NULL,            -- v1: 'coco_rle' only
    "mask_data"               text NULL,
    "mask_width_pixels"       integer NULL,
    "mask_height_pixels"      integer NULL,

    -- provenance
    "generator"               varchar(128) NULL,
    "confidence"              real NULL,                   -- 0..1
    "observer"                varchar(128) NULL,
    "observation_timestamp"   timestamptz NULL,
    "verifier"                varchar(128) NULL,
    "verification_timestamp"  timestamptz NULL,
    "comment"                 varchar(1024) NULL,

    -- flags
    "training"                boolean NOT NULL DEFAULT false,
    "truncated"               boolean NOT NULL DEFAULT false,

    "last_updated_timestamp"  timestamp NOT NULL DEFAULT now(),

    CONSTRAINT "fk_localizations__observation_uuid"
        FOREIGN KEY ("observation_uuid") REFERENCES "observations"("uuid"),

    CONSTRAINT "uk_localizations__observation_uuid_type"
        UNIQUE ("observation_uuid", "localization_type"),

    CONSTRAINT "ck_localizations__type" CHECK (
        "localization_type" IN ('bounding_box','point','line','polygon','mask')),

    CONSTRAINT "ck_localizations__confidence" CHECK (
        "confidence" IS NULL OR ("confidence" >= 0 AND "confidence" <= 1)),

    -- Normalized bounds. The 1e-6 slack on the far edge is the same EPS the
    -- validators use (§6.2); it absorbs accumulated double error in x + width.
    CONSTRAINT "ck_localizations__extent" CHECK (
        "x" >= 0 AND "y" >= 0
        AND "width" >= 0 AND "height" >= 0
        AND "x" + "width" <= 1.000001 AND "y" + "height" <= 1.000001),

    CONSTRAINT "ck_localizations__shape" CHECK (
        ("localization_type" = 'point'
            AND "width" = 0 AND "height" = 0
            AND "vertices" IS NULL AND "mask_data" IS NULL)
     OR ("localization_type" = 'bounding_box'
            AND "width" >= 1e-9 AND "height" >= 1e-9
            AND "vertices" IS NULL AND "mask_data" IS NULL)
     OR ("localization_type" IN ('line','polygon')
            AND "vertices" IS NOT NULL AND "mask_data" IS NULL)
     OR ("localization_type" = 'mask'
            AND "vertices" IS NULL
            AND "mask_encoding" IS NOT NULL AND "mask_data" IS NOT NULL
            AND "mask_width_pixels" IS NOT NULL AND "mask_height_pixels" IS NOT NULL))
);

CREATE INDEX "idx_localizations__observation_uuid"  ON "localizations"("observation_uuid");
CREATE INDEX "idx_localizations__localization_type" ON "localizations"("localization_type");
CREATE INDEX "idx_localizations__generator"         ON "localizations"("generator");
```

### 5.2 SQL Server

Same shape, dialect-adjusted to match the conventions already in
`db/migrations/sqlserver/V1.0.0__init.sql`:

| PostgreSQL | SQL Server |
|---|---|
| `uuid` | `uniqueidentifier` |
| `uuid_generate_v4()` | `NEWID()` |
| `double precision` | `float(53)` |
| `real` | `float(24)` |
| `text` | `varchar(max)` |
| `boolean` | `bit` |
| `timestamptz` | `datetimeoffset(6)` |
| `timestamp` | `datetime2(6)` |
| `now()` | `SYSUTCDATETIME()` |

Every `CHECK`, `UNIQUE`, and `FOREIGN KEY` above is portable to SQL Server as written.

### 5.3 Foreign key delete behavior

`observation_uuid` — delete of an observation cascades to its localizations. JPA
`orphanRemoval = true` on the `@OneToMany` handles this consistently with how
`AssociationEntity` is already managed.

There is no longer a foreign key to `image_references`, so deleting an image reference has no effect
on localizations. (An earlier draft proposed `ON DELETE RESTRICT` there; that is moot.)

### 5.4 Type matrix

| Type | `x`/`y` | `width`/`height` | `vertices` | `mask_*` |
|---|---|---|---|---|
| `bounding_box` | authoritative | authoritative, `> 0` | null | null |
| `point` | authoritative | must be `0` | null | null |
| `line` | derived | derived | required, ≥ 2 pairs | null |
| `polygon` | derived | derived | required, ≥ 3 pairs | null |
| `mask` | derived | derived | null | required |

"Derived" means the server computes the axis-aligned bounding box of the geometry on write. The
client does not supply it and cannot override it. This is what makes *"find every localization
overlapping this region"* a single query with no type branching and no null handling. A perfectly
horizontal or vertical line legitimately derives a zero `height` or `width`, which is why only
`bounding_box` requires strictly positive extents.

For a `mask`, the extent is the bounding box of the set pixels divided by
`mask_width_pixels` × `mask_height_pixels`.

## 6. Contracts

### 6.1 Vertex encoding

`vertices` is a JSON array of finite numbers in `[0,1]`, **interleaved** as `[x0,y0,x1,y1,…]`, of even
length. Interleaving rather than the current parallel-array form is deliberate: a single array cannot
desynchronize against itself, which removes defect §2.3 by construction.

- `line` — at least 2 pairs.
- `polygon` — at least 3 pairs, **implicitly closed**. The first vertex is *not* repeated at the end.
  This matches the observed data: in the real polygon example the first pair `(2208,1525)` and the
  last `(4252,1449)` differ.

**Consecutive duplicate vertices are normalized, not rejected.** The real polygon example ends in
`(4252,1449)` twice. A naive "no duplicate vertices" rule would reject existing production data
during migration. The server instead collapses consecutive duplicates on write, and the migration
applies the same normalization. Normalization is idempotent, so a round-trip is stable.

### 6.2 Coordinate space

Coordinates are **normalized, continuous, and top-left-origin**, the model used by YOLO, COCO,
torchvision, TF Object Detection, and Albumentations (full rationale in
[bounding-box-coordinates.md](bounding-box-coordinates.md)):

- `(0, 0)` is the top-left corner of the frame; `(1, 1)` is the bottom-right corner.
- An integer pixel coordinate `k` names the **edge before** pixel `k`. Pixel `i` covers the half-open
  interval `[i, i+1)`. For a 1920-wide frame, `x = 1.0` is pixel-space 1920 — the outer edge of the
  last pixel (index 1919), not a pixel index.
- Normalizing from pixels is pure scaling: `x / imageWidth`, `width / imageWidth`, and so on.

**Media assumption.** Normalized coordinates are only meaningful if every image and video attached to
an observation shows the **same field of view at the same aspect ratio, uncropped and unletterboxed**.
This holds for our data in practice (images and videos for an observation share size and aspect
ratio), and it is what allows a box drawn on a framegrab to be rendered on a proxy video. It is an
assumption, not something the service can verify; §8.1 surveys the existing data against it.

**Validation** runs on every write and needs no image or video dimensions:

| Type | Rule |
|---|---|
| all | every coordinate finite and within `[0, 1]` |
| `bounding_box` | `width ≥ MIN_EXTENT`, `height ≥ MIN_EXTENT`, `x + width ≤ 1`, `y + height ≤ 1` |
| `point` | `x`, `y` in `[0, 1]`; extent exactly `0` |
| `line` / `polygon` | every vertex in `[0, 1]`; derived extent inherits the box rule |
| `mask` | the one exception — the RLE is a pixel raster, validated for well-formedness and that its decoded size equals `mask_width_pixels` × `mask_height_pixels` |

`EPS = 1e-6` absorbs accumulated double error, and `MIN_EXTENT = 1e-9` guards against subnormal and
corrupt extents. Both come from the companion doc; `EPS` and the precision of any text serialization
must move together.

**Snapping is done on edges, never on origin and extent separately.** A near-boundary value is snapped
onto `0` or `1` (a box's near edges `x`, `y`, its far edges `x + width`, `y + height`, and each vertex),
and `width` / `height` are then *derived* from the snapped edges. Snapping `x` alone leaves `width`
unchanged and moves the far edge: a box with `y = -7.8e-7` and `height = 1.00000135` is inside `EPS` as
given, but snapping `y` to `0` alone makes `y + height` exceed `1 + EPS` and the database `CHECK`
rejects it. (Found by differential testing of the companion code against §5.1; see §6.4.)

**Truncated objects.** Real footage has organisms entering and leaving the frame, so a box may arrive
extending past the edge. Rather than reject it, the client (or the migration routine) clamps it to the
frame and sets `truncated = true`. The box stays valid, and downstream training can still tell a
half-visible animal from a whole one. A box with no overlap with the frame at all is rejected.

**Precision.** The columns are `double precision` and the REST payload carries full doubles, so
annosaurus itself introduces no quantization. A client that writes text (a YOLO label file, for
instance) must pick a precision deliberately and keep `EPS` consistent with it.

**Conversion is the client's job.** Normalized ↔ float pixels is exact and invertible; quantizing to
whole pixels (for cropping) quantizes the *edges* — `floor` the minimum, `ceil` the maximum, derive the
width — never origin and extent independently. Pixel-to-normalized-to-pixel round trips need the
integer snap described in the companion doc (`(15.0/22.0) * 22.0 == 14.999999999999998`). YOLO
(center-based, already normalized) maps one-to-one in the storage direction but can arrive out of bounds
and needs an explicit strict/visible policy. Pascal VOC has no single coordinate convention, so the
convention must be stated at every conversion and recorded with the data. All of that lives with the
reference implementations, not in this service.

### 6.3 Provenance and flags

| Column | Meaning |
|---|---|
| `generator` | The tool or model that produced it, e.g. `mbari452k_yolov8` |
| `confidence` | **0–1.** `CHECK (confidence BETWEEN 0 AND 1)` |
| `observer` | Who drew it, distinct from the observation's own observer |
| `observation_timestamp` | When it was drawn |
| `verifier` | Who confirmed it, e.g. `kwalz` |
| `verification_timestamp` | When it was confirmed |
| `comment` | Free text about the localization itself |
| `training` | Include this localization in training sets. Replaces the legacy `tags: ["training"]` |
| `truncated` | The object extends past the frame edge and the geometry was clipped to it |

`verification_timestamp` earns its place by making staleness detectable: if
`verification_timestamp < last_updated_timestamp`, the geometry has been edited since it was approved,
and the verification should no longer be trusted. That question is unanswerable today.

> **Note on `confidence`.** The scale is 0–1 to match what classifiers actually emit, so a native
> softmax/sigmoid score is stored unchanged. A legacy writer still emitting 0–100 is now caught loudly
> for any value above 1 (the `CHECK` rejects `43.0`). The remaining silent case is a legacy value
> `≤ 1` that really meant a percentage. Separately, `confidence` is a classification-plus-objectness
> score (per YOLO), not a property of the geometry alone; it stays on this table for now because it
> remains useful, but it is a candidate to move if classification confidence ever gets its own home.

### 6.4 Companion code: where it matches this spec

[bounding-box-coordinates.md](bounding-box-coordinates.md) holds the reference Java for the client
side of §6.2. It was checked against this spec by compiling it (it builds clean with `-Xlint:all` on
`--release 21`) and by running it against the §5.1 `CHECK` constraints and the values the migration
routine produced.

| This spec | Companion doc | Result |
|---|---|---|
| Box validation: `width, height ≥ MIN_EXTENT`, `x + width ≤ 1`, `y + height ≤ 1` (§6.2, `ck_localizations__extent` / `__shape`) | §5 constraints; `NormalizedBoundingBox` constructor | **Match.** Across 2M boxes concentrated on the boundaries there is no box the database accepts that the constructor rejects |
| `EPS = 1e-6`, `MIN_EXTENT = 1e-9` (§6.2; the `1.000001` slack in the `CHECK`) | §4, §7; `NormalizedBoundingBox.EPS` / `MIN_EXTENT` | **Match** |
| Points: `x`, `y` in `[0,1]`, extent `0` | §6 `NormalizedPoint` | **Match**, including `x = 1.0` mapping to the last pixel |
| `confidence` on `[0,1]` (D-5) | §7 `YoloBoundingBox` | **Match.** It rejects `43.0`, the legacy scale |
| Normalizing is pure scaling `x / W` (§6.2) | `PixelBoundingBox.normalized` | **Match.** Real example #1 gives the same values the migration routine stored |
| Quantize *edges*, with the integer snap (§6.2) | `toCropRegion`, `snap` | **Match** |
| Valid boxes always export to YOLO and back | §7 `toLabelLine` / `parseLabelLine` | **Match.** 300k boxes, many flush to the frame edge, never throw, which supports the `EPS` / `%.6f` coupling |
| Near-boundary values are snapped by edge, then derived (§6.2) | none | **Gap 1.** The constructor tolerates `x` and `y` down to `-EPS`, but the database `CHECK` is exactly `>= 0`. Those boxes only reach storage if something snaps them, and no Java does |
| A client clamps a box that crosses the frame edge and sets `truncated` (§6.2) | `clampToImage()`; `PixelBoundingBox.normalized` | **Gap 2.** The constructor rejects out-of-bounds boxes, so `clampToImage()` can only ever change something within `EPS`, and `normalized()` throws on a box that crosses the edge. `YoloBoundingBox.toNormalizedVisible()` clamps but returns no `truncated` flag, and VOC has no equivalent. The migration routine does exactly this clamp-and-flag in SQL; the client code has nothing to match |
| `line`, `polygon`, `mask` are normalized (§5.4, §6.1) | none | **Gap 3.** Only `bounding_box` and `point` have reference code. A line's derived extent can be zero-width or zero-height, which `NormalizedBoundingBox` cannot represent, so a row of those types must not be mapped onto it |
| Source dimensions are **not** stored (§11.2) | §8 and §9: "store the source image dimensions", marked settled | **Conflict in prose.** The code is fine, since `VocAnnotation` takes `width` and `height` from the file or the caller, but the open item says the opposite of this spec |
| `difficult` has no column (open question 6) | `VocObject.difficult`; §9 "preserve `difficult` on ingest" | **Open.** A VOC import has nowhere to put it |
| Precision: columns are `double`, no quantization (§6.2) | §9 "confirm the serialized precision" | **Resolved by this spec.** The only `%.6f` is in the YOLO text writer |

## 7. Removal of the legacy association pathway

This is a hard cutover (decision D-8). Neither side of the legacy pathway survives, and the reason is
the same for both: with normalized storage, the service cannot produce the legacy shape.

### 7.1 What changes for association endpoints

**Reads.** Association queries exclude rows whose `link_name` is in the localization set, so no
client sees a localization as an association:

```sql
-- association reads, from cutover onward
WHERE a.link_name NOT IN ('bounding box','localization-point',
                          'localization-line','localization-polygon','localization-mask')
```

There is **no read projection.** An earlier draft synthesized legacy associations from the new table
so existing readers would keep working. That is not feasible here: the legacy JSON holds pixels, and
reproducing it would require each image's dimensions, which the service deliberately does not have.
A projection that returned normalized values under the legacy keys would be worse than none — it
would parse fine and be silently wrong. Readers move to `Annotation.localizations` or the endpoints
below.

**Writes.** Any attempt to create or modify an association whose `link_name` is in the localization set
is rejected with `400 Bad Request` and a message naming the localization endpoints. This includes
`PUT`/`DELETE` of a legacy row by `uuid`. The guard must live at a single choke point shared by every
path that accepts an `Association` — `/v1/associations`, and every annotation, imaged-moment, and bulk
endpoint that can carry associations in its payload — rather than being repeated per endpoint. The
exact list is enumerated during implementation.

Rejected attempts are logged at `WARN`, rate-limited per `(link_name, user_agent)` with a counter, so
stragglers are findable without a bulk ingest logging fifty thousand lines:

```
WARN LegacyLocalizationWriteRejected path=POST /v1/associations link_name="localization-polygon"
     user_agent="vars-annotation/1.9.2" remote_addr=10.0.4.17 count_since_last_report=1432
```

> **Attribution is weaker than it looks.** `BasicJwtService` mints tokens carrying only `iss`, `iat`,
> and `exp`, validated against a **single shared `apiKey`**. There is no `sub` claim and no
> per-client credential, so **the bearer token cannot identify which tool made the call.** The only
> usable attribution signals are the `User-Agent` header and, where present, the payload itself. If
> chasing down stragglers matters, per-client secrets or a `sub` claim is a prerequisite, and that is
> a separate change to the auth layer.

**The `annotations` view.** It exposes `ass.link_name` / `ass.link_value` directly from the
`associations` table, and it is a public read surface. From cutover it must exclude the localization
link names, matching the association reads above. Localizations are not added to the view: it is
flat, and an observation can have up to five. Whether the view should expose them some other way is an
open question (§13).

### 7.2 New endpoints

```
GET    /v1/localizations/{uuid}
GET    /v1/localizations/observation/{observation_uuid}          -> 0..5 localizations
GET    /v1/localizations/videoreference/{video_reference_uuid}   (paged; ML export)
PUT    /v1/localizations/{observation_uuid}/{localization_type}  (upsert)
DELETE /v1/localizations/{observation_uuid}/{localization_type}
POST   /v1/localizations/bulk                                    (ML pipelines)
GET    /v1/localizations/list/types                              (list the 5 localization_types)
GET    /v1/localizations/list/maskencodings                      (list accepted values for mask_encoding)
GET    /v1/localizations/list/constraints                        (return localization_types, mask_encodings)
```

There is no `imagereference` lookup: localizations no longer reference image references. To find the
localizations for an image, resolve its imaged moment first.

`PUT` against the natural key is idempotent. It optionally accepts the caller's
`last_updated_timestamp`; if supplied and stale, the server returns **`409 Conflict`** with the
current row rather than overwriting. This is the mechanism that converts a silent stomp into a
recoverable error, and it is the single most important behavioral change in this proposal. A `PUT`
without the timestamp is last-write-wins, which matches de facto behavior today.

`/list/constraints` is the preferred way for a tool to discover what it may write; the two narrower
list endpoints are conveniences over the same data.

Reads follow the non-blocking classification; writes go on the blocking worker pool, per
`Endpoints.scala`.

`Annotation` gains `localizations: Seq[Localization] = Nil`, with the matching `AnnotationSC`
snake-case variant and Circe codecs. `associations` never contains localizations.

## 8. Migration

The changeover is only acceptable if every existing localization either arrives in `localizations` or
is accounted for in a quarantine table. First survey what is there (§8.1), then convert it with a SQL
routine that quarantines whatever it cannot (§8.2–8.3).

### 8.1 Survey first

These run before anything is written. Each answers a question that changes the migration. They assume
well-formed JSON in `link_value` (PostgreSQL 16's `pg_input_is_valid(link_value, 'jsonb')` can screen
for malformed rows first); on SQL Server, use `ISJSON`, `JSON_VALUE`, and `OPENJSON`, which **require
compatibility level 130 (SQL Server 2016) or higher.** They are read-only and safe to run against
production.

```sql
-- (a) Confirm the complete set of localization link names. Only five are known;
--     this catches any type in use that this proposal has not accounted for.
SELECT link_name, mime_type, COUNT(*)
FROM associations
WHERE mime_type = 'application/json'
   OR link_name LIKE 'localization%'
   OR link_name = 'bounding box'
GROUP BY link_name, mime_type
ORDER BY 3 DESC;

-- (b) Which JSON keys are in use? Anything outside
--     x, y, width, height, generator, confidence, verifier, tags, image_reference_uuid
--     is NOT migrated, so each surprise needs a decision.
SELECT k, COUNT(*)
FROM associations a, LATERAL jsonb_object_keys(a.link_value::jsonb) AS k
WHERE a.link_name IN ('bounding box','localization-point',
                      'localization-line','localization-polygon')
GROUP BY k ORDER BY 2 DESC;

-- (c) Which tags are in use? Only 'training' is mapped; anything else is dropped.
SELECT t, COUNT(*)
FROM associations a,
     LATERAL jsonb_array_elements_text(a.link_value::jsonb -> 'tags') AS t
WHERE a.link_name = 'bounding box'
  AND jsonb_typeof(a.link_value::jsonb -> 'tags') = 'array'
GROUP BY t ORDER BY 2 DESC;

-- (d) THE BLOCKER: observations with more than one localization of the same type.
--     The UNIQUE constraint makes these unmigratable as-is.
SELECT link_name, COUNT(*) AS n_conflicts FROM (
    SELECT observation_uuid, link_name
    FROM associations
    WHERE link_name IN ('bounding box','localization-point',
                        'localization-line','localization-polygon')
    GROUP BY observation_uuid, link_name
    HAVING COUNT(*) > 1
) d GROUP BY link_name;

-- (e) Where will the dimensions come from? Every legacy pixel value needs a width and
--     height to normalize against. 'orphaned_image_reference' is the dangerous bucket:
--     the referenced image row is already gone, and it must not silently become a
--     video-frame localization. 'no dimensions anywhere' will be rejected by the migration.
--     (On MBARI's SQL Server: M3_VIDEO_ASSETS.dbo.video_references. Elsewhere: video_references.)
WITH loc AS (
    SELECT a.uuid, o.imaged_moment_uuid,
           (a.link_value::jsonb ->> 'image_reference_uuid')::uuid AS image_reference_uuid
    FROM associations a
    JOIN observations o ON o.uuid = a.observation_uuid
    WHERE a.link_name IN ('bounding box','localization-point',
                          'localization-line','localization-polygon')
)
SELECT CASE
         WHEN loc.image_reference_uuid IS NOT NULL AND ir.uuid IS NULL
                                              THEN 'orphaned_image_reference'
         WHEN ir.width_pixels > 0 AND ir.height_pixels > 0
                                              THEN 'image_reference_dimensions'
         WHEN vr.width > 0 AND vr.height > 0  THEN 'video_dimensions'
         ELSE 'no dimensions anywhere'
       END AS dimension_source,
       COUNT(*)
FROM loc
LEFT JOIN image_references ir ON ir.uuid = loc.image_reference_uuid
LEFT JOIN imaged_moments im   ON im.uuid = loc.imaged_moment_uuid
LEFT JOIN video_references vr ON vr.uuid = im.video_reference_uuid
GROUP BY 1;

-- (f) Prevalence of the historical cross-moment case: a localization whose image
--     reference belongs to a different imaged moment than its observation. VARS Localize
--     once pulled annotations from nearby moments onto a framegrab's image reference.
--     After this change the case simply stops existing, but the number says how many
--     localizations were drawn on an image from another moment.
WITH loc AS (
    SELECT a.observation_uuid,
           (a.link_value::jsonb ->> 'image_reference_uuid')::uuid AS image_reference_uuid
    FROM associations a
    WHERE a.link_name IN ('bounding box','localization-point',
                          'localization-line','localization-polygon')
)
SELECT COUNT(*)
FROM loc
JOIN observations o      ON o.uuid  = loc.observation_uuid
JOIN image_references ir ON ir.uuid = loc.image_reference_uuid
WHERE ir.imaged_moment_uuid <> o.imaged_moment_uuid;

-- (g) Values at risk: link_values at the 1024 ceiling, i.e. candidates for
--     prior silent truncation. (LENGTH -> LEN on SQL Server.)
SELECT COUNT(*) FROM associations
WHERE link_name LIKE 'localization-%' AND LENGTH(link_value) >= 1020;

-- (h) Who writes these? Inventory of generators, to cross-check against the list of
--     localization-dependent apps.
SELECT link_name, link_value::jsonb ->> 'generator' AS generator, COUNT(*)
FROM associations
WHERE link_name IN ('bounding box','localization-point',
                    'localization-line','localization-polygon')
GROUP BY 1, 2 ORDER BY 3 DESC;
```

```sql
-- (i) THE MEDIA ASSUMPTION (section 6.2): for every image reference with dimensions, does its
--     aspect ratio match its video's? Each mismatch is a localization whose normalized
--     coordinates would be wrong. Expected: zero, or a number small enough to fix by hand.
SELECT COUNT(*)
FROM image_references ir
JOIN imaged_moments im   ON im.uuid = ir.imaged_moment_uuid
JOIN video_references vr ON vr.uuid = im.video_reference_uuid
WHERE ir.width_pixels > 0 AND ir.height_pixels > 0 AND vr.width > 0 AND vr.height > 0
  AND ABS(ir.width_pixels::float / ir.height_pixels - vr.width::float / vr.height)
      / (vr.width::float / vr.height) > 0.01;
```

The dry run of the migration routine (§8.2) reports the same aspect-ratio check restricted to the
localizations being migrated, and how many would be clamped with `truncated = true`.

Alongside the queries, write down which apps **read** and which **write** localizations. The survey
of `generator` values (h) scopes that list, and it is the checklist for the cutover (§12).

### 8.2 The migration routine

Normalizing a pixel needs the frame's dimensions. Image sizes are in annosaurus's own
`image_references`; video sizes are in vampire-squid's `video_references` (`uuid`, nullable `width`
and `height`). Both are reachable from SQL: MBARI's production SQL Server hosts the vampire-squid
database (`M3_VIDEO_ASSETS`) on the same server, and external deployments run both services against a
single database. So the conversion is plain SQL.

Two scripts, one per dialect, are drafted in [`localizations-migration/`](localizations-migration/):
[`postgres.sql`](localizations-migration/postgres.sql) and
[`sqlserver.sql`](localizations-migration/sqlserver.sql). Each becomes `V1.1.0__localizations.sql` for
its dialect and contains:

1. The `localizations` and `localization_migration_rejects` tables (§5).
2. A routine, `migrate_localizations()` on PostgreSQL and `dbo.migrate_localizations` on SQL Server.

**Flyway creates the routine; it does not run it.** Converting data at startup would leave no room to
review the rejects or to re-run at cutover. An operator runs the routine:

```sql
-- dry run: do everything, report, roll back
BEGIN; SELECT * FROM migrate_localizations(); ROLLBACK;                   -- PostgreSQL
BEGIN TRAN; EXEC dbo.migrate_localizations; ROLLBACK;                      -- SQL Server

-- for real
SELECT * FROM migrate_localizations();                                     -- PostgreSQL
EXEC dbo.migrate_localizations;                                            -- SQL Server
```

It returns one row per outcome: rows migrated, how many of those were clamped (`truncated`), a count
per rejection reason, and an informational count of rows whose image and video aspect ratios differ by
more than 1% (the media assumption, §6.2).

**Where vampire-squid lives.** The scripts take one Flyway placeholder, `${videoReferencesTable}`:

| Deployment | Value |
|---|---|
| Single database (external users, and the default) | `video_references` (PostgreSQL), `dbo.video_references` (SQL Server) |
| MBARI production SQL Server | `M3_VIDEO_ASSETS.dbo.video_references` |

`FlywayMigrator` must supply it from configuration (a new `database.*` key, overridable by environment
variable like the rest), defaulting to the single-database value. The database user needs `SELECT` on
that table. On SQL Server the name is bound through a synonym, and on PostgreSQL the reference sits
inside the function body, so in both cases it is resolved when the routine **runs**. The integration
suites, which have no vampire-squid tables, therefore migrate cleanly; running the routine in such a
database fails with a plain "object does not exist" error.

**What the routine does,** for each legacy localization association:

1. **Resolve dimensions**, in this order:
   1. `image_reference_uuid` in the JSON, and that row exists with a positive width and height →
      the image's `width_pixels` / `height_pixels`.
   2. `image_reference_uuid` in the JSON but the row **does not exist** → quarantine as
      `orphaned_image_reference`. It is never silently treated as a video frame.
   3. No `image_reference_uuid`, or the image row exists but has no size → the source video's
      `width` / `height`, via `observations → imaged_moments.video_reference_uuid →
      video_references`. Falling back for a sizeless image relies on the media assumption (§6.2).
   4. Nothing resolves → quarantine as `unresolved_dimensions`.
2. **Convert** pixels to normalized coordinates (`x / W`, `y / H`, and so on). For a bounding box the
   *edges* are normalized and then clamped, with the width derived from them. Line and polygon
   vertices are interleaved and normalized the same way, then written at 9 decimal places (far finer
   than `EPS`, §6.2).
3. **Map fields:**

   | Legacy | New |
   |---|---|
   | `x`, `y`, `width`, `height`, `x[]`, `y[]` | `x`, `y`, `width`, `height`, `vertices` (normalized) |
   | `generator`, `verifier` | `generator`, `verifier` (truncated to 128 characters) |
   | `confidence` (0–100) | `confidence` ÷ 100 |
   | `tags` containing `"training"` | `training = true` |
   | any other `tags` value, any other key | **not migrated**; surfaced by survey (b) and (c) |
   | association `uuid` | localization `uuid` (reused, so a migrated row traces to its source) |
   | association `last_updated_timestamp` | `last_updated_timestamp` |

4. **Clamp, don't reject, when geometry crosses the frame edge:** clamp to `[0,1]` and set
   `truncated = true`, for every type. A box with no overlap with the frame at all is quarantined as
   `entirely_out_of_bounds`.
5. **Normalize** consecutive duplicate vertices (§6.1), and enforce the minimum vertex count.
6. **Choose one per `(observation_uuid, localization_type)`:** the newest valid row wins, and every
   other valid row is quarantined as `duplicate_type`. That includes a row that loses to a
   localization already in the table, whether migrated on an earlier run or written through the new
   endpoints since. The routine never overwrites.
7. **Copy** the winners into `localizations`.

The routine is **idempotent**: it skips associations that are already migrated and rebuilds the
rejects table from scratch each run. That is what makes the final delta pass at cutover (§12) safe.

**Status.** Both scripts have been run against the real annosaurus schema (PostgreSQL 17 and
SQL Server 2022) with the same 20 seeded legacy cases, including the real examples from §2. Both
produce identical results, including a dry run that leaves no rows behind and a second run that
changes nothing. They have not been run against production data or a real vampire-squid database.
SQL Server needs **2017 or later** for `STRING_AGG`, **and the database's compatibility level must be
140 or higher**. MBARI's production server is SQL Server 2025, but a database restored from an older
server keeps its old compatibility level, so check it: `SELECT compatibility_level FROM sys.databases
WHERE name = DB_NAME()`. The script was run on SQL Server 2022, not 2025; the T-SQL it uses is all
available in both, but the run is worth repeating on a 2025 instance. (`LEAST`/`GREATEST`, available
since 2022, would shorten the clamping `CASE` expressions; they are not used so the script also runs on
older instances.)

### 8.3 Quarantine, never drop

Rejected rows go to `localization_migration_rejects` (`association_uuid`, `observation_uuid`,
`link_name`, the original `link_value`, `reason`, and a timestamp), defined in the scripts above.

Rejection reasons: `duplicate_type` (lost the tie-break), `malformed_json` (not a JSON object, or a
required key is missing or non-numeric), `array_length_mismatch`, `confidence_out_of_range`,
`insufficient_vertices`, `invalid_geometry` (a box with zero or negative size), `unknown_link_name`
(including `localization-mask`, which has no legacy form), `orphaned_image_reference`,
`unresolved_dimensions`, `entirely_out_of_bounds`.

Conflict resolution for duplicates: **keep the valid row with the newest `last_updated_timestamp`,
quarantine the rest.** Nothing is deleted. The rejects table is the migration's audit trail and
should be reviewed before the legacy rows are deleted. A reject can be repaired by correcting the
legacy association and re-running the routine; because rejects are rebuilt on every run, a repaired
row simply moves into `localizations`.

### 8.4 What the migration does *not* do

The migration copies; it does not delete. Legacy association rows survive cutover untouched — they are
simply excluded from reads (§7.1). Deleting them is a separate, later step taken only after the rejects
table has been reviewed. Until then, the application version can be rolled back and the legacy rows
are still there (but see §12 for what rollback costs after new writes begin).

## 9. Audit and messaging

`LocalizationEntity` carries `@org.hibernate.envers.Audited` and
`@EntityListeners({TransactionLogger.class, TransactionNotifier.class})`, matching
`AssociationEntity`.

Auditing is not incidental here. Envers history on this table is what finally makes the tug-of-war
*visible*: who changed a box, when, from what, to what. That question cannot be answered today at all.
Log at `WARN` whenever a write **replaces an existing localization whose `generator` or `verifier`
differs from the incoming one** — that is the tug-of-war event itself, and it is currently invisible.

**Breaking change for ZeroMQ subscribers.** A bounding box edit currently fires an *association*
change notification. Once the localization table is authoritative, it fires a *localization*
notification instead, and no association notification is ever emitted for a localization again. Any
subscriber filtering on association events for localization link names stops seeing them. This must
be in the release notes, and subscribers need updating in step with cutover.

## 10. Testing

| Area | Coverage |
|---|---|
| Validators | Vertex parsing, odd-length arrays, non-finite values, minimum vertex counts, consecutive-duplicate normalization, confidence range `[0,1]`, per-type shape rules, `EPS` snapping, `MIN_EXTENT` |
| Constraints | `it-postgres` and `it-sqlserver`: inserting a second localization of the same type fails; every `ck_localizations__shape` and `ck_localizations__extent` branch rejects what it should |
| Endpoints | `LocalizationEndpointsSuite` in both `it-postgres` and `it-sqlserver`, following `BaseEndpointsSuite` |
| Concurrency | Stale `last_updated_timestamp` on `PUT` yields `409`, not an overwrite |
| Legacy rejection | Every association-accepting path returns `400` for each localization link name, including `PUT`/`DELETE` by `uuid` of a legacy row; non-localization link names are unaffected |
| Legacy exclusion | Association reads and the `annotations` view omit legacy localization rows while those rows still exist on disk |
| Bounds | Enforced on every write with no dimensions needed; boundary cases at exactly `0` and `1`; a point at `x = 1.0` is accepted |
| Round trip | Property tests: pixel → normalized → pixel within `EPS`, including the integer-snap cases (`15/22`, `13/23`, `31/39`) |
| Companion code | A differential test of the reference `NormalizedBoundingBox` against the §5.1 `CHECK` constraints on boundary-heavy random boxes, so the Java and the database cannot drift apart (§6.4). It already found the snapping gap in §6.2 |
| Migration | Seed legacy associations including duplicates, malformed JSON, mismatched array lengths, an orphaned `image_reference_uuid`, unresolved dimensions, a box crossing the frame edge, `tags` with and without `training`, 0–100 confidences, and the real degenerate polygon; run the routine; assert correct rows migrated, `truncated` set, confidence divided, dimensions taken from the right source, and every reject quarantined with the right reason. A dry run leaves no rows; a second run changes nothing. Run on both dialects, including with vampire-squid in a separate database |
| Flyway | The `${videoReferencesTable}` placeholder resolves; the `it-*` suites still migrate with no vampire-squid tables present |
| Derived extent | Line/polygon/mask extents computed correctly; point extent is exactly `0`; a horizontal line has `height = 0` |

Tests run sequentially under Testcontainers with UTC enforced, per existing project setup.

## 11. Known gaps

### 11.1 Cross-table imaged-moment integrity — dropped (decision D-9)

An earlier draft recorded as a gap that a localization could reference an `image_references` row from
a *different* imaged moment than its own observation, and sketched composite foreign keys to prevent it.
That entire concern is dissolved by removing the image link.

The historical cause was VARS Localize applying a time window around a framegrab and pulling in
annotations from neighboring imaged moments, so boxes drawn on that image reference landed on
observations in another moment. Under normalized coordinates and the media assumption (§6.2), the image
is no longer part of the localization's identity, and nothing in the model can express "this box was
drawn on that image." Survey query (f) measures how often the old situation occurred, but nothing is
enforced and no mitigation is needed.

### 11.2 Accepted limitations

- **The media assumption is unenforceable** (§6.2). If media for one observation is ever cropped,
  letterboxed, or differs in aspect ratio, normalized coordinates will be wrong for some of it, with
  no error. The migration dry run checks existing data; nothing checks new data.
- **Source dimensions are not stored.** A localization cannot tell you what resolution it was drawn
  at, and a later re-encode at a different size is invisible. Exporters that need pixels (COCO, VOC
  `<size>`) must look up the media's dimensions at export time.
- **No fallback for orphaned image references** in the migration (§8.2); they are quarantined for a
  human decision. An image that exists but has no recorded size does fall back to the video's.
- **Legacy callers fail hard.** There is no compatibility window; a tool that is not updated gets a
  `400` on every localization write and silently sees no localizations in associations.
- **Legacy callers cannot be reliably attributed** (§7.1).
- **Masks are pixel rasters** and so cannot be normalized; they carry their own dimensions.
- **Other `tags` are dropped.** Only `training` survives (D-10).

## 12. Rollout

| Phase | Action | Reversible |
|---|---|---|
| 0 | Run the §8.1 survey and a dry run of the migration routine. Resolve duplicates (query d). Inventory localization-dependent apps (readers vs writers) | n/a |
| 1 | Ship the table, entity, controller, endpoints, and migration routine (created, not run). Nothing reads or writes the table yet; the association endpoints are unchanged | yes |
| 2 | Run the migration routine (copy only). Review `localization_migration_rejects`. Update client apps to the new endpoints in parallel, so they are ready to flip | yes — drop the rows |
| 3 | **Cutover**, in an announced window: stop legacy writers, run the routine again as a delta pass, deploy the application version that excludes legacy rows from reads and rejects legacy writes, switch clients, notify ZeroMQ subscribers | until new-path writes begin |
| 4 | Review rejects one last time, take a backup, delete legacy localization associations | **no** |

Phase 3 is the one that breaks clients, so it is gated on every app in the Phase 0 inventory having a
release ready. Rolling the *application* back after Phase 3 is possible, but any localization written
or edited through the new endpoints after cutover exists only in `localizations`, and the old version
cannot see it. Roll back early or not at all.

Phase 4 is the only irreversible step, and it is gated on the rejects review and the legacy-write
rejection logs having gone quiet.

## 13. Open questions

1. **Is there an existing mask link name?** Survey query (a) will confirm. Given the 1024-character
   ceiling, masks almost certainly do not exist in production today.
2. **When is the cutover window?** It is gated on the Phase 0 app inventory and on client releases,
   not on a `Sunset` date, since there is no deprecation period.
3. **Orphaned image references:** quarantine for manual repair (proposed), or fall back to video
   dimensions? The fallback is justified by the media assumption but is exactly the silent
   reinterpretation we want to avoid. Survey query (e) sizes the problem; if it is small, quarantine
   is cheap.
4. **Should `POST /v1/localizations/bulk` be transactional per-batch or per-row?** Per-row partial
   success suits ML ingest; per-batch suits correctness. Not yet decided.
5. **Should the `annotations` view expose localizations?** Currently proposed: no, it only drops the
   legacy rows (§7.1). Does anything downstream rely on seeing bounding boxes through it?
6. **`difficult` flag.** VOC's `difficult` must survive ingest or evaluation metrics silently change
   (see the companion doc). It is not a column today. Add it now, or when a VOC import exists?
7. **A second tag.** `training` is a boolean because it is the only tag in use. Is there a planned
   second one that would argue for a tags table or array instead?
8. **SQL Server compatibility level.** Production runs SQL Server 2025, which is fine, but the
   migration routine needs the *database* at compatibility level 140+ (`STRING_AGG`); the survey queries
   alone need 130. Confirm both the annosaurus and `M3_VIDEO_ASSETS` databases. Any external SQL Server
   deployment must be 2017+.
9. **Where does `${videoReferencesTable}` come from** — a new `database.*` config key with an
   environment override (proposed), and does the annosaurus database user have `SELECT` on
   `M3_VIDEO_ASSETS.dbo.video_references` in production?
