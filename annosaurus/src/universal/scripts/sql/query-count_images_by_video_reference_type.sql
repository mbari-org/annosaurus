SELECT
    CASE
        WHEN vr.uuid IS NULL THEN 'missing_video_reference'
        WHEN vr.uri IS NULL THEN 'null_uri'
        WHEN vr.uri LIKE 'urn:%tid%' THEN 'Tape'
        WHEN vr.uri LIKE 'urn:%image%' THEN 'Image Collection'
        WHEN vr.uri LIKE 'urn:%rtva%' THEN 'Real Time'
        WHEN vr.uri LIKE 'http%' THEN 'Video'
        ELSE 'other'
        END AS uri_prefix,
    COUNT(DISTINCT ir.uuid) AS image_reference_count
FROM
    image_references ir
        LEFT JOIN imaged_moments im
                  ON ir.imaged_moment_uuid = im.uuid
        LEFT JOIN M3_VIDEO_ASSETS.dbo.video_references vr
                  ON im.video_reference_uuid = vr.uuid
WHERE
    ir.url IS NOT NULL
GROUP BY
    CASE
        WHEN vr.uuid IS NULL THEN 'missing_video_reference'
        WHEN vr.uri IS NULL THEN 'null_uri'
        WHEN vr.uri LIKE 'urn:%tid%' THEN 'Tape'
        WHEN vr.uri LIKE 'urn:%image%' THEN 'Image Collection'
        WHEN vr.uri LIKE 'urn:%rtva%' THEN 'Real Time'
        WHEN vr.uri LIKE 'http%' THEN 'Video'
        ELSE 'other'
        END
ORDER BY
    image_reference_count DESC;