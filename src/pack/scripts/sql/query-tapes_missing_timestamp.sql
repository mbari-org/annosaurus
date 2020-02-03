WITH annos AS 
(SELECT DISTINCT
    index_recorded_timestamp,
    video_sequence_name,
    observation_uuid,
    video_uri
FROM
    annotations
WHERE 
    video_uri LIKE 'urn:tid%')
SELECT
    bb.video_sequence_name,
    ISNULL(missing_idx_count, 0) as missing_idx_count,
    ISNULL(total_anno_count, 0) AS total_anno_count,
    ISNULL(100.0 * ((missing_idx_count * 1.0) / (total_anno_count * 1.0)), 0) AS percent_bad
FROM
    (SELECT
        COUNT(*) AS missing_idx_count,
        video_sequence_name
    FROM
        annos
    WHERE
        index_recorded_timestamp IS NULL AND
        video_uri LIKE 'urn:tid%'
    GROUP BY
        video_sequence_name) AS aa RIGHT JOIN
    (SELECT
        COUNT(*) AS total_anno_count,
        video_sequence_name
    FROM 
        annos
    GROUP BY
        video_sequence_name) AS bb on aa.video_sequence_name = bb.video_sequence_name
ORDER BY
    percent_bad DESC



    
