-- V1.1.0__localizations.sql (SQL Server) -- DRAFT, see docs/proposals/2026-08-25-localizations.md
--
-- Creates the localizations table, the quarantine table, and dbo.migrate_localizations, the
-- procedure that converts legacy localization associations. Flyway applies the DDL on startup;
-- it does NOT run the procedure. An operator runs it (see section 8.2 of the proposal).
--
-- Requires SQL Server 2017+ (STRING_AGG) and database compatibility level 140+; the JSON
-- functions (ISJSON, JSON_VALUE, OPENJSON) need 130+. MBARI production is SQL Server 2025, but
-- check the DATABASE's level, not the server's: a restored database keeps its old level.
--   SELECT compatibility_level FROM sys.databases WHERE name = DB_NAME()
-- Tested on SQL Server 2022.
--
-- Flyway placeholder:
--   ${videoReferencesTable}  vampire-squid's video_references table.
--       Same database as annosaurus (external users) : dbo.video_references
--       MBARI production (same server)               : M3_VIDEO_ASSETS.dbo.video_references
--   It is bound through a synonym, which SQL Server resolves lazily, so the annosaurus-only
--   test databases (no vampire-squid) still migrate cleanly.

CREATE TABLE "dbo"."localizations" (
    "uuid"                    uniqueidentifier NOT NULL DEFAULT NEWID(),
    "observation_uuid"        uniqueidentifier NOT NULL,
    "localization_type"       varchar(32) NOT NULL,

    "x"                       float(53) NOT NULL,
    "y"                       float(53) NOT NULL,
    "width"                   float(53) NOT NULL,
    "height"                  float(53) NOT NULL,

    "vertices"                varchar(max) NULL,

    "mask_encoding"           varchar(32) NULL,
    "mask_data"               varchar(max) NULL,
    "mask_width_pixels"       int NULL,
    "mask_height_pixels"      int NULL,

    "generator"               varchar(128) NULL,
    "confidence"              float(24) NULL,
    "observer"                varchar(128) NULL,
    "observation_timestamp"   datetimeoffset(6) NULL,
    "verifier"                varchar(128) NULL,
    "verification_timestamp"  datetimeoffset(6) NULL,
    "comment"                 varchar(1024) NULL,

    "training"                bit NOT NULL DEFAULT 0,
    "truncated"               bit NOT NULL DEFAULT 0,

    "last_updated_timestamp"  datetime2(6) NOT NULL DEFAULT SYSUTCDATETIME(),

    CONSTRAINT "pk_localizations" PRIMARY KEY ("uuid"),

    CONSTRAINT "fk_localizations__observation_uuid"
        FOREIGN KEY ("observation_uuid") REFERENCES "dbo"."observations"("uuid"),

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
)
GO

CREATE INDEX "idx_localizations__observation_uuid"  ON "dbo"."localizations"("observation_uuid")
GO
CREATE INDEX "idx_localizations__localization_type" ON "dbo"."localizations"("localization_type")
GO
CREATE INDEX "idx_localizations__generator"         ON "dbo"."localizations"("generator")
GO

CREATE TABLE "dbo"."localization_migration_rejects" (
    "uuid"                   uniqueidentifier NOT NULL DEFAULT NEWID(),
    "association_uuid"       uniqueidentifier NOT NULL,
    "observation_uuid"       uniqueidentifier NOT NULL,
    "link_name"              varchar(128) NOT NULL,
    "link_value"             varchar(1024) NULL,
    "reason"                 varchar(256) NOT NULL,
    "migrated_timestamp"     datetime2(6) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT "pk_localization_migration_rejects" PRIMARY KEY ("uuid")
)
GO

CREATE INDEX "idx_localization_migration_rejects__association_uuid"
    ON "dbo"."localization_migration_rejects"("association_uuid")
GO

-- ---------------------------------------------------------------------------------------
-- vampire-squid's video_references, wherever it lives.
-- ---------------------------------------------------------------------------------------

IF OBJECT_ID(N'dbo.vampire_squid_video_references', N'SN') IS NOT NULL
    DROP SYNONYM dbo.vampire_squid_video_references
GO

CREATE SYNONYM dbo.vampire_squid_video_references FOR ${videoReferencesTable}
GO

-- ---------------------------------------------------------------------------------------
-- dbo.migrate_localizations
--
-- Converts legacy localization associations (pixels) to localizations (normalized [0,1]).
--   * Idempotent. Skips associations already migrated, never overwrites an existing
--     localization (the loser is quarantined as duplicate_type), and regenerates the rejects
--     table on every run, so it doubles as the delta pass at cutover.
--   * Dry run: BEGIN TRAN; EXEC dbo.migrate_localizations; ROLLBACK;
--   * Returns one row per outcome.
-- ---------------------------------------------------------------------------------------

CREATE OR ALTER PROCEDURE dbo.migrate_localizations
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @eps float = 1e-6;

    DROP TABLE IF EXISTS #loc_work;
    DROP TABLE IF EXISTS #loc_arr;
    DROP TABLE IF EXISTS #loc_verts;
    DELETE FROM dbo.localization_migration_rejects;

    -- 1. Candidates. JSON functions are only applied to rows already known to be JSON objects.
    SELECT a.uuid                         AS association_uuid,
           a.observation_uuid,
           a.link_name,
           a.link_value,
           a.last_updated_timestamp,
           CASE a.link_name
               WHEN 'bounding box'          THEN 'bounding_box'
               WHEN 'localization-point'    THEN 'point'
               WHEN 'localization-line'     THEN 'line'
               WHEN 'localization-polygon'  THEN 'polygon'
           END                            AS localization_type,
           o.imaged_moment_uuid,
           CAST(CASE WHEN ISJSON(a.link_value) = 1 AND LEFT(LTRIM(a.link_value), 1) = '{'
                     THEN 1 ELSE 0 END AS bit) AS is_obj,
           CAST(NULL AS nvarchar(100))    AS ref_text,
           CAST(NULL AS uniqueidentifier) AS ref_uuid,
           CAST(NULL AS uniqueidentifier) AS image_uuid,
           CAST(NULL AS int)              AS image_w,
           CAST(NULL AS int)              AS image_h,
           CAST(NULL AS int)              AS video_w,
           CAST(NULL AS int)              AS video_h,
           CAST(NULL AS int)              AS w,
           CAST(NULL AS int)              AS h,
           CAST(NULL AS float) AS px, CAST(NULL AS float) AS py,
           CAST(NULL AS float) AS pw, CAST(NULL AS float) AS ph,
           CAST(NULL AS float) AS nx, CAST(NULL AS float) AS ny,
           CAST(NULL AS float) AS nw, CAST(NULL AS float) AS nh,
           CAST(NULL AS varchar(max))     AS vertices,
           CAST(0 AS bit)                 AS truncated,
           CAST(NULL AS float)            AS confidence,
           CAST(0 AS bit)                 AS training,
           CAST(NULL AS varchar(64))      AS reason
    INTO #loc_work
    FROM dbo.associations a
    JOIN dbo.observations o ON o.uuid = a.observation_uuid
    WHERE (a.link_name IN ('bounding box','localization-point','localization-line','localization-polygon')
           OR a.link_name LIKE 'localization-%')
      AND NOT EXISTS (SELECT 1 FROM dbo.localizations l WHERE l.uuid = a.uuid);

    UPDATE #loc_work
       SET ref_text = CAST(JSON_VALUE(link_value, '$.image_reference_uuid') AS nvarchar(100))
     WHERE is_obj = 1;
    UPDATE #loc_work SET ref_uuid = TRY_CAST(ref_text AS uniqueidentifier) WHERE ref_text IS NOT NULL;

    UPDATE s SET s.image_uuid = ir.uuid, s.image_w = ir.width_pixels, s.image_h = ir.height_pixels
      FROM #loc_work s JOIN dbo.image_references ir ON ir.uuid = s.ref_uuid;
    UPDATE s SET s.video_w = vr.width, s.video_h = vr.height
      FROM #loc_work s
      JOIN dbo.imaged_moments im ON im.uuid = s.imaged_moment_uuid
      JOIN dbo.vampire_squid_video_references vr ON vr.uuid = im.video_reference_uuid;

    -- 2. Pre-checks, in order of precedence.
    UPDATE #loc_work SET reason = 'unknown_link_name' WHERE localization_type IS NULL;
    UPDATE #loc_work SET reason = 'malformed_json'
     WHERE reason IS NULL AND (is_obj = 0 OR (ref_text IS NOT NULL AND ref_uuid IS NULL));

    -- Frame size. The image the pixels were drawn on wins; a missing image row is never
    -- silently turned into a video frame. An image that exists but has no size falls back to
    -- the video's, on the same-size assumption of section 6.2.
    UPDATE #loc_work SET reason = 'orphaned_image_reference'
     WHERE reason IS NULL AND ref_uuid IS NOT NULL AND image_uuid IS NULL;
    UPDATE #loc_work SET
        w = CASE WHEN image_w > 0 AND image_h > 0 THEN image_w
                 WHEN video_w > 0 AND video_h > 0 THEN video_w END,
        h = CASE WHEN image_w > 0 AND image_h > 0 THEN image_h
                 WHEN video_w > 0 AND video_h > 0 THEN video_h END
     WHERE reason IS NULL;
    UPDATE #loc_work SET reason = 'unresolved_dimensions' WHERE reason IS NULL AND (w IS NULL OR h IS NULL);

    -- 3. Provenance (confidence 0-100 -> 0-1; tags -> training).
    UPDATE #loc_work SET confidence = TRY_CAST(JSON_VALUE(link_value, '$.confidence') AS float)
     WHERE reason IS NULL AND JSON_VALUE(link_value, '$.confidence') IS NOT NULL;
    UPDATE #loc_work SET reason = 'malformed_json'
     WHERE reason IS NULL AND JSON_VALUE(link_value, '$.confidence') IS NOT NULL AND confidence IS NULL;
    UPDATE #loc_work SET reason = 'confidence_out_of_range'
     WHERE reason IS NULL AND (confidence < 0 OR confidence > 100);
    UPDATE #loc_work SET confidence = confidence / 100.0 WHERE reason IS NULL AND confidence IS NOT NULL;
    UPDATE #loc_work SET training = 1
     WHERE reason IS NULL
       AND LEFT(LTRIM(JSON_QUERY(link_value, '$.tags')), 1) = '['
       AND EXISTS (SELECT 1 FROM OPENJSON(link_value, '$.tags') t WHERE t.type = 1 AND t.value = 'training');

    -- 4a. Bounding boxes: scalar keys, edges normalized then clamped to the frame.
    UPDATE #loc_work SET px = TRY_CAST(JSON_VALUE(link_value, '$.x') AS float),
                         py = TRY_CAST(JSON_VALUE(link_value, '$.y') AS float),
                         pw = TRY_CAST(JSON_VALUE(link_value, '$.width') AS float),
                         ph = TRY_CAST(JSON_VALUE(link_value, '$.height') AS float)
     WHERE reason IS NULL AND localization_type = 'bounding_box';
    UPDATE #loc_work SET reason = 'malformed_json'
     WHERE reason IS NULL AND localization_type = 'bounding_box'
       AND (px IS NULL OR py IS NULL OR pw IS NULL OR ph IS NULL);
    UPDATE #loc_work SET reason = 'invalid_geometry'
     WHERE reason IS NULL AND localization_type = 'bounding_box' AND (pw <= 0 OR ph <= 0);
    UPDATE #loc_work SET
        truncated = CASE WHEN px / w < -@eps OR py / h < -@eps
                           OR (px + pw) / w > 1 + @eps OR (py + ph) / h > 1 + @eps THEN 1 ELSE 0 END,
        nx = CASE WHEN px / w < 0 THEN 0 WHEN px / w > 1 THEN 1 ELSE px / w END,
        ny = CASE WHEN py / h < 0 THEN 0 WHEN py / h > 1 THEN 1 ELSE py / h END,
        nw = (CASE WHEN (px + pw) / w < 0 THEN 0 WHEN (px + pw) / w > 1 THEN 1 ELSE (px + pw) / w END)
           - (CASE WHEN px / w < 0 THEN 0 WHEN px / w > 1 THEN 1 ELSE px / w END),
        nh = (CASE WHEN (py + ph) / h < 0 THEN 0 WHEN (py + ph) / h > 1 THEN 1 ELSE (py + ph) / h END)
           - (CASE WHEN py / h < 0 THEN 0 WHEN py / h > 1 THEN 1 ELSE py / h END)
     WHERE reason IS NULL AND localization_type = 'bounding_box';
    UPDATE #loc_work SET reason = 'entirely_out_of_bounds'
     WHERE reason IS NULL AND localization_type = 'bounding_box' AND (nw < 1e-9 OR nh < 1e-9);

    -- 4b. Points, lines, polygons: parallel arrays x[], y[]. Materialize the rows that really
    --     have two arrays, so OPENJSON only ever sees valid input.
    UPDATE #loc_work SET reason = 'malformed_json'
     WHERE reason IS NULL AND localization_type IN ('point','line','polygon')
       AND NOT (LEFT(LTRIM(JSON_QUERY(link_value, '$.x')), 1) = '['
            AND LEFT(LTRIM(JSON_QUERY(link_value, '$.y')), 1) = '[');

    SELECT association_uuid, link_value
      INTO #loc_arr
      FROM #loc_work
     WHERE reason IS NULL AND localization_type IN ('point','line','polygon');

    UPDATE s SET s.reason = 'array_length_mismatch'
      FROM #loc_work s JOIN #loc_arr r ON r.association_uuid = s.association_uuid
     WHERE (SELECT COUNT(*) FROM OPENJSON(r.link_value, '$.x'))
        <> (SELECT COUNT(*) FROM OPENJSON(r.link_value, '$.y'));

    SELECT r.association_uuid,
           CAST(xs.[key] AS int) AS idx,
           CASE WHEN xs.type = 2 THEN TRY_CAST(xs.value AS float) END AS px,
           CASE WHEN ys.type = 2 THEN TRY_CAST(ys.value AS float) END AS py
      INTO #loc_verts
      FROM #loc_arr r
      JOIN #loc_work s ON s.association_uuid = r.association_uuid AND s.reason IS NULL
     CROSS APPLY OPENJSON(r.link_value, '$.x') xs
     CROSS APPLY OPENJSON(r.link_value, '$.y') ys
     WHERE ys.[key] = xs.[key];

    UPDATE s SET s.reason = 'malformed_json'
      FROM #loc_work s
     WHERE s.reason IS NULL AND EXISTS (
         SELECT 1 FROM #loc_verts v
          WHERE v.association_uuid = s.association_uuid AND (v.px IS NULL OR v.py IS NULL));

    -- Collapse consecutive duplicate vertices (section 6.1).
    DELETE v
      FROM #loc_verts v
      JOIN (SELECT association_uuid, idx,
                   CASE WHEN px = LAG(px) OVER (PARTITION BY association_uuid ORDER BY idx)
                         AND py = LAG(py) OVER (PARTITION BY association_uuid ORDER BY idx)
                        THEN 1 ELSE 0 END AS dup
              FROM #loc_verts) d ON d.association_uuid = v.association_uuid AND d.idx = v.idx
     WHERE d.dup = 1;

    UPDATE s SET s.reason = 'insufficient_vertices'
      FROM #loc_work s
     WHERE s.reason IS NULL AND s.localization_type IN ('point','line','polygon')
       AND (SELECT COUNT(*) FROM #loc_verts v WHERE v.association_uuid = s.association_uuid)
           < CASE s.localization_type WHEN 'point' THEN 1 WHEN 'line' THEN 2 ELSE 3 END;
    UPDATE s SET s.reason = 'malformed_json'          -- a "point" must be exactly one pair
      FROM #loc_work s JOIN #loc_arr r ON r.association_uuid = s.association_uuid
     WHERE s.reason IS NULL AND s.localization_type = 'point'
       AND (SELECT COUNT(*) FROM OPENJSON(r.link_value, '$.x')) <> 1;

    -- Normalize + clamp every vertex; extent and the interleaved vertex string follow.
    ;WITH nv AS (
        SELECT v.association_uuid, v.idx,
               CASE WHEN v.px / s.w < 0 THEN 0 WHEN v.px / s.w > 1 THEN 1 ELSE v.px / s.w END AS vx,
               CASE WHEN v.py / s.h < 0 THEN 0 WHEN v.py / s.h > 1 THEN 1 ELSE v.py / s.h END AS vy,
               CASE WHEN v.px / s.w < -@eps OR v.px / s.w > 1 + @eps
                      OR v.py / s.h < -@eps OR v.py / s.h > 1 + @eps THEN 1 ELSE 0 END AS clipped
          FROM #loc_verts v JOIN #loc_work s ON s.association_uuid = v.association_uuid
         WHERE s.reason IS NULL
    ), agg AS (
        SELECT association_uuid,
               MIN(vx) AS minx, MAX(vx) AS maxx, MIN(vy) AS miny, MAX(vy) AS maxy,
               MAX(clipped) AS clipped,
               '[' + STRING_AGG(
                        CAST(CAST(CONVERT(decimal(12,9), ROUND(vx, 9)) AS varchar(32)) + ','
                           + CAST(CONVERT(decimal(12,9), ROUND(vy, 9)) AS varchar(32)) AS varchar(max)),
                        ',') WITHIN GROUP (ORDER BY idx) + ']' AS vertices
          FROM nv GROUP BY association_uuid
    )
    UPDATE s SET
        s.nx = agg.minx, s.ny = agg.miny,
        s.nw = CASE s.localization_type WHEN 'point' THEN 0 ELSE agg.maxx - agg.minx END,
        s.nh = CASE s.localization_type WHEN 'point' THEN 0 ELSE agg.maxy - agg.miny END,
        s.truncated = agg.clipped,
        s.vertices = CASE s.localization_type WHEN 'point' THEN NULL ELSE agg.vertices END
      FROM #loc_work s JOIN agg ON agg.association_uuid = s.association_uuid
     WHERE s.reason IS NULL AND s.localization_type IN ('point','line','polygon');

    -- 5. One localization per (observation, type). The newest valid row wins; every other valid
    --    row is quarantined, including one that loses to a localization that already exists
    --    (migrated on an earlier run, or written through the new endpoints since).
    ;WITH r AS (
        SELECT reason,
               ROW_NUMBER() OVER (PARTITION BY observation_uuid, localization_type
                                  ORDER BY ISNULL(last_updated_timestamp, '0001-01-01') DESC, association_uuid) AS rn
          FROM #loc_work WHERE reason IS NULL
    )
    UPDATE r SET reason = 'duplicate_type' WHERE rn > 1;

    UPDATE s SET s.reason = 'duplicate_type'
      FROM #loc_work s
     WHERE s.reason IS NULL AND EXISTS (SELECT 1 FROM dbo.localizations l
                                         WHERE l.observation_uuid = s.observation_uuid
                                           AND l.localization_type = s.localization_type);

    -- 6. Quarantine, then copy.
    INSERT INTO dbo.localization_migration_rejects (association_uuid, observation_uuid, link_name, link_value, reason)
    SELECT association_uuid, observation_uuid, link_name, link_value, reason
      FROM #loc_work WHERE reason IS NOT NULL;

    INSERT INTO dbo.localizations
        (uuid, observation_uuid, localization_type, x, y, width, height, vertices,
         generator, confidence, verifier, training, truncated, last_updated_timestamp)
    SELECT s.association_uuid, s.observation_uuid, s.localization_type, s.nx, s.ny, s.nw, s.nh, s.vertices,
           LEFT(JSON_VALUE(s.link_value, '$.generator'), 128), s.confidence,
           LEFT(JSON_VALUE(s.link_value, '$.verifier'), 128),
           s.training, s.truncated, ISNULL(s.last_updated_timestamp, SYSUTCDATETIME())
      FROM #loc_work s
     WHERE s.reason IS NULL;

    -- 7. Report.
    SELECT CAST('migrated' AS varchar(100)) AS outcome, COUNT_BIG(*) AS n
      FROM #loc_work WHERE reason IS NULL
    UNION ALL
    SELECT '  of which truncated (clamped to frame)', COUNT_BIG(*)
      FROM #loc_work WHERE reason IS NULL AND truncated = 1
    UNION ALL
    SELECT 'rejected: ' + reason, COUNT_BIG(*)
      FROM #loc_work WHERE reason IS NOT NULL GROUP BY reason
    UNION ALL
    SELECT 'info: image/video aspect ratio differs by >1% (media assumption)', COUNT_BIG(*)
      FROM #loc_work
     WHERE reason IS NULL AND image_w > 0 AND image_h > 0 AND video_w > 0 AND video_h > 0
       AND ABS(CAST(image_w AS float) / image_h - CAST(video_w AS float) / video_h)
           / (CAST(video_w AS float) / video_h) > 0.01;
END
GO
