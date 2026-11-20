-- V1.1.0__localizations.sql (PostgreSQL) -- localizations, see docs/proposals/2026-08-25-localizations.md
--
-- Deployment requirements (the code cannot enforce these):
--   1. Set DATABASE_VIDEO_REFERENCES_TABLE (the ${videoReferencesTable} placeholder below) BEFORE
--      the first start that applies this migration. Flyway applies it once; changing the
--      variable afterwards does not re-apply it. Unset or blank means the default.
--   2. Database version: SQL Server needs compatibility level >= 140 for its copy of this
--      migration; PostgreSQL has no equivalent requirement.
--   3. The migrating user needs permission to create tables, views and functions in the schema
--      (on SQL Server: CREATE SYNONYM, CREATE PROCEDURE and CREATE VIEW).
--
-- Creates the localizations table, the quarantine table, and migrate_localizations(), the
-- routine that converts legacy localization associations. Flyway applies the DDL on startup;
-- it does NOT run the routine. An operator runs it (see section 8.2 of the proposal).
--
-- Flyway placeholder:
--   ${videoReferencesTable}  vampire-squid's video_references table.
--       Same database as annosaurus (the default) : video_references
--       Separate database, same server            : <database>.<schema>.video_references
--   The reference is resolved when the function RUNS, not when it is created, so the
--   annosaurus-only test databases (no vampire-squid) still migrate cleanly.

CREATE TABLE "localizations" (
    "uuid"                    uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
    "observation_uuid"        uuid NOT NULL,
    "localization_type"       varchar(32) NOT NULL,

    "x"                       double precision NOT NULL,
    "y"                       double precision NOT NULL,
    "width"                   double precision NOT NULL,
    "height"                  double precision NOT NULL,

    "vertices"                text NULL,

    "mask_encoding"           varchar(32) NULL,
    "mask_data"               text NULL,
    "mask_width_pixels"       integer NULL,
    "mask_height_pixels"      integer NULL,

    "generator"               varchar(128) NULL,
    "confidence"              real NULL,
    "observer"                varchar(128) NULL,
    "observation_timestamp"   timestamptz NULL,
    "verifier"                varchar(128) NULL,
    "verification_timestamp"  timestamptz NULL,
    "comment"                 varchar(1024) NULL,

    "part"                    varchar(128) NULL,

    "training"                boolean NOT NULL DEFAULT false,
    "truncated"               boolean NOT NULL DEFAULT false,
    "difficult"               boolean NOT NULL DEFAULT false,

    "last_updated_timestamp"  timestamp NOT NULL DEFAULT now(),

    CONSTRAINT "fk_localizations__observation_uuid"
        FOREIGN KEY ("observation_uuid") REFERENCES "observations"("uuid"),

    CONSTRAINT "uk_localizations__observation_uuid_type"
        UNIQUE ("observation_uuid", "localization_type"),

    CONSTRAINT "ck_localizations__type" CHECK (
        "localization_type" IN ('bounding_box','point','line','polygon','mask')),

    CONSTRAINT "ck_localizations__confidence" CHECK (
        "confidence" IS NULL OR ("confidence" >= 0 AND "confidence" <= 1)),

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

CREATE TABLE "localization_migration_rejects" (
    "uuid"                   uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
    "association_uuid"       uuid NOT NULL,
    "observation_uuid"       uuid NOT NULL,
    "link_name"              varchar(128) NOT NULL,
    "link_value"             varchar(1024) NULL,
    "reason"                 varchar(256) NOT NULL,
    "migrated_timestamp"     timestamp NOT NULL DEFAULT now()
);

CREATE INDEX "idx_localization_migration_rejects__association_uuid"
    ON "localization_migration_rejects"("association_uuid");

-- ---------------------------------------------------------------------------------------
-- localized_annotations: one row per localization (proposal section 7.3, D-14).
--
-- Same observation / imaged-moment / ancillary / video-reference-info columns as the
-- "annotations" view, but NO association columns and NO image_references join (that join would
-- multiply rows, and a localization no longer belongs to an image). Where a name would collide:
--   ancillary x, y, z               -> ancillary_x, ancillary_y, ancillary_z   (x, y are the box)
--   the localization's observer     -> localization_observer                    (observer is the observation's)
--   the localization's timestamp    -> localization_observation_timestamp
-- ---------------------------------------------------------------------------------------

CREATE VIEW "localized_annotations"
AS
SELECT
    l.uuid                   AS localization_uuid,
    l.localization_type,
    l.x,
    l.y,
    l.width,
    l.height,
    l.vertices,
    l.mask_encoding,
    l.mask_data,
    l.mask_width_pixels,
    l.mask_height_pixels,
    l.generator,
    l.confidence,
    l.observer               AS localization_observer,
    l.observation_timestamp  AS localization_observation_timestamp,
    l.verifier,
    l.verification_timestamp,
    l.comment,
    l.part,
    l.training,
    l.truncated,
    l.difficult,
    l.last_updated_timestamp,
    im.uuid                  AS imaged_moment_uuid,
    im.video_reference_uuid,
    im.elapsed_time_millis   AS index_elapsed_time_millis,
    im.recorded_timestamp    AS index_recorded_timestamp,
    im.timecode              AS index_timecode,
    obs.uuid                 AS observation_uuid,
    obs.activity,
    obs.concept,
    obs.duration_millis,
    obs.observation_group,
    obs.observation_timestamp,
    obs.observer,
    ad.altitude,
    ad.coordinate_reference_system,
    ad.depth_meters,
    ad.latitude,
    ad.longitude,
    ad.oxygen_ml_per_l,
    ad.phi,
    ad.xyz_position_units,
    ad.pressure_dbar,
    ad.psi,
    ad.salinity,
    ad.temperature_celsius,
    ad.theta,
    ad.x                     AS ancillary_x,
    ad.y                     AS ancillary_y,
    ad.z                     AS ancillary_z,
    ad.light_transmission,
    info.mission_contact     AS chief_scientist,
    info.mission_id          AS dive_number,
    info.platform_name       AS camera_platform
FROM
    localizations l
        JOIN observations obs ON obs.uuid = l.observation_uuid
        JOIN imaged_moments im ON im.uuid = obs.imaged_moment_uuid
        LEFT JOIN ancillary_data ad ON ad.imaged_moment_uuid = im.uuid
        LEFT JOIN video_reference_information info ON info.video_reference_uuid = im.video_reference_uuid;

-- ---------------------------------------------------------------------------------------
-- Helpers: a malformed legacy value must become a reject row, never an aborted migration.
-- ---------------------------------------------------------------------------------------

CREATE FUNCTION localization_try_jsonb(s text) RETURNS jsonb
LANGUAGE plpgsql IMMUTABLE AS $$
BEGIN
    RETURN s::jsonb;
EXCEPTION WHEN others THEN
    RETURN NULL;
END
$$;

CREATE FUNCTION localization_try_float(s text) RETURNS double precision
LANGUAGE sql IMMUTABLE AS $$
    SELECT CASE WHEN s ~ '^-?[0-9]+(\.[0-9]+)?([eE][-+]?[0-9]+)?$'
                THEN s::double precision END
$$;

-- ---------------------------------------------------------------------------------------
-- migrate_localizations()
--
-- Converts legacy localization associations (pixels) to localizations (normalized [0,1]).
--   * Idempotent. Skips associations already migrated, never overwrites an existing
--     localization (the loser is quarantined as duplicate_type), and regenerates the rejects
--     table on every run, so it doubles as the delta pass at cutover.
--   * Dry run: BEGIN; SELECT * FROM migrate_localizations(); ROLLBACK;
--   * Returns one row per outcome.
-- ---------------------------------------------------------------------------------------

CREATE FUNCTION migrate_localizations()
RETURNS TABLE (outcome text, n bigint)
LANGUAGE plpgsql AS $fn$
DECLARE
    eps constant double precision := 1e-6;
    uuid_re constant text := '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';
    -- to_concept values that mean "no part" (a placeholder, not a real part) become NULL. Every
    -- legacy writer uses 'self'. Add any other placeholder that survey (k) of the proposal
    -- (section 8.1) finds. Matching is exact and case-sensitive.
    no_part constant text[] := ARRAY['self'];
BEGIN
    DROP TABLE IF EXISTS loc_work;
    DROP TABLE IF EXISTS loc_verts;
    DROP TABLE IF EXISTS loc_video_scale;
    DELETE FROM localization_migration_rejects;

    -- 1. Candidates, with the frame size each one's pixels must be divided by.
    CREATE TEMP TABLE loc_work ON COMMIT DROP AS
    SELECT a.uuid                      AS association_uuid,
           a.observation_uuid,
           a.link_name,
           a.link_value,
           a.to_concept,
           a.last_updated_timestamp,
           CASE a.link_name
               WHEN 'bounding box'          THEN 'bounding_box'
               WHEN 'localization-point'    THEN 'point'
               WHEN 'localization-line'     THEN 'line'
               WHEN 'localization-polygon'  THEN 'polygon'
           END                         AS localization_type,
           p.j,
           p.ref_text,
           CASE WHEN lower(p.ref_text) ~ uuid_re THEN p.ref_text::uuid END AS ref_uuid,
           ir.uuid                     AS image_uuid,
           ir.width_pixels             AS image_w,
           ir.height_pixels            AS image_h,
           vr.width                    AS video_w,
           vr.height                   AS video_h,
           im.video_reference_uuid,
           false                       AS orphaned,
           CAST(NULL AS boolean)       AS scale100,
           CAST(NULL AS integer)       AS w,
           CAST(NULL AS integer)       AS h,
           CAST(NULL AS double precision) AS px, CAST(NULL AS double precision) AS py,
           CAST(NULL AS double precision) AS pw, CAST(NULL AS double precision) AS ph,
           CAST(NULL AS double precision) AS nx, CAST(NULL AS double precision) AS ny,
           CAST(NULL AS double precision) AS nw, CAST(NULL AS double precision) AS nh,
           CAST(NULL AS text)             AS vertices,
           false                          AS truncated,
           CAST(NULL AS double precision) AS confidence,
           false                          AS training,
           CAST(NULL AS varchar(64))      AS reason
    FROM associations a
    JOIN observations o ON o.uuid = a.observation_uuid
    CROSS JOIN LATERAL (
        SELECT CASE WHEN jsonb_typeof(localization_try_jsonb(a.link_value)) = 'object'
                    THEN localization_try_jsonb(a.link_value) END AS j,
               (localization_try_jsonb(a.link_value) ->> 'image_reference_uuid') AS ref_text
    ) p
    LEFT JOIN image_references ir ON ir.uuid = CASE WHEN lower(p.ref_text) ~ uuid_re THEN p.ref_text::uuid END
    LEFT JOIN imaged_moments im   ON im.uuid = o.imaged_moment_uuid
    LEFT JOIN ${videoReferencesTable} vr ON vr.uuid = im.video_reference_uuid
    WHERE (a.link_name IN ('bounding box','localization-point','localization-line','localization-polygon')
           OR a.link_name LIKE 'localization-%')
      AND NOT EXISTS (SELECT 1 FROM localizations l WHERE l.uuid = a.uuid);

    -- 2. Pre-checks, in order of precedence.
    UPDATE loc_work SET reason = 'unknown_link_name' WHERE localization_type IS NULL;
    UPDATE loc_work SET reason = 'malformed_json'
     WHERE reason IS NULL AND (j IS NULL OR (ref_text IS NOT NULL AND ref_uuid IS NULL));

    -- Frame size. The image the pixels were drawn on wins. An image row that is gone (an
    -- orphaned reference) and an image that exists but has no size both fall back to the video's,
    -- on the same-size assumption of section 6.2 (proposal section 13, question 3). Orphans are
    -- flagged so the report can count them apart.
    UPDATE loc_work SET orphaned = true
     WHERE reason IS NULL AND ref_uuid IS NOT NULL AND image_uuid IS NULL;
    UPDATE loc_work SET
        w = CASE WHEN image_w > 0 AND image_h > 0 THEN image_w
                 WHEN video_w > 0 AND video_h > 0 THEN video_w END,
        h = CASE WHEN image_w > 0 AND image_h > 0 THEN image_h
                 WHEN video_w > 0 AND video_h > 0 THEN video_h END
     WHERE reason IS NULL;
    UPDATE loc_work SET reason = 'unresolved_dimensions' WHERE reason IS NULL AND (w IS NULL OR h IS NULL);

    -- 3. Provenance (confidence -> 0-1; tags -> training).
    --
    -- Legacy confidence is on 0-100 or on 0-1, and a value <= 1 cannot say which, so the scale is
    -- decided per VIDEO by a hard rule (proposal section 8.2, D-15): if ANY legacy confidence in the
    -- video exceeds 1, the whole video is 0-100 and is divided by 100; otherwise it is 0-1 and used
    -- as is. It is computed over EVERY legacy localization in the video, not only the ones still to
    -- migrate, so a delta pass classifies a video the same way the first pass did.
    CREATE TEMP TABLE loc_video_scale ON COMMIT DROP AS
    SELECT im.video_reference_uuid, bool_or(c.conf > 1) AS scale100
    FROM associations a
    JOIN observations o    ON o.uuid  = a.observation_uuid
    JOIN imaged_moments im ON im.uuid = o.imaged_moment_uuid
    CROSS JOIN LATERAL (
        SELECT localization_try_float(localization_try_jsonb(a.link_value) ->> 'confidence') AS conf
    ) c
    WHERE a.link_name IN ('bounding box','localization-point','localization-line','localization-polygon')
      AND c.conf IS NOT NULL
    GROUP BY im.video_reference_uuid;

    UPDATE loc_work SET confidence = localization_try_float(j ->> 'confidence')
     WHERE reason IS NULL AND j ->> 'confidence' IS NOT NULL;
    UPDATE loc_work SET reason = 'malformed_json'
     WHERE reason IS NULL AND j ->> 'confidence' IS NOT NULL AND confidence IS NULL;
    UPDATE loc_work s SET scale100   = vs.scale100,
                          confidence = s.confidence / CASE WHEN vs.scale100 THEN 100.0 ELSE 1.0 END
      FROM loc_video_scale vs
     WHERE s.reason IS NULL AND s.confidence IS NOT NULL
       AND vs.video_reference_uuid IS NOT DISTINCT FROM s.video_reference_uuid;
    UPDATE loc_work SET reason = 'confidence_out_of_range'
     WHERE reason IS NULL AND (confidence < 0 OR confidence > 1);
    UPDATE loc_work SET training = true
     WHERE reason IS NULL AND jsonb_typeof(j -> 'tags') = 'array' AND (j -> 'tags') ? 'training';

    -- 4a. Bounding boxes: scalar keys, edges normalized then clamped to the frame.
    UPDATE loc_work SET px = localization_try_float(j ->> 'x'),     py = localization_try_float(j ->> 'y'),
                        pw = localization_try_float(j ->> 'width'), ph = localization_try_float(j ->> 'height')
     WHERE reason IS NULL AND localization_type = 'bounding_box';
    UPDATE loc_work SET reason = 'malformed_json'
     WHERE reason IS NULL AND localization_type = 'bounding_box'
       AND (px IS NULL OR py IS NULL OR pw IS NULL OR ph IS NULL);
    UPDATE loc_work SET reason = 'invalid_geometry'
     WHERE reason IS NULL AND localization_type = 'bounding_box' AND (pw <= 0 OR ph <= 0);
    UPDATE loc_work SET
        truncated = (px / w < -eps OR py / h < -eps OR (px + pw) / w > 1 + eps OR (py + ph) / h > 1 + eps),
        nx = least(greatest(px / w, 0), 1),
        ny = least(greatest(py / h, 0), 1),
        nw = least(greatest((px + pw) / w, 0), 1) - least(greatest(px / w, 0), 1),
        nh = least(greatest((py + ph) / h, 0), 1) - least(greatest(py / h, 0), 1)
     WHERE reason IS NULL AND localization_type = 'bounding_box';
    UPDATE loc_work SET reason = 'entirely_out_of_bounds'
     WHERE reason IS NULL AND localization_type = 'bounding_box' AND (nw < 1e-9 OR nh < 1e-9);

    -- 4b. Points, lines, polygons: parallel arrays x[], y[].
    UPDATE loc_work SET reason = 'malformed_json'
     WHERE reason IS NULL AND localization_type IN ('point','line','polygon')
       AND NOT (jsonb_typeof(j -> 'x') = 'array' AND jsonb_typeof(j -> 'y') = 'array');
    UPDATE loc_work SET reason = 'array_length_mismatch'
     WHERE reason IS NULL AND localization_type IN ('point','line','polygon')
       AND jsonb_array_length(j -> 'x') <> jsonb_array_length(j -> 'y');

    CREATE TEMP TABLE loc_verts ON COMMIT DROP AS
    SELECT s.association_uuid, xs.ord AS idx,
           localization_try_float(xs.v) AS px, localization_try_float(ys.v) AS py
    FROM loc_work s
    CROSS JOIN LATERAL jsonb_array_elements_text(s.j -> 'x') WITH ORDINALITY AS xs(v, ord)
    JOIN LATERAL jsonb_array_elements_text(s.j -> 'y') WITH ORDINALITY AS ys(v, ord) ON ys.ord = xs.ord
    WHERE s.reason IS NULL AND s.localization_type IN ('point','line','polygon');

    UPDATE loc_work s SET reason = 'malformed_json'
     WHERE s.reason IS NULL AND EXISTS (
         SELECT 1 FROM loc_verts v
          WHERE v.association_uuid = s.association_uuid AND (v.px IS NULL OR v.py IS NULL));

    -- Collapse consecutive duplicate vertices (section 6.1), then renumber.
    DELETE FROM loc_verts v USING (
        SELECT association_uuid, idx,
               px IS NOT DISTINCT FROM lag(px) OVER (PARTITION BY association_uuid ORDER BY idx)
           AND py IS NOT DISTINCT FROM lag(py) OVER (PARTITION BY association_uuid ORDER BY idx) AS dup
        FROM loc_verts
    ) d
    WHERE d.association_uuid = v.association_uuid AND d.idx = v.idx AND d.dup;

    -- A polygon sent explicitly closed (last vertex = first) is stored open: drop the closing
    -- vertex once (section 6.1), before the minimum-vertex check, so [A,B,A] has 2 vertices.
    DELETE FROM loc_verts v USING (
        SELECT association_uuid, min(idx) AS first_idx, max(idx) AS last_idx
        FROM loc_verts GROUP BY association_uuid HAVING count(*) > 1
    ) e, loc_verts f, loc_work s
    WHERE e.association_uuid = v.association_uuid AND v.idx = e.last_idx
      AND f.association_uuid = v.association_uuid AND f.idx = e.first_idx
      AND s.association_uuid = v.association_uuid AND s.reason IS NULL AND s.localization_type = 'polygon'
      AND f.px IS NOT DISTINCT FROM v.px AND f.py IS NOT DISTINCT FROM v.py;

    UPDATE loc_work s SET reason = 'insufficient_vertices'
     WHERE s.reason IS NULL AND s.localization_type IN ('point','line','polygon')
       AND (SELECT count(*) FROM loc_verts v WHERE v.association_uuid = s.association_uuid)
           < CASE s.localization_type WHEN 'point' THEN 1 WHEN 'line' THEN 2 ELSE 3 END;
    UPDATE loc_work s SET reason = 'malformed_json'          -- a "point" must be exactly one pair
     WHERE s.reason IS NULL AND s.localization_type = 'point'
       AND jsonb_array_length(s.j -> 'x') <> 1;

    -- Normalize + clamp every vertex; extent and the interleaved vertex string follow.
    WITH nv AS (
        SELECT v.association_uuid, v.idx,
               least(greatest(v.px / s.w, 0), 1) AS vx,
               least(greatest(v.py / s.h, 0), 1) AS vy,
               (v.px / s.w < -eps OR v.px / s.w > 1 + eps OR v.py / s.h < -eps OR v.py / s.h > 1 + eps) AS clipped
        FROM loc_verts v JOIN loc_work s USING (association_uuid)
        WHERE s.reason IS NULL
    ), agg AS (
        SELECT association_uuid,
               min(vx) AS minx, max(vx) AS maxx, min(vy) AS miny, max(vy) AS maxy,
               bool_or(clipped) AS clipped,
               '[' || string_agg(round(vx::numeric, 9)::text || ',' || round(vy::numeric, 9)::text, ','
                                 ORDER BY idx) || ']' AS vertices
        FROM nv GROUP BY association_uuid
    )
    UPDATE loc_work s SET
        nx = agg.minx, ny = agg.miny,
        nw = CASE s.localization_type WHEN 'point' THEN 0 ELSE agg.maxx - agg.minx END,
        nh = CASE s.localization_type WHEN 'point' THEN 0 ELSE agg.maxy - agg.miny END,
        truncated = agg.clipped,
        vertices = CASE s.localization_type WHEN 'point' THEN NULL ELSE agg.vertices END
    FROM agg
    WHERE agg.association_uuid = s.association_uuid AND s.reason IS NULL
      AND s.localization_type IN ('point','line','polygon');

    -- 5. One localization per (observation, type). The newest valid row wins; every other valid
    --    row is quarantined, including one that loses to a localization that already exists
    --    (migrated on an earlier run, or written through the new endpoints since).
    UPDATE loc_work s SET reason = 'duplicate_type'
    FROM (SELECT association_uuid,
                 row_number() OVER (PARTITION BY observation_uuid, localization_type
                                    ORDER BY last_updated_timestamp DESC NULLS LAST, association_uuid) AS rn
          FROM loc_work WHERE reason IS NULL) r
    WHERE r.association_uuid = s.association_uuid AND r.rn > 1;
    UPDATE loc_work s SET reason = 'duplicate_type'
     WHERE s.reason IS NULL AND EXISTS (SELECT 1 FROM localizations l
                                         WHERE l.observation_uuid = s.observation_uuid
                                           AND l.localization_type = s.localization_type);

    -- 6. Quarantine, then copy. Never overwrite a localization that is already there.
    INSERT INTO localization_migration_rejects (association_uuid, observation_uuid, link_name, link_value, reason)
    SELECT association_uuid, observation_uuid, link_name, link_value, reason
    FROM loc_work WHERE reason IS NOT NULL;

    INSERT INTO localizations
        (uuid, observation_uuid, localization_type, x, y, width, height, vertices,
         generator, confidence, verifier, part, training, truncated, last_updated_timestamp)
    SELECT s.association_uuid, s.observation_uuid, s.localization_type, s.nx, s.ny, s.nw, s.nh, s.vertices,
           left(s.j ->> 'generator', 128), s.confidence, left(s.j ->> 'verifier', 128),
           CASE WHEN s.to_concept = ANY (no_part) THEN NULL ELSE left(s.to_concept, 128) END,
           s.training, s.truncated, coalesce(s.last_updated_timestamp, now())
    FROM loc_work s
    WHERE s.reason IS NULL;

    -- 7. Report.
    RETURN QUERY
    SELECT 'migrated'::text, count(*) FROM loc_work s
     WHERE s.reason IS NULL AND EXISTS (SELECT 1 FROM localizations l WHERE l.uuid = s.association_uuid)
    UNION ALL
    SELECT '  of which truncated (clamped to frame)', count(*) FROM loc_work s
     WHERE s.reason IS NULL AND s.truncated AND EXISTS (SELECT 1 FROM localizations l WHERE l.uuid = s.association_uuid)
    UNION ALL
    SELECT 'rejected: ' || s.reason, count(*) FROM loc_work s WHERE s.reason IS NOT NULL GROUP BY s.reason
    UNION ALL
    SELECT 'info: orphaned image reference, used the video''s dimensions (media assumption)', count(*) FROM loc_work s
     WHERE s.reason IS NULL AND s.orphaned
    UNION ALL
    SELECT 'info: videos on the 0-100 confidence scale (divided by 100)', count(*) FROM loc_video_scale vs
     WHERE vs.scale100
    UNION ALL
    SELECT 'info: rows divided by 100 (video is 0-100)', count(*) FROM loc_work s
     WHERE s.reason IS NULL AND s.scale100
    UNION ALL
    SELECT 'info: rows used as is (video is 0-1)', count(*) FROM loc_work s
     WHERE s.reason IS NULL AND s.scale100 IS NOT NULL AND NOT s.scale100
    UNION ALL
    SELECT 'info: image/video aspect ratio differs by >1% (media assumption)', count(*) FROM loc_work s
     WHERE s.reason IS NULL AND s.image_w > 0 AND s.image_h > 0 AND s.video_w > 0 AND s.video_h > 0
       AND abs(s.image_w::double precision / s.image_h - s.video_w::double precision / s.video_h)
           / (s.video_w::double precision / s.video_h) > 0.01;
END
$fn$;

-- ---------------------------------------------------------------------------------------
-- Envers audit table for localizations (the @Version column is not audited).
-- ---------------------------------------------------------------------------------------

create table localizations_AUD (REV integer not null, REVTYPE smallint, uuid uuid not null, observation_uuid uuid, localization_type varchar(32), x double precision, y double precision, width double precision, height double precision, vertices text, mask_encoding varchar(32), mask_data text, mask_width_pixels integer, mask_height_pixels integer, generator varchar(128), confidence real, observer varchar(128), observation_timestamp timestamp(6) with time zone, verifier varchar(128), verification_timestamp timestamp(6) with time zone, comment varchar(1024), part varchar(128), training boolean, truncated boolean, difficult boolean, primary key (REV, uuid));
alter table if exists localizations_AUD add constraint fk_localizations_aud__revinfo foreign key (REV) references REVINFO;
