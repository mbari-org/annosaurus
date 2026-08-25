# Proposal: First-class localizations

- **Status:** Draft, for review
- **Date:** 2026-08-25
- **Affects:** schema, `Annotation` payload, a new endpoint group, Flyway migrations, ZeroMQ subscribers
- **Does not affect:** the association model for non-localization link names

## 1. Summary

Localizations (bounding boxes, points, lines, polygons, segmentation masks) are stored today as
`associations` rows whose `link_value` holds a JSON blob. Nothing in the database or the service
constrains that blob's shape, its cardinality, or its relationship to a specific image. Different
tools write different JSON for the same concept and overwrite each other silently.

This proposal promotes localizations to a first-class table, `localizations`, whose natural key is
`(observation_uuid, localization_type)`. An observation may carry **at most one localization of each
type** — up to one bounding box, one point, one line, one polygon, and one mask. Geometry becomes
real typed columns rather than negotiable JSON keys, and the optional link to a specific
`image_references` row becomes a real foreign key rather than a convention buried in a string.

The existing association representation is preserved as a **read-only projection** so current clients
keep working unchanged.

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
lines, and polygons. A tool that round-trips a localization through its own model silently drops
every key it does not know about. This is the mechanism behind the tug-of-war: it is not malice,
it is lossy deserialization.

**2.5 — `confidence` has no declared scale.** The observed values `43.0` and `17.0` imply a 0–100
percentage, but nothing states it. A tool emitting a native 0–1 softmax score would write `0.43`,
which is indistinguishable from a legitimate 0.43%.

**2.6 — Pixels with no declared frame.** The bounding box examples carry no image reference at all.
`x: 982` is meaningless without knowing which raster it indexes into. An imaged moment can have
several `image_references` at different resolutions, so the pixel space is genuinely ambiguous.

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
3. Make the link to a specific `image_references` row explicit and referentially enforced.
4. Remove the 1024-character ceiling so polygons and masks are storable.
5. Give writes a concurrency story so a second writer cannot silently discard a first writer's edit.
6. Break no existing client on day one.

**Non-goals**

- Changing the association model for anything that is not a localization.
- Adding geospatial (PostGIS) indexing or geometry types. Pixel coordinates are not geographic.
- Resolving video frame dimensions inside annosaurus. That stays with vampire-squid (§6.2).
- Enforcing cross-table imaged-moment consistency in this phase (§11.1).

## 4. Design decisions

| # | Decision | Rationale |
|---|---|---|
| D-1 | New table is the source of truth; associations become a **read-only projection** | Preserves every current reader while ending the two-writers problem |
| D-2 | **One table**, `localization_type` discriminator, hybrid columns | Uniform cross-type queries; bbox/point fully columnar; one entity, one DAO, one endpoint group |
| D-3 | **Pixel** coordinates, `image_reference_uuid` **nullable** | Matches what every tool already writes; null means video-frame space (§6.2) |
| D-4 | Provenance as real nullable columns | A column cannot be dropped by a tool that does not know about it |
| D-5 | `confidence` on a **0–100** scale | Matches existing data; migrates with no arithmetic |
| D-6 | Masks stored **inline** as COCO RLE | Atomic with the annotation; no object store, no orphaned blobs |
| D-7 | Vertices stored as **interleaved pairs** `[x0,y0,x1,y1,…]` | One array cannot desynchronize against itself (fixes §2.3) |
| D-8 | Legacy association **write** path kept, with deprecation signalling | Chosen explicitly; see §7.2 |
| D-9 | Cross-table imaged-moment integrity **not** enforced | Chosen explicitly; would break existing tools for a rare case. Recorded as a gap in §11.1 |

### 4.1 Why the natural key does most of the work

`UNIQUE (observation_uuid, localization_type)` is the load-bearing constraint. Because at most one
bounding box can exist per observation, the write becomes an **idempotent upsert against a natural
key** rather than a `POST` that can create a duplicate. That single change removes an entire class of
conflict: two tools writing a box for the same observation now provably converge on one row instead
of racing to create two, and a version token on that row (§7.3) turns a silent overwrite into an
explicit `409`.

## 5. Schema

### 5.1 PostgreSQL

```sql
CREATE TABLE "localizations" (
    "uuid"                    uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
    "observation_uuid"        uuid NOT NULL,
    "image_reference_uuid"    uuid NULL,
    "localization_type"       varchar(32) NOT NULL,

    -- Axis-aligned extent in pixels. ALWAYS populated; server-derived for
    -- line / polygon / mask so that every type is spatially queryable.
    "x_pixels"                double precision NOT NULL,
    "y_pixels"                double precision NOT NULL,
    "width_pixels"            double precision NOT NULL,   -- 0 for point
    "height_pixels"           double precision NOT NULL,   -- 0 for point

    -- line / polygon: interleaved JSON pairs [x0,y0,x1,y1,...]
    "vertices"                text NULL,

    -- mask
    "mask_encoding"           varchar(32) NULL,            -- v1: 'coco_rle' only
    "mask_data"               text NULL,
    "mask_width_pixels"       integer NULL,
    "mask_height_pixels"      integer NULL,

    -- provenance
    "generator"               varchar(128) NULL,
    "confidence"              real NULL,                   -- 0..100 percent
    "observer"                varchar(128) NULL,
    "observation_timestamp"   timestamptz NULL,
    "verifier"                varchar(128) NULL,
    "verified_timestamp"      timestamptz NULL,
    "comment"                 varchar(1024) NULL,

    "last_updated_timestamp"  timestamp NOT NULL DEFAULT now(),

    CONSTRAINT "fk_localizations__observation_uuid"
        FOREIGN KEY ("observation_uuid") REFERENCES "observations"("uuid"),
    CONSTRAINT "fk_localizations__image_reference_uuid"
        FOREIGN KEY ("image_reference_uuid") REFERENCES "image_references"("uuid"),

    CONSTRAINT "uk_localizations__observation_uuid_type"
        UNIQUE ("observation_uuid", "localization_type"),

    CONSTRAINT "ck_localizations__type" CHECK (
        "localization_type" IN ('bounding_box','point','line','polygon','mask')),

    CONSTRAINT "ck_localizations__confidence" CHECK (
        "confidence" IS NULL OR ("confidence" >= 0 AND "confidence" <= 100)),

    CONSTRAINT "ck_localizations__extent" CHECK (
        "width_pixels" >= 0 AND "height_pixels" >= 0),

    CONSTRAINT "ck_localizations__shape" CHECK (
        ("localization_type" = 'point'
            AND "width_pixels" = 0 AND "height_pixels" = 0
            AND "vertices" IS NULL AND "mask_data" IS NULL)
     OR ("localization_type" = 'bounding_box'
            AND "vertices" IS NULL AND "mask_data" IS NULL)
     OR ("localization_type" IN ('line','polygon')
            AND "vertices" IS NOT NULL AND "mask_data" IS NULL)
     OR ("localization_type" = 'mask'
            AND "vertices" IS NULL
            AND "mask_encoding" IS NOT NULL AND "mask_data" IS NOT NULL
            AND "mask_width_pixels" IS NOT NULL AND "mask_height_pixels" IS NOT NULL))
);

CREATE INDEX "idx_localizations__observation_uuid"     ON "localizations"("observation_uuid");
CREATE INDEX "idx_localizations__image_reference_uuid" ON "localizations"("image_reference_uuid");
CREATE INDEX "idx_localizations__localization_type"    ON "localizations"("localization_type");
CREATE INDEX "idx_localizations__generator"            ON "localizations"("generator");
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
| `timestamptz` | `datetimeoffset(6)` |
| `timestamp` | `datetime2(6)` |
| `now()` | `SYSUTCDATETIME()` |

Every `CHECK`, `UNIQUE`, and `FOREIGN KEY` above is portable to SQL Server as written.

### 5.3 Foreign key delete behavior

- `observation_uuid` — delete of an observation cascades to its localizations. JPA
  `orphanRemoval = true` on the `@OneToMany` handles this consistently with how
  `AssociationEntity` is already managed.
- `image_reference_uuid` — **`ON DELETE RESTRICT`** (no action). Nulling the FK on delete would be
  actively harmful: the pixels would silently be reinterpreted as video-frame coordinates (§6.2),
  changing their meaning without any error. Blocking the delete forces an explicit decision.
  This is a behavior change for anyone currently deleting `image_references` freely and should be
  called out in release notes.

### 5.4 Type matrix

| Type | `x/y_pixels` | `width/height_pixels` | `vertices` | `mask_*` |
|---|---|---|---|---|
| `bounding_box` | authoritative | authoritative | null | null |
| `point` | authoritative | must be `0` | null | null |
| `line` | derived | derived | required, ≥ 2 pairs | null |
| `polygon` | derived | derived | required, ≥ 3 pairs | null |
| `mask` | derived | derived | null | required |

"Derived" means the server computes the axis-aligned bounding box of the geometry on write. The
client does not supply it and cannot override it. This is what makes *"find every localization
overlapping this region"* a single query with no type branching and no null handling.

## 6. Contracts

### 6.1 Vertex encoding

`vertices` is a JSON array of finite numbers, **interleaved** as `[x0,y0,x1,y1,…]`, of even length.
Interleaving rather than the current parallel-array form is deliberate: a single array cannot
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

Coordinates are pixels. Which raster they index is determined by one rule:

| `image_reference_uuid` | Pixel space | Dimensions come from |
|---|---|---|
| present | that image | `image_references.width_pixels` / `.height_pixels` (already columns) |
| null | the **source video frame** | vampire-squid, via `imaged_moments.video_reference_uuid` |

Annosaurus never calls vampire-squid. It stores and publishes the rule; resolution is the client's
job in the null case. Consequently:

- **Bounds validation runs only when `image_reference_uuid` is present** and the referenced image has
  non-null dimensions. Then the server enforces `0 <= x`, `0 <= y`,
  `x + width <= width_pixels`, `y + height <= height_pixels`, and the same for every vertex and for
  the derived extent.
- When `image_reference_uuid` is null, no bounds validation is possible. This is a deliberate,
  documented limitation rather than a silent gap.

### 6.3 Provenance

| Column | Meaning |
|---|---|
| `generator` | The tool or model that produced it, e.g. `mbari452k_yolov8` |
| `confidence` | **0–100 percent.** `CHECK (confidence BETWEEN 0 AND 100)` |
| `observer` | Who drew it, distinct from the observation's own observer |
| `observation_timestamp` | When it was drawn |
| `verifier` | Who confirmed it, e.g. `kwalz` |
| `verified_timestamp` | When it was confirmed |
| `comment` | Free text about the localization itself |

`verified_timestamp` earns its place by making staleness detectable: if
`verified_timestamp < last_updated_timestamp`, the geometry has been edited since it was approved,
and the verification should no longer be trusted. That question is unanswerable today.

> **Note on `confidence`.** The 0–100 scale was chosen to match existing data with no migration
> arithmetic. The trade-off accepted: a client emitting a native 0–1 score writes `0.43`, which
> passes the CHECK and is stored as 0.43%. This failure is silent. It is the one place in this
> design where a wrong value is not loudly rejected. The repository's own convention elsewhere
> (`duration_millis`, `depth_meters`, `width_pixels`) would suggest `confidence_percent`; that
> naming was considered and not adopted. Revisit if 0–1 writers appear in practice.

## 7. Backward compatibility

### 7.1 Read projection

Every read path that returns associations continues to synthesize the legacy association for each
localization, in exactly the historical shape:

| Type | Synthesized `link_name` | Synthesized `link_value` |
|---|---|---|
| `bounding_box` | `bounding box` | `{"x":…,"y":…,"width":…,"height":…,"generator":…,"confidence":…,"verifier":…}` |
| `point` | `localization-point` | `{"x":[x0],"y":[y0]}` |
| `line` | `localization-line` | `{"x":[x0,x1],"y":[y0,y1]}` |
| `polygon` | `localization-polygon` | `{"x":[…],"y":[…]}` |
| `mask` | *(not projected — see below)* | |

With `to_concept = "self"` and `mime_type = "application/json"`.

Two rules make the projection genuinely transparent:

1. **The synthesized association reuses the localization's own `uuid`.** Clients that cache
   association UUIDs keep working, and a legacy `PUT`/`DELETE /v1/associations/{uuid}` routes
   unambiguously to the correct localization.
2. **Keys are omitted when null**, matching current behavior — the observed data shows `verifier`
   present in one box and absent in another.

Masks are not projected. They cannot round-trip through `varchar(1024)` (§2.7), so there is no
legacy shape to be compatible with. Masks are available only through the new endpoints.

**Suppression rule — required to avoid double-emission.** Between Phase 2 and Phase 4 the legacy rows
still physically exist in `associations` while the projection is also generating them. Without a rule
every localization would be returned **twice** in the same `associations` array. Therefore, from the
moment the projection is switched on (Phase 3), the association read query must **exclude rows whose
`link_name` is in the localization set**, and the projection supplies them instead:

```sql
-- association reads, Phase 3 onward
WHERE a.link_name NOT IN ('bounding box','localization-point',
                          'localization-line','localization-polygon','localization-mask')
```

The legacy rows remain on disk as the rollback path (§8.3); they simply stop being read. Exactly one
representation of each localization reaches the client at every phase.

**The `annotations` view must be updated.** It currently exposes `ass.link_name` / `ass.link_value`
directly from the `associations` table. During coexistence the legacy rows are still present so the
view is unaffected, but the Phase 4 delete (§8) would silently remove localizations from the view.
The view must be reworked to `UNION` the projection before those rows are deleted. This is a public
read surface and a real breakage risk if missed.

### 7.2 Legacy write path (decision D-8)

The association write path stays functional. A `POST`/`PUT` to `/v1/associations` carrying a
localization link name is **translated into a localization upsert** rather than inserting into
`associations`. This keeps one source of truth while keeping every current writer working.

Semantics: the translated write **replaces** any existing localization of that type. This matches the
de facto behavior today (last write wins) and is the compatible choice. The modern endpoint (§7.3) is
the one that offers conflict detection; the legacy path deliberately does not gain new failure modes.

**Deprecation signalling.** Two mechanisms, because neither is sufficient alone.

*Response headers* — machine-readable, ignorable, breaks nothing:

```http
Deprecation: true
Sunset: Wed, 30 Sep 2026 00:00:00 GMT
Link: <https://docs.mbari.org/annosaurus/localizations>; rel="deprecation"; type="text/html"
```

(`Sunset` is RFC 8594; `Deprecation` follows the IETF HTTP API deprecation draft.)

*Rate-limited `WARN` log* — for finding stragglers, using the existing `log.atWarn` idiom:

```
WARN LegacyLocalizationWrite path=POST /v1/associations link_name="localization-polygon"
     generator="mbari452k_yolov8" user_agent="vars-annotation/1.9.2" remote_addr=10.0.4.17
     observation_uuid=… count_since_last_report=1432
```

Emitted at most once per interval per `(link_name, user_agent, generator)` key, with a counter, so a
bulk ML ingest logs one line rather than fifty thousand.

> **Attribution is weaker than it looks.** `BasicJwtService` mints tokens carrying only `iss`, `iat`,
> and `exp`, validated against a **single shared `apiKey`**. There is no `sub` claim and no
> per-client credential, so **the bearer token cannot identify which tool made the call.** The only
> usable attribution signals are the `User-Agent` header and the `generator` value inside the payload
> itself — both client-supplied and both optional. If chasing down the last legacy writers matters,
> per-client secrets or a `sub` claim is a prerequisite, and that is a separate change to the auth
> layer.

A second, higher-value warning: log at `WARN` whenever a write **replaces an existing localization
whose `generator` or `verifier` differs from the incoming one**. That is the tug-of-war event itself,
and it is currently invisible on both the old and new paths.

### 7.3 New endpoints

```
GET    /v1/localizations/{uuid}
GET    /v1/localizations/observation/{observation_uuid}      -> 0..5 localizations
GET    /v1/localizations/imagereference/{image_reference_uuid}
GET    /v1/localizations/videoreference/{video_reference_uuid}   (paged; ML export)
PUT    /v1/localizations/{observation_uuid}/{localization_type}  (upsert)
DELETE /v1/localizations/{observation_uuid}/{localization_type}
POST   /v1/localizations/bulk                                    (ML pipelines)
GET    /v1/localizations/list/types                              (list the 5 localization_types)
GET    /v1/localizations/list/maskencodings                      (list accepted values for mask_encoding)
GET    /v1/localizations/list/constraints                        (return localization_types, mask_encodings)
```

`PUT` against the natural key is idempotent. It optionally accepts the caller's
`last_updated_timestamp`; if supplied and stale, the server returns **`409 Conflict`** with the
current row rather than overwriting. This is the mechanism that converts a silent stomp into a
recoverable error, and it is the single most important behavioral change in this proposal.

Reads follow the non-blocking classification; writes go on the blocking worker pool, per
`Endpoints.scala`.

`Annotation` gains `localizations: Seq[Localization] = Nil`, with the matching `AnnotationSC`
snake-case variant and Circe codecs. `associations` continues to include the projected rows, so a
client that upgrades sees the same localization twice, in two forms, until it stops reading
`associations` for that purpose. That redundancy is intentional during the transition and ends at
Phase 4.

## 8. Migration

Two dialect-specific Flyway scripts, `V1.1.0__localizations.sql`, applied on startup by
`FlywayMigrator`. PostgreSQL parses `link_value` with `jsonb` operators; SQL Server uses `OPENJSON` /
`JSON_VALUE`, which **requires compatibility level 130 (SQL Server 2016) or higher — verify this on
the target instance before scheduling.**

### 8.1 Survey first

These run before anything is written. Each answers a question that changes the migration.

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

-- (b) THE BLOCKER: observations with more than one localization of the same type.
--     The UNIQUE constraint makes these unmigratable as-is.
SELECT link_name, COUNT(*) AS n_conflicts FROM (
    SELECT observation_uuid, link_name
    FROM associations
    WHERE link_name IN ('bounding box','localization-point',
                        'localization-line','localization-polygon')
    GROUP BY observation_uuid, link_name
    HAVING COUNT(*) > 1
) d GROUP BY link_name;

-- (c) How many localizations sit on an imaged_moment with no image_references at all?
--     Informational: these necessarily migrate with a null FK (video-frame space).
SELECT COUNT(*)
FROM associations a
JOIN observations o ON a.observation_uuid = o.uuid
LEFT JOIN image_references ir ON ir.imaged_moment_uuid = o.imaged_moment_uuid
WHERE (a.link_name = 'bounding box' OR a.link_name LIKE 'localization-%')
  AND ir.uuid IS NULL;

-- (d) Values at risk: link_values at the 1024 ceiling, i.e. candidates for
--     prior silent truncation. (LENGTH -> LEN on SQL Server.)
SELECT COUNT(*) FROM associations
WHERE link_name LIKE 'localization-%' AND LENGTH(link_value) >= 1020;
```

These are written in PostgreSQL dialect; `LENGTH` becomes `LEN` on SQL Server. They are read-only and
safe to run against production.

### 8.2 Quarantine, never drop

```sql
CREATE TABLE "localization_migration_rejects" (
    "uuid"                   uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
    "association_uuid"       uuid NOT NULL,
    "observation_uuid"       uuid NOT NULL,
    "link_name"              varchar(128) NOT NULL,
    "link_value"             varchar(1024) NULL,
    "reason"                 varchar(256) NOT NULL,
    "migrated_timestamp"     timestamp NOT NULL DEFAULT now()
);
```

Rejection reasons: `duplicate_type` (lost the tie-break), `malformed_json`,
`array_length_mismatch`, `confidence_out_of_range`, `insufficient_vertices`, `unknown_link_name`.

Conflict resolution for duplicates: **keep the row with the newest `last_updated_timestamp`,
quarantine the rest.** Nothing is deleted. The rejects table is the migration's audit trail and
should be reviewed before Phase 4.

### 8.3 What the migration does *not* do

The migration copies; it does not delete. Legacy association rows survive Phase 3 untouched. This
keeps the whole change reversible: if the new path misbehaves, roll back the application and the old
rows are still authoritative. Deleting them is a separate, later migration taken only after the
rejects table has been reviewed and the `annotations` view (§7.1) has been reworked.

Bounds validation (§6.2) is **not** applied retroactively. Existing out-of-bounds coordinates migrate
as-is; enforcing them would fail the migration on real historical data. Bounds are enforced on new
writes only.

## 9. Audit and messaging

`LocalizationEntity` carries `@org.hibernate.envers.Audited` and
`@EntityListeners({TransactionLogger.class, TransactionNotifier.class})`, matching
`AssociationEntity`.

Auditing is not incidental here. Envers history on this table is what finally makes the tug-of-war
*visible*: who changed a box, when, from what, to what. That question cannot be answered today at all.

**Breaking change for ZeroMQ subscribers.** A bounding box edit currently fires an *association*
change notification. Once the localization table is authoritative, it fires a *localization*
notification instead. Any subscriber filtering on association events for localization link names
stops seeing them. This must be in the release notes, and subscribers need updating in step with
Phase 3.

## 10. Testing

| Area | Coverage |
|---|---|
| Validators | Vertex parsing, odd-length arrays, non-finite values, minimum vertex counts, consecutive-duplicate normalization, confidence range, per-type shape rules |
| Constraints | `it-postgres` and `it-sqlserver`: inserting a second localization of the same type fails; every `ck_localizations__shape` branch rejects what it should |
| Endpoints | `LocalizationEndpointsSuite` in both `it-postgres` and `it-sqlserver`, following `BaseEndpointsSuite` |
| Concurrency | Stale `last_updated_timestamp` on `PUT` yields `409`, not an overwrite |
| Projection round-trip | For each of the five real `link_value` examples in §2: write via the legacy path, read back as an association, assert **byte-identical** JSON. This is the regression test that proves no client breaks |
| Bounds | Enforced when `image_reference_uuid` is present; skipped when null |
| Migration | Seed legacy associations including duplicates, malformed JSON, mismatched array lengths, and the real degenerate polygon; run migration; assert correct rows migrated and every reject quarantined with the right reason |
| Derived extent | Line/polygon/mask extents computed correctly; point extent is exactly `0` |

Tests run sequentially under Testcontainers with UTC enforced, per existing project setup.

## 11. Known gaps

### 11.1 Cross-table imaged-moment integrity (decision D-9)

Nothing in this design prevents a localization from referencing an `image_references` row belonging
to a **different imaged moment** than its own observation. Such a row is semantically meaningless —
its pixels index an image from another moment in the video.

This could be enforced entirely in the database by denormalizing `imaged_moment_uuid` onto
`localizations` and adding two composite foreign keys, which would require new unique indexes on
`observations(uuid, imaged_moment_uuid)` and `image_references(uuid, imaged_moment_uuid)`.

**It is deliberately not done in this phase.** It would break existing tools, and in practice the
condition is rare. It also interacts badly with `Observation.updateImagedMomentUUID` and
`MoveImagedMoments`: moving an observation between imaged moments would have to re-resolve or null
every localization's image link, which is a larger behavioral change than this proposal should carry.

Mitigations available later, in rough order of cost:

1. A periodic consistency report (a read-only query), so the condition is at least *measured*.
2. A controller-level check on write only, leaving historical rows alone.
3. The composite-FK enforcement above, once tooling can tolerate it.

Recommended now: option 1, so the decision to escalate is driven by a real number.

### 11.2 Other accepted limitations

- No bounds validation when `image_reference_uuid` is null (§6.2).
- Silent misinterpretation of a 0–1 confidence value (§6.3).
- Legacy writers cannot be reliably attributed (§7.2).
- Masks have no legacy representation, so mask-aware clients must use the new endpoints (§7.1).

## 12. Rollout

| Phase | Action | Reversible |
|---|---|---|
| 0 | Run the §8.1 survey queries. Resolve duplicates found by query (b) | n/a |
| 1 | Ship the table, entity, controller, and endpoints. Nothing reads or writes them yet | yes |
| 2 | Run the data migration (copy only). Review `localization_migration_rejects` | yes |
| 3 | Switch reads to the projection; route legacy writes through translation; notify ZeroMQ subscribers | yes — legacy rows still present |
| 4 | Rework the `annotations` view; delete legacy localization associations; enforce `Sunset` | **no** |

Phase 4 is the only irreversible step, and it is gated on the rejects review, the view rework, and
the legacy-write logs going quiet.

## 13. Open questions

1. **Is there an existing mask link name?** Survey query (a) will confirm. Given the 1024-character
   ceiling, masks almost certainly do not exist in production today.
2. **What `Sunset` date?** Phase 4 cannot be scheduled until the legacy-write logs show which tools
   remain.
3. **SQL Server compatibility level** — is every target instance at 130+ for `OPENJSON`?
4. **Should `POST /v1/localizations/bulk` be transactional per-batch or per-row?** Per-row partial
   success suits ML ingest; per-batch suits correctness. Not yet decided.
